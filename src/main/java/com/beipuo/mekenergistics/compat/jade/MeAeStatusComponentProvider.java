package com.beipuo.mekenergistics.compat.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MeAeStatusComponentProvider implements IComponentProvider<BlockAccessor> {
    static final MeAeStatusComponentProvider INSTANCE = new MeAeStatusComponentProvider();

    @Override
    public ResourceLocation getUid() {
        return MekEnergisticsJadePlugin.AE_STATUS;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(MekEnergisticsJadePlugin.AE_STATUS) || !accessor.getServerData().contains(MeAeStatusDataProvider.TAG_AE_ONLINE)) {
            return;
        }
        boolean online = accessor.getServerData().getBoolean(MeAeStatusDataProvider.TAG_AE_ONLINE);
        tooltip.add(Component.translatable(online ? "tooltip.mekenergistics.ae_status.online" : "tooltip.mekenergistics.ae_status.offline"));
    }
}
