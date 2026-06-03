package com.beipuo.mekenergistics.compat.jade;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public class MeAeStatusDataProvider implements IServerDataProvider<BlockAccessor> {
    static final MeAeStatusDataProvider INSTANCE = new MeAeStatusDataProvider();
    static final String TAG_AE_STATE = "MekEnergisticsAeState";

    @Override
    public ResourceLocation getUid() {
        return MekEnergisticsJadePlugin.AE_STATUS;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof MeAeMachine || blockEntity instanceof MeFactoryAeMachine) {
            data.putByte(TAG_AE_STATE, (byte) getAeState(blockEntity).ordinal());
        }
    }

    private static AeState getAeState(BlockEntity blockEntity) {
        if (blockEntity instanceof IActionHost actionHost) {
            IGridNode node = actionHost.getActionableNode();
            if (node != null && node.isPowered()) {
                if (!node.hasGridBooted()) {
                    return AeState.NETWORK_BOOTING;
                }
                if (!node.meetsChannelRequirements()) {
                    return AeState.MISSING_CHANNEL;
                }
                return AeState.ONLINE;
            }
        }
        return AeState.OFFLINE;
    }

    enum AeState {
        OFFLINE,
        NETWORK_BOOTING,
        MISSING_CHANNEL,
        ONLINE
    }
}
