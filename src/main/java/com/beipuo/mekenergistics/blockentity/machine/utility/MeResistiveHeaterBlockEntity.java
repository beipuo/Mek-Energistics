package com.beipuo.mekenergistics.blockentity.machine.utility;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeResistiveHeaterBlockEntity extends TileEntityResistiveHeater {
    public MeResistiveHeaterBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }
}
