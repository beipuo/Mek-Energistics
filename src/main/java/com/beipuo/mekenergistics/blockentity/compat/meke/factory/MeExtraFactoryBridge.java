package com.beipuo.mekenergistics.blockentity.compat.meke.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.compat.shared.MeExternalFactorySupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

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

    static boolean processSingleItemSmartPatterns(Owner owner) {
        return MeExternalFactorySupport.processSingleItemSmartPatterns(owner);
    }

    static boolean finishSingleItemSmartPatterns(Owner owner) {
        return MeExternalFactorySupport.finishSingleItemSmartPatterns(owner);
    }

    static boolean finishSingleItemSmartPatterns(Owner owner, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.finishSingleItemSmartPatterns(owner, outputTanks);
    }

    static boolean finishSingleItemSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank outputTank) {
        return MeExternalFactorySupport.finishSingleItemSmartPatterns(owner, outputTank);
    }

    static boolean processSingleItemSmartPatterns(Owner owner, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.processSingleItemSmartPatterns(owner, outputTanks);
    }

    static boolean processSingleItemSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank outputTank) {
        return MeExternalFactorySupport.processSingleItemSmartPatterns(owner, outputTank);
    }

    static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushSingleItemWithRequiredExtraSlot(owner, inputHolder, extraSlot);
    }

    static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushSingleItemWithRequiredExtraSlot(owner, patternDetails, inputHolder, extraSlot);
    }

    static boolean processSingleItemWithRequiredExtraSlotSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.processSingleItemWithRequiredExtraSlotSmartPatterns(owner, extraSlot);
    }

    static boolean finishSingleItemWithRequiredExtraSlotSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.finishSingleItemWithRequiredExtraSlotSmartPatterns(owner, extraSlot);
    }

    static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushTwoItems(owner, inputHolder, extraSlot);
    }

    static boolean pushTwoItems(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.pushTwoItems(owner, patternDetails, inputHolder, extraSlot);
    }

    static boolean processTwoItemsSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.processTwoItemsSmartPatterns(owner, extraSlot);
    }

    static boolean finishTwoItemsSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return MeExternalFactorySupport.finishTwoItemsSmartPatterns(owner, extraSlot);
    }

    static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemChemical(owner, inputHolder, chemicalTank);
    }

    static boolean pushItemChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemChemical(owner, patternDetails, inputHolder, chemicalTank);
    }

    static boolean processItemChemicalSmartPatterns(Owner owner, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.processItemChemicalSmartPatterns(owner, chemicalTank);
    }

    static boolean finishItemChemicalSmartPatterns(Owner owner, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.finishItemChemicalSmartPatterns(owner, chemicalTank);
    }

    static boolean finishItemChemicalSmartPatterns(Owner owner, mekanism.api.chemical.IChemicalTank chemicalTank,
            java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.finishItemChemicalSmartPatterns(owner, chemicalTank, outputTanks);
    }

    static boolean processItemChemicalSmartPatterns(Owner owner, mekanism.api.chemical.IChemicalTank chemicalTank, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.processItemChemicalSmartPatterns(owner, chemicalTank, outputTanks);
    }

    static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushChemical(owner, inputHolder, chemicalTank);
    }

    static boolean pushChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushChemical(owner, patternDetails, inputHolder, chemicalTank);
    }

    static boolean processChemicalSmartPatterns(Owner owner, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.processChemicalSmartPatterns(owner, chemicalTank);
    }

    static boolean finishChemicalSmartPatterns(Owner owner, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.finishChemicalSmartPatterns(owner, chemicalTank);
    }

    static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushChemical(owner, inputHolder, chemicalTanks);
    }

    static boolean pushChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushChemical(owner, patternDetails, inputHolder, chemicalTanks);
    }

    static boolean processChemicalSmartPatterns(Owner owner, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.processChemicalSmartPatterns(owner, chemicalTanks);
    }

    static boolean finishChemicalSmartPatterns(Owner owner, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.finishChemicalSmartPatterns(owner, chemicalTanks);
    }

    static boolean finishChemicalSmartPatterns(Owner owner, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks,
            java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.finishChemicalSmartPatterns(owner, chemicalTanks, outputTanks);
    }

    static boolean processChemicalSmartPatterns(Owner owner, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.processChemicalSmartPatterns(owner, chemicalTanks, outputTanks);
    }

    static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushFluidChemical(owner, inputHolder, fluidTank, chemicalTank);
    }

    static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushFluidChemical(owner, patternDetails, inputHolder, fluidTank, chemicalTank);
    }

    static boolean processFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.processFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank);
    }

    static boolean finishFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.finishFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank);
    }

    static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushFluidChemical(owner, inputHolder, fluidTank, chemicalTanks);
    }

    static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.pushFluidChemical(owner, patternDetails, inputHolder, fluidTank, chemicalTanks);
    }

    static boolean processFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.processFluidChemicalSmartPatterns(owner, fluidTank, chemicalTanks);
    }

    static boolean finishFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks) {
        return MeExternalFactorySupport.finishFluidChemicalSmartPatterns(owner, fluidTank, chemicalTanks);
    }

    static boolean finishFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank,
            java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.finishFluidChemicalSmartPatterns(owner, fluidTank, chemicalTanks, outputTanks);
    }

    static boolean processFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, java.util.List<mekanism.api.chemical.IChemicalTank> chemicalTanks, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.processFluidChemicalSmartPatterns(owner, fluidTank, chemicalTanks, outputTanks);
    }

    static boolean pushItemFluidChemical(Owner owner, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemFluidChemical(owner, inputHolder, fluidTank, chemicalTank);
    }

    static boolean pushItemFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.pushItemFluidChemical(owner, patternDetails, inputHolder, fluidTank, chemicalTank);
    }

    static boolean processItemFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.processItemFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank);
    }

    static boolean finishItemFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank) {
        return MeExternalFactorySupport.finishItemFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank);
    }

    static boolean finishItemFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank,
            mekanism.api.chemical.IChemicalTank chemicalTank, mekanism.api.chemical.IChemicalTank outputTank) {
        return MeExternalFactorySupport.finishItemFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank, outputTank);
    }

    static boolean processItemFluidChemicalSmartPatterns(Owner owner, mekanism.api.fluid.IExtendedFluidTank fluidTank, mekanism.api.chemical.IChemicalTank chemicalTank, mekanism.api.chemical.IChemicalTank outputTank) {
        return MeExternalFactorySupport.processItemFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank, outputTank);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, java.util.function.BooleanSupplier processor) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, processor);
    }

    static boolean drainOutputs(Owner owner) {
        return MeExternalFactorySupport.drainOutputs(owner);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTanks);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.chemical.IChemicalTank outputTank) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTank);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.fluid.IExtendedFluidTank outputTank) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTank);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, java.util.List<mekanism.api.chemical.IChemicalTank> outputTanks,
            java.util.function.BooleanSupplier processor) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTanks, processor);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.chemical.IChemicalTank outputTank,
            java.util.function.BooleanSupplier processor) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTank, processor);
    }

    static boolean updateServer(Owner owner, boolean sendUpdatePacket, mekanism.api.fluid.IExtendedFluidTank outputTank,
            java.util.function.BooleanSupplier processor) {
        return MeExternalFactorySupport.updateServer(owner, sendUpdatePacket, outputTank, processor);
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

    static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> wrapRecipeEnergy(
            Owner owner, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return MeFactoryAeSupport.withAeRecipeEnergy(owner, energyContainer, cachedRecipe);
    }
}

