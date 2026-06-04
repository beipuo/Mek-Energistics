package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import net.neoforged.fml.ModList;

public final class MeNetworkEnergyHelper {
    private static final String APPLIED_FLUX_MODID = "appflux";

    private MeNetworkEnergyHelper() {
    }

    public static long extractWithLocalBuffer(MachineEnergyContainer<?> localEnergy, IGrid grid, IActionSource source, long amount,
            Action action, AutomationType automationType) {
        if (amount <= 0) {
            return 0;
        }
        if (automationType != AutomationType.INTERNAL || grid == null) {
            return localEnergy.extract(amount, action, automationType);
        }
        if (MekEnergisticsConfig.preferLocalFe()) {
            long localExtracted = localEnergy.extract(amount, action, automationType);
            return localExtracted + extractNetworkFe(grid, source, amount - localExtracted, action);
        }
        long networkExtracted = extractNetworkFe(grid, source, amount, action);
        return networkExtracted + localEnergy.extract(amount - networkExtracted, action, automationType);
    }

    public static long availableWithLocalBuffer(MachineEnergyContainer<?> localEnergy, IGrid grid, IActionSource source) {
        long local = localEnergy.getEnergy();
        long needed = localEnergy.getMaxEnergy() - local;
        if (needed <= 0 || grid == null) {
            return local;
        }
        return Math.min(localEnergy.getMaxEnergy(), local + extractNetworkFe(grid, source, needed, Action.SIMULATE));
    }

    public static long extractNetworkFe(IGrid grid, IActionSource source, long requestedFe, Action action) {
        if (requestedFe <= 0 || grid == null) {
            return 0;
        }
        Actionable actionable = action.execute() ? Actionable.MODULATE : Actionable.SIMULATE;
        long extracted = 0;
        boolean hasAppliedFlux = ModList.get().isLoaded(APPLIED_FLUX_MODID);
        if (hasAppliedFlux && MekEnergisticsConfig.preferAppliedFluxNetworkFe()) {
            extracted = AppliedFluxEnergyBridge.extractFe(grid, requestedFe, actionable, source);
            requestedFe -= extracted;
        }
        long aeExtracted = extractAeEnergyAsFe(grid, requestedFe, actionable);
        extracted += aeExtracted;
        requestedFe -= aeExtracted;
        if (hasAppliedFlux && !MekEnergisticsConfig.preferAppliedFluxNetworkFe()) {
            extracted += AppliedFluxEnergyBridge.extractFe(grid, requestedFe, actionable, source);
        }
        return extracted;
    }

    private static long extractAeEnergyAsFe(IGrid grid, long requestedFe, Actionable action) {
        if (requestedFe <= 0) {
            return 0;
        }
        double requestedAe = PowerUnit.FE.convertTo(PowerUnit.AE, requestedFe);
        double extractedAe = grid.getEnergyService().extractAEPower(requestedAe, action, PowerMultiplier.ONE);
        return Math.min(requestedFe, (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, extractedAe)));
    }
}
