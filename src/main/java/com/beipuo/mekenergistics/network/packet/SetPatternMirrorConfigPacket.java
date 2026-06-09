package com.beipuo.mekenergistics.network.packet;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.api.PatternMirrorRole;
import com.beipuo.mekenergistics.blockentity.support.MePatternMirrorSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetPatternMirrorConfigPacket(BlockPos pos, int channel, int role) implements CustomPacketPayload {
    public static final Type<SetPatternMirrorConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "set_pattern_mirror_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPatternMirrorConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetPatternMirrorConfigPacket::pos,
            ByteBufCodecs.VAR_INT, SetPatternMirrorConfigPacket::channel,
            ByteBufCodecs.VAR_INT, SetPatternMirrorConfigPacket::role,
            SetPatternMirrorConfigPacket::new
    );

    @Override
    public Type<SetPatternMirrorConfigPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.blockPosition().closerThan(this.pos, 8)) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(this.pos);
            MePatternMirrorSupport support = null;
            if (blockEntity instanceof MeAeMachine machine) {
                support = machine.getPatternMirrorSupport();
            } else if (blockEntity instanceof MeFactoryAeMachine machine) {
                support = machine.getPatternMirrorSupport();
            }
            if (support != null && support.isChannelCardInstalled()) {
                support.setConfig(this.channel, PatternMirrorRole.byId(this.role));
            }
        });
    }
}
