package com.beipuo.mekenergistics.blockentity.slot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekanism.api.AutomationType;
import org.junit.jupiter.api.Test;

class MePatternInventorySlotTest {
    @Test
    void hiddenPatternSlotsRejectAutomationButAllowManualInsertion() {
        assertTrue(MePatternInventorySlot.allowsInsertion(AutomationType.MANUAL));
        assertFalse(MePatternInventorySlot.allowsInsertion(AutomationType.INTERNAL));
        assertFalse(MePatternInventorySlot.allowsInsertion(AutomationType.EXTERNAL));
    }

    @Test
    void externalAutomationCannotPullPatternsOut() {
        assertTrue(MePatternInventorySlot.allowsExtraction(AutomationType.MANUAL));
        assertTrue(MePatternInventorySlot.allowsExtraction(AutomationType.INTERNAL));
        assertFalse(MePatternInventorySlot.allowsExtraction(AutomationType.EXTERNAL));
    }
}
