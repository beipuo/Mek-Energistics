package com.beipuo.mekenergistics.network.packet;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSmartPatternMultiplicationPacket(BlockPos pos, boolean enabled) implements CustomPacketPayload {
    public static final Type<SetSmartPatternMultiplicationPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "set_smart_pattern_multiplication"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetSmartPatternMultiplicationPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetSmartPatternMultiplicationPacket::pos,
            ByteBufCodecs.BOOL, SetSmartPatternMultiplicationPacket::enabled,
            SetSmartPatternMultiplicationPacket::new
    );

    @Override
    public Type<SetSmartPatternMultiplicationPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.blockPosition().closerThan(this.pos, 8)) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(this.pos);
            if (blockEntity instanceof MeAeMachine machine) {
                machine.setSmartPatternMultiplicationEnabled(this.enabled);
            } else if (blockEntity instanceof MeFactoryAeMachine machine) {
                machine.setSmartPatternMultiplicationEnabled(this.enabled);
            }
        });
    }
}
