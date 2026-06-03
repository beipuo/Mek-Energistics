package com.beipuo.mekenergistics.network.packet;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleAeOutputTypePacket(BlockPos pos, TransmissionType transmissionType) implements CustomPacketPayload {
    public static final Type<CycleAeOutputTypePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "cycle_ae_output_type"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleAeOutputTypePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CycleAeOutputTypePacket::pos,
            TransmissionType.STREAM_CODEC, CycleAeOutputTypePacket::transmissionType,
            CycleAeOutputTypePacket::new
    );

    @Override
    public Type<CycleAeOutputTypePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(this.pos);
            if (blockEntity instanceof MeAeMachine machine) {
                machine.cycleAeOutputMode(this.transmissionType);
            }
        });
    }
}
