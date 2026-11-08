package com.beipuo.mekenergistics.blockentity.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.networking.ticking.TickRateModulation;
import org.junit.jupiter.api.Test;

/**
 * The AE ticker returns machine outputs and services the output interface. Work that cannot make
 * progress (full network, missing stock) used to be retried every single tick forever, because the
 * ticker asked for a fixed 1-tick rate and re-alerted itself. These rules keep progress immediate
 * while letting a stuck backlog back off.
 */
class MeAeTickRateTest {
    @Test
    void sleepsWhenNoWorkRemains() {
        assertEquals(TickRateModulation.SLEEP, AbstractMeAeSupport.nextTickRate(true, false));
        assertEquals(TickRateModulation.SLEEP, AbstractMeAeSupport.nextTickRate(false, false));
    }

    @Test
    void progressKeepsTheTickerAtFullSpeed() {
        assertEquals(TickRateModulation.URGENT, AbstractMeAeSupport.nextTickRate(true, true));
    }

    @Test
    void stuckWorkBacksOff() {
        assertEquals(TickRateModulation.SLOWER, AbstractMeAeSupport.nextTickRate(false, true));
    }

    @Test
    void backoffIsBoundedSoStuckWorkIsStillRetried() {
        assertTrue(AbstractMeAeSupport.MAX_IDLE_TICK_RATE > 1, "a 1-tick ceiling makes SLOWER a no-op");
        assertTrue(AbstractMeAeSupport.MAX_IDLE_TICK_RATE <= 20, "stuck output must be retried at least once a second");
    }
}
