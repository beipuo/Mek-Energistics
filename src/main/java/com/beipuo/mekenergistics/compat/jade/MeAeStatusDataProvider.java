package com.beipuo.mekenergistics.compat.jade;

import com.beipuo.mekenergistics.blockentity.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.MeFactoryAeMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public class MeAeStatusDataProvider implements IServerDataProvider<BlockAccessor> {
    static final MeAeStatusDataProvider INSTANCE = new MeAeStatusDataProvider();
    static final String TAG_AE_ONLINE = "MekEnergisticsAeOnline";

    @Override
    public ResourceLocation getUid() {
        return MekEnergisticsJadePlugin.AE_STATUS;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof MeAeMachine machine) {
            data.putBoolean(TAG_AE_ONLINE, machine.getGrid() != null);
        } else if (blockEntity instanceof MeFactoryAeMachine machine) {
            data.putBoolean(TAG_AE_ONLINE, machine.getGrid() != null);
        }
    }
}
