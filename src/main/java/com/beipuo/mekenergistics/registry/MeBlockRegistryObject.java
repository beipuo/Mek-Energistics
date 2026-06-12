package com.beipuo.mekenergistics.registry;

import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.IHasTranslationKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

public record MeBlockRegistryObject<BLOCK extends Block, ITEM extends BlockItem>(
        DeferredBlock<BLOCK> blockHolder,
        DeferredItem<ITEM> itemHolder
) implements ItemLike, IHasTextComponent, IHasTranslationKey {

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

    public @NotNull ItemStack asStack() {
        return asStack(1);
    }

    public @NotNull ItemStack asStack(int count) {
        return new ItemStack(asItem(), count);
    }

    @Override
    public @NotNull String getTranslationKey() {
        return get().getDescriptionId();
    }

    @Override
    public @NotNull Component getTextComponent() {
        return get().getName();
    }
}
