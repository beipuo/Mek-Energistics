package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;

import appeng.api.config.Actionable;
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
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.slot.MePatternInventorySlot;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
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
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
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
    private static final String TAG_PATTERN_TERMINAL_NAME = "PatternTerminalName";

    private final TILE owner;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<BasicInventorySlot> patternSlots = new ArrayList<>(MekEnergisticsConfig.patternSlots());
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private final List<OutputInventorySlot> knownOutputSlots = new ArrayList<>();
    private final List<IChemicalTank> knownChemicalOutputTanks = new ArrayList<>();
    private final List<IExtendedFluidTank> knownFluidOutputTanks = new ArrayList<>();
    private final MeSmartPatternMultiplication smartPatternMultiplication = new MeSmartPatternMultiplication();
    private int patternPriority;
    private String patternTerminalName = "";

    public MeRecipeMachineAeSupport(TILE owner) {
        this.owner = owner;
        this.actionSource = IActionSource.ofMachine(owner);
        this.mainNode = GridHelper.createManagedNode(owner, NodeListener.INSTANCE)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, owner)
                .addService(IGridTickable.class, new AeTicker());
        for (int i = 0; i < MekEnergisticsConfig.patternSlots(); i++) {
            this.patternSlots.add(MePatternInventorySlot.create(PatternDetailsHelper::isEncodedPattern, this::updatePatterns));
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

    public String getPatternTerminalName() {
        return MePatternTerminalNames.get(this.owner, this.patternTerminalName);
    }

    public void setPatternTerminalName(String name) {
        if (!MePatternTerminalNames.set(this.owner, name, this.patternTerminalName)) {
            return;
        }
        this.patternTerminalName = MeAeMachine.sanitizePatternTerminalName(name);
        if (this.mainNode.getNode() != null) {
            ICraftingProvider.requestUpdate(this.mainNode);
        }
    }

    public boolean isSmartPatternMultiplicationEnabled() {
        return this.smartPatternMultiplication.isEnabled();
    }

    public void setSmartPatternMultiplicationEnabled(boolean enabled) {
        if (this.smartPatternMultiplication.isEnabled() == enabled) {
            return;
        }
        this.smartPatternMultiplication.setEnabled(enabled);
        this.owner.setChanged();
        alertAeTicker();
    }

    public boolean enqueueSmartPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!this.smartPatternMultiplication.enqueue(patternDetails, inputHolder)) {
            return false;
        }
        this.owner.setChanged();
        alertAeTicker();
        return true;
    }

    public boolean processSmartPattern(MeSmartPatternMultiplication.Feeder feeder) {
        boolean changed = this.smartPatternMultiplication.processNext(feeder);
        if (changed) {
            this.owner.setChanged();
            if (this.smartPatternMultiplication.hasPendingWork()) {
                alertAeTicker();
            }
        }
        return changed;
    }

    private boolean processSmartPatternViaOwner() {
        boolean wasEnabled = this.smartPatternMultiplication.isEnabled();
        this.smartPatternMultiplication.setEnabled(false);
        try {
            return processSmartPatternWithPattern(this.owner::pushPattern);
        } finally {
            this.smartPatternMultiplication.setEnabled(wasEnabled);
        }
    }

    private boolean processSmartPatternWithPattern(MeSmartPatternMultiplication.PatternFeeder feeder) {
        boolean changed = this.smartPatternMultiplication.processNext(this.patterns, feeder);
        if (changed) {
            this.owner.setChanged();
            if (this.smartPatternMultiplication.hasPendingWork()) {
                alertAeTicker();
            }
        }
        return changed;
    }

    public void create(Level level, BlockPos pos) {
        this.mainNode.create(level, pos);
        updatePatterns();
    }

    public void destroy() {
        this.mainNode.destroy();
    }

    public IGrid getGrid() {
        return this.mainNode.getGrid();
    }

    public boolean insertOutputSlotIntoNetwork(OutputInventorySlot outputSlot, AeOutputMode mode) {
        rememberOutputSlot(outputSlot);
        return mode.items() && insertOutputStackIntoNetwork(outputSlot);
    }

    public boolean insertOutputSlotsIntoNetwork(AeOutputMode mode, OutputInventorySlot... outputSlots) {
        boolean changed = false;
        for (OutputInventorySlot outputSlot : outputSlots) {
            rememberOutputSlot(outputSlot);
            if (mode.items()) {
                changed |= insertOutputStackIntoNetwork(outputSlot);
            }
        }
        return changed;
    }

    public boolean insertChemicalTankIntoNetwork(IChemicalTank tank, AeOutputMode mode) {
        rememberChemicalTank(tank);
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

    public boolean insertFluidTankIntoNetwork(IExtendedFluidTank tank, AeOutputMode mode) {
        rememberFluidTank(tank);
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

    public boolean drainOutputs(AeOutputMode mode, boolean sendUpdatePacket, OutputInventorySlot... outputSlots) {
        return insertOutputSlotsIntoNetwork(mode, outputSlots) || sendUpdatePacket;
    }

    public boolean drainOutputs(AeOutputMode mode, boolean sendUpdatePacket, Iterable<? extends IInventorySlot> outputSlots) {
        boolean changed = false;
        for (IInventorySlot slot : outputSlots) {
            if (slot instanceof OutputInventorySlot outputSlot) {
                changed |= insertOutputSlotIntoNetwork(outputSlot, mode);
            }
        }
        return changed || sendUpdatePacket;
    }

    public boolean drainChemicalOutputs(AeOutputMode mode, boolean sendUpdatePacket, IChemicalTank... tanks) {
        boolean changed = false;
        for (IChemicalTank tank : tanks) {
            changed |= insertChemicalTankIntoNetwork(tank, mode);
        }
        return changed || sendUpdatePacket;
    }

    public boolean drainFluidOutputs(AeOutputMode mode, boolean sendUpdatePacket, IExtendedFluidTank... tanks) {
        boolean changed = false;
        for (IExtendedFluidTank tank : tanks) {
            changed |= insertFluidTankIntoNetwork(tank, mode);
        }
        return changed || sendUpdatePacket;
    }

    public boolean drainMixedOutputs(AeOutputMode mode, boolean sendUpdatePacket, OutputInventorySlot outputSlot, IChemicalTank chemicalTank) {
        boolean changed = insertOutputSlotIntoNetwork(outputSlot, mode);
        changed |= insertChemicalTankIntoNetwork(chemicalTank, mode);
        return changed || sendUpdatePacket;
    }

    public boolean drainMixedOutputs(AeOutputMode mode, boolean sendUpdatePacket, OutputInventorySlot outputSlot, IExtendedFluidTank fluidTank) {
        boolean changed = insertOutputSlotIntoNetwork(outputSlot, mode);
        changed |= insertFluidTankIntoNetwork(fluidTank, mode);
        return changed || sendUpdatePacket;
    }

    public boolean pushSingleItem(KeyCounter[] inputHolder, IInventorySlot inputSlot) {
        if (inputHolder == null || inputHolder.length != 1 || inputSlot == null) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        if (input == null || !input.isItem()) {
            return false;
        }
        ItemStack itemInput = input.item();
        if (!inputSlot.insertItem(itemInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        inputSlot.insertItem(itemInput, Action.EXECUTE, AutomationType.INTERNAL);
        this.owner.setChanged();
        return true;
    }

    private void rememberOutputSlot(OutputInventorySlot outputSlot) {
        if (outputSlot != null && !this.knownOutputSlots.contains(outputSlot)) {
            this.knownOutputSlots.add(outputSlot);
        }
    }

    private void rememberChemicalTank(IChemicalTank tank) {
        if (tank != null && !this.knownChemicalOutputTanks.contains(tank)) {
            this.knownChemicalOutputTanks.add(tank);
        }
    }

    private void rememberFluidTank(IExtendedFluidTank tank) {
        if (tank != null && !this.knownFluidOutputTanks.contains(tank)) {
            this.knownFluidOutputTanks.add(tank);
        }
    }

    private boolean hasAeOutputWork() {
        AeOutputMode mode = this.owner.getAeOutputMode();
        if (mode.items()) {
            for (OutputInventorySlot slot : this.knownOutputSlots) {
                if (slot != null && !slot.getStack().isEmpty()) {
                    return true;
                }
            }
            for (IExtendedFluidTank tank : this.knownFluidOutputTanks) {
                if (tank != null && !tank.isEmpty()) {
                    return true;
                }
            }
        }
        if (mode.chemicals()) {
            for (IChemicalTank tank : this.knownChemicalOutputTanks) {
                if (tank != null && !tank.isEmpty()) {
                    return true;
                }
            }
        }
        return this.smartPatternMultiplication.hasPendingWork();
    }

    private boolean processAeOutputWork() {
        boolean hadWork = hasAeOutputWork();
        processSmartPatternViaOwner();
        AeOutputMode mode = this.owner.getAeOutputMode();
        for (OutputInventorySlot slot : this.knownOutputSlots) {
            insertOutputSlotIntoNetwork(slot, mode);
        }
        for (IChemicalTank tank : this.knownChemicalOutputTanks) {
            insertChemicalTankIntoNetwork(tank, mode);
        }
        for (IExtendedFluidTank tank : this.knownFluidOutputTanks) {
            insertFluidTankIntoNetwork(tank, mode);
        }
        boolean hasWork = hasAeOutputWork();
        if (hasWork) {
            alertAeTicker();
        }
        return hadWork && !hasWork;
    }

    private void alertAeTicker() {
        this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    private boolean insertOutputStackIntoNetwork(OutputInventorySlot outputSlot) {
        if (outputSlot == null) {
            return false;
        }
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
        return insertIntoNetwork(key, stack.getCount());
    }

    private long insertIntoNetwork(AEKey key, long amount) {
        IGrid grid = getGrid();
        MEStorage storage = getNetworkStorage(grid);
        return storage == null || key == null || amount <= 0 ? 0
                : StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, amount, this.actionSource);
    }

    private MEStorage getNetworkStorage() {
        return getNetworkStorage(getGrid());
    }

    private MEStorage getNetworkStorage(IGrid grid) {
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
        this.smartPatternMultiplication.saveConfig(tag);
        tag.remove(TAG_PATTERN_TERMINAL_NAME);
        this.mainNode.saveToNBT(tag);
    }

    public void load(CompoundTag tag) {
        this.patternPriority = tag.getInt("PatternPriority");
        this.smartPatternMultiplication.loadConfig(tag);
        this.patternTerminalName = MePatternTerminalNames.migrateLegacy(this.owner, tag.getString(TAG_PATTERN_TERMINAL_NAME));
        this.mainNode.loadFromNBT(tag);
        updatePatterns();
    }

    public void saveSlots(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            tag.put("MePatternSlot" + i, this.patternSlots.get(i).serializeNBT(registries));
        }
        this.smartPatternMultiplication.savePending(tag, registries);
    }

    public void loadSlots(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            if (tag.contains("MePatternSlot" + i)) {
                this.patternSlots.get(i).deserializeNBT(registries, tag.getCompound("MePatternSlot" + i));
            }
        }
        this.smartPatternMultiplication.loadPending(tag, registries);
        updatePatterns();
    }

    public static final class AeBackedEnergyContainer<TILE extends TileEntityMekanism> extends MachineEnergyContainer<TILE>
            implements MeNetworkEnergyHelper.LocalEnergyBuffer {
        private final MeAeMachine aeMachine;
        private final IActionSource actionSource;

        public AeBackedEnergyContainer(TILE owner, MeRecipeMachineAeSupport<?> support, IContentsListener listener) {
            super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                    BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
            this.aeMachine = (MeAeMachine) owner;
            this.actionSource = IActionSource.ofMachine((IActionHost) owner);
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            return MeNetworkEnergyHelper.extractWithLocalBuffer(this, this.aeMachine.getGrid(), this.actionSource, amount, action, automationType);
        }

        @Override
        public long getLocalEnergy() {
            return super.getEnergy();
        }

        @Override
        public long extractLocal(long amount, Action action, AutomationType automationType) {
            return super.extract(amount, action, automationType);
        }

        private long extractAeAsFe(long requestedFe, Action action) {
            return MeNetworkEnergyHelper.extractNetworkFe(this.aeMachine.getGrid(), this.actionSource, requestedFe, action);
        }
    }

    public static final class RecipeEnergyView implements IEnergyContainer {
        private final MeAeMachine aeMachine;
        private final IActionSource actionSource;
        private final MachineEnergyContainer<?> energyContainer;

        public RecipeEnergyView(AeBackedEnergyContainer<?> energyContainer) {
            this(energyContainer.aeMachine, energyContainer.actionSource, energyContainer);
        }

        public RecipeEnergyView(MeAeMachine aeMachine, IActionSource actionSource, MachineEnergyContainer<?> energyContainer) {
            this.aeMachine = aeMachine;
            this.actionSource = actionSource;
            this.energyContainer = energyContainer;
        }

        @Override
        public long getEnergy() {
            return MeNetworkEnergyHelper.availableWithLocalBuffer(this.energyContainer, this.aeMachine.getGrid(), this.actionSource);
        }

        @Override
        public void setEnergy(long energy) {
            this.energyContainer.setEnergy(energy);
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            return MeNetworkEnergyHelper.extractWithLocalBuffer(this.energyContainer, this.aeMachine.getGrid(), this.actionSource, amount, action, automationType);
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

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return energyContainer instanceof AeBackedEnergyContainer<?> aeBackedEnergyContainer
                ? withAeRecipeEnergy(aeBackedEnergyContainer.aeMachine, aeBackedEnergyContainer.actionSource, energyContainer, cachedRecipe)
                : cachedRecipe;
    }

    public <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> wrapRecipeEnergy(
            MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return withAeRecipeEnergy(this.owner, this.actionSource, energyContainer, cachedRecipe);
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MeAeMachine aeMachine, IActionSource actionSource, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return cachedRecipe.setEnergyRequirements(energyContainer::getEnergyPerTick, new RecipeEnergyView(aeMachine, actionSource, energyContainer));
    }

    private enum NodeListener implements IGridNodeListener<TileEntityMekanism> {
        INSTANCE;

        @Override
        public void onSaveChanges(TileEntityMekanism nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }

        @Override
        public void onStateChanged(TileEntityMekanism nodeOwner, IGridNode node, State state) {
            if (node.isActive()) {
                node.getGrid().getTickManager().alertDevice(node);
            }
        }
    }

    private final class AeTicker implements IGridTickable {
        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(1, 1, !hasAeOutputWork());
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!mainNode.isActive()) {
                return TickRateModulation.SLEEP;
            }
            boolean hadWork = hasAeOutputWork();
            boolean finished = processAeOutputWork();
            if (!hasAeOutputWork()) {
                return TickRateModulation.SLEEP;
            }
            return finished || hadWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }
}
