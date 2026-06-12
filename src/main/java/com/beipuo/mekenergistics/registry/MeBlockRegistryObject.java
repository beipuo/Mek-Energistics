package com.beipuo.mekenergistics.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

public record MeBlockRegistryObject<BLOCK extends Block, ITEM extends BlockItem>(
        DeferredBlock<BLOCK> blockHolder,
        DeferredItem<ITEM> itemHolder
) implements ItemLike {

    public @NotNull BLOCK get() {
        return blockHolder.get();
    }

    public @NotNull BlockState defaultState() {
        return get().defaultBlockState();
    }

    @Override
    public @NotNull Item asItem() {
        return itemHolder.get();
    }
}
