package com.beipuo.mekenergistics.compat.jade;

import com.beipuo.mekenergistics.MekEnergistics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MekEnergisticsJadePlugin implements IWailaPlugin {
    public static final ResourceLocation AE_STATUS = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "ae_status");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MeAeStatusDataProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent((IComponentProvider<BlockAccessor>) MeAeStatusComponentProvider.INSTANCE, Block.class);
    }
}
