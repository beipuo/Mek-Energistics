package com.beipuo.mekenergistics.compat.neoecoae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import com.beipuo.mekenergistics.testfixture.FakeKey;
import java.util.List;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class NeoEcoBatchCompatTest {
    private static final AEKey INPUT = new FakeKey("input");
    private static final AEKey SECONDARY = new FakeKey("secondary");
    private static final TestPattern PATTERN = new TestPattern();

    @Test
    void acceptsOnlyPhysicalCapacityAndReservesExtraCpuInputs() {
        RecordingTarget target = new RecordingTarget(4, true);
        ListCraftingInventory inventory = inventoryWith(27);

        int accepted = NeoEcoBatchCompat.tryPushBatch(
                target, PATTERN, inputs(3), inventory, 5, 10, power -> power <= 20);

        assertEquals(4, accepted);
        assertEquals(18, inventory.list.get(INPUT));
        assertEquals(12, target.routed[0].get(INPUT));
        assertSame(PATTERN, target.pattern);
    }

    @Test
    void energyLimitReducesTheBatchBeforeOwnershipTransfer() {
        RecordingTarget target = new RecordingTarget(4, true);
        ListCraftingInventory inventory = inventoryWith(27);

        int accepted = NeoEcoBatchCompat.tryPushBatch(
                target, PATTERN, inputs(3), inventory, 5, 10, power -> power <= 10);

        assertEquals(2, accepted);
        assertEquals(24, inventory.list.get(INPUT));
        assertEquals(6, target.routed[0].get(INPUT));
    }

    @Test
    void singleRemainingCraftStillBypassesTheMachineMultiplier() {
        RecordingTarget target = new RecordingTarget(4, true);
        ListCraftingInventory inventory = inventoryWith(27);

        int accepted = NeoEcoBatchCompat.tryPushBatch(
                target, PATTERN, inputs(3), inventory, 5, 1, power -> power <= 5);

        assertEquals(1, accepted);
        assertEquals(27, inventory.list.get(INPUT));
        assertEquals(3, target.routed[0].get(INPUT));
    }

    @Test
    void energyLimitCanReduceTheDirectAdmissionToOneCraft() {
        RecordingTarget target = new RecordingTarget(4, true);
        ListCraftingInventory inventory = inventoryWith(27);

        int accepted = NeoEcoBatchCompat.tryPushBatch(
                target, PATTERN, inputs(3), inventory, 5, 10, power -> power <= 5);

        assertEquals(1, accepted);
        assertEquals(27, inventory.list.get(INPUT));
        assertEquals(3, target.routed[0].get(INPUT));
    }

    @Test
    void failedTransferRestoresEveryReservedExtraInput() {
        RecordingTarget target = new RecordingTarget(4, false);
        ListCraftingInventory inventory = inventoryWith(27);

        assertEquals(0, NeoEcoBatchCompat.tryPushBatch(
                target, PATTERN, inputs(3), inventory, 5, 10, power -> true));
        assertEquals(27, inventory.list.get(INPUT));
    }

    @Test
    void fullInputValidationPreservesMultipleKeysAndMultipliers() {
        KeyCounter first = new KeyCounter();
        first.add(INPUT, 2);
        first.add(SECONDARY, 3);
        KeyCounter second = new KeyCounter();
        second.add(INPUT, 5);

        KeyCounter[] prototype = new KeyCounter[] {first, second};
        KeyCounter expectedFirst = new KeyCounter();
        expectedFirst.add(INPUT, 8);
        expectedFirst.add(SECONDARY, 12);
        KeyCounter expectedSecond = new KeyCounter();
        expectedSecond.add(INPUT, 20);
        assertTrue(NeoEcoBatchCompat.sameInputs(
                NeoEcoBatchCompat.scale(prototype, 4), new KeyCounter[] {expectedFirst, expectedSecond}));
        assertFalse(NeoEcoBatchCompat.sameInputs(
                NeoEcoBatchCompat.scale(prototype, 4), new KeyCounter[] {first, second}));
    }

    @Test
    void rejectsBusyUnregisteredAndOverflowingRequestsWithoutRouting() {
        RecordingTarget busy = new RecordingTarget(4, true);
        busy.busy = true;
        assertEquals(0, NeoEcoBatchCompat.tryPushBatch(
                busy, PATTERN, inputs(1), inventoryWith(10), 1, 4, power -> true));

        RecordingTarget missing = new RecordingTarget(4, true);
        missing.registered = false;
        assertEquals(0, NeoEcoBatchCompat.tryPushBatch(
                missing, PATTERN, inputs(1), inventoryWith(10), 1, 4, power -> true));

        assertNull(NeoEcoBatchCompat.scale(inputs(Long.MAX_VALUE), 2));
    }

    private static ListCraftingInventory inventoryWith(long amount) {
        ListCraftingInventory inventory = new ListCraftingInventory(key -> {
        });
        inventory.insert(INPUT, amount, Actionable.MODULATE);
        return inventory;
    }

    private static KeyCounter[] inputs(long amount) {
        KeyCounter counter = new KeyCounter();
        counter.add(INPUT, amount);
        return new KeyCounter[] {counter};
    }

    private static final class RecordingTarget implements NeoEcoBatchCompat.BatchTarget {
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
        public boolean hasRegisteredPattern(IPatternDetails patternDetails) {
            this.pattern = patternDetails;
            return registered;
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
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
            return new IInput[] {new TestInput()};
        }

        @Override
        public List<GenericStack> getOutputs() {
            return List.of();
        }
    }

    private static final class TestInput implements IPatternDetails.IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[] {new GenericStack(INPUT, 1)};
        }

        @Override
        public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return INPUT.equals(input);
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
