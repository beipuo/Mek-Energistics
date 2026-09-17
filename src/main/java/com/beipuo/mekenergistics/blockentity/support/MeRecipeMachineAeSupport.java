package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;

import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.upgrade.MeMachineMode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class MeRecipeMachineAeSupport<TILE extends TileEntityMekanism & MeAeMachine & ICraftingProvider & IActionHost>
        extends AbstractMeAeSupport<TILE> {
    public MeRecipeMachineAeSupport(TILE owner) {
        super(owner);
    }

    public void addAeTrackers(MekanismContainer container, Supplier<AeOutputMode> outputModeSupplier,
            Consumer<AeOutputMode> outputModeSetter, boolean trackSmartPatternMultiplication) {
        container.track(SyncableBoolean.create(this.owner::hasPassiveCraftingUpgrade,
                this::setClientPassiveCraftingEnabled));
        container.track(SyncableBoolean.create(this::isInterfaceMode, this::setClientInterfaceMode));
        container.track(SyncableInt.create(() -> outputModeSupplier.get().ordinal(), mode -> outputModeSetter.accept(AeOutputMode.byId(mode))));
        if (trackSmartPatternMultiplication) {
            container.track(SyncableBoolean.create(this.owner::isSmartPatternMultiplicationEnabled, this.owner::setSmartPatternMultiplicationEnabled));
        }
        container.track(SyncableBoolean.create(this::isVisibleInPatternAccessTerminal, this::setVisibleInPatternAccessTerminal));
        container.track(SyncableInt.create(() -> getPassiveCraftingSettings().intervalTicks(), value -> getPassiveCraftingSettings().set(value, getPassiveCraftingSettings().multiplier())));
        container.track(SyncableLong.create(() -> getPassiveCraftingSettings().multiplier(), value -> getPassiveCraftingSettings().set(getPassiveCraftingSettings().intervalTicks(), value)));
    }

    /** Drains declared outputs before allowing another smart batch into the machine. */
    public boolean processPatternIo(AeOutputMode mode, boolean sendUpdatePacket) {
        if (isInterfaceMode()) {
            if (hasInterfaceWork()) {
                alertAeTicker();
            }
            return sendUpdatePacket;
        }
        boolean changed = drainPatternOutputs(mode) || sendUpdatePacket;
        if (!hasPatternOutputBacklog(mode)) {
            if (this.owner.hasPassiveCraftingUpgrade()) {
                setSmartPatternMultiplicationEnabled(false);
                changed |= processSmartPatternViaAdapter();
                if (!this.smartPatternMultiplication.hasPendingWork()) {
                    changed |= processPassiveCrafting(true);
                }
            } else {
                changed |= processSmartPatternViaAdapter();
            }
        }
        return changed;
    }

    @Override
    protected boolean hasAeOutputWork() {
        if (isInterfaceMode()) {
            return hasInterfaceWork();
        }
        AeOutputMode mode = this.owner.getAeOutputMode();
        if (hasPatternOutputBacklog(mode)) {
            return true;
        }
        return this.smartPatternMultiplication.hasPendingWork();
    }

    @Override
    protected boolean processAeOutputWork() {
        if (isInterfaceMode()) {
            boolean hadWork = hasInterfaceWork();
            boolean changed = processInterfaceMode();
            boolean hasWork = hasInterfaceWork();
            if (hasWork) {
                alertAeTicker();
            }
            return hadWork && !hasWork;
        }
        boolean hadWork = hasAeOutputWork();
        AeOutputMode mode = this.owner.getAeOutputMode();
        drainPatternOutputs(mode);
        if (!hasPatternOutputBacklog(mode)) {
            processSmartPatternViaAdapter();
        }
        boolean hasWork = hasAeOutputWork();
        if (hasWork) {
            alertAeTicker();
        }
        return hadWork && !hasWork;
    }

    @Override
    protected String patternOwnerName() {
        return this.owner.getBlockState().getBlock().getDescriptionId();
    }

    public void saveAeState(CompoundTag tag, HolderLookup.Provider registries, AeOutputMode aeOutputMode) {
        tag.putInt("AeOutputMode", aeOutputMode.ordinal());
        saveCommon(tag, registries);
    }

    public AeOutputMode loadAeState(CompoundTag tag, HolderLookup.Provider registries) {
        AeOutputMode aeOutputMode = AeOutputMode.byId(tag.getInt("AeOutputMode"));
        loadCommon(tag, registries);
        return aeOutputMode;
    }

    public static final class AeBackedEnergyContainer<TILE extends TileEntityMekanism>
            extends MeNetworkEnergyHelper.NetworkBackedEnergyContainer<TILE> {
        private final MeAeMachine aeMachine;

        public AeBackedEnergyContainer(TILE owner, MeRecipeMachineAeSupport<?> support, IContentsListener listener) {
            super(owner, listener, () -> ((MeAeMachine) owner).getGrid(), () -> recipeActionSource((MeAeMachine) owner));
            this.aeMachine = (MeAeMachine) owner;
        }

        private IActionSource actionSource() {
            return recipeActionSource(this.aeMachine);
        }
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return energyContainer instanceof AeBackedEnergyContainer<?> aeBackedEnergyContainer
                ? withAeRecipeEnergy(aeBackedEnergyContainer.aeMachine, aeBackedEnergyContainer.actionSource(), energyContainer, cachedRecipe)
                : cachedRecipe;
    }

    public <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> wrapRecipeEnergy(
            MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return withAeRecipeEnergy(this.owner, this.actionSource, energyContainer, cachedRecipe);
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MeAeMachine aeMachine, IActionSource actionSource, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        // Bind once even before installation. CachedRecipe survives upgrade changes;
        // deciding at construction time leaves it using the old power source until reload.
        return cachedRecipe.setEnergyRequirements(energyContainer::getEnergyPerTick,
                MeNetworkEnergyHelper.recipeEnergyView(energyContainer, () -> recipeGrid(aeMachine), actionSource));
    }

    private static appeng.api.networking.IGrid recipeGrid(MeAeMachine machine) {
        if (MeAeMachine.modeOf(machine).isOutputInterface()
                || machine instanceof com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine upgradeable
                && upgradeable.isMeUpgradeTarget() && !upgradeable.isMeUpgradeActive()) {
            return null;
        }
        return machine.getGrid();
    }

    private static IActionSource recipeActionSource(MeAeMachine aeMachine) {
        AbstractMeAeSupport<?> support = aeMachine.getRecipeAeSupport();
        return support.actionSource;
    }

}
