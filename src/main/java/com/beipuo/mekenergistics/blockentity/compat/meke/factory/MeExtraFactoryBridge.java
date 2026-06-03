package com.beipuo.mekenergistics.blockentity.compat.meke.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.compat.shared.MeExternalFactorySupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.List;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MeExtraFactoryBridge {
    private MeExtraFactoryBridge() {
    }

    interface Owner extends MeExtraFactoryAeMachine {
        List<IInventorySlot> meInputSlots();

        List<IInventorySlot> meOutputSlots();

        void unpauseRecipeMonitors();
    }

    static IInventorySlotHolder withPatternSlots(IInventorySlotHolder original, Owner owner) {
        return MeExternalFactorySupport.withPatternSlots(original, adapt(owner));
    }

    static boolean pushSingleItem(Owner owner, KeyCounter[] inputHolder) {
        return MeExternalFactorySupport.pushSingleItem(adapt(owner), inputHolder);
    }

    static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushTwoItems(adapt(owner), inputHolder, extraSlot);
    }

    static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemChemical(adapt(owner), inputHolder, chemicalTank);
    }

    static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushChemical(adapt(owner), inputHolder, chemicalTank);
    }

    static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushChemical(adapt(owner), inputHolder, chemicalTanks);
    }

    static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushFluidChemical(adapt(owner), inputHolder, fluidTank, chemicalTank);
    }

    static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushFluidChemical(adapt(owner), inputHolder, fluidTank, chemicalTanks);
    }

    static boolean pushItemFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemFluidChemical(adapt(owner), inputHolder, fluidTank, chemicalTank);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket) {
        return MeExternalFactorySupport.updateServer(adapt(owner), sendUpdatePacket);
    }

    static boolean drainOutputs(Owner owner) {
        return MeExternalFactorySupport.drainOutputs(adapt(owner));
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return updateServer(owner, sendUpdatePacket) || owner.getAeSupport().insertChemicalTanksIntoNetwork(outputTanks);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.chemical.IChemicalTank outputTank) {
        return updateServer(owner, sendUpdatePacket) || owner.getAeSupport().insertChemicalTankIntoNetwork(outputTank);
    }

    static void createNodeOnFirstTick(TileEntityMekanism tile, MeFactoryAeSupport support, Level level, BlockPos pos) {
        MeExternalFactorySupport.createNodeOnFirstTick(tile, support, level, pos);
    }

    static void save(MeFactoryAeSupport support, CompoundTag tag, HolderLookup.Provider registries) {
        MeExternalFactorySupport.save(support, tag, registries);
    }

    static void load(MeFactoryAeSupport support, CompoundTag tag, HolderLookup.Provider registries) {
        MeExternalFactorySupport.load(support, tag, registries);
    }

    static List<IPatternDetails> getAvailablePatterns(MeFactoryAeSupport support) {
        return MeExternalFactorySupport.getAvailablePatterns(support);
    }

    @Nullable
    static IGridNode gridNode(Owner owner, Direction dir) {
        return owner.getMainNode().getNode();
    }

    private static MeExternalFactorySupport.Owner adapt(Owner owner) {
        return new MeExternalFactorySupport.Owner() {
            @Override public List<IInventorySlot> meInputSlots() { return owner.meInputSlots(); }
            @Override public List<IInventorySlot> meOutputSlots() { return owner.meOutputSlots(); }
            @Override public void unpauseRecipeMonitors() { owner.unpauseRecipeMonitors(); }
            @Override public MeFactoryAeSupport getAeSupport() { return owner.getAeSupport(); }
            @Override public MeMekanismMachine getMachine() { return owner.getMachine(); }
            @Override public Level getOwnerLevel() { return owner.getOwnerLevel(); }
            @Override public List<IPatternDetails> getAvailablePatterns() { return owner.getAvailablePatterns(); }
            @Override public int getPatternPriority() { return owner.getPatternPriority(); }
            @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return owner.pushPattern(patternDetails, inputHolder); }
            @Override public boolean isBusy() { return owner.isBusy(); }
            @Nullable @Override public IGridNode getGridNode(Direction dir) { return owner.getGridNode(dir); }
        };
    }
}
