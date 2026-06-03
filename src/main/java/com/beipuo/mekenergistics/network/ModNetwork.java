package com.beipuo.mekenergistics.network;

import com.beipuo.mekenergistics.network.packet.CycleAeOutputModePacket;
import com.beipuo.mekenergistics.network.packet.CycleAeOutputTypePacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CycleAeOutputModePacket.TYPE, CycleAeOutputModePacket.STREAM_CODEC, CycleAeOutputModePacket::handle);
        registrar.playToServer(CycleAeOutputTypePacket.TYPE, CycleAeOutputTypePacket.STREAM_CODEC, CycleAeOutputTypePacket::handle);
    }
}
