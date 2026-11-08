package com.beipuo.mekenergistics.blockentity.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class MeNetworkEnergyHelperTest {

    @Test
    void networkEnergyIsNotCappedByTheLocalBufferCapacity() {
        long localEnergy = 0;
        long localBufferCapacity = 20_000;
        long networkEnergy = 50_000;
        long energyPerTick = 50_000;

        AtomicLong requestedFromNetwork = new AtomicLong();
        long available = MeNetworkEnergyHelper.availableWithNetwork(localEnergy, requested -> {
            requestedFromNetwork.set(requested);
            return Math.min(requested, networkEnergy);
        });

        assertEquals(50_000, available);
        assertEquals(Long.MAX_VALUE, requestedFromNetwork.get(),
                "availability must query the network beyond the local FE buffer capacity");
        assertEquals(1, available / energyPerTick,
                "Mekanism must see enough network energy for one operation even when its cost exceeds the local buffer");
        assertTrue(available > localBufferCapacity,
                "network-backed recipe energy must be able to exceed the local FE buffer");
    }

    @Test
    void cappedAvailabilityOnlyQueriesTheNetworkUpToTheCap() {
        AtomicLong requestedFromNetwork = new AtomicLong();
        long available = MeNetworkEnergyHelper.availableWithNetwork(5_000, 100_000, requested -> {
            requestedFromNetwork.set(requested);
            return Math.min(requested, 1_000_000);
        });

        assertEquals(95_000, requestedFromNetwork.get(), "the network must only be asked for what the cap allows");
        assertEquals(100_000, available);
    }

    @Test
    void cappedAvailabilitySkipsTheNetworkWhenLocalEnergySuffices() {
        long available = MeNetworkEnergyHelper.availableWithNetwork(200_000, 100_000, requested -> {
            throw new AssertionError("network must not be queried");
        });

        assertEquals(200_000, available);
    }

    @Test
    void localAndNetworkEnergyAreCombined() {
        assertEquals(50_000, MeNetworkEnergyHelper.totalAvailableEnergy(10_000, 40_000));
    }

    @Test
    void combinedEnergySaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, MeNetworkEnergyHelper.totalAvailableEnergy(Long.MAX_VALUE - 10, 20));
    }

    @Test
    void emptyLocalBufferIsFilledToCapacityFromTheNetwork() {
        AtomicLong requested = new AtomicLong();
        AtomicLong stored = new AtomicLong();

        long injected = MeNetworkEnergyHelper.refillEnergyBuffer(0, 100, amount -> {
            requested.set(amount);
            return amount;
        }, stored::set);

        assertEquals(100, requested.get());
        assertEquals(100, injected);
        assertEquals(100, stored.get());
    }

    @Test
    void partialBufferOnlyRequestsItsMissingEnergy() {
        AtomicLong requested = new AtomicLong();
        AtomicLong stored = new AtomicLong();

        long injected = MeNetworkEnergyHelper.refillEnergyBuffer(40, 100, amount -> {
            requested.set(amount);
            return amount;
        }, stored::set);

        assertEquals(60, requested.get());
        assertEquals(60, injected);
        assertEquals(100, stored.get());
    }

    @Test
    void networkShortageOnlyAddsEnergyActuallyExtracted() {
        AtomicLong requested = new AtomicLong();
        AtomicLong stored = new AtomicLong();

        long injected = MeNetworkEnergyHelper.refillEnergyBuffer(40, 100, amount -> {
            requested.set(amount);
            return 25;
        }, stored::set);

        assertEquals(60, requested.get());
        assertEquals(25, injected);
        assertEquals(65, stored.get());
    }

    @Test
    void fullBufferDoesNotTouchTheNetworkOrRewriteEnergy() {
        AtomicBoolean extracted = new AtomicBoolean();
        AtomicBoolean stored = new AtomicBoolean();

        long injected = MeNetworkEnergyHelper.refillEnergyBuffer(100, 100, amount -> {
            extracted.set(true);
            return amount;
        }, amount -> stored.set(true));

        assertEquals(0, injected);
        assertFalse(extracted.get());
        assertFalse(stored.get());
    }

    @Test
    void unavailableNetworkEnergyLeavesTheLocalBufferUnchanged() {
        AtomicBoolean stored = new AtomicBoolean();

        long injected = MeNetworkEnergyHelper.refillEnergyBuffer(40, 100, amount -> 0,
                ignored -> stored.set(true));

        assertEquals(0, injected);
        assertFalse(stored.get());
    }

    @Test
    void allMeMachinesRefillBeforeTheirMekanismServerTick() throws Exception {
        String lifecycleMixin = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityMekanismMeUpgradeLifecycleMixin.java"));
        String aeSupport = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/blockentity/support/AbstractMeAeSupport.java"));

        assertTrue(lifecycleMixin.contains("@Inject(method = \"tickServer\""));
        assertTrue(lifecycleMixin.contains("TileEntityMekanism;onUpdateServer()Z"));
        assertTrue(lifecycleMixin.contains("TileEntityMekanism tile, CallbackInfo ci"));
        assertTrue(lifecycleMixin.contains("state.getBlock() instanceof MeMekanismMachineBlock"));
        assertTrue(lifecycleMixin.contains("!upgradeable.isMeUpgradeTarget() || !upgradeable.isMeUpgradeActive()"));
        assertTrue(lifecycleMixin.contains("machine.getRecipeAeSupport().refillLocalEnergyBuffers()"));
        assertTrue(aeSupport.contains("ownerTile.getEnergyContainers(null)"));
    }
}
