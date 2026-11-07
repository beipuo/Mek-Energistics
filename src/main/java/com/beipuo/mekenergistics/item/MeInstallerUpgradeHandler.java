package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MePatternSlotTransfer;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.provider.CompatMachineProvider;
import com.beipuo.mekenergistics.compat.provider.CompatMachineProviders;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

public final class MeInstallerUpgradeHandler {
    private MeInstallerUpgradeHandler() {
    }

    public static ItemInteractionResult tryUpgrade(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
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
        if (stack.getItem() instanceof MeTierInstallerItem) {
            return null;
        }
        for (CompatMachineProvider provider : CompatMachineProviders.available().toList()) {
            MeMekanismMachine target = provider.resolveInstallerUpgrade(current, stack);
            if (target != null && CompatMachineCatalog.isAvailable(target)) {
                return target;
            }
        }
        return null;
    }

    /**
     * Replaces the machine at {@code pos} with {@code target}, carrying over Mekanism data, patterns
     * and ME state, and rolling everything back if any step fails. Shared by the ME factory
     * installer and the tier installers so both paths have identical guards and feedback.
     */
    static InteractionResult upgrade(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, MeMekanismMachine target) {
        BlockEntity oldTile = WorldUtils.getTileEntity(level, pos);
        if (oldTile == null) {
            return InteractionResult.PASS;
        }
        // Installers replace the block, so they need build permission (adventure mode, spawn
        // protection and claim mods all hook mayBuild/mayInteract).
        if (!player.mayBuild() || !level.mayInteract(player, pos)) {
            feedback(player, "no_permission");
            return InteractionResult.FAIL;
        }
        if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, pos, oldTile)) {
            return InteractionResult.FAIL;
        }
        if (oldTile instanceof TileEntityMekanism tileMek && !tileMek.playersUsing.isEmpty()) {
            feedback(player, "in_use");
            return InteractionResult.FAIL;
        }
        IUpgradeData upgradeData = null;
        boolean copyComponents = oldTile instanceof TileEntityMekanism;
        if (oldTile instanceof ITierUpgradable tierUpgradable) {
            upgradeData = tierUpgradable.getUpgradeData(level.registryAccess());
            if (upgradeData == null && tierUpgradable.canBeUpgraded()) {
                MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: no upgrade data at {}", target.registryName(), pos);
                feedback(player, "no_upgrade_data");
                return InteractionResult.FAIL;
            }
            copyComponents = upgradeData == null && copyComponents;
        } else if (!copyComponents) {
            MekEnergistics.LOGGER.debug("ME installer target {} at {} is not a Mekanism tile", target.registryName(), pos);
            return InteractionResult.PASS;
        }
        CompoundTag mePatternSlots = MePatternSlotTransfer.save(oldTile, level.registryAccess());
        CompoundTag meState = MePatternSlotTransfer.saveMeState(oldTile, level.registryAccess());
        Block targetBlock = ModBlocks.getMachineBlock(target).get();
        BlockState upgradeState = BlockStateHelper.copyStateData(state, targetBlock.defaultBlockState());
        AttributeHasBounding upgradeBounding = Attribute.get(upgradeState, AttributeHasBounding.class);
        if (upgradeBounding != null && !canPlaceBoundingBlocks(level, pos, upgradeState, upgradeBounding)) {
            MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: bounding blocks cannot be placed at {}",
                    target.registryName(), pos);
            feedback(player, "obstructed");
            return InteractionResult.FAIL;
        }
        BlockState oldState = level.getBlockState(pos);
        CompoundTag oldTileNbt = oldTile.saveWithoutMetadata(level.registryAccess());
        if (!level.setBlockAndUpdate(pos, upgradeState)) {
            MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: setBlockAndUpdate returned false at {}",
                    target.registryName(), pos);
            feedback(player, "failed");
            return InteractionResult.FAIL;
        }
        try {
            if (upgradeBounding != null) {
                upgradeBounding.placeBoundingBlocks(level, pos, upgradeState);
            }
            TileEntityMekanism upgradedTile = WorldUtils.getTileEntity(TileEntityMekanism.class, level, pos);
            if (upgradedTile == null) {
                MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed: upgraded tile missing at {}",
                        target.registryName(), pos);
                feedback(player, "failed");
                return rollbackUpgrade(level, pos, oldState, oldTileNbt, upgradeBounding, upgradeState);
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
            MePatternSlotTransfer.loadMeState(upgradedTile, level.registryAccess(), meState);
            refreshMeLifecycle(upgradedTile);
            if (player instanceof ServerPlayer serverPlayer && MeOwnerHelper.mayClaimOwnership(upgradedTile, serverPlayer)) {
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
        } catch (RuntimeException failure) {
            MekEnergistics.LOGGER.warn("ME installer upgrade to {} failed at {}, restoring the previous machine",
                    target.registryName(), pos, failure);
            feedback(player, "failed");
            return rollbackUpgrade(level, pos, oldState, oldTileNbt, upgradeBounding, upgradeState);
        }
    }

    /** Tells the player why an installer did nothing, on the action bar. */
    static void feedback(Player player, String reason) {
        player.displayClientMessage(Component.translatable("message.mekenergistics.installer." + reason)
                .withStyle(ChatFormatting.RED), true);
    }

    private static InteractionResult rollbackUpgrade(Level level, BlockPos pos, BlockState oldState, CompoundTag oldTileNbt,
            AttributeHasBounding upgradeBounding, BlockState upgradeState) {
        if (upgradeBounding != null) {
            upgradeBounding.removeBoundingBlocks(level, pos, upgradeState);
        }
        level.setBlockAndUpdate(pos, oldState);
        BlockEntity restoredTile = WorldUtils.getTileEntity(level, pos);
        if (restoredTile != null && oldTileNbt != null) {
            restoredTile.loadWithComponents(oldTileNbt, level.registryAccess());
        }
        AttributeHasBounding oldBounding = Attribute.get(oldState, AttributeHasBounding.class);
        if (oldBounding != null) {
            oldBounding.placeBoundingBlocks(level, pos, oldState);
        }
        if (restoredTile instanceof TileEntityMekanism tileMek) {
            tileMek.resyncMasterToBounding();
            tileMek.sendUpdatePacket();
            tileMek.setChanged();
            tileMek.invalidateCapabilitiesFull();
        }
        return InteractionResult.FAIL;
    }

    private static void refreshMeLifecycle(TileEntityMekanism upgradedTile) {
        if (upgradedTile instanceof MeAeMachine machine) {
            machine.getRecipeAeSupport().refreshAfterWorldMutation();
        } else if (upgradedTile instanceof MeFactoryAeMachine machine) {
            machine.getAeSupport().refreshAfterWorldMutation();
        }
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
