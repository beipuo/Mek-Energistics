package com.beipuo.mekenergistics.blockentity.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.io.MePatternInputRouter;
import com.beipuo.mekenergistics.testfixture.FakeInputPort;
import com.beipuo.mekenergistics.testfixture.FakeKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class MeSmartPatternMultiplicationTest {
    @Test
    void missingSavedSettingKeepsConfiguredInitialDefault() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        multiplication.setEnabled(false);
        multiplication.loadConfig(new CompoundTag());
        assertFalse(multiplication.isEnabled());
    }

    @Test
    void savedSettingOverridesConfiguredInitialDefault() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("SmartPatternMultiplication", true);
        multiplication.setEnabled(false);
        multiplication.loadConfig(tag);
        assertTrue(multiplication.isEnabled());
    }

    @Test
    void saveConfigPersistsDisabledState() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        multiplication.setEnabled(false);
        CompoundTag tag = new CompoundTag();
        multiplication.saveConfig(tag);
        assertFalse(tag.getBoolean("SmartPatternMultiplication"));
    }

    @Test
    void capacityAwareFeederConsumesMoreThanVanillaStackInOnePass() {
        FakeKey inputKey = new FakeKey("iron");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 4_096));

        CountingFeeder feeder = new CountingFeeder(inputKey, 4_096);
        assertTrue(multiplication.processNext(feeder));

        assertEquals(4_096, feeder.acceptedCopies);
        assertEquals(1, feeder.feedCalls);
        assertFalse(multiplication.hasPendingWork());
    }

    @Test
    void capacityAwareFeederUsesCombinedFactorySlotCapacity() {
        FakeKey inputKey = new FakeKey("iron_factory");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 8)), 1_000));

        CountingFeeder feeder = new CountingFeeder(inputKey, 464 / 8);
        assertTrue(multiplication.processNext(feeder));

        assertEquals(56, feeder.acceptedCopies);
        assertEquals(1, feeder.feedCalls);
        assertTrue(multiplication.hasPendingWork());
    }

    @Test
    void unchangedQueueUsesBackoffUntilNewWorkWakesIt() {
        FakeKey inputKey = new FakeKey("blocked_until_wake");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 8));

        CountingFeeder feeder = new CountingFeeder(inputKey, 0);
        assertFalse(multiplication.processNext(feeder));
        assertFalse(multiplication.processNext(feeder));
        assertFalse(multiplication.processNext(feeder));
        assertTrue(multiplication.enqueueForTesting(new FakeKey("wake_definition"),
                List.of(new GenericStack(new FakeKey("wake_input"), 1)), 1));
        assertTrue(multiplication.processNext(new CountingFeeder(new FakeKey("wake_input"), 1)));
    }

    @Test
    void successfulFeedResetsBackoffForRemainingQueue() {
        FakeKey blockedKey = new FakeKey("blocked_then_available");
        FakeKey availableKey = new FakeKey("available_after_success");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        assertTrue(multiplication.enqueueForTesting(blockedKey, List.of(new GenericStack(blockedKey, 1)), 1));
        assertTrue(multiplication.enqueueForTesting(availableKey, List.of(new GenericStack(availableKey, 1)), 1));

        SelectiveFeeder feeder = new SelectiveFeeder(blockedKey, 0, availableKey, 1);
        assertTrue(multiplication.processNext(feeder));
        assertTrue(multiplication.hasPendingWork());
    }

    @Test
    void repeatedPatternsMergeBeforeProcessing() {
        FakeKey inputKey = new FakeKey("gold");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 96));
        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 160));

        CountingFeeder feeder = new CountingFeeder(inputKey, 512);
        assertTrue(multiplication.processNext(feeder));

        assertEquals(256, feeder.acceptedCopies);
        assertEquals(1, feeder.feedCalls);
        assertFalse(multiplication.hasPendingWork());
    }

    @Test
    void failedLargeBatchFallsBackToSmallerAcceptedBatch() {
        FakeKey inputKey = new FakeKey("copper");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 512));

        MaximumBatchFeeder feeder = new MaximumBatchFeeder(inputKey, 64);
        assertTrue(multiplication.processNext(feeder));

        assertEquals(512, feeder.acceptedCopies);
        assertEquals(8, feeder.successfulFeeds);
        assertTrue(feeder.failedFeeds > 0);
        assertFalse(multiplication.hasPendingWork());
    }

    @Test
    void blockedPendingDoesNotStarveLaterPending() {
        FakeKey blockedKey = new FakeKey("diamond");
        FakeKey availableKey = new FakeKey("emerald");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        assertTrue(multiplication.enqueueForTesting(blockedKey, List.of(new GenericStack(blockedKey, 1)), 128));
        assertTrue(multiplication.enqueueForTesting(availableKey, List.of(new GenericStack(availableKey, 1)), 256));

        SelectiveFeeder feeder = new SelectiveFeeder(blockedKey, 0, availableKey, 256);
        assertTrue(multiplication.processNext(feeder));

        assertEquals(256, feeder.acceptedCopies);
        assertTrue(multiplication.hasPendingWork());
    }

    @Test
    void boundedPassStillFeedsHundredsOfSmallBatches() {
        FakeKey inputKey = new FakeKey("tin");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 32_768));

        CapacityLimitedFeeder feeder = new CapacityLimitedFeeder(inputKey, 32);
        assertTrue(multiplication.processNext(feeder));

        assertEquals(16_384, feeder.acceptedCopies);
        assertEquals(512, feeder.feedCalls);
        assertTrue(multiplication.hasPendingWork());

        assertTrue(multiplication.processNext(feeder));

        assertEquals(32_768, feeder.acceptedCopies);
        assertEquals(1_024, feeder.feedCalls);
        assertFalse(multiplication.hasPendingWork());
    }

    @Test
    void highSpeedFactoryRefillsCapacityAfterEveryRecipeTick() {
        FakeKey inputKey = new FakeKey("high_speed_factory_input");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 1_000_000));

        RouterBackedFactoryFeeder feeder = new RouterBackedFactoryFeeder(inputKey, 17, 4_096);
        for (int tick = 0; tick < 8; tick++) {
            feeder.autoBalance();
            feeder.consumeFromEverySlot(256);
            assertTrue(multiplication.processNext(feeder));
            assertEquals(17L * 4_096, feeder.loaded(),
                    "smart input should refill all parallel factory slots after tick " + tick);
        }

        assertTrue(multiplication.hasPendingWork());
    }

    @Test
    void activeFactoryInputRefillsEvenWhenRoundRobinCursorMovesPastScanBudget() {
        FakeKey hotKey = new FakeKey("hot_factory_input");
        FakeKey blockedKey = new FakeKey("blocked_factory_input");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        assertTrue(multiplication.enqueueForTesting(hotKey, List.of(new GenericStack(hotKey, 1)), 100_000));
        for (int i = 0; i < 600; i++) {
            FakeKey definition = new FakeKey("queued_pattern_" + i);
            assertTrue(multiplication.enqueueForTesting(definition, List.of(new GenericStack(blockedKey, 1)), 1));
        }

        HotFactoryFeeder feeder = new HotFactoryFeeder(hotKey, 512);
        assertTrue(multiplication.processNext(feeder));
        assertEquals(512, feeder.loaded);

        feeder.consume(192);
        assertTrue(multiplication.processNext(feeder));
        assertEquals(512, feeder.loaded,
                "the active input must refill before scanning later queued pattern definitions");
    }

    @Test
    void staleHotPatternsDoNotConsumeTheNormalPendingScanBudget() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        List<FakeKey> staleInputs = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            FakeKey definition = new FakeKey("stale_hot_definition_" + i);
            FakeKey input = new FakeKey("stale_hot_input_" + i);
            staleInputs.add(input);
            assertTrue(multiplication.enqueueForTesting(definition, List.of(new GenericStack(input, 1)), 2));
        }
        FakeKey targetDefinition = new FakeKey("target_after_hot_entries");
        FakeKey targetInput = new FakeKey("target_input_after_hot_entries");
        assertTrue(multiplication.enqueueForTesting(
                targetDefinition, List.of(new GenericStack(targetInput, 1)), 1));

        OneShotPerKeyFeeder feeder = new OneShotPerKeyFeeder(staleInputs);
        assertTrue(multiplication.processNext(feeder));
        assertFalse(feeder.accepted(targetInput));

        feeder.allow(targetInput);
        assertTrue(multiplication.processNext(feeder));
        assertTrue(feeder.accepted(targetInput),
                "stale hot entries must not prevent the round-robin scan from reaching new orders");
    }

    @Test
    void activeMachineInputRestoresPriorityAfterHotStateIsLost() {
        FakeKey blockedInput = new FakeKey("blocked_before_reload");
        FakeKey activeInput = new FakeKey("active_after_reload");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        for (int i = 0; i < 600; i++) {
            assertTrue(multiplication.enqueueForTesting(new FakeKey("queued_before_active_" + i),
                    List.of(new GenericStack(blockedInput, 1)), 1));
        }
        assertTrue(multiplication.enqueueForTesting(new FakeKey("active_definition"),
                List.of(new GenericStack(activeInput, 1)), 128));

        ActiveInputFeeder feeder = new ActiveInputFeeder(activeInput, 128);
        assertTrue(multiplication.processNext(feeder));
        assertEquals(128, feeder.acceptedCopies,
                "material already in the machine must bypass the pending scan cursor after reload");
    }

    @Test
    void scanCursorReachesPendingBeyondSinglePassBudget() {
        FakeKey blockedKey = new FakeKey("blocked");
        FakeKey availableKey = new FakeKey("available_after_budget");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();

        for (int i = 0; i < 300; i++) {
            FakeKey key = new FakeKey("blocked_" + i);
            assertTrue(multiplication.enqueueForTesting(key, List.of(new GenericStack(blockedKey, 1)), 1));
        }
        assertTrue(multiplication.enqueueForTesting(availableKey, List.of(new GenericStack(availableKey, 1)), 64));

        SelectiveFeeder feeder = new SelectiveFeeder(blockedKey, 0, availableKey, 64);

        assertFalse(multiplication.processNext(feeder));
        assertTrue(multiplication.processNext(feeder));

        assertEquals(64, feeder.acceptedCopies);
        assertTrue(multiplication.hasPendingWork());
    }

    @Test
    void corruptedPendingEntriesAreQuarantinedAndKeptOutOfTheLiveQueue() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        CompoundTag tag = new CompoundTag();
        ListTag pending = new ListTag();
        pending.add(pendingEntry(8, garbageDefinition(), "iron"));
        pending.add(pendingEntry(4, badTypeDefinition(), "iron"));
        pending.add(pendingEntry(0, garbageDefinition(), "iron"));
        tag.put("SmartPatternMultiplicationPending", pending);

        multiplication.loadPending(tag, RegistryAccess.EMPTY, null);

        assertFalse(multiplication.hasPendingWork());
        assertEquals(2, multiplication.quarantinedPendingCount());
    }

    @Test
    void quarantinedEntriesArePersistedBySavePending() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        CompoundTag tag = new CompoundTag();
        ListTag pending = new ListTag();
        pending.add(pendingEntry(8, garbageDefinition(), "iron"));
        pending.add(pendingEntry(4, badTypeDefinition(), "iron"));
        tag.put("SmartPatternMultiplicationPending", pending);
        multiplication.loadPending(tag, RegistryAccess.EMPTY, null);
        assertEquals(2, multiplication.quarantinedPendingCount());

        CompoundTag saved = new CompoundTag();
        multiplication.savePending(saved, RegistryAccess.EMPTY);

        MeSmartPatternMultiplication restored = new MeSmartPatternMultiplication();
        restored.loadPending(saved, RegistryAccess.EMPTY, null);
        assertFalse(restored.hasPendingWork());
        assertEquals(2, restored.quarantinedPendingCount());
    }

    @Test
    void quarantinedLoadDoesNotPoisonLaterEnqueues() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        CompoundTag tag = new CompoundTag();
        ListTag pending = new ListTag();
        pending.add(pendingEntry(8, garbageDefinition(), "iron"));
        tag.put("SmartPatternMultiplicationPending", pending);
        multiplication.loadPending(tag, RegistryAccess.EMPTY, null);
        assertEquals(1, multiplication.quarantinedPendingCount());

        FakeKey inputKey = new FakeKey("after_quarantine");
        assertTrue(multiplication.enqueueForTesting(inputKey, List.of(new GenericStack(inputKey, 1)), 16));
        CountingFeeder feeder = new CountingFeeder(inputKey, 16);
        assertTrue(multiplication.processNext(feeder));
        assertEquals(16, feeder.acceptedCopies);
        assertFalse(multiplication.hasPendingWork());
        assertEquals(1, multiplication.quarantinedPendingCount());
    }

    @Test
    void nonPositiveRemainingEntriesAreDroppedWithoutQuarantine() {
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        CompoundTag tag = new CompoundTag();
        ListTag pending = new ListTag();
        pending.add(pendingEntry(0, garbageDefinition(), "iron"));
        pending.add(pendingEntry(-5, garbageDefinition(), "iron"));
        tag.put("SmartPatternMultiplicationPending", pending);

        multiplication.loadPending(tag, RegistryAccess.EMPTY, null);

        assertFalse(multiplication.hasPendingWork());
        assertEquals(0, multiplication.quarantinedPendingCount());
    }

    @Test
    void refundableBalanceScalesRemainingCopiesWithoutLossOrOverflow() {
        FakeKey iron = new FakeKey("refund_iron");
        assertEquals(20, MePendingPatternStore.refundableBalance(
                List.of(new GenericStack(iron, 4)), 5));
        assertEquals(0, MePendingPatternStore.refundableBalance(
                List.of(new GenericStack(iron, 4)), 0));
        assertEquals(0, MePendingPatternStore.refundableBalance(List.of(), 5));
        assertEquals(0, MePendingPatternStore.refundableBalance(null, 5));
        assertEquals(Long.MAX_VALUE, MePendingPatternStore.refundableBalance(
                List.of(new GenericStack(iron, Long.MAX_VALUE)), 2));
    }

    private static CompoundTag pendingEntry(long remaining, CompoundTag definition, String... inputs) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("Remaining", remaining);
        entry.put("Definition", definition);
        ListTag inputList = new ListTag();
        for (String input : inputs) {
            CompoundTag inputTag = new CompoundTag();
            inputTag.putString("Input", input);
            inputList.add(inputTag);
        }
        entry.put("Inputs", inputList);
        return entry;
    }

    private static CompoundTag garbageDefinition() {
        return new CompoundTag();
    }

    private static CompoundTag badTypeDefinition() {
        CompoundTag definition = new CompoundTag();
        definition.putString("#t", "mekenergistics_test:item");
        return definition;
    }

    private static final class CountingFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey key;
        private long remainingCapacity;
        private long acceptedCopies;
        private int feedCalls;

        private CountingFeeder(AEKey key, long capacity) {
            this.key = key;
            this.remainingCapacity = capacity;
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            return this.remainingCapacity;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            long copies = oneCraftInputs[0].get(this.key);
            if (copies <= 0 || copies > this.remainingCapacity) {
                return false;
            }
            this.remainingCapacity -= copies;
            this.acceptedCopies += copies;
            this.feedCalls++;
            return true;
        }
    }

    private static final class MaximumBatchFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey key;
        private final long maxAcceptedPerCall;
        private long acceptedCopies;
        private int successfulFeeds;
        private int failedFeeds;

        private MaximumBatchFeeder(AEKey key, long maxAcceptedPerCall) {
            this.key = key;
            this.maxAcceptedPerCall = maxAcceptedPerCall;
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            return Long.MAX_VALUE;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            long copies = oneCraftInputs[0].get(this.key);
            if (copies <= 0 || copies > this.maxAcceptedPerCall) {
                this.failedFeeds++;
                return false;
            }
            this.acceptedCopies += copies;
            this.successfulFeeds++;
            return true;
        }
    }

    private static final class CapacityLimitedFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey key;
        private final long capacityPerCall;
        private long acceptedCopies;
        private int feedCalls;

        private CapacityLimitedFeeder(AEKey key, long capacityPerCall) {
            this.key = key;
            this.capacityPerCall = capacityPerCall;
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            return this.capacityPerCall;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            long copies = oneCraftInputs[0].get(this.key);
            if (copies <= 0 || copies > this.capacityPerCall) {
                return false;
            }
            this.acceptedCopies += copies;
            this.feedCalls++;
            return true;
        }
    }

    private static final class RouterBackedFactoryFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey key;
        private final List<FakeInputPort> ports;

        private RouterBackedFactoryFeeder(AEKey key, int slotCount, long capacityPerSlot) {
            this.key = key;
            this.ports = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                this.ports.add(new FakeInputPort(key, capacityPerSlot));
            }
        }

        private void consumeFromEverySlot(long amount) {
            this.ports.forEach(port -> port.setAmount(Math.max(0, port.amount() - amount)));
        }

        private void autoBalance() {
            long total = loaded();
            long perSlot = total / this.ports.size();
            long remainder = total % this.ports.size();
            for (FakeInputPort port : this.ports) {
                port.setAmount(perSlot + (remainder-- > 0 ? 1 : 0));
            }
        }

        private long loaded() {
            return this.ports.stream().mapToLong(FakeInputPort::amount).sum();
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            return MePatternInputRouter.maxAcceptedCopies(oneCraftInputs, this.ports);
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return oneCraftInputs[0].get(this.key) > 0
                    && MePatternInputRouter.route(oneCraftInputs, this.ports);
        }
    }

    private static final class HotFactoryFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey acceptedKey;
        private final long capacity;
        private long loaded;

        private HotFactoryFeeder(AEKey acceptedKey, long capacity) {
            this.acceptedKey = acceptedKey;
            this.capacity = capacity;
        }

        private void consume(long copies) {
            this.loaded = Math.max(0, this.loaded - copies);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            return oneCraftInputs[0].get(this.acceptedKey) > 0 ? this.capacity - this.loaded : 0;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            long copies = oneCraftInputs[0].get(this.acceptedKey);
            if (copies <= 0 || copies > this.capacity - this.loaded) {
                return false;
            }
            this.loaded += copies;
            return true;
        }
    }

    private static final class OneShotPerKeyFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Set<AEKey> allowed = new HashSet<>();
        private final Set<AEKey> accepted = new HashSet<>();

        private OneShotPerKeyFeeder(List<? extends AEKey> initiallyAllowed) {
            this.allowed.addAll(initiallyAllowed);
        }

        private void allow(AEKey key) {
            this.allowed.add(key);
        }

        private boolean accepted(AEKey key) {
            return this.accepted.contains(key);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            AEKey key = oneCraftInputs[0].getFirstEntry().getKey();
            return this.allowed.contains(key) && !this.accepted.contains(key) ? 1 : 0;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            AEKey key = oneCraftInputs[0].getFirstEntry().getKey();
            return this.allowed.contains(key) && this.accepted.add(key);
        }
    }

    private static final class ActiveInputFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey activeInput;
        private final long capacity;
        private long acceptedCopies;

        private ActiveInputFeeder(AEKey activeInput, long capacity) {
            this.activeInput = activeInput;
            this.capacity = capacity;
        }

        @Override
        public Iterable<AEKey> activeInputKeys() {
            return List.of(this.activeInput);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            return oneCraftInputs[0].get(this.activeInput) > 0 ? this.capacity - this.acceptedCopies : 0;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            long copies = oneCraftInputs[0].get(this.activeInput);
            if (copies <= 0 || copies > this.capacity - this.acceptedCopies) {
                return false;
            }
            this.acceptedCopies += copies;
            return true;
        }
    }

    private static final class SelectiveFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final AEKey firstKey;
        private final long firstCapacity;
        private final AEKey secondKey;
        private final long secondCapacity;
        private long acceptedCopies;

        private SelectiveFeeder(AEKey firstKey, long firstCapacity, AEKey secondKey, long secondCapacity) {
            this.firstKey = firstKey;
            this.firstCapacity = firstCapacity;
            this.secondKey = secondKey;
            this.secondCapacity = secondCapacity;
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            if (oneCraftInputs[0].get(this.firstKey) > 0) {
                return this.firstCapacity;
            }
            if (oneCraftInputs[0].get(this.secondKey) > 0) {
                return this.secondCapacity;
            }
            return 0;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            long copies = oneCraftInputs[0].get(this.secondKey);
            if (copies <= 0 || copies > this.secondCapacity) {
                return false;
            }
            this.acceptedCopies += copies;
            return true;
        }
    }
}
