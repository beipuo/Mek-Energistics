package com.beipuo.mekenergistics.blockentity.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class MeSmartPatternMultiplicationTest {
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

    private static final class FakeKey extends AEKey {
        private final String id;

        private FakeKey(String id) {
            this.id = id;
        }

        @Override
        public AEKeyType getType() {
            throw new UnsupportedOperationException("Tests do not serialize fake keys");
        }

        @Override
        public AEKey dropSecondary() {
            return this;
        }

        @Override
        public CompoundTag toTag(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.id);
            return tag;
        }

        @Override
        public Object getPrimaryKey() {
            return this.id;
        }

        @Override
        public ResourceLocation getId() {
            return ResourceLocation.fromNamespaceAndPath("mekenergistics_test", this.id);
        }

        @Override
        public void writeToPacket(RegistryFriendlyByteBuf data) {
            data.writeUtf(this.id);
        }

        @Override
        protected Component computeDisplayName() {
            return Component.literal(this.id);
        }

        @Override
        public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        }

        @Override
        public boolean hasComponents() {
            return false;
        }

        @Override
        public boolean equals(Object object) {
            return this == object || object instanceof FakeKey other && this.id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
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
