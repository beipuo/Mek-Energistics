package com.beipuo.mekenergistics.blockentity.machine.utility;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeDimensionalStabilizerBlockEntity extends TileEntityDimensionalStabilizer {
    public MeDimensionalStabilizerBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }
}
