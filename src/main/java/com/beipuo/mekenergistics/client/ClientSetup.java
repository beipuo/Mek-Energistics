package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.client.screen.MeMekanismMachineScreen;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MekEnergistics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ME_MEKANISM_MACHINE.get(), MeMekanismMachineScreen::new);
    }
}
