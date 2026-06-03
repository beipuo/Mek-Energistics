package com.beipuo.mekenergistics.item;

import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.util.WorldUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
}
