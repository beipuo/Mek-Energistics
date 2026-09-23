package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

import com.beipuo.mekenergistics.config.MekEnergisticsConfig;

public final class MeSmartPatternMultiplication {
    private static final int MAX_PENDING_ENTRIES_SCANNED_PER_PASS = 256;
    private static final int MAX_HOT_PENDING_ENTRIES = 64;
    private static final int MAX_FEED_ATTEMPTS_PER_PASS = 512;
    private static final String TAG_ENABLED = "SmartPatternMultiplication";
    private static final String TAG_PENDING = "SmartPatternMultiplicationPending";
    private static final String TAG_REMAINING = "Remaining";
    private static final String TAG_DEFINITION = "Definition";
    private static final String TAG_INPUTS = "Inputs";
    private static final String TAG_INPUT = "Input";

    private final List<PendingPattern> pendingPatterns = new ArrayList<>();
    private final Set<PendingPattern> pendingSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<PendingKey, PendingPattern> pendingByKey = new HashMap<>();
    private final Map<AEKey, Set<PendingPattern>> pendingByInputKey = new HashMap<>();
    private final List<PendingPattern> hotPendingPatterns = new ArrayList<>();
    private final MePendingPatternStore pendingStore = new MePendingPatternStore();
    private boolean enabled = MekEnergisticsConfig.smartPatternMultiplicationDefault();
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
        this.pendingSet.add(pendingPattern);
        this.pendingByKey.put(pendingPattern.key(), pendingPattern);
        indexPendingInputs(pendingPattern);
        return true;
    }

    public boolean processNext(Feeder feeder) {
        boolean changed = false;
        int feeds = 0;
        clampPendingScanIndex();
        Set<PendingPattern> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        // The in-memory hot list is intentionally not serialized. Rebuild its most important
        // entries from material already present in the machine so a world reload cannot move
        // an active factory input behind hundreds of unrelated pending patterns.
        for (AEKey activeInputKey : feeder.activeInputKeys()) {
            Set<PendingPattern> matching = this.pendingByInputKey.get(activeInputKey);
            if (matching != null) {
                for (PendingPattern pendingPattern : List.copyOf(matching)) {
                    if (containsPending(pendingPattern)) {
                        rememberHotPending(pendingPattern);
                    }
                }
            }
        }

        // Inputs already present in the machine must win the next refill pass. Otherwise a
        // large heterogeneous queue can move the round-robin cursor hundreds of entries away
        // from the pattern that the factory just consumed.
        int hotPatternsVisited = 0;
        for (PendingPattern pendingPattern : List.copyOf(this.hotPendingPatterns)) {
            if (hotPatternsVisited >= MAX_HOT_PENDING_ENTRIES || feeds >= MAX_FEED_ATTEMPTS_PER_PASS) {
                break;
            }
            if (!containsPending(pendingPattern)) {
                forgetHotPending(pendingPattern);
                continue;
            }
            hotPatternsVisited++;
            visited.add(pendingPattern);
            if (pendingPattern.remaining <= 0) {
                removePending(pendingPattern);
                changed = true;
                continue;
            }
            FeedResult result = feedBestBatch(pendingPattern, feeder, MAX_FEED_ATTEMPTS_PER_PASS - feeds);
            feeds += result.feedAttempts();
            changed |= result.changed();
            if (pendingPattern.remaining <= 0) {
                removePending(pendingPattern);
            } else if (result.changed()) {
                rememberHotPending(pendingPattern);
            }
        }

        int patternsVisited = 0;
        int scanBudget = Math.min(this.pendingPatterns.size(), MAX_PENDING_ENTRIES_SCANNED_PER_PASS);
        int cursorSteps = 0;
        int maxCursorSteps = this.pendingPatterns.size();
        while (!this.pendingPatterns.isEmpty() && patternsVisited < scanBudget
                && cursorSteps++ < maxCursorSteps && feeds < MAX_FEED_ATTEMPTS_PER_PASS) {
            PendingPattern pendingPattern = this.pendingPatterns.get(this.nextPendingScanIndex);
            if (!visited.add(pendingPattern)) {
                advancePendingScanIndex();
                continue;
            }
            patternsVisited++;
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
            if (result.changed()) {
                rememberHotPending(pendingPattern);
            }
            advancePendingScanIndex();
            // Keep scanning later pending entries. Smart multiplication favors machine throughput over strict FIFO
            // order so one temporarily full or output-blocked recipe does not stall unrelated recipes.
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
        removePending(this.pendingPatterns.get(this.nextPendingScanIndex));
    }

    private void removePending(PendingPattern removed) {
        int index = indexOfPending(removed);
        if (index < 0) {
            forgetHotPending(removed);
            return;
        }
        this.pendingPatterns.remove(index);
        this.pendingSet.remove(removed);
        unindexPendingInputs(removed);
        if (index < this.nextPendingScanIndex) {
            this.nextPendingScanIndex--;
        }
        if (this.pendingByKey.remove(removed.key(), removed)) {
            reindexFirstPendingWithKey(removed.key());
        }
        forgetHotPending(removed);
        clampPendingScanIndex();
    }

    private boolean containsPending(PendingPattern pendingPattern) {
        return this.pendingSet.contains(pendingPattern);
    }

    private int indexOfPending(PendingPattern pendingPattern) {
        for (int i = 0; i < this.pendingPatterns.size(); i++) {
            if (this.pendingPatterns.get(i) == pendingPattern) {
                return i;
            }
        }
        return -1;
    }

    private void rememberHotPending(PendingPattern pendingPattern) {
        forgetHotPending(pendingPattern);
        this.hotPendingPatterns.addFirst(pendingPattern);
        if (this.hotPendingPatterns.size() > MAX_HOT_PENDING_ENTRIES) {
            this.hotPendingPatterns.removeLast();
        }
    }

    private void forgetHotPending(PendingPattern pendingPattern) {
        this.hotPendingPatterns.removeIf(candidate -> candidate == pendingPattern);
    }

    private void indexPendingInputs(PendingPattern pendingPattern) {
        for (AEKey inputKey : pendingPattern.inputKeys()) {
            this.pendingByInputKey.computeIfAbsent(inputKey,
                    ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(pendingPattern);
        }
    }

    private void unindexPendingInputs(PendingPattern pendingPattern) {
        for (AEKey inputKey : pendingPattern.inputKeys()) {
            Set<PendingPattern> matching = this.pendingByInputKey.get(inputKey);
            if (matching != null) {
                matching.remove(pendingPattern);
                if (matching.isEmpty()) {
                    this.pendingByInputKey.remove(inputKey);
                }
            }
        }
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
        this.pendingSet.add(pendingPattern);
        this.pendingByKey.put(pendingPattern.key(), pendingPattern);
        indexPendingInputs(pendingPattern);
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

    public void saveConfig(CompoundTag tag) {
        tag.putBoolean(TAG_ENABLED, this.enabled);
    }

    public void loadConfig(CompoundTag tag) {
        if (tag.contains(TAG_ENABLED)) {
            this.enabled = tag.getBoolean(TAG_ENABLED);
        }
    }

    public void savePending(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.pendingPatterns.isEmpty() && this.pendingStore.quarantinedCount() == 0) {
            tag.remove(TAG_PENDING);
            this.pendingStore.saveQuarantined(tag);
            return;
        }
        if (this.pendingPatterns.isEmpty()) {
            tag.remove(TAG_PENDING);
        } else {
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
        this.pendingStore.saveQuarantined(tag);
    }

    public void loadPending(CompoundTag tag, HolderLookup.Provider registries) {
        loadPending(tag, registries, null);
    }

    public void loadPending(CompoundTag tag, HolderLookup.Provider registries,
            @Nullable MePendingPatternStore.PendingBalanceRefund refund) {
        this.pendingPatterns.clear();
        this.pendingSet.clear();
        this.pendingByKey.clear();
        this.pendingByInputKey.clear();
        this.hotPendingPatterns.clear();
        this.pendingStore.loadQuarantined(tag);
        ListTag pending = tag.getList(TAG_PENDING, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag pendingTag = pending.getCompound(i);
            long remaining = pendingTag.getLong(TAG_REMAINING);
            if (remaining <= 0) {
                MePendingPatternStore.logDroppedPending(i, "non-positive remaining " + remaining);
                continue;
            }
            String reason = null;
            List<GenericStack> decodedInputs = List.of();
            try {
                ListTag inputTags = pendingTag.getList(TAG_INPUTS, CompoundTag.TAG_COMPOUND);
                decodedInputs = inputTags.isEmpty() ? List.of() : MePendingPatternStore.decodeInputs(registries, inputTags);
                GenericStack definition = GenericStack.readTag(registries, pendingTag.getCompound(TAG_DEFINITION));
                if (definition == null) {
                    reason = "undecodable definition";
                } else if (!(definition.what() instanceof AEItemKey definitionKey)) {
                    reason = "definition is not an item key: " + definition.what();
                } else if (inputTags.isEmpty()) {
                    reason = "no inputs listed";
                } else if (decodedInputs.isEmpty()) {
                    reason = "no usable inputs";
                } else if (decodedInputs.size() < inputTags.size()) {
                    reason = "only " + decodedInputs.size() + " of " + inputTags.size() + " inputs decoded";
                } else {
                    addLoadedPending(new PendingPattern(definitionKey, decodedInputs, remaining));
                    continue;
                }
            } catch (RuntimeException ex) {
                reason = "decode failed: " + ex.getMessage();
            }
            MePendingPatternStore.logDroppedPending(i, reason);
            long balance = MePendingPatternStore.refundableBalance(decodedInputs, remaining);
            if (balance > 0 && refund != null && refund.refund(decodedInputs, remaining) >= balance) {
                continue;
            }
            this.pendingStore.quarantine(i, reason, pendingTag);
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
        this.pendingSet.add(pendingPattern);
        this.pendingByKey.putIfAbsent(pendingPattern.key(), pendingPattern);
        indexPendingInputs(pendingPattern);
    }

    public int quarantinedPendingCount() {
        return this.pendingStore.quarantinedCount();
    }

    public interface Feeder {
        boolean feed(KeyCounter[] oneCraftInputs);

        default Iterable<AEKey> activeInputKeys() {
            return List.of();
        }
    }

    public interface CapacityAwareFeeder extends Feeder {
        long maxAcceptedCopies(KeyCounter[] oneCraftInputs);
    }

    private record FeedResult(boolean changed, int feedAttempts) {
    }

    private record PendingKey(AEKey definition, List<GenericStack> inputs) {
        private PendingKey {
            inputs = List.copyOf(inputs);
        }
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

        private Iterable<AEKey> inputKeys() {
            Set<AEKey> keys = new java.util.LinkedHashSet<>();
            for (GenericStack input : this.inputs) {
                keys.add(input.what());
            }
            return keys;
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
            return MePendingPatternStore.scaleAmountClamped(amount, copies);
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
