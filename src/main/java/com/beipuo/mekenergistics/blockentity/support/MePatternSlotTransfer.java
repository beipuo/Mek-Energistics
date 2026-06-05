package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class MePatternSlotTransfer {
    private static final String TAG_PATTERN_SLOT = "MePatternSlot";

    private MePatternSlotTransfer() {
    }

    public static CompoundTag save(BlockEntity tile, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        List<BasicInventorySlot> patternSlots = getPatternSlots(tile);
        if (patternSlots == null) {
            return tag;
        }
        for (int i = 0; i < patternSlots.size(); i++) {
            tag.put(TAG_PATTERN_SLOT + i, patternSlots.get(i).serializeNBT(registries));
        }
        return tag;
    }

    public static void load(BlockEntity tile, HolderLookup.Provider registries, CompoundTag tag) {
        List<BasicInventorySlot> patternSlots = getPatternSlots(tile);
        if (patternSlots == null || tag.isEmpty()) {
            return;
        }
        for (int i = 0; i < patternSlots.size(); i++) {
            String key = TAG_PATTERN_SLOT + i;
            if (tag.contains(key)) {
                patternSlots.get(i).deserializeNBT(registries, tag.getCompound(key));
            }
        }
    }

    public static void dropAndClear(Level level, BlockPos pos, BlockEntity tile) {
        if (level.isClientSide) {
            return;
        }
        List<BasicInventorySlot> patternSlots = getPatternSlots(tile);
        if (patternSlots == null) {
            return;
        }
        for (BasicInventorySlot patternSlot : patternSlots) {
            ItemStack stack = patternSlot.getStack();
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
                patternSlot.setStack(ItemStack.EMPTY);
            }
        }
    }

    public static void copyMekanismComponents(BlockEntity source, TileEntityMekanism target, Block targetBlock) {
        if (!(source instanceof TileEntityUpdateable updateable)) {
            return;
        }
        ItemStack stack = new ItemStack(targetBlock);
        stack.applyComponents(updateable.collectComponents());
        target.applyComponentsFromItemStack(stack);
    }

    @Nullable
    private static List<BasicInventorySlot> getPatternSlots(BlockEntity tile) {
        if (tile instanceof MeAeMachine machine) {
            return machine.getPatternSlots();
        }
        if (tile instanceof MeFactoryAeMachine machine) {
            return machine.getPatternSlots();
        }
        return null;
    }
}
