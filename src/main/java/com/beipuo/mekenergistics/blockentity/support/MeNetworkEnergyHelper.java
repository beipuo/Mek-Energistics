package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.ModList;

public final class MeNetworkEnergyHelper {
    private static final String APPLIED_FLUX_MODID = "appflux";

    private MeNetworkEnergyHelper() {
    }

    public interface LocalEnergyBuffer {
        long getLocalEnergy();

        long extractLocal(long amount, Action action, AutomationType automationType);
    }

    public static long extractWithLocalBuffer(MachineEnergyContainer<?> localEnergy, IGrid grid, IActionSource source, long amount,
            Action action, AutomationType automationType) {
        if (amount <= 0) {
            return 0;
        }
        if (automationType != AutomationType.INTERNAL || grid == null) {
            return extractLocal(localEnergy, amount, action, automationType);
        }
        if (MekEnergisticsConfig.preferLocalFe()) {
            long localExtracted = extractLocal(localEnergy, amount, action, automationType);
            return localExtracted + extractNetworkFe(grid, source, amount - localExtracted, action);
        }
        long networkExtracted = extractNetworkFe(grid, source, amount, action);
        return networkExtracted + extractLocal(localEnergy, amount - networkExtracted, action, automationType);
    }

    private static long extractLocal(MachineEnergyContainer<?> localEnergy, long amount, Action action, AutomationType automationType) {
        if (amount <= 0) {
            return 0;
        }
        return localEnergy instanceof LocalEnergyBuffer buffer ? buffer.extractLocal(amount, action, automationType)
                : localEnergy.extract(amount, action, automationType);
    }

    public static long availableWithLocalBuffer(MachineEnergyContainer<?> localEnergy, IGrid grid, IActionSource source) {
        long local = localEnergy instanceof LocalEnergyBuffer buffer ? buffer.getLocalEnergy() : localEnergy.getEnergy();
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

    public static IEnergyContainer recipeEnergyView(MachineEnergyContainer<?> energyContainer, Supplier<IGrid> gridSupplier, IActionSource source) {
        return new NetworkRecipeEnergyView(energyContainer, gridSupplier, source);
    }

    private static long extractAeEnergyAsFe(IGrid grid, long requestedFe, Actionable action) {
        if (requestedFe <= 0) {
            return 0;
        }
        double requestedAe = PowerUnit.FE.convertTo(PowerUnit.AE, requestedFe);
        double extractedAe = grid.getEnergyService().extractAEPower(requestedAe, action, PowerMultiplier.ONE);
        return Math.min(requestedFe, (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, extractedAe)));
    }

    private static final class NetworkRecipeEnergyView implements IEnergyContainer {
        private final MachineEnergyContainer<?> energyContainer;
        private final Supplier<IGrid> gridSupplier;
        private final IActionSource actionSource;

        private NetworkRecipeEnergyView(MachineEnergyContainer<?> energyContainer, Supplier<IGrid> gridSupplier, IActionSource actionSource) {
            this.energyContainer = energyContainer;
            this.gridSupplier = gridSupplier;
            this.actionSource = actionSource;
        }

        @Override
        public long getEnergy() {
            return availableWithLocalBuffer(this.energyContainer, this.gridSupplier.get(), this.actionSource);
        }

        @Override
        public void setEnergy(long energy) {
            this.energyContainer.setEnergy(energy);
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            return extractWithLocalBuffer(this.energyContainer, this.gridSupplier.get(), this.actionSource, amount, action, automationType);
        }

        @Override
        public long getMaxEnergy() {
            return this.energyContainer.getMaxEnergy();
        }

        @Override
        public void onContentsChanged() {
            this.energyContainer.onContentsChanged();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return this.energyContainer.serializeNBT(provider);
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            this.energyContainer.deserializeNBT(provider, nbt);
        }
    }
}
