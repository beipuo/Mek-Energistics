package com.beipuo.mekenergistics.crafting;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeSupportOwner;
import com.beipuo.mekenergistics.mixin.ae2.CraftingTaskProgressAccessor;
import com.beipuo.mekenergistics.mixin.ae2.ElapsedTimeTrackerAccessor;
import com.beipuo.mekenergistics.mixin.ae2.ExecutingCraftingJobAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Batches already-planned AE2 tasks without changing pattern planning granularity. */
public final class MeCraftingCpuBatching {

    private MeCraftingCpuBatching() {
    }

    public static boolean pushPattern(ICraftingProvider provider, IPatternDetails details,
            KeyCounter[] baseInputs, ListCraftingInventory inventory, ExecutingCraftingJob job,
            IEnergyService energyService, Level level, PatternPusher pusher) {
        if (!(provider instanceof MeAeSupportOwner owner)
                || !owner.isSmartPatternMultiplicationEnabled()) {
            return pusher.push(provider, details, baseInputs);
        }

        ExecutingCraftingJobAccessor jobAccess = (ExecutingCraftingJobAccessor) job;
        // Native AE2 CPUs can submit the whole extractable task balance to the persistent smart queue.
        // Immediate physical capacity is only an admission limit for counted third-party provider APIs.
        Batch batch = prepareBatch(details, baseInputs, inventory, jobAccess, energyService, level);
        if (batch == null) {
            return pusher.push(provider, details, baseInputs);
        }

        boolean accepted = false;
        try {
            accepted = pusher.push(provider, details, baseInputs);
            if (accepted) {
                try {
                    commitBatch(batch, jobAccess.mekenergistics$getWaitingFor(),
                            (ElapsedTimeTrackerAccessor) jobAccess.mekenergistics$getTimeTracker(), energyService);
                } catch (RuntimeException failure) {
                    // The provider already owns the inputs, so the push itself succeeded and must
                    // be reported as such. commitBatch restored the CPU bookkeeping; letting the
                    // exception escape would crash AE2's crafting CPU tick and the server with it.
                    MekEnergistics.LOGGER.error("Failed to record a batched ME crafting push for {}; "
                            + "extra outputs will be returned to the network as unexpected items",
                            details.getDefinition(), failure);
                }
            }
            return accepted;
        } finally {
            if (!accepted) {
                rollbackBatch(batch, baseInputs, inventory);
            }
        }
    }

    @Nullable
    private static Batch prepareBatch(IPatternDetails details, KeyCounter[] baseInputs,
            ListCraftingInventory inventory, ExecutingCraftingJobAccessor jobAccess,
            IEnergyService energyService, Level level) {
        if (details == null || baseInputs == null || baseInputs.length == 0 || jobAccess == null) {
            return null;
        }

        Object progress = jobAccess.mekenergistics$getTasks().get(details);
        if (!(progress instanceof CraftingTaskProgressAccessor taskProgress)) {
            return null;
        }
        long remainingTasks = taskProgress.mekenergistics$getValue();
        long maxExtraCopies = maxAdditionalCopies(details, remainingTasks);
        if (maxExtraCopies <= 0) {
            return null;
        }

        Batch extractedCandidate = null;
        try {
            Batch candidate = extractCandidate(details, maxExtraCopies, inventory, energyService, level, taskProgress);
            if (candidate != null) {
                extractedCandidate = candidate;
                mergeInputs(baseInputs, candidate.extraInputs());
                extractedCandidate = null;
                return candidate;
            }

            long low = 0;
            long high = maxExtraCopies;
            while (low + 1 < high) {
                long middle = low + (high - low) / 2;
                candidate = extractCandidate(details, middle, inventory, energyService, level, taskProgress);
                if (candidate == null) {
                    high = middle;
                } else {
                    extractedCandidate = candidate;
                    CraftingCpuHelper.reinjectPatternInputs(inventory, candidate.extraInputs());
                    extractedCandidate = null;
                    low = middle;
                }
            }
            if (low <= 0) {
                return null;
            }
            candidate = extractCandidate(details, low, inventory, energyService, level, taskProgress);
            if (candidate != null) {
                extractedCandidate = candidate;
                mergeInputs(baseInputs, candidate.extraInputs());
                extractedCandidate = null;
            }
            return candidate;
        } catch (RuntimeException ex) {
            if (extractedCandidate != null) {
                CraftingCpuHelper.reinjectPatternInputs(inventory, extractedCandidate.extraInputs());
            }
            MekEnergistics.LOGGER.warn("Unable to batch AE crafting task {}; using normal dispatch", details.getDefinition(), ex);
            return null;
        }
    }

    @Nullable
    private static Batch extractCandidate(IPatternDetails details, long copies,
            ListCraftingInventory inventory, IEnergyService energyService, Level level,
            CraftingTaskProgressAccessor taskProgress) {
        if (copies <= 0) {
            return null;
        }
        KeyCounter outputs = new KeyCounter();
        KeyCounter containerItems = new KeyCounter();
        KeyCounter[] inputs = CraftingCpuHelper.extractPatternInputs(
                new ScaledPatternDetails(details, copies), inventory, level, outputs, containerItems);
        if (inputs == null) {
            return null;
        }

        double power = CraftingCpuHelper.calculatePatternPower(inputs);
        if (energyService.extractAEPower(power, Actionable.SIMULATE, PowerMultiplier.CONFIG) < power - 0.01) {
            CraftingCpuHelper.reinjectPatternInputs(inventory, inputs);
            return null;
        }
        return new Batch(copies, inputs, outputs, containerItems, power, taskProgress);
    }

    static void commitBatch(Batch batch, ListCraftingInventory waitingFor,
            ElapsedTimeTrackerAccessor timeTracker, IEnergyService energyService) {
        long taskProgressBefore = batch.taskProgress().mekenergistics$getValue();
        KeyCounter waitingForBefore = new KeyCounter();
        waitingForBefore.addAll(waitingFor.list);
        double powerBefore = energyService.getStoredPower();
        try {
            for (var output : batch.outputs()) {
                waitingFor.insert(output.getKey(), output.getLongValue(), Actionable.MODULATE);
            }
            for (var containerItem : batch.containerItems()) {
                waitingFor.insert(containerItem.getKey(), containerItem.getLongValue(), Actionable.MODULATE);
                timeTracker.mekenergistics$addMaxItems(
                        containerItem.getLongValue(), containerItem.getKey().getType());
            }
            batch.taskProgress().mekenergistics$setValue(taskProgressBefore - batch.extraCopies());
            energyService.extractAEPower(batch.power(), Actionable.MODULATE, PowerMultiplier.CONFIG);
        } catch (RuntimeException ex) {
            batch.taskProgress().mekenergistics$setValue(taskProgressBefore);
            restoreCounter(waitingFor.list, waitingForBefore);
            double extracted = powerBefore - energyService.getStoredPower();
            if (extracted > 0) {
                try {
                    energyService.injectPower(extracted, Actionable.MODULATE);
                } catch (RuntimeException restoreEx) {
                    MekEnergistics.LOGGER.warn("Unable to restore AE power after failed batch commit", restoreEx);
                }
            }
            throw ex;
        }
    }

    private static void restoreCounter(KeyCounter target, KeyCounter snapshot) {
        target.clear();
        target.addAll(snapshot);
    }

    static void rollbackBatch(Batch batch, KeyCounter[] baseInputs,
            ListCraftingInventory inventory) {
        unmergeInputs(baseInputs, batch.extraInputs());
        CraftingCpuHelper.reinjectPatternInputs(inventory, batch.extraInputs());
    }

    static long maxAdditionalCopies(IPatternDetails details, long remainingTasks) {
        if (details == null || remainingTasks <= 1) {
            return 0;
        }
        long totalLimit = remainingTasks;
        for (IPatternDetails.IInput input : details.getInputs()) {
            long multiplier = input.getMultiplier();
            if (multiplier <= 0) {
                return 0;
            }
            totalLimit = Math.min(totalLimit, Long.MAX_VALUE / multiplier);
            for (GenericStack possible : input.getPossibleInputs()) {
                if (possible == null || possible.amount() <= 0) {
                    return 0;
                }
                totalLimit = Math.min(totalLimit, Long.MAX_VALUE / possible.amount() / multiplier);
            }
        }
        for (GenericStack output : details.getOutputs()) {
            if (output == null || output.amount() <= 0) {
                return 0;
            }
            totalLimit = Math.min(totalLimit, Long.MAX_VALUE / output.amount());
        }
        return Math.max(0, totalLimit - 1);
    }

    private static void mergeInputs(KeyCounter[] target, KeyCounter[] extra) {
        if (target.length != extra.length) {
            throw new IllegalArgumentException("Batch input lane count changed");
        }
        for (int i = 0; i < target.length; i++) {
            target[i].addAll(extra[i]);
        }
    }

    private static void unmergeInputs(KeyCounter[] target, KeyCounter[] extra) {
        for (int i = 0; i < target.length; i++) {
            for (var entry : extra[i]) {
                target[i].remove(entry.getKey(), entry.getLongValue());
            }
        }
    }

    record Batch(long extraCopies, KeyCounter[] extraInputs,
            KeyCounter outputs, KeyCounter containerItems, double power,
            CraftingTaskProgressAccessor taskProgress) {
    }

    @FunctionalInterface
    public interface PatternPusher {
        boolean push(ICraftingProvider provider, IPatternDetails details, KeyCounter[] inputs);
    }

    static final class ScaledPatternDetails implements IPatternDetails {
        private final IPatternDetails delegate;
        private final IInput[] inputs;
        private final List<GenericStack> outputs;

        ScaledPatternDetails(IPatternDetails delegate, long copies) {
            if (delegate == null || copies <= 0) {
                throw new IllegalArgumentException("Pattern and positive copy count are required");
            }
            this.delegate = delegate;
            IPatternDetails.IInput[] sourceInputs = delegate.getInputs();
            this.inputs = new IInput[sourceInputs.length];
            for (int i = 0; i < sourceInputs.length; i++) {
                this.inputs[i] = new ScaledInput(sourceInputs[i], copies);
            }
            this.outputs = new ArrayList<>(delegate.getOutputs().size());
            for (GenericStack output : delegate.getOutputs()) {
                this.outputs.add(new GenericStack(
                        output.what(), Math.multiplyExact(output.amount(), copies)));
            }
        }

        @Override
        public appeng.api.stacks.AEItemKey getDefinition() {
            return this.delegate.getDefinition();
        }

        @Override
        public IInput[] getInputs() {
            return this.inputs;
        }

        @Override
        public List<GenericStack> getOutputs() {
            return this.outputs;
        }

        @Override
        public boolean supportsPushInputsToExternalInventory() {
            return this.delegate.supportsPushInputsToExternalInventory();
        }

        @Override
        public PatternDetailsTooltip getTooltip(Level level, TooltipFlag flags) {
            return this.delegate.getTooltip(level, flags);
        }
    }

    private record ScaledInput(IPatternDetails.IInput delegate, long copies)
            implements IPatternDetails.IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return this.delegate.getPossibleInputs();
        }

        @Override
        public long getMultiplier() {
            return Math.multiplyExact(this.delegate.getMultiplier(), this.copies);
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return this.delegate.isValid(input, level);
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return this.delegate.getRemainingKey(template);
        }
    }
}
