package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.MekEnergistics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MekEnergistics.MODID, dist = Dist.CLIENT)
public final class MekEnergisticsClient {
    public MekEnergisticsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
