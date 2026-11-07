package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.common.block.BlockBounding;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MeTierInstallerItem extends Item {
    public MeTierInstallerItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return tryInstall(context.getItemInHand(), context.getLevel(), context.getClickedPos(), context.getPlayer());
    }

    public static InteractionResult tryInstall(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(MekanismBlocks.BOUNDING_BLOCK)) {
            BlockPos mainPos = BlockBounding.getMainBlockPos(level, pos);
            if (mainPos == null) {
                return InteractionResult.FAIL;
            }
            pos = mainPos;
            state = level.getBlockState(pos);
        }
        MeMekanismMachine current = ModBlocks.getMachine(state.getBlock());
        if (current != null) {
            return MeInstallerUpgradeHandler.tryUpgrade(stack, state, level, pos, player).result();
        }
        MeMekanismMachine target = MeInstallerTargetResolver.resolve(state);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (ModBlocks.getMachineBlock(target).get() == state.getBlock()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        return MeInstallerUpgradeHandler.upgrade(stack, state, level, pos, player, target);
    }
}
