package com.beipuo.mekenergistics.blockentity.compat.mekmm.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.jerry.mekmm.common.tile.machine.TileEntityPlantingStation;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MePlantingStationBlockEntity extends TileEntityPlantingStation implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private MeMekmmItemChemicalMachineSupport<MePlantingStationBlockEntity> meSupport;

    public MePlantingStationBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        return this.meSupport().captureChemicalTank(super.getInitialChemicalTanks(listener, recipeCacheListener, recipeCacheUnpauseListener));
    }

    @Nullable
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        return this.meSupport().withPatternSlots(super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener));
    }

    @Override
    protected boolean onUpdateServer() {
        return this.meSupport().drainOutputs(super.onUpdateServer());
    }

    private MeMekmmItemChemicalMachineSupport<MePlantingStationBlockEntity> meSupport() {
        if (this.meSupport == null) {
            this.meSupport = new MeMekmmItemChemicalMachineSupport<>(this, MeMekanismMachine.PLANTING_STATION);
        }
        return this.meSupport;
    }

    @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return this.meSupport().pushPattern(patternDetails, inputHolder); }
    @Override public boolean isBusy() { return this.meSupport().isBusy(); }
    @Override public List<IPatternDetails> getAvailablePatterns() { return this.meSupport().getAvailablePatterns(); }
    @Override public int getPatternPriority() { return this.meSupport().getPatternPriority(); }
    @Override public String getCustomPatternTerminalName() { return this.meSupport().getCustomPatternTerminalName(); }
    @Override public void setCustomPatternTerminalName(String name) { this.meSupport().setCustomPatternTerminalName(name); }
    @Override public List<BasicInventorySlot> getPatternSlots() { return this.meSupport().getPatternSlots(); }
    @Override public MeMekanismMachine getMachine() { return this.meSupport().getMachine(); }
    @Override public ItemStack getTerminalIconStack() { return this.meSupport().getTerminalIconStack(); }
    @Override public IGrid getGrid() { return this.meSupport().getGrid(); }
    public appeng.api.networking.IManagedGridNode getMainNode() { return this.meSupport().getMainNode(); }
    @Override public void setOwner(ServerPlayer player) { this.meSupport().setOwner(player); }
    @Nullable @Override public IGridNode getGridNode(Direction dir) { return this.meSupport().getGridNode(dir); }
    @Nullable @Override public IGridNode getActionableNode() { return this.meSupport().getActionableNode(); }
    @Override public AeOutputMode getAeOutputMode() { return this.meSupport().getAeOutputMode(); }
    @Override public void cycleAeOutputMode() { this.meSupport().cycleAeOutputMode(); }
    @Override public void clearRemoved() { super.clearRemoved(); this.meSupport().clearRemoved(); }
    @Override public void setRemoved() { this.meSupport().setRemoved(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.meSupport().onChunkUnloaded(); super.onChunkUnloaded(); }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); this.meSupport().addContainerTrackers(container); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.saveAdditional(tag, registries); this.meSupport().saveAdditional(tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.loadAdditional(tag, registries); this.meSupport().loadAdditional(tag, registries); }
}
