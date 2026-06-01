package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeResistiveHeaterBlockEntity extends TileEntityResistiveHeater {
    public MeResistiveHeaterBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }
}
