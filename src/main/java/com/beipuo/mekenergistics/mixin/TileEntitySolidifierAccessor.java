package com.beipuo.mekenergistics.mixin;

import fr.iglee42.evolvedmekanism.tiles.machine.SolidifierEnergyContainer;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntitySolidifier;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntitySolidifier.class, remap = false)
public interface TileEntitySolidifierAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("inputFluidTank")
    BasicFluidTank mekenergistics$getInputFluidTank();

    @Accessor("inputFluidExtraTank")
    BasicFluidTank mekenergistics$getInputFluidExtraTank();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(SolidifierEnergyContainer energyContainer);
}
