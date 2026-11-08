package com.beipuo.mekenergistics.event;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import com.beipuo.mekenergistics.compat.extendedae.ExtendedAeRenamerCompat;
import com.beipuo.mekenergistics.item.MeInstallerUpgradeHandler;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import appeng.core.definitions.AEItems;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tags.MekanismTags;
import mekanism.common.tile.base.TileEntityMekanism;
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
        if (!event.getEntity().isShiftKeyDown()
                && OptionalCompatClasses.hasExtendedAe()
                && isQuartzCuttingKnife(stack)
                && event.getLevel().getBlockEntity(event.getPos()) instanceof MeUpgradeableMachine machine
                && machine.isMeUpgradeActive()
                && event.getLevel().getBlockEntity(event.getPos()) instanceof TileEntityMekanism tile) {
            if (event.getLevel().isClientSide) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (ExtendedAeRenamerCompat.openRenamer(event.getEntity(), tile)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }
        if (stack.is(MekanismTags.Items.CONFIGURATORS) && !stack.is(MekanismItems.CONFIGURATOR)
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MeMekanismMachineBlock) {
            event.setUseBlock(TriState.TRUE);
        }
        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (!event.getLevel().isClientSide && stack.getItem() instanceof MeTierInstallerItem) {
            InteractionResult result = MeTierInstallerItem.tryInstall(stack, event.getLevel(), event.getPos(), event.getEntity());
            // A FAIL is final too: letting the event continue would run the same installer again
            // through Item#useOn and show its error message twice.
            if (result.consumesAction() || result == InteractionResult.FAIL) {
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
        if (result.consumesAction() || result == ItemInteractionResult.FAIL) {
            event.setCanceled(true);
            event.setCancellationResult(result.result());
        }
    }

    private static boolean isQuartzCuttingKnife(ItemStack stack) {
        return stack.is(AEItems.CERTUS_QUARTZ_KNIFE.asItem())
                || stack.is(AEItems.NETHER_QUARTZ_KNIFE.asItem());
    }
}
