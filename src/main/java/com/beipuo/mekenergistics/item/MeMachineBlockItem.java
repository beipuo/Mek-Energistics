package com.beipuo.mekenergistics.item;

import java.util.List;
import java.util.Map.Entry;
import mekanism.api.Upgrade;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.key.MekKeyHandler;
import mekanism.client.key.MekanismKeyHandler;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.Attributes.AttributeInventory;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.WorldUtils;
import mekanism.common.util.text.BooleanStateDisplay.YesNo;
import mekanism.common.util.text.UpgradeDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import org.jetbrains.annotations.NotNull;

public class MeMachineBlockItem extends BlockItem {
    public MeMachineBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean placeBlock(@NotNull BlockPlaceContext context, @NotNull BlockState state) {
        AttributeHasBounding hasBounding = Attribute.get(state, AttributeHasBounding.class);
        if (hasBounding == null) {
            return super.placeBlock(context, state);
        }
        return hasBounding.handle(context.getLevel(), context.getClickedPos(), state, context,
                (level, pos, ctx) -> WorldUtils.isValidReplaceableBlock(level, ctx, pos)) && super.placeBlock(context, state);
    }

    @NotNull
    @Override
    public Component getName(@NotNull ItemStack stack) {
        if (getBlock() instanceof MeMekanismMachineBlock block) {
            TextColor color = block.getMachine().nameColor();
            if (color != null) {
                return TextComponentUtil.build(color, super.getName(stack));
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (getBlock() instanceof MeMekanismMachineBlock block) {
            if (MekKeyHandler.isKeyPressed(MekanismKeyHandler.descriptionKey)) {
                tooltip.add(block.getDescription().translate());
            } else if (MekKeyHandler.isKeyPressed(MekanismKeyHandler.detailsKey)) {
                addDetails(block, stack, tooltip);
            } else {
                tooltip.add(MekanismLang.HOLD_FOR_DETAILS.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                        MekanismKeyHandler.detailsKey.getTranslatedKeyMessage()));
                tooltip.add(MekanismLang.HOLD_FOR_DESCRIPTION.translateColored(EnumColor.GRAY, EnumColor.AQUA,
                        MekanismKeyHandler.descriptionKey.getTranslatedKeyMessage()));
            }
        }
    }

    private static void addDetails(MeMekanismMachineBlock block, ItemStack stack, List<Component> tooltip) {
        MeMekanismMachine machine = block.getMachine();
        FactoryType factoryType = machine.factoryType();
        if (factoryType != null && machine.isFactory()) {
            tooltip.add(MekanismLang.FACTORY_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, factoryType));
        }
        StorageUtils.addStoredEnergy(stack, tooltip, false);
        if (Attribute.has(block, AttributeInventory.class) && ContainerType.ITEM.supports(stack)) {
            tooltip.add(MekanismLang.HAS_INVENTORY.translateColored(EnumColor.AQUA, EnumColor.GRAY, YesNo.hasInventory(stack)));
        }
        if (Attribute.has(block, AttributeUpgradeSupport.class)) {
            UpgradeAware upgradeAware = stack.get(MekanismDataComponents.UPGRADES);
            if (upgradeAware != null) {
                for (Entry<Upgrade, Integer> entry : upgradeAware.upgrades().entrySet()) {
                    tooltip.add(UpgradeDisplay.of(entry.getKey(), entry.getValue()).getTextComponent());
                }
            }
        }
    }
}
