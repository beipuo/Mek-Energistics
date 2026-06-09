package com.beipuo.mekenergistics.event;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.item.MeInstallerUpgradeHandler;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber(modid = MekEnergistics.MODID)
public final class MeInstallerUpgradeEvents {
    private MeInstallerUpgradeEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!event.getLevel().isClientSide && stack.getItem() instanceof MeTierInstallerItem) {
            InteractionResult result = MeTierInstallerItem.tryInstall(stack, event.getLevel(), event.getPos(), event.getEntity());
            if (result.consumesAction()) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
            return;
        }
        ItemInteractionResult result = MeInstallerUpgradeHandler.tryUpgrade(
                stack,
                event.getLevel().getBlockState(event.getPos()),
                event.getLevel(),
                event.getPos(),
                event.getEntity()
        );
        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(result.result());
        }
    }
}
