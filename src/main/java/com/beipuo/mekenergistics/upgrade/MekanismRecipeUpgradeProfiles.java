package com.beipuo.mekenergistics.upgrade;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeInfusionModePolicy;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.mixin.TileEntityAdvancedElectricMachineAccessor;
import com.beipuo.mekenergistics.mixin.TileEntityCombinerAccessor;
import com.beipuo.mekenergistics.mixin.TileEntityMetallurgicInfuserAccessor;
import com.beipuo.mekenergistics.mixin.TileEntityPrecisionSawmillAccessor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityCombiner;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Slot/tank profiles for Mekanism recipe-machine classes that are not electric-machine subclasses. */
public final class MekanismRecipeUpgradeProfiles {
    private static final Map<ResourceLocation, MeMekanismMachine> MACHINES = CompatMachineCatalog.all()
            .filter(spec -> !spec.machine().isFactory())
            .collect(Collectors.toUnmodifiableMap(spec -> spec.sourceBlockId(), spec -> spec.machine(), (left, right) -> left));

    public static MeUpgradeMachineProfile<?> forTile(TileEntityMekanism tile) {
        MeMekanismMachine machine = MACHINES.get(BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()));
        if (machine == null) return null;
        if (tile instanceof TileEntityAdvancedElectricMachine advanced) return advanced(advanced, machine);
        if (tile instanceof TileEntityCombiner combiner) return combiner(combiner, machine);
        if (tile instanceof TileEntityPrecisionSawmill sawmill) return sawmill(sawmill, machine);
        if (tile instanceof TileEntityMetallurgicInfuser infuser) return infuser(infuser, machine);
        return null;
    }

    public static boolean isSupportedBlockItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                net.minecraft.world.level.block.Block.byItem(stack.getItem()));
        return CompatMachineCatalog.findBySourceBlockId(id)
                .map(spec -> !spec.machine().isFactory())
                .orElse(false);
    }

    public static boolean isSupportedFactoryBlockItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                net.minecraft.world.level.block.Block.byItem(stack.getItem()));
        return CompatMachineCatalog.findBySourceBlockId(id)
                .map(spec -> spec.machine().family()
                                == com.beipuo.mekenergistics.compat.catalog.CompatMachineFamily.MEKANISM_FACTORY)
                .orElse(false);
    }

    private static MeUpgradeMachineProfile<TileEntityAdvancedElectricMachine> advanced(
            TileEntityAdvancedElectricMachine tile, MeMekanismMachine machine) {
        return profile(tile, machine,
                candidate -> MeInputLayout.unordered(List.of(
                        MeMachineIoAdapter.itemInput(((TileEntityAdvancedElectricMachineAccessor) candidate).mekenergistics$getInputSlot()),
                        MeMachineIoAdapter.chemicalInput(candidate.chemicalTank),
                        MeMachineIoAdapter.itemInput(((TileEntityAdvancedElectricMachineAccessor) candidate).mekenergistics$getSecondarySlot()))),
                candidate -> List.of(MeMachineIoAdapter.itemOutput(
                        ((TileEntityAdvancedElectricMachineAccessor) candidate).mekenergistics$getOutputSlot())));
    }

    private static MeUpgradeMachineProfile<TileEntityCombiner> combiner(
            TileEntityCombiner tile, MeMekanismMachine machine) {
        return profile(tile, machine,
                candidate -> MeInputLayout.unordered(List.of(
                        MeMachineIoAdapter.itemInput(((TileEntityCombinerAccessor) candidate).mekenergistics$getMainInputSlot()),
                        MeMachineIoAdapter.itemInput(((TileEntityCombinerAccessor) candidate).mekenergistics$getExtraInputSlot()))),
                candidate -> List.of(MeMachineIoAdapter.itemOutput(
                        ((TileEntityCombinerAccessor) candidate).mekenergistics$getOutputSlot())));
    }

    private static MeUpgradeMachineProfile<TileEntityPrecisionSawmill> sawmill(
            TileEntityPrecisionSawmill tile, MeMekanismMachine machine) {
        return profile(tile, machine,
                candidate -> MeInputLayout.unordered(List.of(MeMachineIoAdapter.itemInput(
                        ((TileEntityPrecisionSawmillAccessor) candidate).mekenergistics$getInputSlot()))),
                candidate -> List.of(
                        MeMachineIoAdapter.itemOutput(((TileEntityPrecisionSawmillAccessor) candidate).mekenergistics$getOutputSlot()),
                        MeMachineIoAdapter.itemOutput(((TileEntityPrecisionSawmillAccessor) candidate).mekenergistics$getSecondaryOutputSlot())));
    }

    private static MeUpgradeMachineProfile<TileEntityMetallurgicInfuser> infuser(
            TileEntityMetallurgicInfuser tile, MeMekanismMachine machine) {
        return profile(tile, machine,
                candidate -> MeInfusionModePolicy.isConversionMode(
                                ((MeAeMachine) candidate).getAeOutputMode())
                        ? MeInputLayout.unordered(List.of(MeMachineIoAdapter.itemInput(
                                ((TileEntityMetallurgicInfuserAccessor) candidate).mekenergistics$getInfusionSlot())))
                        : MeInputLayout.unordered(List.of(
                                MeMachineIoAdapter.itemInput(((TileEntityMetallurgicInfuserAccessor) candidate).mekenergistics$getInputSlot()),
                                MeMachineIoAdapter.manualItemInput(((TileEntityMetallurgicInfuserAccessor) candidate).mekenergistics$getInfusionSlot()),
                                MeMachineIoAdapter.chemicalInput(candidate.infusionTank))),
                candidate -> List.of(
                        MeMachineIoAdapter.itemOutput(((TileEntityMetallurgicInfuserAccessor) candidate).mekenergistics$getOutputSlot()),
                        MeMachineIoAdapter.chemicalOutput(candidate.infusionTank)));
    }

    private static <TILE extends TileEntityMekanism> MeUpgradeMachineProfile<TILE> profile(
            TILE tile, MeMekanismMachine machine, Function<TILE, MeInputLayout> inputs,
            Function<TILE, List<? extends MeOutputPort>> outputs) {
        return new MeUpgradeMachineProfile<>(candidate -> candidate == tile, inputs, outputs, machine,
                candidate -> new net.minecraft.world.item.ItemStack(candidate.getBlockState().getBlock()),
                candidate -> candidate.getBlockState().getBlock().getName());
    }

    private MekanismRecipeUpgradeProfiles() {
    }
}
