package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

public final class MeSmartPatternMultiplication {
    private static final int MAX_PENDING_ENTRIES_SCANNED_PER_PASS = 256;
    private static final int MAX_FEED_ATTEMPTS_PER_PASS = 512;
    private static final String TAG_ENABLED = "SmartPatternMultiplication";
    private static final String TAG_PENDING = "SmartPatternMultiplicationPending";
    private static final String TAG_REMAINING = "Remaining";
    private static final String TAG_DEFINITION = "Definition";
    private static final String TAG_INPUTS = "Inputs";
    private static final String TAG_INPUT = "Input";

    private final List<PendingPattern> pendingPatterns = new ArrayList<>();
    private final Map<PendingKey, PendingPattern> pendingByKey = new HashMap<>();
    private boolean enabled = true;
    private int nextPendingScanIndex;

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
        PendingPattern existing = this.pendingByKey.get(pendingPattern.key());
        if (existing != null) {
            return existing.tryMerge(pendingPattern);
        }
        this.pendingPatterns.add(pendingPattern);
        this.pendingByKey.put(pendingPattern.key(), pendingPattern);
        return true;
    }

    public boolean processNext(Feeder feeder) {
        boolean changed = false;
        int patternsVisited = 0;
        int feeds = 0;
        clampPendingScanIndex();
        int scanBudget = Math.min(this.pendingPatterns.size(), MAX_PENDING_ENTRIES_SCANNED_PER_PASS);
        while (!this.pendingPatterns.isEmpty() && patternsVisited++ < scanBudget && feeds < MAX_FEED_ATTEMPTS_PER_PASS) {
            PendingPattern pendingPattern = this.pendingPatterns.get(this.nextPendingScanIndex);
            if (pendingPattern.remaining <= 0) {
                removePendingAtScanIndex();
                changed = true;
                continue;
            }
            FeedResult result = feedBestBatch(pendingPattern, feeder, MAX_FEED_ATTEMPTS_PER_PASS - feeds);
            feeds += result.feedAttempts();
            changed |= result.changed();
            if (pendingPattern.remaining <= 0) {
                removePendingAtScanIndex();
                continue;
            }
            advancePendingScanIndex();
            // Keep scanning later pending entries. Smart multiplication favors machine throughput over strict FIFO
            // order so one temporarily full or output-blocked recipe does not stall unrelated recipes.
        }
        return changed;
    }

    public boolean processNext(List<IPatternDetails> patterns, PatternFeeder feeder) {
        boolean changed = false;
        int patternsVisited = 0;
        int feeds = 0;
        clampPendingScanIndex();
        int scanBudget = Math.min(this.pendingPatterns.size(), MAX_PENDING_ENTRIES_SCANNED_PER_PASS);
        while (!this.pendingPatterns.isEmpty() && patternsVisited++ < scanBudget && feeds < MAX_FEED_ATTEMPTS_PER_PASS) {
            PendingPattern pendingPattern = this.pendingPatterns.get(this.nextPendingScanIndex);
            if (pendingPattern.remaining <= 0) {
                removePendingAtScanIndex();
                changed = true;
                continue;
            }
            IPatternDetails patternDetails = findPattern(patterns, pendingPattern.definition);
            if (patternDetails == null) {
                advancePendingScanIndex();
                continue;
            }
            FeedResult result = feedBestBatch(pendingPattern, patternDetails, feeder, MAX_FEED_ATTEMPTS_PER_PASS - feeds);
            feeds += result.feedAttempts();
            changed |= result.changed();
            if (pendingPattern.remaining <= 0) {
                removePendingAtScanIndex();
                continue;
            }
            advancePendingScanIndex();
        }
        return changed;
    }

    private void clampPendingScanIndex() {
        if (this.nextPendingScanIndex < 0 || this.nextPendingScanIndex >= this.pendingPatterns.size()) {
            this.nextPendingScanIndex = 0;
        }
    }

    private void advancePendingScanIndex() {
        if (this.pendingPatterns.isEmpty()) {
            this.nextPendingScanIndex = 0;
        } else {
            this.nextPendingScanIndex = (this.nextPendingScanIndex + 1) % this.pendingPatterns.size();
        }
    }

    private void removePendingAtScanIndex() {
        PendingPattern removed = this.pendingPatterns.remove(this.nextPendingScanIndex);
        if (this.pendingByKey.remove(removed.key(), removed)) {
            reindexFirstPendingWithKey(removed.key());
        }
        clampPendingScanIndex();
    }

    private void reindexFirstPendingWithKey(PendingKey key) {
        for (PendingPattern pendingPattern : this.pendingPatterns) {
            if (pendingPattern.key().equals(key)) {
                this.pendingByKey.put(key, pendingPattern);
                return;
            }
        }
    }

    boolean enqueueForTesting(AEKey definition, List<GenericStack> oneCopyInputs, long copies) {
        if (definition == null || oneCopyInputs == null || oneCopyInputs.isEmpty() || copies <= 0) {
            return false;
        }
        PendingPattern pendingPattern = new PendingPattern(definition, oneCopyInputs, copies);
        PendingPattern existing = this.pendingByKey.get(pendingPattern.key());
        if (existing != null) {
            return existing.tryMerge(pendingPattern);
        }
        this.pendingPatterns.add(pendingPattern);
        this.pendingByKey.put(pendingPattern.key(), pendingPattern);
        return true;
    }

    private static FeedResult feedBestBatch(PendingPattern pendingPattern, Feeder feeder, int feedBudget) {
        boolean changed = false;
        int feedAttempts = 0;
        while (pendingPattern.remaining > 0) {
            long capacity = pendingPattern.maxAcceptedBy(feeder);
            if (capacity <= 0) {
                return new FeedResult(changed, feedAttempts);
            }
            long attempt = Math.min(pendingPattern.nextBatchAttempt(), capacity);
            boolean fed = false;
            while (attempt > 0 && feedAttempts < feedBudget) {
                feedAttempts++;
                if (feeder.feed(pendingPattern.toKeyCounters(attempt))) {
                    pendingPattern.remaining -= attempt;
                    pendingPattern.recordSuccessfulBatch(attempt);
                    changed = true;
                    fed = true;
                    break;
                }
                pendingPattern.recordFailedBatch(attempt);
                attempt /= 2;
            }
            if (!fed) {
                return new FeedResult(changed, feedAttempts);
            }
        }
        return new FeedResult(changed, feedAttempts);
    }

    private static FeedResult feedBestBatch(PendingPattern pendingPattern, IPatternDetails patternDetails, PatternFeeder feeder, int feedBudget) {
        boolean changed = false;
        int feedAttempts = 0;
        while (pendingPattern.remaining > 0) {
            long attempt = pendingPattern.nextBatchAttempt();
            boolean fed = false;
            while (attempt > 0 && feedAttempts < feedBudget) {
                feedAttempts++;
                if (feeder.feed(patternDetails, pendingPattern.toKeyCounters(attempt))) {
                    pendingPattern.remaining -= attempt;
                    pendingPattern.recordSuccessfulBatch(attempt);
                    changed = true;
                    fed = true;
                    break;
                }
                pendingPattern.recordFailedBatch(attempt);
                attempt /= 2;
            }
            if (!fed) {
                return new FeedResult(changed, feedAttempts);
            }
        }
        return new FeedResult(changed, feedAttempts);
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
        this.pendingByKey.clear();
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
                addLoadedPending(new PendingPattern(definitionKey, stacks, remaining));
            }
        }
    }

    private void addLoadedPending(PendingPattern pendingPattern) {
        PendingPattern existing = this.pendingByKey.get(pendingPattern.key());
        if (existing != null) {
            if (existing.tryMerge(pendingPattern)) {
                return;
            }
        }
        this.pendingPatterns.add(pendingPattern);
        this.pendingByKey.putIfAbsent(pendingPattern.key(), pendingPattern);
    }

    public interface Feeder {
        boolean feed(KeyCounter[] oneCraftInputs);
    }

    public interface CapacityAwareFeeder extends Feeder {
        long maxAcceptedCopies(KeyCounter[] oneCraftInputs);
    }

    public interface PatternFeeder {
        boolean feed(IPatternDetails patternDetails, KeyCounter[] oneCraftInputs);
    }

    private record FeedResult(boolean changed, int feedAttempts) {
    }

    private record PendingKey(AEKey definition, List<GenericStack> inputs) {
        private PendingKey {
            inputs = List.copyOf(inputs);
        }
    }

    @Nullable
    private static IPatternDetails findPattern(List<IPatternDetails> patterns, AEKey definition) {
        for (IPatternDetails pattern : patterns) {
            if (pattern.getDefinition().equals(definition)) {
                return pattern;
            }
        }
        return null;
    }

    private static final class PendingPattern {
        private final AEKey definition;
        private final List<GenericStack> inputs;
        private final PendingKey key;
        private final KeyCounter[] oneCopyInputs;
        private final long maxBatchCopies;
        private long remaining;
        private long preferredBatch;

        private PendingPattern(AEKey definition, List<GenericStack> inputs, long remaining) {
            this.definition = definition;
            this.inputs = List.copyOf(inputs);
            this.key = new PendingKey(definition, this.inputs);
            this.oneCopyInputs = createKeyCounters(inputs, 1);
            this.maxBatchCopies = calculateMaxBatchCopies(this.inputs);
            this.remaining = remaining;
            this.preferredBatch = -1;
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

        private PendingKey key() {
            return this.key;
        }

        private KeyCounter[] toKeyCounters() {
            return toKeyCounters(1);
        }

        private KeyCounter[] toKeyCounters(long copies) {
            return copies == 1 ? this.oneCopyInputs : createKeyCounters(this.inputs, copies);
        }

        private static KeyCounter[] createKeyCounters(List<GenericStack> inputs, long copies) {
            KeyCounter[] counters = new KeyCounter[inputs.size()];
            for (int i = 0; i < inputs.size(); i++) {
                GenericStack input = inputs.get(i);
                KeyCounter counter = new KeyCounter();
                counter.add(input.what(), multiplyClamped(input.amount(), copies));
                counters[i] = counter;
            }
            return counters;
        }

        private boolean tryMerge(PendingPattern other) {
            if (other.remaining > Long.MAX_VALUE - this.remaining) {
                return false;
            }
            this.remaining += other.remaining;
            return true;
        }

        private long maxBatchCopies() {
            return this.maxBatchCopies;
        }

        private static long calculateMaxBatchCopies(List<GenericStack> inputs) {
            long maxCopies = Long.MAX_VALUE;
            for (GenericStack input : inputs) {
                if (input.amount() <= 0) {
                    return 1;
                }
                if (input.what() instanceof AEItemKey || input.what() instanceof AEFluidKey) {
                    maxCopies = Math.min(maxCopies, Integer.MAX_VALUE / input.amount());
                }
                maxCopies = Math.min(maxCopies, Long.MAX_VALUE / input.amount());
            }
            return Math.max(1, maxCopies);
        }

        private long nextBatchAttempt() {
            long maxBatch = Math.min(this.remaining, maxBatchCopies());
            if (this.preferredBatch <= 0) {
                return maxBatch;
            }
            return Math.max(1, Math.min(maxBatch, this.preferredBatch));
        }

        private void recordSuccessfulBatch(long copies) {
            if (copies <= 0) {
                return;
            }
            long maxBatch = maxBatchCopies();
            if (copies >= maxBatch || copies > Long.MAX_VALUE / 2) {
                this.preferredBatch = maxBatch;
            } else {
                this.preferredBatch = Math.max(copies + 1, copies * 2);
            }
        }

        private void recordFailedBatch(long copies) {
            if (copies <= 1) {
                this.preferredBatch = 1;
                return;
            }
            long reduced = copies / 2;
            if (this.preferredBatch <= 0 || this.preferredBatch >= copies) {
                this.preferredBatch = Math.max(1, reduced);
            }
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

        private long maxAcceptedBy(Feeder feeder) {
            if (!(feeder instanceof CapacityAwareFeeder capacityAwareFeeder)) {
                return Long.MAX_VALUE;
            }
            long accepted = capacityAwareFeeder.maxAcceptedCopies(this.oneCopyInputs);
            return accepted <= 0 ? 0 : accepted;
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
