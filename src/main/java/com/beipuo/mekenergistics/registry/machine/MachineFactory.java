package com.beipuo.mekenergistics.registry.machine;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface MachineFactory<TILE extends TileEntityMekanism> {
    TILE create(MeMekanismMachine machine, BlockPos pos, BlockState state);
}
