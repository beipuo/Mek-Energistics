package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import mekanism.common.tile.machine.TileEntityOredictionificator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeOredictionificatorBlockEntity extends TileEntityOredictionificator {
    public MeOredictionificatorBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }
}
