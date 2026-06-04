package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.Optional;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MeTierInstallerItem extends Item {
    public MeTierInstallerItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        if (state.is(MekanismBlocks.BOUNDING_BLOCK)) {
            BlockPos mainPos = BlockBounding.getMainBlockPos(context.getLevel(), pos);
            if (mainPos == null) {
                return InteractionResult.FAIL;
            }
            pos = mainPos;
            state = context.getLevel().getBlockState(pos);
        }
        MeMekanismMachine target = MeInstallerTargetResolver.resolve(state);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (ModBlocks.getMachineBlock(target).get() == state.getBlock()) {
            return InteractionResult.PASS;
        }
        BlockEntity oldTile = WorldUtils.getTileEntity(context.getLevel(), pos);
        if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(context.getPlayer(), context.getLevel(), pos, oldTile)) {
            return InteractionResult.FAIL;
        }
        IUpgradeData upgradeData = null;
        if (oldTile instanceof ITierUpgradable tierUpgradable) {
            upgradeData = tierUpgradable.getUpgradeData(context.getLevel().registryAccess());
            if (upgradeData == null && tierUpgradable.canBeUpgraded()) {
                return InteractionResult.FAIL;
            }
        }
        BlockState upgradeState = BlockStateHelper.copyStateData(state, ModBlocks.getMachineBlock(target).get().defaultBlockState());
        AttributeHasBounding upgradeBounding = Attribute.get(upgradeState, AttributeHasBounding.class);
        if (upgradeBounding != null && !canPlaceBoundingBlocks(context.getLevel(), pos, upgradeState, upgradeBounding)) {
            return InteractionResult.FAIL;
        }
        if (!context.getLevel().setBlockAndUpdate(pos, upgradeState)) {
            return InteractionResult.FAIL;
        }
        if (upgradeBounding != null) {
            upgradeBounding.placeBoundingBlocks(context.getLevel(), pos, upgradeState);
        }
        TileEntityMekanism upgradedTile = WorldUtils.getTileEntity(TileEntityMekanism.class, context.getLevel(), pos);
        if (upgradedTile != null) {
            if (oldTile instanceof ITileDirectional directional && directional.isDirectional()) {
                upgradedTile.setFacing(directional.getDirection(), false);
            }
            if (upgradeData != null) {
                upgradedTile.parseUpgradeData(context.getLevel().registryAccess(), upgradeData);
            }
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
                if (upgradedTile instanceof MeAeMachine machine) {
                    machine.setOwner(player);
                } else if (upgradedTile instanceof MeFactoryAeMachine machine) {
                    machine.setOwner(player);
                } else {
                    MeOwnerHelper.claimMekanismOwnerIfMissing(upgradedTile, player);
                }
            }
            upgradedTile.resyncMasterToBounding();
            upgradedTile.sendUpdatePacket();
            upgradedTile.setChanged();
            upgradedTile.invalidateCapabilitiesFull();
        }
        if (!context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean canPlaceBoundingBlocks(net.minecraft.world.level.Level level, BlockPos pos, BlockState upgradeState, AttributeHasBounding upgradeBounding) {
        return upgradeBounding.handle(level, pos, upgradeState, pos, (world, boundingPos, mainPos) -> {
            Optional<BlockState> blockState = WorldUtils.getBlockState(world, boundingPos);
            if (blockState.isEmpty()) {
                return false;
            }
            BlockState current = blockState.get();
            return current.canBeReplaced()
                    || current.is(MekanismBlocks.BOUNDING_BLOCK) && mainPos.equals(BlockBounding.getMainBlockPos(world, boundingPos));
        });
    }

}
