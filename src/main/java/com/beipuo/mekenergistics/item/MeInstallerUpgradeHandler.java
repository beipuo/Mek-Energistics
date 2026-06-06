package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MePatternSlotTransfer;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.Optional;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class MeInstallerUpgradeHandler {
    private MeInstallerUpgradeHandler() {
    }

    public static ItemInteractionResult tryUpgrade(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.is(MekanismBlocks.BOUNDING_BLOCK)) {
            BlockPos mainPos = BlockBounding.getMainBlockPos(level, pos);
            if (mainPos == null) {
                return ItemInteractionResult.FAIL;
            }
            pos = mainPos;
            state = level.getBlockState(mainPos);
        }
        MeMekanismMachine current = ModBlocks.getMachine(state.getBlock());
        if (current == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        MeMekanismMachine target = getTarget(current, stack);
        if (target == null) {
            MekEnergistics.LOGGER.debug("No ME installer upgrade target for {} using {}", current.registryName(), stack.getItem());
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = upgrade(stack, state, level, pos, player, target);
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }

    @Nullable
    private static MeMekanismMachine getTarget(MeMekanismMachine current, ItemStack stack) {
        if (stack.getItem() instanceof ItemTierInstaller installer) {
            return getMekanismTarget(current, installer.getFromTier(), installer.getToTier());
        }
        if (ModList.get().isLoaded("mekanism_extras")) {
            MeMekanismMachine target = MekanismExtrasCompat.getInstallerTarget(current, stack);
            if (target != null) {
                return target;
            }
        }
        if (ModList.get().isLoaded("emextras")) {
            return EvolvedMekanismExtrasCompat.getInstallerTarget(current, stack);
        }
        return null;
    }

    @Nullable
    private static MeMekanismMachine getMekanismTarget(MeMekanismMachine current, @Nullable BaseTier fromTier, BaseTier toTier) {
        BaseTier currentTier = current.baseTier();
        if (currentTier != fromTier || currentTier == toTier) {
            return null;
        }
        MeMekanismMachine target = fromTier == null ? current.getBasicFactory() : current.getNextFactory();
        return target != null && target.baseTier() == toTier ? target : null;
    }

    private static InteractionResult upgrade(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, MeMekanismMachine target) {
        BlockEntity oldTile = WorldUtils.getTileEntity(level, pos);
        if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, pos, oldTile)) {
            return InteractionResult.FAIL;
        }
        if (oldTile instanceof TileEntityMekanism tileMek && !tileMek.playersUsing.isEmpty()) {
            return InteractionResult.FAIL;
        }
        IUpgradeData upgradeData = null;
        boolean copyComponents = oldTile instanceof TileEntityMekanism;
        if (oldTile instanceof ITierUpgradable tierUpgradable) {
            upgradeData = tierUpgradable.getUpgradeData(level.registryAccess());
            if (upgradeData == null && tierUpgradable.canBeUpgraded()) {
                MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: no upgrade data at {}", target.registryName(), pos);
                return InteractionResult.FAIL;
            }
            copyComponents = upgradeData == null && copyComponents;
        } else if (!copyComponents) {
            MekEnergistics.LOGGER.debug("ME installer target {} at {} is not a Mekanism tile", target.registryName(), pos);
            return InteractionResult.PASS;
        }
        CompoundTag mePatternSlots = MePatternSlotTransfer.save(oldTile, level.registryAccess());
        Block targetBlock = ModBlocks.getMachineBlock(target).get();
        BlockState upgradeState = BlockStateHelper.copyStateData(state, targetBlock.defaultBlockState());
        AttributeHasBounding upgradeBounding = Attribute.get(upgradeState, AttributeHasBounding.class);
        if (upgradeBounding != null && !canPlaceBoundingBlocks(level, pos, upgradeState, upgradeBounding)) {
            MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: bounding blocks cannot be placed at {}",
                    target.registryName(), pos);
            return InteractionResult.FAIL;
        }
        if (!level.setBlockAndUpdate(pos, upgradeState)) {
            MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: setBlockAndUpdate returned false at {}",
                    target.registryName(), pos);
            return InteractionResult.FAIL;
        }
        if (upgradeBounding != null) {
            upgradeBounding.placeBoundingBlocks(level, pos, upgradeState);
        }
        TileEntityMekanism upgradedTile = WorldUtils.getTileEntity(TileEntityMekanism.class, level, pos);
        if (upgradedTile == null) {
            MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: upgraded tile missing at {}",
                    target.registryName(), pos);
            return InteractionResult.FAIL;
        }
        if (oldTile instanceof ITileDirectional directional && directional.isDirectional()) {
            upgradedTile.setFacing(directional.getDirection(), false);
        }
        if (upgradeData != null) {
            upgradedTile.parseUpgradeData(level.registryAccess(), upgradeData);
        } else if (copyComponents) {
            MePatternSlotTransfer.copyMekanismComponents(oldTile, upgradedTile, targetBlock);
        }
        MePatternSlotTransfer.load(upgradedTile, level.registryAccess(), mePatternSlots);
        if (player instanceof ServerPlayer serverPlayer) {
            if (upgradedTile instanceof MeAeMachine machine) {
                machine.setOwner(serverPlayer);
            } else if (upgradedTile instanceof MeFactoryAeMachine machine) {
                machine.setOwner(serverPlayer);
            } else {
                MeOwnerHelper.claimMekanismOwnerIfMissing(upgradedTile, serverPlayer);
            }
        }
        upgradedTile.resyncMasterToBounding();
        upgradedTile.sendUpdatePacket();
        upgradedTile.setChanged();
        upgradedTile.invalidateCapabilitiesFull();
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        MekEnergistics.LOGGER.debug("ME installer upgraded machine to {} at {}", target.registryName(), pos);
        return InteractionResult.CONSUME;
    }

    private static boolean canPlaceBoundingBlocks(Level level, BlockPos pos, BlockState upgradeState, AttributeHasBounding upgradeBounding) {
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
