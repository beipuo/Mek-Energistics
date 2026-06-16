package com.beipuo.mekenergistics.mixin;

import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityMelter;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.inventory.slot.InputInventorySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityMelter.class, remap = false)
public interface TileEntityMelterAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("fluidTank")
    IExtendedFluidTank mekenergistics$getFluidTank();
}
