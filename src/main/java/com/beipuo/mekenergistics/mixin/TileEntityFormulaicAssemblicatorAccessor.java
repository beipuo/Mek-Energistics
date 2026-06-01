package com.beipuo.mekenergistics.mixin;

import java.util.List;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityFormulaicAssemblicator.class, remap = false)
public interface TileEntityFormulaicAssemblicatorAccessor {
    @Accessor("inputSlots")
    List<IInventorySlot> mekenergistics$getInputSlots();

    @Accessor("outputSlots")
    List<IInventorySlot> mekenergistics$getOutputSlots();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityFormulaicAssemblicator> energyContainer);
}
