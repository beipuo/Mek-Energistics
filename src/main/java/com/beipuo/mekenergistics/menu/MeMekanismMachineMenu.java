package com.beipuo.mekenergistics.menu;

import appeng.api.crafting.PatternDetailsHelper;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MeMekanismMachineMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final MeMekanismMachineBlockEntity blockEntity;
    private final MeMekanismMachine machine;
    private int machineSlotCount;

    public MeMekanismMachineMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public MeMekanismMachineMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        this(containerId, playerInventory, (MeMekanismMachineBlockEntity) blockEntity);
    }

    public MeMekanismMachineMenu(int containerId, Inventory playerInventory, MeMekanismMachineBlockEntity blockEntity) {
        super(ModMenuTypes.ME_MEKANISM_MACHINE.get(), containerId);
        this.blockEntity = blockEntity;
        this.machine = blockEntity.getMachine();
        addMachineSlots();
        addPlayerInventory(playerInventory);
    }

    private void addMachineSlots() {
        var inventory = this.blockEntity.getInventory();
        addSlot(new SlotItemHandler(inventory, MeMekanismMachineBlockEntity.INPUT_SLOT, 51, 43));
        machineSlotCount++;

        switch (this.machine.slotLayout()) {
            case ITEM_CHEMICAL -> {
                addSlot(new SlotItemHandler(inventory, MeMekanismMachineBlockEntity.SECONDARY_INPUT_SLOT, 17, 35));
                machineSlotCount++;
            }
            case DOUBLE_ITEM -> {
                addSlot(new SlotItemHandler(inventory, MeMekanismMachineBlockEntity.SECONDARY_INPUT_SLOT, 17, 43));
                machineSlotCount++;
            }
            case SAWING, SINGLE_ITEM -> {
            }
        }

        addOutputSlot(MeMekanismMachineBlockEntity.OUTPUT_SLOT, 109, 43);
        machineSlotCount++;
        if (this.machine.hasSecondaryOutput()) {
            addOutputSlot(MeMekanismMachineBlockEntity.SECONDARY_OUTPUT_SLOT, 133, 43);
            machineSlotCount++;
        }

        int slot = MeMekanismMachineBlockEntity.PATTERN_SLOTS_START;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new PatternSlotItemHandler(inventory, slot++, -54 + col * 18, 17 + row * 18));
                machineSlotCount++;
            }
        }
    }

    private void addOutputSlot(int slot, int x, int y) {
        addSlot(new SlotItemHandler(this.blockEntity.getInventory(), slot, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = this.machineSlotCount;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (PatternDetailsHelper.isEncodedPattern(stack)) {
            if (!moveItemStackTo(stack, machineSlots - MeMekanismMachineBlockEntity.PATTERN_SLOTS_COUNT, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, this.machine.hasSecondaryItemInput() || this.machine.hasChemicalInput() ? 2 : 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.blockEntity != null && !this.blockEntity.isRemoved();
    }

    public MeMekanismMachineBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    private static class PatternSlotItemHandler extends SlotItemHandler {
        private PatternSlotItemHandler(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return PatternDetailsHelper.isEncodedPattern(stack);
        }
    }
}
