package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.client.screen.MeAdvancedElectricMachineScreen;
import com.beipuo.mekenergistics.client.screen.MeCombinerScreen;
import com.beipuo.mekenergistics.client.screen.MeElectricMachineScreen;
import com.beipuo.mekenergistics.client.screen.MeMetallurgicInfuserScreen;
import com.beipuo.mekenergistics.client.screen.MePrecisionSawmillScreen;
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
        event.register(ModMenuTypes.ME_ELECTRIC_MACHINE.get(), MeElectricMachineScreen::new);
        event.register(ModMenuTypes.ME_ADVANCED_ELECTRIC_MACHINE.get(), MeAdvancedElectricMachineScreen::new);
        event.register(ModMenuTypes.ME_METALLURGIC_INFUSER.get(), MeMetallurgicInfuserScreen::new);
        event.register(ModMenuTypes.ME_COMBINER.get(), MeCombinerScreen::new);
        event.register(ModMenuTypes.ME_PRECISION_SAWMILL.get(), MePrecisionSawmillScreen::new);
    }
}
