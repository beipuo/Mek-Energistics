package com.beipuo.mekenergistics.blockentity.compat.meke.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.compat.shared.MeExternalFactorySupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import java.util.List;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
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

    interface Owner extends MeExternalFactorySupport.Owner {
    }

    static IInventorySlotHolder withPatternSlots(IInventorySlotHolder original, Owner owner) {
        return MeExternalFactorySupport.withPatternSlots(original, owner);
    }

    static boolean pushSingleItem(Owner owner, KeyCounter[] inputHolder) {
        return MeExternalFactorySupport.pushSingleItem(owner, inputHolder);
    }

    static boolean pushSingleItem(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return MeExternalFactorySupport.pushSingleItem(owner, patternDetails, inputHolder);
    }

    static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushSingleItemWithRequiredExtraSlot(owner, inputHolder, extraSlot);
    }

    static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushSingleItemWithRequiredExtraSlot(owner, patternDetails, inputHolder, extraSlot);
    }

    static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushTwoItems(owner, inputHolder, extraSlot);
    }

    static boolean pushTwoItems(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushTwoItems(owner, patternDetails, inputHolder, extraSlot);
    }

    static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemChemical(owner, inputHolder, chemicalTank);
    }

    static boolean pushItemChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemChemical(owner, patternDetails, inputHolder, chemicalTank);
    }

    static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushChemical(owner, inputHolder, chemicalTank);
    }

    static boolean pushChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushChemical(owner, patternDetails, inputHolder, chemicalTank);
    }

    static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushChemical(owner, inputHolder, chemicalTanks);
    }

    static boolean pushChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushChemical(owner, patternDetails, inputHolder, chemicalTanks);
    }

    static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushFluidChemical(owner, inputHolder, fluidTank, chemicalTank);
    }

    static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushFluidChemical(owner, patternDetails, inputHolder, fluidTank, chemicalTank);
    }

    static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushFluidChemical(owner, inputHolder, fluidTank, chemicalTanks);
    }

    static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushFluidChemical(owner, patternDetails, inputHolder, fluidTank, chemicalTanks);
    }

    static boolean pushItemFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemFluidChemical(owner, inputHolder, fluidTank, chemicalTank);
    }

    static boolean pushItemFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemFluidChemical(owner, patternDetails, inputHolder, fluidTank, chemicalTank);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket);
    }

    static boolean drainOutputs(Owner owner) {
        return MeExternalFactorySupport.drainOutputs(owner);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return updateServer(owner, sendUpdatePacket) || owner.getAeSupport().insertChemicalTanksIntoNetwork(outputTanks);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.chemical.IChemicalTank outputTank) {
        return updateServer(owner, sendUpdatePacket) || owner.getAeSupport().insertChemicalTankIntoNetwork(outputTank);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.fluid.IExtendedFluidTank outputTank) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTank);
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

    static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> wrapRecipeEnergy(
            Owner owner, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return MeFactoryAeSupport.withAeRecipeEnergy(owner, energyContainer, cachedRecipe);
    }

    @Nullable
    static IGridNode gridNode(Owner owner, Direction dir) {
        return owner.getMainNode().getNode();
    }

}

