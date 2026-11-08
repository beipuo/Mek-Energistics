package com.beipuo.mekenergistics.network.packet;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeSupportOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RefundPendingPatternsPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RefundPendingPatternsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "refund_pending_patterns"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefundPendingPatternsPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RefundPendingPatternsPacket::pos, RefundPendingPatternsPacket::new);

    @Override
    public Type<RefundPendingPatternsPacket> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerPacketTarget.find(player, pos, MeAeSupportOwner.class)
                        .ifPresent(owner -> owner.getPatternAeSupport().requestPendingPatternRefund());
            }
        });
    }
}
