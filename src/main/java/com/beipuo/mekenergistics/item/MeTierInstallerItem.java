package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineBaseCompat;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        MeMekanismMachine target = getTargetMachine(state);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (ModBlocks.getMachineBlock(target).get() == state.getBlock()) {
            return InteractionResult.PASS;
        }
        BlockEntity oldTile = WorldUtils.getTileEntity(context.getLevel(), context.getClickedPos());
        if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(context.getPlayer(), context.getLevel(), context.getClickedPos(), oldTile)) {
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
        if (!context.getLevel().setBlockAndUpdate(context.getClickedPos(), upgradeState)) {
            return InteractionResult.FAIL;
        }
        TileEntityMekanism upgradedTile = WorldUtils.getTileEntity(TileEntityMekanism.class, context.getLevel(), context.getClickedPos());
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

    @Nullable
    private static MeMekanismMachine getTargetMachine(BlockState state) {
        if (ModList.get().isLoaded("mekmm")) {
            MeMekanismMachine moreMachineTarget = MekanismMoreMachineBaseCompat.getFactoryTarget(state);
            if (moreMachineTarget != null) {
                return moreMachineTarget;
            }
        }
        AttributeFactoryType attribute = Attribute.get(state, AttributeFactoryType.class);
        if (attribute == null) {
            return getTargetByRegistryName(state);
        }
        FactoryType factoryType = attribute.getFactoryType();
        if (ModList.get().isLoaded("mekanism_extras")) {
            MeMekanismMachine extraTarget = MekanismExtrasCompat.getFactoryTarget(state, factoryType);
            if (extraTarget != null) {
                return extraTarget;
            }
        }
        AttributeTier<?> tier = Attribute.get(state, AttributeTier.class);
        if (tier == null) {
            return MeMekanismMachine.getBaseMachine(factoryType);
        }
        if (tier.tier() instanceof FactoryTier factoryTier) {
            return MeMekanismMachine.getFactory(factoryTier, factoryType);
        }
        MeMekanismMachine directTarget = getTargetByRegistryName(state);
        if (directTarget != null) {
            return directTarget;
        }
        return null;
    }

    @Nullable
    private static MeMekanismMachine getTargetByRegistryName(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (path.startsWith("me_")) {
            return null;
        }
        return MeMekanismMachine.getByRegistryName("me_" + path);
    }
}
