package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import mekanism.common.tile.machine.TileEntityFluidicPlenisher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeFluidicPlenisherBlockEntity extends TileEntityFluidicPlenisher {
    public MeFluidicPlenisherBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }
}
