package com.beipuo.mekenergistics.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.MEStorage;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MeRecipeMachineAeSupport<TILE extends TileEntityMekanism & MeAeMachine & ICraftingProvider & IActionHost> {
    public static final int PATTERN_SLOTS = 9;

    private final TILE owner;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<BasicInventorySlot> patternSlots = new ArrayList<>(PATTERN_SLOTS);
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private int patternPriority;

    public MeRecipeMachineAeSupport(TILE owner) {
        this.owner = owner;
        this.actionSource = IActionSource.ofMachine(owner);
        this.mainNode = GridHelper.createManagedNode(owner, NodeListener.INSTANCE)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, owner);
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            int x = -54 + i % 3 * 18;
            int y = 17 + i / 3 * 18;
            this.patternSlots.add(BasicInventorySlot.at(PatternDetailsHelper::isEncodedPattern, this::updatePatterns, x, y, 1));
        }
    }

    public IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    public List<BasicInventorySlot> getPatternSlots() {
        return Collections.unmodifiableList(this.patternSlots);
    }

    public List<IPatternDetails> getAvailablePatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    public int getPatternPriority() {
        return this.patternPriority;
    }

    public void create(Level level, BlockPos pos) {
        this.mainNode.create(level, pos);
        updatePatterns();
    }

    public void destroy() {
        this.mainNode.destroy();
    }

    public IGrid getGrid() {
        IGridNode node = this.mainNode.getNode();
        return node == null || !node.isActive() ? null : node.getGrid();
    }

    public boolean insertOutputSlotIntoNetwork(OutputInventorySlot outputSlot, MeMekanismMachineBlockEntity.AeOutputMode mode) {
        return mode.items() && insertOutputStackIntoNetwork(outputSlot);
    }

    public boolean insertOutputSlotsIntoNetwork(MeMekanismMachineBlockEntity.AeOutputMode mode, OutputInventorySlot... outputSlots) {
        if (!mode.items()) {
            return false;
        }
        boolean changed = false;
        for (OutputInventorySlot outputSlot : outputSlots) {
            changed |= insertOutputStackIntoNetwork(outputSlot);
        }
        return changed;
    }

    public boolean insertChemicalTankIntoNetwork(IChemicalTank tank, MeMekanismMachineBlockEntity.AeOutputMode mode) {
        if (!mode.chemicals() || tank == null || tank.isEmpty()) {
            return false;
        }
        ChemicalStack stack = tank.getStack();
        MekanismKey key = MekanismKey.of(stack);
        if (key == null) {
            return false;
        }
        long inserted = insertIntoNetwork(key, stack.getAmount());
        if (inserted <= 0) {
            return false;
        }
        tank.shrinkStack(inserted, Action.EXECUTE);
        this.owner.setChanged();
        return true;
    }

    public boolean insertFluidTankIntoNetwork(IExtendedFluidTank tank, MeMekanismMachineBlockEntity.AeOutputMode mode) {
        if (!mode.items() || tank == null || tank.isEmpty()) {
            return false;
        }
        FluidStack stack = tank.getFluid();
        AEFluidKey key = AEFluidKey.of(stack);
        if (key == null) {
            return false;
        }
        long inserted = insertIntoNetwork(key, stack.getAmount());
        if (inserted <= 0) {
            return false;
        }
        tank.shrinkStack((int) Math.min(Integer.MAX_VALUE, inserted), Action.EXECUTE);
        this.owner.setChanged();
        return true;
    }

    private boolean insertOutputStackIntoNetwork(OutputInventorySlot outputSlot) {
        ItemStack output = outputSlot.getStack();
        if (output.isEmpty()) {
            return false;
        }
        long inserted = insertIntoNetwork(output);
        if (inserted <= 0) {
            return false;
        }
        output.shrink((int) inserted);
        outputSlot.setStack(output.isEmpty() ? ItemStack.EMPTY : output);
        this.owner.setChanged();
        return true;
    }

    private long insertIntoNetwork(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        MEStorage storage = getNetworkStorage();
        return storage == null ? 0 : storage.insert(key, stack.getCount(), Actionable.MODULATE, this.actionSource);
    }

    private long insertIntoNetwork(AEKey key, long amount) {
        MEStorage storage = getNetworkStorage();
        return storage == null || key == null || amount <= 0 ? 0 : storage.insert(key, amount, Actionable.MODULATE, this.actionSource);
    }

    private MEStorage getNetworkStorage() {
        IGrid grid = getGrid();
        IStorageService storageService = grid == null ? null : grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
    }

    public void updatePatterns() {
        this.patterns.clear();
        for (BasicInventorySlot patternSlot : this.patternSlots) {
            ItemStack stack = patternSlot.getStack();
            if (!stack.isEmpty()) {
                IPatternDetails pattern = PatternDetailsHelper.decodePattern(stack, this.owner.getLevel());
                if (pattern != null) {
                    this.patterns.add(pattern);
                }
            }
        }
        if (this.mainNode.getNode() != null) {
            ICraftingProvider.requestUpdate(this.mainNode);
        }
        this.owner.setChanged();
    }

    public void save(CompoundTag tag) {
        tag.putInt("PatternPriority", this.patternPriority);
        this.mainNode.saveToNBT(tag);
    }

    public void load(CompoundTag tag) {
        this.patternPriority = tag.getInt("PatternPriority");
        this.mainNode.loadFromNBT(tag);
        updatePatterns();
    }

    public void saveSlots(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            tag.put("MePatternSlot" + i, this.patternSlots.get(i).serializeNBT(registries));
        }
    }

    public void loadSlots(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            if (tag.contains("MePatternSlot" + i)) {
                this.patternSlots.get(i).deserializeNBT(registries, tag.getCompound("MePatternSlot" + i));
            }
        }
        updatePatterns();
    }

    public static final class AeBackedEnergyContainer<TILE extends TileEntityMekanism> extends MachineEnergyContainer<TILE> {
        private final MeRecipeMachineAeSupport<?> support;

        public AeBackedEnergyContainer(TILE owner, MeRecipeMachineAeSupport<?> support, IContentsListener listener) {
            super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                    BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
            this.support = support;
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            long localExtracted = super.extract(amount, action, automationType);
            long remaining = amount - localExtracted;
            if (remaining <= 0 || automationType != AutomationType.INTERNAL) {
                return localExtracted;
            }
            return localExtracted + extractAeAsFe(remaining, action);
        }

        private long extractAeAsFe(long requestedFe, Action action) {
            IGrid grid = this.support.getGrid();
            if (grid == null) {
                return 0;
            }
            double requestedAe = PowerUnit.FE.convertTo(PowerUnit.AE, requestedFe);
            double extractedAe = grid.getEnergyService().extractAEPower(requestedAe,
                    action.execute() ? Actionable.MODULATE : Actionable.SIMULATE,
                    appeng.api.config.PowerMultiplier.ONE);
            return Math.min(requestedFe, (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, extractedAe)));
        }
    }

    public static final class RecipeEnergyView implements IEnergyContainer {
        private final AeBackedEnergyContainer<?> energyContainer;

        public RecipeEnergyView(AeBackedEnergyContainer<?> energyContainer) {
            this.energyContainer = energyContainer;
        }

        @Override
        public long getEnergy() {
            long local = this.energyContainer.getEnergy();
            long needed = this.energyContainer.getMaxEnergy() - local;
            if (needed <= 0) {
                return local;
            }
            return Math.min(this.energyContainer.getMaxEnergy(), local + this.energyContainer.extractAeAsFe(needed, Action.SIMULATE));
        }

        @Override
        public void setEnergy(long energy) {
            this.energyContainer.setEnergy(energy);
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            return this.energyContainer.extract(amount, action, automationType);
        }

        @Override
        public long getMaxEnergy() {
            return this.energyContainer.getMaxEnergy();
        }

        @Override
        public void onContentsChanged() {
            this.energyContainer.onContentsChanged();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return this.energyContainer.serializeNBT(provider);
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            this.energyContainer.deserializeNBT(provider, nbt);
        }
    }

    private enum NodeListener implements IGridNodeListener<TileEntityMekanism> {
        INSTANCE;

        @Override
        public void onSaveChanges(TileEntityMekanism nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }
    }
}
