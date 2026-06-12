package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

public final class MeSmartPatternMultiplication {
    private static final String TAG_ENABLED = "SmartPatternMultiplication";
    private static final String TAG_PENDING = "SmartPatternMultiplicationPending";
    private static final String TAG_REMAINING = "Remaining";
    private static final String TAG_DEFINITION = "Definition";
    private static final String TAG_INPUTS = "Inputs";
    private static final String TAG_INPUT = "Input";

    private final List<PendingPattern> pendingPatterns = new ArrayList<>();
    private boolean enabled = true;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasPendingWork() {
        return !this.pendingPatterns.isEmpty();
    }

    public boolean enqueue(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!this.enabled) {
            return false;
        }
        PendingPattern pendingPattern = PendingPattern.create(patternDetails, inputHolder);
        if (pendingPattern == null) {
            return false;
        }
        for (PendingPattern existing : this.pendingPatterns) {
            if (existing.canMerge(pendingPattern)) {
                existing.remaining += pendingPattern.remaining;
                return true;
            }
        }
        this.pendingPatterns.add(pendingPattern);
        return true;
    }

    public boolean processNext(Feeder feeder) {
        boolean changed = false;
        Iterator<PendingPattern> iterator = this.pendingPatterns.iterator();
        while (iterator.hasNext()) {
            PendingPattern pendingPattern = iterator.next();
            if (pendingPattern.remaining <= 0) {
                iterator.remove();
                changed = true;
                continue;
            }
            boolean fed = feedBestBatch(pendingPattern, feeder);
            changed |= fed;
            if (pendingPattern.remaining <= 0) {
                iterator.remove();
                continue;
            }
            return changed;
        }
        return changed;
    }

    public boolean processNext(List<IPatternDetails> patterns, PatternFeeder feeder) {
        boolean changed = false;
        Iterator<PendingPattern> iterator = this.pendingPatterns.iterator();
        while (iterator.hasNext()) {
            PendingPattern pendingPattern = iterator.next();
            if (pendingPattern.remaining <= 0) {
                iterator.remove();
                changed = true;
                continue;
            }
            IPatternDetails patternDetails = findPattern(patterns, pendingPattern.definition);
            if (patternDetails == null) {
                return changed;
            }
            boolean fed = feedBestBatch(pendingPattern, patternDetails, feeder);
            changed |= fed;
            if (pendingPattern.remaining <= 0) {
                iterator.remove();
                continue;
            }
            return changed;
        }
        return changed;
    }

    private static boolean feedBestBatch(PendingPattern pendingPattern, Feeder feeder) {
        boolean changed = false;
        while (pendingPattern.remaining > 0) {
            long attempt = Math.min(pendingPattern.remaining, pendingPattern.maxBatchCopies());
            boolean fed = false;
            while (attempt > 0) {
                if (feeder.feed(pendingPattern.toKeyCounters(attempt))) {
                    pendingPattern.remaining -= attempt;
                    changed = true;
                    fed = true;
                    break;
                }
                attempt /= 2;
            }
            if (!fed) {
                return changed;
            }
        }
        return changed;
    }

    private static boolean feedBestBatch(PendingPattern pendingPattern, IPatternDetails patternDetails, PatternFeeder feeder) {
        boolean changed = false;
        while (pendingPattern.remaining > 0) {
            long attempt = Math.min(pendingPattern.remaining, pendingPattern.maxBatchCopies());
            boolean fed = false;
            while (attempt > 0) {
                if (feeder.feed(patternDetails, pendingPattern.toKeyCounters(attempt))) {
                    pendingPattern.remaining -= attempt;
                    changed = true;
                    fed = true;
                    break;
                }
                attempt /= 2;
            }
            if (!fed) {
                return changed;
            }
        }
        return changed;
    }

    public void saveConfig(CompoundTag tag) {
        tag.putBoolean(TAG_ENABLED, this.enabled);
    }

    public void loadConfig(CompoundTag tag) {
        this.enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
    }

    public void savePending(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.pendingPatterns.isEmpty()) {
            tag.remove(TAG_PENDING);
            return;
        }
        ListTag pending = new ListTag();
        for (PendingPattern pendingPattern : this.pendingPatterns) {
            CompoundTag pendingTag = new CompoundTag();
            pendingTag.putLong(TAG_REMAINING, pendingPattern.remaining);
            pendingTag.put(TAG_DEFINITION, GenericStack.writeTag(registries, new GenericStack(pendingPattern.definition, 1)));
            ListTag inputs = new ListTag();
            for (GenericStack input : pendingPattern.inputs) {
                CompoundTag inputTag = new CompoundTag();
                inputTag.put(TAG_INPUT, GenericStack.writeTag(registries, input));
                inputs.add(inputTag);
            }
            pendingTag.put(TAG_INPUTS, inputs);
            pending.add(pendingTag);
        }
        tag.put(TAG_PENDING, pending);
    }

    public void loadPending(CompoundTag tag, HolderLookup.Provider registries) {
        this.pendingPatterns.clear();
        ListTag pending = tag.getList(TAG_PENDING, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag pendingTag = pending.getCompound(i);
            long remaining = pendingTag.getLong(TAG_REMAINING);
            if (remaining <= 0) {
                continue;
            }
            GenericStack definition = GenericStack.readTag(registries, pendingTag.getCompound(TAG_DEFINITION));
            if (definition == null || !(definition.what() instanceof AEItemKey definitionKey)) {
                continue;
            }
            ListTag inputs = pendingTag.getList(TAG_INPUTS, CompoundTag.TAG_COMPOUND);
            List<GenericStack> stacks = new ArrayList<>(inputs.size());
            for (int j = 0; j < inputs.size(); j++) {
                GenericStack stack = GenericStack.readTag(registries, inputs.getCompound(j).getCompound(TAG_INPUT));
                if (stack != null && stack.amount() > 0) {
                    stacks.add(stack);
                }
            }
            if (!stacks.isEmpty()) {
                this.pendingPatterns.add(new PendingPattern(definitionKey, stacks, remaining));
            }
        }
    }

    public interface Feeder {
        boolean feed(KeyCounter[] oneCraftInputs);
    }

    public interface PatternFeeder {
        boolean feed(IPatternDetails patternDetails, KeyCounter[] oneCraftInputs);
    }

    @Nullable
    private static IPatternDetails findPattern(List<IPatternDetails> patterns, AEItemKey definition) {
        for (IPatternDetails pattern : patterns) {
            if (pattern.getDefinition().equals(definition)) {
                return pattern;
            }
        }
        return null;
    }

    private static final class PendingPattern {
        private final AEItemKey definition;
        private final List<GenericStack> inputs;
        private long remaining;

        private PendingPattern(AEItemKey definition, List<GenericStack> inputs, long remaining) {
            this.definition = definition;
            this.inputs = inputs;
            this.remaining = remaining;
        }

        @Nullable
        private static PendingPattern create(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            if (patternDetails == null || inputHolder == null || inputHolder.length == 0) {
                return null;
            }
            IPatternDetails.IInput[] patternInputs = patternDetails.getInputs();
            if (patternInputs.length != inputHolder.length) {
                return null;
            }
            List<GenericStack> oneCraftInputs = new ArrayList<>(inputHolder.length);
            long copies = -1;
            for (int i = 0; i < inputHolder.length; i++) {
                Object2LongMap.Entry<AEKey> pushed = singleEntry(inputHolder[i]);
                if (pushed == null || pushed.getLongValue() <= 0) {
                    return null;
                }
                long perCraft = inputAmountPerCraft(patternInputs[i], pushed.getKey());
                if (perCraft <= 0 || pushed.getLongValue() % perCraft != 0) {
                    return null;
                }
                long inputCopies = pushed.getLongValue() / perCraft;
                if (copies < 0) {
                    copies = inputCopies;
                } else if (copies != inputCopies) {
                    return null;
                }
                oneCraftInputs.add(new GenericStack(pushed.getKey(), perCraft));
            }
            return copies <= 0 ? null : new PendingPattern(patternDetails.getDefinition(), oneCraftInputs, copies);
        }

        private KeyCounter[] toKeyCounters() {
            return toKeyCounters(1);
        }

        private KeyCounter[] toKeyCounters(long copies) {
            KeyCounter[] counters = new KeyCounter[this.inputs.size()];
            for (int i = 0; i < this.inputs.size(); i++) {
                GenericStack input = this.inputs.get(i);
                KeyCounter counter = new KeyCounter();
                counter.add(input.what(), multiplyClamped(input.amount(), copies));
                counters[i] = counter;
            }
            return counters;
        }

        private boolean canMerge(PendingPattern other) {
            return this.definition.equals(other.definition) && this.inputs.equals(other.inputs);
        }

        private long maxBatchCopies() {
            long maxCopies = Integer.MAX_VALUE;
            for (GenericStack input : this.inputs) {
                if (input.amount() <= 0) {
                    return 1;
                }
                maxCopies = Math.min(maxCopies, Long.MAX_VALUE / input.amount());
            }
            return Math.max(1, maxCopies);
        }

        private static long multiplyClamped(long amount, long copies) {
            if (amount <= 0 || copies <= 0) {
                return 0;
            }
            if (amount > Long.MAX_VALUE / copies) {
                return Long.MAX_VALUE;
            }
            return amount * copies;
        }

        @Nullable
        private static Object2LongMap.Entry<AEKey> singleEntry(KeyCounter counter) {
            if (counter == null || counter.size() != 1) {
                return null;
            }
            return counter.getFirstEntry();
        }

        private static long inputAmountPerCraft(IPatternDetails.IInput input, AEKey pushedKey) {
            long multiplier = Math.max(1, input.getMultiplier());
            for (GenericStack possibleInput : input.getPossibleInputs()) {
                if (possibleInput != null && possibleInput.what().equals(pushedKey) && possibleInput.amount() > 0) {
                    return possibleInput.amount() * multiplier;
                }
            }
            return 0;
        }
    }
}
