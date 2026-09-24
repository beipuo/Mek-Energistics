package com.beipuo.mekenergistics.compat.neoecoae;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import com.beipuo.mekenergistics.blockentity.api.MeAeSupportOwner;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeCountedInputAdmission;
import java.util.List;
import java.util.UUID;
import java.util.function.DoublePredicate;
import org.jetbrains.annotations.Nullable;

/** Optional NeoECO CPU counted-dispatch bridge with no linkage to NeoECO classes. */
public final class NeoEcoBatchCompat {
    private static final double POWER_EPSILON = 0.01;

    private NeoEcoBatchCompat() {
    }

    public static int tryPushBatch(List<ICraftingProvider> providers,
            IPatternDetails patternDetails, KeyCounter[] oneCraftInputs,
            ListCraftingInventory inventory, IEnergyService energyService,
            double patternPower, long maxCrafts) {
        if (providers == null || inventory == null || energyService == null) {
            return 0;
        }
        DoublePredicate hasPower = power -> energyService.extractAEPower(
                power, Actionable.SIMULATE, PowerMultiplier.CONFIG) >= power - POWER_EPSILON;
        for (ICraftingProvider provider : providers) {
            if (provider instanceof MeAeSupportOwner owner) {
                int accepted = tryPushBatch(target(owner.getPatternAeSupport()), patternDetails,
                        oneCraftInputs, inventory, patternPower, maxCrafts, hasPower);
                if (accepted > 0) {
                    return accepted;
                }
            }
        }
        return 0;
    }

    public static boolean pushPatternBatch(MeAeSupportOwner owner, IPatternDetails patternDetails,
            KeyCounter[] inputTotal, long craftCount) {
        if (owner == null || patternDetails == null || !validPrototype(inputTotal) || craftCount <= 0
                || owner.getPatternAeSupport().isPatternBusy()
                || !owner.getPatternAeSupport().hasRegisteredPattern(patternDetails)) {
            return false;
        }
        KeyCounter[] oneCopy = oneCopyInputs(patternDetails);
        KeyCounter[] expected = scale(oneCopy, craftCount);
        if (oneCopy == null || expected == null || !sameInputs(expected, inputTotal)
                || owner.maxAcceptedPatternCopies(oneCopy) < craftCount) {
            return false;
        }
        return owner.getPatternAeSupport().routeDataPatternInputs(inputTotal);
    }

    @Nullable
    private static KeyCounter[] oneCopyInputs(IPatternDetails patternDetails) {
        IPatternDetails.IInput[] inputs = patternDetails.getInputs();
        if (inputs == null || inputs.length == 0) {
            return null;
        }
        KeyCounter[] result = new KeyCounter[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            IPatternDetails.IInput input = inputs[i];
            if (input == null || input.getPossibleInputs() == null || input.getPossibleInputs().length != 1
                    || input.getPossibleInputs()[0] == null || input.getMultiplier() <= 0) {
                return null;
            }
            var stack = input.getPossibleInputs()[0];
            try {
                KeyCounter counter = new KeyCounter();
                counter.add(stack.what(), Math.multiplyExact(stack.amount(), input.getMultiplier()));
                result[i] = counter;
            } catch (ArithmeticException exception) {
                return null;
            }
        }
        return result;
    }

    static boolean sameInputs(KeyCounter[] expected, KeyCounter[] actual) {
        if (expected == null || actual == null || expected.length != actual.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i].size() != actual[i].size()) {
                return false;
            }
            for (var entry : expected[i]) {
                if (actual[i].get(entry.getKey()) != entry.getLongValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    static int tryPushBatch(@Nullable BatchTarget target, IPatternDetails patternDetails,
            KeyCounter[] oneCraftInputs, ListCraftingInventory inventory,
            double patternPower, long maxCrafts, DoublePredicate hasPower) {
        if (target == null || patternDetails == null || inventory == null || hasPower == null
                || !validPrototype(oneCraftInputs) || maxCrafts < 1 || patternPower < 0
                || target.isBusy() || !target.hasRegisteredPattern(patternDetails)) {
            return 0;
        }
        long capacity = Math.max(0, target.maxAcceptedCopies(oneCraftInputs));
        int requested = (int) Math.min(Integer.MAX_VALUE, Math.min(maxCrafts, capacity));
        requested = Math.min(requested, maxCraftsFromInventory(
                inventory.list, oneCraftInputs, requested));
        requested = maxAffordableCrafts(patternPower, requested, hasPower);
        if (requested < 1) {
            return 0;
        }

        KeyCounter extraInputs = requested == 1
                ? new KeyCounter()
                : totals(oneCraftInputs, requested - 1L);
        MeCountedInputAdmission admission = MeCountedInputAdmission.prepare(oneCraftInputs, requested,
                target::maxAcceptedCopies, scaled -> target.routeInputs(scaled));
        if (extraInputs == null || admission == null
                || (!extraInputs.isEmpty() && !extractExact(inventory, extraInputs))) {
            return 0;
        }
        if (admission.commit(oneCraftInputs)) {
            return (int) admission.count();
        }
        if (!extraInputs.isEmpty()) {
            restore(inventory, extraInputs);
        }
        return 0;
    }

    static int maxCraftsFromInventory(KeyCounter inventory, KeyCounter[] oneCraftInputs, int limit) {
        if (inventory == null || limit < 2 || !validPrototype(oneCraftInputs)) {
            return Math.min(1, Math.max(0, limit));
        }
        KeyCounter perCraft = totals(oneCraftInputs, 1);
        if (perCraft == null || perCraft.isEmpty()) {
            return 1;
        }
        long extraCrafts = limit - 1L;
        for (var entry : perCraft) {
            extraCrafts = Math.min(extraCrafts,
                    inventory.get(entry.getKey()) / entry.getLongValue());
        }
        return (int) Math.min(limit, extraCrafts + 1L);
    }

    static int maxAffordableCrafts(double powerPerCraft, int limit, DoublePredicate hasPower) {
        if (limit < 2 || powerPerCraft < 0 || hasPower == null) {
            return Math.min(1, Math.max(0, limit));
        }
        if (powerPerCraft == 0) {
            return limit;
        }
        int low = 0;
        int high = limit;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            double required = powerPerCraft * middle;
            if (Double.isFinite(required) && hasPower.test(required)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    @Nullable
    static KeyCounter[] scale(KeyCounter[] prototype, long count) {
        return MeCountedInputAdmission.scale(prototype, count);
    }

    @Nullable
    private static KeyCounter totals(KeyCounter[] prototype, long count) {
        KeyCounter[] scaled = scale(prototype, count);
        if (scaled == null) {
            return null;
        }
        KeyCounter totals = new KeyCounter();
        for (KeyCounter lane : scaled) {
            for (var entry : lane) {
                long existing = totals.get(entry.getKey());
                if (existing > Long.MAX_VALUE - entry.getLongValue()) {
                    return null;
                }
                totals.add(entry.getKey(), entry.getLongValue());
            }
        }
        return totals;
    }

    private static boolean extractExact(ListCraftingInventory inventory, KeyCounter requested) {
        KeyCounter extracted = new KeyCounter();
        for (var entry : requested) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            long received = inventory.extract(key, amount, Actionable.MODULATE);
            if (received > 0) {
                extracted.add(key, received);
            }
            if (received != amount) {
                restore(inventory, extracted);
                return false;
            }
        }
        return true;
    }

    private static void restore(ListCraftingInventory inventory, KeyCounter inputs) {
        for (var entry : inputs) {
            inventory.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
        }
    }

    private static boolean validPrototype(@Nullable KeyCounter[] prototype) {
        if (prototype == null || prototype.length == 0) {
            return false;
        }
        for (KeyCounter counter : prototype) {
            if (counter == null || counter.isEmpty()) {
                return false;
            }
            for (var entry : counter) {
                if (entry.getKey() == null || entry.getLongValue() <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BatchTarget target(AbstractMeAeSupport<?> support) {
        return new BatchTarget() {
            @Override
            public boolean isBusy() {
                return support.isPatternBusy();
            }

            @Override
            public boolean hasRegisteredPattern(IPatternDetails patternDetails) {
                return support.hasRegisteredPattern(patternDetails);
            }

            @Override
            public long maxAcceptedCopies(KeyCounter[] inputs) {
                return support.maxAcceptedCopies(inputs);
            }

            @Override
            public boolean routeInputs(KeyCounter[] inputs) {
                return support.routeDataPatternInputs(inputs);
            }
        };
    }

    interface BatchTarget {
        boolean isBusy();

        boolean hasRegisteredPattern(IPatternDetails patternDetails);

        long maxAcceptedCopies(KeyCounter[] oneCraftInputs);

        boolean routeInputs(KeyCounter[] scaledInputs);
    }
}
