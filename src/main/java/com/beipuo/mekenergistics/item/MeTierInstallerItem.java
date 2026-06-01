package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.api.tier.BaseTier;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.WorldUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeTierInstallerItem extends Item {
    @Nullable
    private final BaseTier fromTier;
    @NotNull
    private final BaseTier toTier;

    public MeTierInstallerItem(@Nullable BaseTier fromTier, @NotNull BaseTier toTier, Properties properties) {
        super(properties);
        this.fromTier = fromTier;
        this.toTier = toTier;
    }

    @NotNull
    @Override
    public Component getName(@NotNull ItemStack stack) {
        return TextComponentUtil.build(this.toTier.getColor(), super.getName(stack));
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
        FactoryType factoryType = getFactoryType(state);
        if (factoryType == null) {
            return InteractionResult.PASS;
        }
        BaseTier currentTier = Attribute.getBaseTier(state.getBlockHolder());
        if (currentTier != this.fromTier) {
            return InteractionResult.PASS;
        }
        FactoryTier targetTier = getFactoryTier(this.toTier);
        if (targetTier == null) {
            return InteractionResult.PASS;
        }
        MeMekanismMachine target = MeMekanismMachine.getFactory(targetTier, factoryType);
        if (target == null) {
            return InteractionResult.PASS;
        }
        BlockEntity oldTile = WorldUtils.getTileEntity(context.getLevel(), context.getClickedPos());
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
    private static FactoryType getFactoryType(BlockState state) {
        AttributeFactoryType attribute = Attribute.get(state, AttributeFactoryType.class);
        return attribute == null ? null : attribute.getFactoryType();
    }

    @Nullable
    private static FactoryTier getFactoryTier(BaseTier tier) {
        for (FactoryTier factoryTier : FactoryTier.values()) {
            if (factoryTier.getBaseTier() == tier) {
                return factoryTier;
            }
        }
        return null;
    }
}
