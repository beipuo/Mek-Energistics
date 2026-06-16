package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.inventories.InternalInventory;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.items.tools.MemoryCardItem;
import appeng.util.InteractionUtil;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MeMemoryCardSettings {
    private MeMemoryCardSettings() {
    }

    public static ItemInteractionResult use(ItemStack heldItem, Level level, Player player, BlockEntity blockEntity) {
        if (!(heldItem.getItem() instanceof IMemoryCard memoryCard) || !(blockEntity instanceof PatternContainer patternContainer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!isMePatternContainer(patternContainer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (InteractionUtil.isInAlternateUseMode(player)) {
                DataComponentMap settings = exportSettings(patternContainer);
                if (!settings.isEmpty()) {
                    MemoryCardItem.clearCard(heldItem);
                    heldItem.applyComponents(settings);
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
                }
            } else {
                Component source = heldItem.get(AEComponents.EXPORTED_SETTINGS_SOURCE);
                if (source != null && source.equals(sourceName(patternContainer))) {
                    importSettings(patternContainer, heldItem.getComponents(), player);
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                } else {
                    memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static DataComponentMap exportSettings(PatternContainer patternContainer) {
        DataComponentMap.Builder builder = DataComponentMap.builder();
        builder.set(AEComponents.EXPORTED_SETTINGS_SOURCE, sourceName(patternContainer));
        builder.set(AEComponents.EXPORTED_PATTERNS, toPatternContents(patternContainer.getTerminalPatternInventory()));
        return builder.build();
    }

    private static void importSettings(PatternContainer patternContainer, DataComponentMap input, Player player) {
        importPatterns(patternContainer, input, player);
        if (patternContainer instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    private static ItemContainerContents toPatternContents(InternalInventory inventory) {
        AppEngInternalInventory exported = new AppEngInternalInventory(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) {
            exported.setItemDirect(slot, inventory.getStackInSlot(slot).copy());
        }
        return exported.toItemContainerContents();
    }

    private static void importPatterns(PatternContainer patternContainer, DataComponentMap input, Player player) {
        InternalInventory patternInventory = patternContainer.getTerminalPatternInventory();
        ItemContainerContents patterns = input.getOrDefault(AEComponents.EXPORTED_PATTERNS, ItemContainerContents.EMPTY);
        clearPatternInventory(patternInventory, player);

        AppEngInternalInventory desiredPatterns = new AppEngInternalInventory(patternInventory.size());
        desiredPatterns.fromItemContainerContents(patterns);

        int blankPatternsAvailable = player.getAbilities().instabuild ? Integer.MAX_VALUE : player.getInventory().countItem(AEItems.BLANK_PATTERN.asItem());
        int blankPatternsUsed = 0;
        for (int slot = 0; slot < desiredPatterns.size(); slot++) {
            ItemStack desired = desiredPatterns.getStackInSlot(slot);
            if (desired.isEmpty()) {
                continue;
            }
            var pattern = MePatternDecodeHelper.safeDecode(desired, player.level(), "memory card import");
            if (pattern == null) {
                continue;
            }
            blankPatternsUsed++;
            if (blankPatternsAvailable >= blankPatternsUsed) {
                ItemStack remainder = patternInventory.addItems(pattern.getDefinition().toStack());
                if (!remainder.isEmpty()) {
                    blankPatternsUsed--;
                }
            }
        }

        if (blankPatternsUsed > 0 && !player.getAbilities().instabuild) {
            new PlayerInternalInventory(player.getInventory()).removeItems(blankPatternsUsed, AEItems.BLANK_PATTERN.stack(), null);
        }
        if (blankPatternsUsed > blankPatternsAvailable) {
            player.sendSystemMessage(PlayerMessages.MissingBlankPatterns.text(blankPatternsUsed - blankPatternsAvailable));
        }
    }

    private static void clearPatternInventory(InternalInventory patternInventory, Player player) {
        if (player.getAbilities().instabuild) {
            patternInventory.clear();
            return;
        }
        int blankPatternCount = 0;
        for (int slot = 0; slot < patternInventory.size(); slot++) {
            ItemStack pattern = patternInventory.getStackInSlot(slot);
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.is(AEItems.CRAFTING_PATTERN.asItem())
                    || pattern.is(AEItems.PROCESSING_PATTERN.asItem())
                    || pattern.is(AEItems.SMITHING_TABLE_PATTERN.asItem())
                    || pattern.is(AEItems.STONECUTTING_PATTERN.asItem())
                    || pattern.is(AEItems.BLANK_PATTERN.asItem())) {
                blankPatternCount += pattern.getCount();
            } else {
                player.getInventory().placeItemBackInInventory(pattern);
            }
            patternInventory.setItemDirect(slot, ItemStack.EMPTY);
        }
        if (blankPatternCount > 0) {
            player.getInventory().placeItemBackInInventory(AEItems.BLANK_PATTERN.stack(blankPatternCount), false);
        }
    }

    private static Component sourceName(PatternContainer patternContainer) {
        if (patternContainer instanceof MeAeMachine meAeMachine) {
            return Component.translatable(meAeMachine.getMachine().translationKey());
        }
        if (patternContainer instanceof MeFactoryAeMachine factoryAeMachine) {
            return Component.translatable(factoryAeMachine.getMachine().translationKey());
        }
        if (patternContainer instanceof BlockEntity blockEntity) {
            MeMekanismMachine machine = ModBlocks.getMachine(blockEntity.getBlockState().getBlock());
            return machine == null ? blockEntity.getBlockState().getBlock().asItem().getDescription() : Component.translatable(machine.translationKey());
        }
        return Component.empty();
    }

    private static boolean isMePatternContainer(PatternContainer patternContainer) {
        return patternContainer instanceof MeAeMachine || patternContainer instanceof MeFactoryAeMachine;
    }
}
