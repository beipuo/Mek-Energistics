package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner;
import com.beipuo.mekenergistics.blockentity.support.io.MeInfusionModePolicy;
import com.beipuo.mekenergistics.blockentity.slot.PatternSlotInternalInventory;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class MeFactoryAeSupport extends AbstractMeAeSupport<MeFactoryAeMachine> {
    private final InternalInventory terminalPatternInventory = new PatternSlotInternalInventory(this::getPatternSlots);

    /** Creates the AE-backed energy container used by external factory owners. */
    public static <TILE extends TileEntityMekanism & ISideConfiguration> IEnergyContainerHolder energyContainers(
            TILE tile, IContentsListener listener, Runnable unpauseRecipeMonitors,
            java.util.function.Consumer<AeBackedFactoryEnergyContainer<TILE>> containerSetter) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(tile);
        AeBackedFactoryEnergyContainer<TILE> container = new AeBackedFactoryEnergyContainer<>(tile, () -> {
            listener.onContentsChanged();
            unpauseRecipeMonitors.run();
        });
        containerSetter.accept(container);
        builder.addContainer(container);
        return builder.build();
    }
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeFactoryAeSupport(MeFactoryAeMachine owner) {
        super(owner);
    }

    /** Processes all I/O declared by the factory owner through the common adapter. */
    public boolean processPatternIo(boolean sendUpdatePacket) {
        if (isInterfaceMode()) {
            wakeAeTickerIfIdle();
            return sendUpdatePacket;
        }
        AeOutputMode mode = effectiveOutputMode();
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

    public AeOutputMode getAeOutputMode() {
        return this.aeOutputMode;
    }

    public void cycleAeOutputMode() {
        this.aeOutputMode = this.aeOutputMode.next();
        invalidatePatternIoCache();
        this.owner.saveChanges();
    }

    public void cycleAeOutputMode(mekanism.common.lib.transmitter.TransmissionType type) {
        this.aeOutputMode = this.aeOutputMode.toggle(type);
        invalidatePatternIoCache();
        this.owner.saveChanges();
    }

    public void setAeOutputMode(AeOutputMode aeOutputMode) {
        this.aeOutputMode = aeOutputMode;
        invalidatePatternIoCache();
    }

    public InternalInventory getTerminalPatternInventory() {
        return this.terminalPatternInventory;
    }

    public PatternContainerGroup getTerminalGroup() {
        ItemStack iconStack = new ItemStack(ModBlocks.getMachineBlock(this.owner.getMachine()).get());
        AEItemKey icon = iconStack.isEmpty() ? null : AEItemKey.of(iconStack);
        String terminalName = getPatternTerminalName();
        Component name = terminalName.isBlank()
                ? Component.translatable(this.owner.getMachine().translationKey())
                : Component.literal(terminalName);
        return new PatternContainerGroup(icon, name, List.of());
    }

    @Override
    protected boolean hasAeOutputWork() {
        if (isInterfaceMode()) {
            return hasInterfaceWork();
        }
        if (hasPatternOutputBacklog(effectiveOutputMode())) {
            return true;
        }
        return this.smartPatternMultiplication.hasPendingWork();
    }

    @Override
    protected boolean processAeOutputWork() {
        if (isInterfaceMode()) {
            return processInterfaceMode();
        }
        AeOutputMode mode = effectiveOutputMode();
        boolean changed = drainPatternOutputs(mode);
        if (!hasPatternOutputBacklog(mode)) {
            changed |= processSmartPatternViaAdapter();
        }
        return changed;
    }

    private AeOutputMode effectiveOutputMode() {
        return this.owner instanceof MeFactoryIoOwner factory && factory.isInfusingFactory()
                ? MeInfusionModePolicy.effectiveOutputMode(this.aeOutputMode)
                : this.aeOutputMode;
    }

    @Override
    protected String patternOwnerName() {
        return this.owner.getMachine().name();
    }

    public static IEnergyContainer recipeEnergyView(MachineEnergyContainer<?> energyContainer) {
        return energyContainer instanceof AeBackedFactoryEnergyContainer<?> aeBackedEnergyContainer
                ? recipeEnergyView(aeBackedEnergyContainer.owner instanceof MeFactoryAeMachine aeMachine ? aeMachine : null, aeBackedEnergyContainer)
                : energyContainer;
    }

    public static IEnergyContainer recipeEnergyView(MeFactoryAeMachine owner, MachineEnergyContainer<?> energyContainer) {
        return owner == null ? energyContainer
                : MeNetworkEnergyHelper.recipeEnergyView(energyContainer, () -> owner.getAeSupport().getGrid(), owner.getAeSupport().actionSource);
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return cachedRecipe.setEnergyRequirements(energyContainer::getEnergyPerTick, recipeEnergyView(energyContainer));
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MeFactoryAeMachine owner, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return cachedRecipe.setEnergyRequirements(energyContainer::getEnergyPerTick, recipeEnergyView(owner, energyContainer));
    }

    public void createNodeOnFirstTick(TileEntityMekanism tile) {
        createOnFirstTick();
        if (this.owner instanceof MeFactoryIoOwner factory) {
            factory.unpauseRecipeMonitors();
        }
    }

    public void destroy() {
        destroyNode();
    }

    public void saveAll(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("AeOutputMode", this.aeOutputMode.ordinal());
        saveCommon(tag, registries);
    }

    public void loadAll(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("AeOutputMode")) {
            this.aeOutputMode = AeOutputMode.byId(tag.getInt("AeOutputMode"));
        }
        loadCommon(tag, registries);
    }

    public static final class AeBackedFactoryEnergyContainer<TILE extends TileEntityMekanism>
            extends MeNetworkEnergyHelper.NetworkBackedEnergyContainer<TILE> {
        private final TILE owner;

        public AeBackedFactoryEnergyContainer(TILE owner, IContentsListener listener) {
            super(owner, listener, () -> factoryGrid(owner), () -> factoryActionSource(owner));
            this.owner = owner;
        }
    }

    private static IGrid factoryGrid(TileEntityMekanism owner) {
        MeFactoryAeSupport support = owner instanceof MeFactoryAeMachine aeMachine ? aeMachine.getAeSupport() : null;
        return support == null ? null : support.getGrid();
    }

    private static IActionSource factoryActionSource(TileEntityMekanism owner) {
        MeFactoryAeSupport support = owner instanceof MeFactoryAeMachine aeMachine ? aeMachine.getAeSupport() : null;
        return support == null ? null : support.actionSource;
    }
}
