package com.beipuo.mekenergistics.event;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.item.MeInstallerUpgradeHandler;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tags.MekanismTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber(modid = MekEnergistics.MODID)
public final class MeInstallerUpgradeEvents {
    private MeInstallerUpgradeEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (stack.is(MekanismTags.Items.CONFIGURATORS) && !stack.is(MekanismItems.CONFIGURATOR)
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MeMekanismMachineBlock) {
            event.setUseBlock(TriState.TRUE);
        }
        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }
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
