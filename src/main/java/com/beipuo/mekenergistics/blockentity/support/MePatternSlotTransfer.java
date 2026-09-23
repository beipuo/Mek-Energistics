package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class MePatternSlotTransfer {
    private static final String TAG_PATTERN_SLOT = "MePatternSlot";
    private static final String TAG_ITEM_DATA = "MekEnergistics";
    private static final String TAG_ITEM_PATTERN_SLOTS = "PatternSlots";
    private static final String TAG_ITEM_ME_STATE = "MeState";

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

    public static CompoundTag saveMeState(BlockEntity tile, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (tile instanceof MeFactoryAeMachine machine) {
            machine.getAeSupport().saveAll(tag, registries);
        } else if (tile instanceof MeUpgradeableMachine machine) {
            machine.saveMeState(tag, registries);
        }
        return tag;
    }

    public static void loadMeState(BlockEntity tile, HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.isEmpty()) {
            return;
        }
        if (tile instanceof MeFactoryAeMachine machine) {
            machine.getAeSupport().loadAll(tag, registries);
        } else if (tile instanceof MeUpgradeableMachine machine) {
            machine.loadMeState(tag, registries);
        }
    }

    public static void saveToItemStack(BlockEntity tile, HolderLookup.Provider registries, ItemStack stack) {
        CompoundTag blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA) == null
                ? new CompoundTag()
                : stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        CompoundTag transferData = new CompoundTag();
        transferData.put(TAG_ITEM_PATTERN_SLOTS, save(tile, registries));
        transferData.put(TAG_ITEM_ME_STATE, saveMeState(tile, registries));
        blockEntityData.put(TAG_ITEM_DATA, transferData);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityData));
    }

    public static void loadFromItemStack(BlockEntity tile, HolderLookup.Provider registries, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) {
            return;
        }
        CompoundTag transferData = customData.copyTag().getCompound(TAG_ITEM_DATA);
        if (transferData.isEmpty()) {
            return;
        }
        load(tile, registries, transferData.getCompound(TAG_ITEM_PATTERN_SLOTS));
        loadMeState(tile, registries, transferData.getCompound(TAG_ITEM_ME_STATE));
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
