package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeCombinerBlockEntity extends MeMekanismMachineBlockEntity {
    public MeCombinerBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(machine, pos, state);
    }
}
