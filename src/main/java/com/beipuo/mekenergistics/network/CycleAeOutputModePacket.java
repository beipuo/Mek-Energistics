package com.beipuo.mekenergistics.network;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.MeAeMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleAeOutputModePacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<CycleAeOutputModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "cycle_ae_output_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleAeOutputModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CycleAeOutputModePacket::pos,
            CycleAeOutputModePacket::new
    );

    @Override
    public Type<CycleAeOutputModePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(this.pos);
            if (blockEntity instanceof MeAeMachine machine) {
                machine.cycleAeOutputMode();
            }
        });
    }
}
