package com.beipuo.mekenergistics.compat.thunderbolt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.testfixture.FakeKey;
import com.moakiee.thunderbolt.api.crafting.batch.BatchDispatchMode;
import org.junit.jupiter.api.Test;

class ThunderboltBatchCompatTest {
    private static final AEKey INPUT = new FakeKey("input");
    private static final IPatternDetails PATTERN = new TestPattern();

    @Test
    void enabledMachineAdvertisesBatchPathAndDisabledMachineUsesOrdinaryPath() {
        RecordingTarget target = new RecordingTarget(8, true);

        assertEquals(Long.MAX_VALUE,
                ThunderboltBatchCompat.getBatchCapacity(target, true, PATTERN));
        assertEquals(BatchDispatchMode.UNBOUNDED,
                ThunderboltBatchCompat.getBatchDispatchMode(true));
        assertEquals(1L,
                ThunderboltBatchCompat.getBatchCapacity(target, false, PATTERN));
        assertEquals(BatchDispatchMode.NORMAL,
                ThunderboltBatchCompat.getBatchDispatchMode(false));
    }

    @Test
    void acceptsOnlyPhysicalCapacityAndReturnsRemainingCopies() {
        RecordingTarget target = new RecordingTarget(4, true);

        long leftover = ThunderboltBatchCompat.pushBatch(
                target, true, PATTERN, inputs(3), 10);

        assertEquals(6L, leftover);
        assertEquals(12L, target.routed[0].get(INPUT));
        assertSame(PATTERN, target.pattern);
    }

    @Test
    void borrowedTemplateRemainsUnchangedAndIsNotRetainedByTheRoute() {
        RecordingTarget target = new RecordingTarget(4, true);
        KeyCounter[] template = inputs(3);

        assertEquals(6L, ThunderboltBatchCompat.pushBatch(
                target, true, PATTERN, template, 10));
        target.routed[0].clear();

        assertEquals(3L, template[0].get(INPUT));
    }

    @Test
    void disabledMultiplicationNeverAcceptsMoreThanOneCopy() {
        RecordingTarget target = new RecordingTarget(8, true);

        assertEquals(4L, ThunderboltBatchCompat.pushBatch(
                target, false, PATTERN, inputs(2), 5));
        assertEquals(2L, target.routed[0].get(INPUT));
    }

    @Test
    void failedRouteLeavesEveryCopyWithTheCpu() {
        RecordingTarget target = new RecordingTarget(4, false);

        assertEquals(10L, ThunderboltBatchCompat.pushBatch(
                target, true, PATTERN, inputs(3), 10));
    }

    @Test
    void busyUnregisteredAndOverflowingRequestsAreRejected() {
        RecordingTarget busy = new RecordingTarget(4, true);
        busy.busy = true;
        assertEquals(0L, ThunderboltBatchCompat.getBatchCapacity(busy, true, PATTERN));
        assertEquals(4L, ThunderboltBatchCompat.pushBatch(
                busy, true, PATTERN, inputs(1), 4));

        RecordingTarget missing = new RecordingTarget(4, true);
        missing.registered = false;
        assertEquals(0L, ThunderboltBatchCompat.getBatchCapacity(missing, true, PATTERN));
        assertEquals(4L, ThunderboltBatchCompat.pushBatch(
                missing, true, PATTERN, inputs(1), 4));

        assertNull(ThunderboltBatchCompat.scale(inputs(Long.MAX_VALUE), 2));
    }

    private static KeyCounter[] inputs(long amount) {
        KeyCounter counter = new KeyCounter();
        counter.add(INPUT, amount);
        return new KeyCounter[] {counter};
    }

    private static final class RecordingTarget implements ThunderboltBatchCompat.BatchTarget {
        private final long capacity;
        private final boolean accept;
        private boolean busy;
        private boolean registered = true;
        private IPatternDetails pattern;
        private KeyCounter[] routed;

        private RecordingTarget(long capacity, boolean accept) {
            this.capacity = capacity;
            this.accept = accept;
        }

        @Override
        public boolean isBusy() {
            return busy;
        }

        @Override
        public boolean hasRegisteredPattern(IPatternDetails details) {
            this.pattern = details;
            return registered;
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCopyTemplate) {
            return capacity;
        }

        @Override
        public boolean routeInputs(KeyCounter[] scaledInputs) {
            this.routed = scaledInputs;
            return accept;
        }
    }

    private static final class TestPattern implements IPatternDetails {
        @Override
        public appeng.api.stacks.AEItemKey getDefinition() {
            return null;
        }

        @Override
        public IInput[] getInputs() {
            return new IInput[0];
        }

        @Override
        public java.util.List<appeng.api.stacks.GenericStack> getOutputs() {
            return java.util.List.of();
        }
    }
}
