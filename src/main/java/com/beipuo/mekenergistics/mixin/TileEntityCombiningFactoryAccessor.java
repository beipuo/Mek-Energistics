package com.beipuo.mekenergistics.mixin;

import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityCombiningFactory.class, remap = false)
public interface TileEntityCombiningFactoryAccessor {
    @Accessor("extraSlot")
    InputInventorySlot mekenergistics$getExtraSlot();
}
