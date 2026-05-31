package com.beipuo.mekenergistics;

import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModCreativeTabs;
import com.beipuo.mekenergistics.registry.ModItems;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MekEnergistics.MODID)
public final class MekEnergistics {
    public static final String MODID = "mekenergistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MekEnergistics(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        LOGGER.info("Loading Mek Energistics");
    }
}
