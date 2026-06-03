package com.beipuo.mekenergistics.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class MeMekanismMachineAeOutput implements IGridTickable {
    private final MeMekanismMachineBlockEntity owner;
    private final List<PendingCraftingOutput> pendingCraftingOutputs = new ArrayList<>();
    private final List<PendingKeyOutput> pendingKeyOutputs = new ArrayList<>();

    MeMekanismMachineAeOutput(MeMekanismMachineBlockEntity owner) {
        this.owner = owner;
    }

    boolean process() {
        boolean hadWork = hasWork();
        processPendingCraftingOutputs();
        processPendingKeyOutputs();
        insertOutputSlotIntoNetwork(MeMekanismMachineBlockEntity.OUTPUT_SLOT);
        insertOutputSlotIntoNetwork(MeMekanismMachineBlockEntity.SECONDARY_OUTPUT_SLOT);
        insertChemicalTankIntoNetwork();
        boolean hasWork = hasWork();
        if (hasWork) {
            alertTicker();
        }
        return hadWork && !hasWork;
    }

    boolean hasWork() {
        if (!this.pendingCraftingOutputs.isEmpty() || !this.pendingKeyOutputs.isEmpty()) {
            return true;
        }
        if (this.owner.getAeOutputMode().items() && (!this.owner.getStack(MeMekanismMachineBlockEntity.OUTPUT_SLOT).isEmpty()
                || !this.owner.getStack(MeMekanismMachineBlockEntity.SECONDARY_OUTPUT_SLOT).isEmpty())) {
            return true;
        }
        return this.owner.getAeOutputMode().chemicals() && this.owner.getChemicalTank() != null && !this.owner.getChemicalTank().isEmpty();
    }

    private long insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        var key = appeng.api.stacks.AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }

        return insertIntoNetwork(key, stack.getCount());
    }

    private long insertIntoNetwork(AEKey key, long amount) {
        MEStorage storage = getNetworkStorage();
        if (storage == null || key == null || amount <= 0) {
            return 0;
        }
        return storage.insert(key, amount, Actionable.MODULATE, this.owner.getActionSource());
    }

    @Nullable
    private MEStorage getNetworkStorage() {
        IGrid grid = this.owner.getGrid();
        if (grid == null) {
            return null;
        }

        IStorageService storageService = grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
    }

    private void insertOutputSlotIntoNetwork(int slot) {
        if (!this.owner.getAeOutputMode().items()) {
            return;
        }
        ItemStack output = this.owner.getStack(slot);
        if (output.isEmpty()) {
            return;
        }

        long inserted = insertIntoNetwork(output);
        if (inserted <= 0) {
            return;
        }

        output.shrink((int) inserted);
        this.owner.setStack(slot, output.isEmpty() ? ItemStack.EMPTY : output);
    }

    private void insertChemicalTankIntoNetwork() {
        if (!this.owner.getAeOutputMode().chemicals()) {
            return;
        }
        ChemicalStack stack = this.owner.getChemicalStack();
        if (stack.isEmpty()) {
            return;
        }
        MekanismKey key = MekanismKey.of(stack);
        if (key == null) {
            return;
        }
        long inserted = insertIntoNetwork(key, stack.getAmount());
        if (inserted <= 0) {
            return;
        }
        this.owner.getChemicalTank().shrinkStack(inserted, Action.EXECUTE);
        this.owner.setChanged();
    }

    private void processPendingCraftingOutputs() {
        for (int i = this.pendingCraftingOutputs.size() - 1; i >= 0; i--) {
            PendingCraftingOutput pending = this.pendingCraftingOutputs.get(i);
            if (pending.delayTicks-- > 0) {
                continue;
            }

            long inserted = insertIntoNetwork(pending.stack);
            if (inserted > 0) {
                pending.stack.shrink((int) inserted);
            }
            if (!pending.stack.isEmpty()) {
                pending.stack = this.owner.insertItem(MeMekanismMachineBlockEntity.OUTPUT_SLOT, pending.stack);
            }
            if (!pending.stack.isEmpty()) {
                pending.stack = this.owner.insertItem(MeMekanismMachineBlockEntity.SECONDARY_OUTPUT_SLOT, pending.stack);
            }
            if (pending.stack.isEmpty()) {
                this.pendingCraftingOutputs.remove(i);
                this.owner.setChanged();
            }
        }
    }

    private void processPendingKeyOutputs() {
        for (int i = this.pendingKeyOutputs.size() - 1; i >= 0; i--) {
            PendingKeyOutput pending = this.pendingKeyOutputs.get(i);
            if (pending.delayTicks-- > 0) {
                continue;
            }

            long inserted = insertIntoNetwork(pending.key, pending.amount);
            if (inserted > 0) {
                pending.amount -= inserted;
            }
            if (pending.amount <= 0) {
                this.pendingKeyOutputs.remove(i);
                this.owner.setChanged();
            }
        }
    }

    private void alertTicker() {
        this.owner.getManagedNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, !hasWork());
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!this.owner.getManagedNode().isActive()) {
            return TickRateModulation.SLEEP;
        }
        boolean hadWork = hasWork();
        boolean finished = process();
        if (!hasWork()) {
            return TickRateModulation.SLEEP;
        }
        return finished || hadWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
    }

    private static final class PendingCraftingOutput {
        private ItemStack stack;
        private int delayTicks;

        private PendingCraftingOutput(ItemStack stack, int delayTicks) {
            this.stack = stack;
            this.delayTicks = delayTicks;
        }
    }

    private static final class PendingKeyOutput {
        private final AEKey key;
        private long amount;
        private int delayTicks;

        private PendingKeyOutput(AEKey key, long amount, int delayTicks) {
            this.key = key;
            this.amount = amount;
            this.delayTicks = delayTicks;
        }
    }
}
