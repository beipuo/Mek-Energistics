package com.beipuo.mekenergistics.network;

import com.beipuo.mekenergistics.network.packet.CycleAeOutputModePacket;
import com.beipuo.mekenergistics.network.packet.InterfaceConfigSyncPacket;
import com.beipuo.mekenergistics.network.packet.RequestInterfaceConfigPacket;
import com.beipuo.mekenergistics.network.packet.SetAeOutputModePacket;
import com.beipuo.mekenergistics.network.packet.SetInterfaceConfigPacket;
import com.beipuo.mekenergistics.network.packet.SetPatternTerminalNamePacket;
import com.beipuo.mekenergistics.network.packet.SetPassiveCraftingSettingsPacket;
import com.beipuo.mekenergistics.network.packet.SetSmartPatternMultiplicationPacket;
import com.beipuo.mekenergistics.network.packet.SetTerminalVisibilityPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CycleAeOutputModePacket.TYPE, CycleAeOutputModePacket.STREAM_CODEC, CycleAeOutputModePacket::handle);
        registrar.playToServer(RequestInterfaceConfigPacket.TYPE, RequestInterfaceConfigPacket.STREAM_CODEC, RequestInterfaceConfigPacket::handle);
        registrar.playToServer(SetInterfaceConfigPacket.TYPE, SetInterfaceConfigPacket.STREAM_CODEC, SetInterfaceConfigPacket::handle);
        registrar.playToClient(InterfaceConfigSyncPacket.TYPE, InterfaceConfigSyncPacket.STREAM_CODEC,
                InterfaceConfigSyncPacket::handle);
        registrar.playToServer(SetAeOutputModePacket.TYPE, SetAeOutputModePacket.STREAM_CODEC, SetAeOutputModePacket::handle);
        registrar.playToServer(SetPatternTerminalNamePacket.TYPE, SetPatternTerminalNamePacket.STREAM_CODEC, SetPatternTerminalNamePacket::handle);
        registrar.playToServer(SetPassiveCraftingSettingsPacket.TYPE, SetPassiveCraftingSettingsPacket.STREAM_CODEC, SetPassiveCraftingSettingsPacket::handle);
        registrar.playToServer(com.beipuo.mekenergistics.network.packet.RefundPendingPatternsPacket.TYPE,
                com.beipuo.mekenergistics.network.packet.RefundPendingPatternsPacket.STREAM_CODEC,
                com.beipuo.mekenergistics.network.packet.RefundPendingPatternsPacket::handle);
        registrar.playToServer(SetSmartPatternMultiplicationPacket.TYPE, SetSmartPatternMultiplicationPacket.STREAM_CODEC, SetSmartPatternMultiplicationPacket::handle);
        registrar.playToServer(SetTerminalVisibilityPacket.TYPE, SetTerminalVisibilityPacket.STREAM_CODEC, SetTerminalVisibilityPacket::handle);
    }
}
