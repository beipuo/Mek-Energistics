package com.beipuo.mekenergistics.blockentity.support.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.testfixture.FakeInputPort;
import com.beipuo.mekenergistics.testfixture.FakeKey;
import org.junit.jupiter.api.Test;

class MeCountedInputAdmissionTest {
    private static final AEKey INPUT = new FakeKey("input");

    private static KeyCounter[] prototype(long amount) {
        KeyCounter counter = new KeyCounter();
        counter.add(INPUT, amount);
        return new KeyCounter[] {counter};
    }

    @Test
    void clampsToPhysicalCapacity() {
        FakeInputPort port = new FakeInputPort(INPUT, 5);
        MeInputLayout layout = MeInputLayout.unordered(java.util.List.of(port));
        MeCountedInputAdmission admission = MeCountedInputAdmission.prepare(
                prototype(2), 10, layout::maxAcceptedCopies, layout::route);
        assertEquals(2, admission.count());
    }

    @Test
    void rejectsOverflowBeforeCommit() {
        KeyCounter[] input = prototype(Long.MAX_VALUE);
        assertEquals(null, MeCountedInputAdmission.prepare(input, 2, ignored -> Long.MAX_VALUE,
                ignored -> true));
        assertEquals(null, MeCountedInputAdmission.scale(input, 2));
    }

    @Test
    void commitIsOneShotAndTransfersOnlyOnSuccess() {
        KeyCounter[] input = prototype(2);
        boolean[] accepted = {false};
        MeCountedInputAdmission admission = MeCountedInputAdmission.prepare(input, 3,
                ignored -> 3, ignored -> accepted[0]);
        assertFalse(admission.commit(input));
        assertFalse(admission.hasTransferredInputOwnership());
        assertThrows(IllegalStateException.class, () -> admission.commit(input));

        KeyCounter[] successfulInput = prototype(2);
        MeCountedInputAdmission successful = MeCountedInputAdmission.prepare(successfulInput, 3,
                ignored -> 3, ignored -> true);
        assertTrue(successful.commit(successfulInput));
        assertTrue(successful.hasTransferredInputOwnership());
        assertThrows(IllegalStateException.class, () -> successful.commit(successfulInput));
    }

    @Test
    void commitRejectsASecondPrototypeAndDoesNotDoubleScale() {
        KeyCounter[] input = prototype(2);
        MeCountedInputAdmission admission = MeCountedInputAdmission.prepare(input, 3,
                ignored -> 3, scaled -> scaled[0].get(INPUT) == 6);
        assertThrows(IllegalArgumentException.class, () -> admission.commit(prototype(2)));
        assertTrue(admission.commit(input));
    }
}
