package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import mekanism.common.tile.machine.TileEntityElectricPump;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MeElectricPumpBlockEntity extends TileEntityElectricPump {
    public MeElectricPumpBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }
}
