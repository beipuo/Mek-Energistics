package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.slot.MePatternInventorySlot;
import com.beipuo.mekenergistics.blockentity.slot.PatternSlotInternalInventory;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MeFactoryAeSupport {
    private static final String TAG_PATTERN_TERMINAL_NAME = "PatternTerminalName";

    private final MeFactoryAeMachine owner;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<BasicInventorySlot> patternSlots = new ArrayList<>(MekEnergisticsConfig.patternSlots());
    private final InternalInventory terminalPatternInventory = new PatternSlotInternalInventory(new PatternSlotOwner());
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private final List<IInventorySlot> knownOutputSlots = new ArrayList<>();
    private final List<IChemicalTank> knownChemicalOutputTanks = new ArrayList<>();
    private final List<IExtendedFluidTank> knownFluidOutputTanks = new ArrayList<>();
    private final MeSmartPatternMultiplication smartPatternMultiplication = new MeSmartPatternMultiplication();
    private int patternPriority;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;
    private String patternTerminalName = "";

    public MeFactoryAeSupport(MeFactoryAeMachine owner) {
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

    public IInventorySlotHolder withPatternSlots(IInventorySlotHolder original) {
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(this.patternSlots);
            return slots;
        };
    }

    public List<IPatternDetails> getAvailablePatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    public int getPatternPriority() {
        return this.patternPriority;
    }

    public String getPatternTerminalName() {
        return MePatternTerminalNames.get((TileEntityMekanism) this.owner, this.patternTerminalName);
    }

    public void setPatternTerminalName(String name) {
        TileEntityMekanism tile = (TileEntityMekanism) this.owner;
        if (!MePatternTerminalNames.set(tile, name, this.patternTerminalName)) {
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
        this.owner.saveChanges();
        alertAeTicker();
    }

    public boolean enqueueSmartPattern(IPatternDetails patternDetails, appeng.api.stacks.KeyCounter[] inputHolder) {
        if (!this.smartPatternMultiplication.enqueue(patternDetails, inputHolder)) {
            return false;
        }
        this.owner.saveChanges();
        alertAeTicker();
        return true;
    }

    public boolean processSmartPattern(MeSmartPatternMultiplication.Feeder feeder) {
        boolean changed = this.smartPatternMultiplication.processNext(feeder);
        if (changed) {
            this.owner.saveChanges();
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
            this.owner.saveChanges();
            if (this.smartPatternMultiplication.hasPendingWork()) {
                alertAeTicker();
            }
        }
        return changed;
    }

    public AeOutputMode getAeOutputMode() {
        return this.aeOutputMode;
    }

    public void cycleAeOutputMode() {
        this.aeOutputMode = this.aeOutputMode.next();
        this.owner.saveChanges();
    }

    public void cycleAeOutputMode(mekanism.common.lib.transmitter.TransmissionType type) {
        this.aeOutputMode = this.aeOutputMode.toggle(type);
        this.owner.saveChanges();
    }

    public void setAeOutputMode(AeOutputMode aeOutputMode) {
        this.aeOutputMode = aeOutputMode;
    }

    public InternalInventory getTerminalPatternInventory() {
        return this.terminalPatternInventory;
    }

    public PatternContainerGroup getTerminalGroup() {
        ItemStack iconStack = new ItemStack(ModBlocks.getMachineBlock(this.owner.getMachine()).get());
        AEItemKey icon = iconStack.isEmpty() ? null : AEItemKey.of(iconStack);
        String terminalName = getPatternTerminalName();
        Component name = terminalName.isBlank()
                ? Component.translatable(this.owner.getMachine().translationKey())
                : Component.literal(terminalName);
        return new PatternContainerGroup(icon, name, List.of());
    }

    public IGrid getGrid() {
        return this.mainNode.getGrid();
    }

    public boolean insertOutputSlotsIntoNetwork(List<IInventorySlot> outputSlots) {
        rememberOutputSlots(outputSlots);
        if (!this.aeOutputMode.items()) {
            return false;
        }
        IGrid grid = getGrid();
        MEStorage storage = getNetworkStorage(grid);
        if (storage == null) {
            return false;
        }
        boolean changed = false;
        for (IInventorySlot outputSlot : outputSlots) {
            ItemStack output = outputSlot.getStack();
            if (output.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(output);
            if (key == null) {
                continue;
            }
            long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, output.getCount(), this.actionSource);
            if (inserted > 0) {
                output.shrink((int) inserted);
                outputSlot.setStack(output.isEmpty() ? ItemStack.EMPTY : output);
                changed = true;
            }
        }
        if (changed) {
            this.owner.saveChanges();
        }
        return changed;
    }

    public boolean insertChemicalTanksIntoNetwork(List<IChemicalTank> tanks) {
        rememberChemicalTanks(tanks);
        boolean changed = false;
        for (IChemicalTank tank : tanks) {
            changed |= insertChemicalTankIntoNetwork(tank);
        }
        return changed;
    }

    public boolean insertChemicalTankIntoNetwork(IChemicalTank tank) {
        rememberChemicalTank(tank);
        if (!this.aeOutputMode.chemicals()) {
            return false;
        }
        if (tank == null || tank.isEmpty()) {
            return false;
        }
        ChemicalStack stack = tank.getStack();
        MekanismKey key = MekanismKey.of(stack);
        long inserted = insertIntoNetwork(key, stack.getAmount());
        if (inserted <= 0) {
            return false;
        }
        tank.shrinkStack(inserted, Action.EXECUTE);
        this.owner.saveChanges();
        return true;
    }

    public boolean insertFluidTankIntoNetwork(IExtendedFluidTank tank) {
        rememberFluidTank(tank);
        if (!this.aeOutputMode.chemicals()) {
            return false;
        }
        if (tank == null || tank.isEmpty()) {
            return false;
        }
        FluidStack stack = tank.getFluid();
        AEFluidKey key = AEFluidKey.of(stack);
        long inserted = insertIntoNetwork(key, stack.getAmount());
        if (inserted <= 0) {
            return false;
        }
        tank.shrinkStack((int) Math.min(Integer.MAX_VALUE, inserted), Action.EXECUTE);
        this.owner.saveChanges();
        return true;
    }

    private void rememberOutputSlots(List<IInventorySlot> outputSlots) {
        for (IInventorySlot outputSlot : outputSlots) {
            if (outputSlot != null && !this.knownOutputSlots.contains(outputSlot)) {
                this.knownOutputSlots.add(outputSlot);
            }
        }
    }

    private void rememberChemicalTanks(List<IChemicalTank> tanks) {
        for (IChemicalTank tank : tanks) {
            rememberChemicalTank(tank);
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
        for (IInventorySlot outputSlot : this.knownOutputSlots) {
            if (outputSlot != null && !outputSlot.getStack().isEmpty()) {
                return true;
            }
        }
        for (IChemicalTank tank : this.knownChemicalOutputTanks) {
            if (tank != null && !tank.isEmpty()) {
                return true;
            }
        }
        for (IExtendedFluidTank tank : this.knownFluidOutputTanks) {
            if (tank != null && !tank.isEmpty()) {
                return true;
            }
        }
        return this.smartPatternMultiplication.hasPendingWork();
    }

    private boolean processAeOutputWork() {
        boolean hadWork = hasAeOutputWork();
        processSmartPatternViaOwner();
        insertOutputSlotsIntoNetwork(this.knownOutputSlots);
        for (IChemicalTank tank : this.knownChemicalOutputTanks) {
            insertChemicalTankIntoNetwork(tank);
        }
        for (IExtendedFluidTank tank : this.knownFluidOutputTanks) {
            insertFluidTankIntoNetwork(tank);
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

    private MEStorage getNetworkStorage() {
        return getNetworkStorage(getGrid());
    }

    private MEStorage getNetworkStorage(IGrid grid) {
        IStorageService storageService = grid == null ? null : grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
    }

    private long insertIntoNetwork(AEKey key, long amount) {
        IGrid grid = getGrid();
        MEStorage storage = getNetworkStorage(grid);
        return storage == null || key == null || amount <= 0 ? 0
                : StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, amount, this.actionSource);
    }

    public static IEnergyContainer recipeEnergyView(MachineEnergyContainer<?> energyContainer) {
        return energyContainer instanceof AeBackedFactoryEnergyContainer<?> aeBackedEnergyContainer
                ? recipeEnergyView(aeBackedEnergyContainer.owner instanceof MeFactoryAeMachine aeMachine ? aeMachine : null, aeBackedEnergyContainer)
                : energyContainer;
    }

    public static IEnergyContainer recipeEnergyView(MeFactoryAeMachine owner, MachineEnergyContainer<?> energyContainer) {
        return owner == null ? energyContainer
                : MeNetworkEnergyHelper.recipeEnergyView(energyContainer, () -> owner.getAeSupport().getGrid(), owner.getAeSupport().actionSource);
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return cachedRecipe.setEnergyRequirements(energyContainer::getEnergyPerTick, recipeEnergyView(energyContainer));
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> withAeRecipeEnergy(
            MeFactoryAeMachine owner, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return cachedRecipe.setEnergyRequirements(energyContainer::getEnergyPerTick, recipeEnergyView(owner, energyContainer));
    }

    public void create(Level level, BlockPos pos) {
        this.mainNode.create(level, pos);
        updatePatterns();
    }

    public void createNodeOnFirstTick(TileEntityMekanism tile) {
        GridHelper.onFirstTick(tile, blockEntity -> create(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    public void destroy() {
        this.mainNode.destroy();
    }

    public void save(CompoundTag tag) {
        tag.putInt("PatternPriority", this.patternPriority);
        tag.putInt("AeOutputMode", this.aeOutputMode.ordinal());
        this.smartPatternMultiplication.saveConfig(tag);
        tag.remove(TAG_PATTERN_TERMINAL_NAME);
        this.mainNode.saveToNBT(tag);
    }

    public void load(CompoundTag tag) {
        this.patternPriority = tag.getInt("PatternPriority");
        this.aeOutputMode = AeOutputMode.byId(tag.getInt("AeOutputMode"));
        this.smartPatternMultiplication.loadConfig(tag);
        this.patternTerminalName = MePatternTerminalNames.migrateLegacy((TileEntityMekanism) this.owner, tag.getString(TAG_PATTERN_TERMINAL_NAME));
        this.mainNode.loadFromNBT(tag);
        updatePatterns();
    }

    public void updatePatterns() {
        this.patterns.clear();
        Level level = this.owner.getOwnerLevel();
        for (BasicInventorySlot patternSlot : this.patternSlots) {
            ItemStack stack = patternSlot.getStack();
            if (!stack.isEmpty()) {
                IPatternDetails pattern = MePatternDecodeHelper.safeDecode(stack, level,
                        ((TileEntityMekanism) this.owner).getBlockPos(), this.owner.getMachine().name());
                if (pattern != null) {
                    this.patterns.add(pattern);
                }
            }
        }
        if (this.mainNode.getNode() != null) {
            ICraftingProvider.requestUpdate(this.mainNode);
        }
        this.owner.saveChanges();
    }

    public void saveSlots(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            tag.put("MePatternSlot" + i, this.patternSlots.get(i).serializeNBT(registries));
        }
        this.smartPatternMultiplication.savePending(tag, registries);
    }

    public void saveAll(CompoundTag tag, HolderLookup.Provider registries) {
        save(tag);
        saveSlots(tag, registries);
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

    public void loadAll(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag);
        loadSlots(tag, registries);
    }

    private final class PatternSlotOwner implements MeAeMachine {
        @Override
        public AeOutputMode getAeOutputMode() {
            return AeOutputMode.BOTH;
        }

        @Override
        public void cycleAeOutputMode() {
        }

        @Override
        public void setOwner(net.minecraft.server.level.ServerPlayer player) {
            mainNode.setOwningPlayer(player);
        }

        @Override
        public List<BasicInventorySlot> getPatternSlots() {
            return patternSlots;
        }

        @Override
        public MeMekanismMachine getMachine() {
            return owner.getMachine();
        }

        @Override
        public ItemStack getTerminalIconStack() {
            return new ItemStack(ModBlocks.getMachineBlock(owner.getMachine()).get());
        }

        @Override
        public IGrid getGrid() {
            return MeFactoryAeSupport.this.getGrid();
        }

        @Override
        public String getCustomPatternTerminalName() {
            return MeFactoryAeSupport.this.getPatternTerminalName();
        }

        @Override
        public void setCustomPatternTerminalName(String name) {
            MeFactoryAeSupport.this.setPatternTerminalName(name);
        }
    }

    private enum NodeListener implements IGridNodeListener<MeFactoryAeMachine> {
        INSTANCE;

        @Override
        public void onSaveChanges(MeFactoryAeMachine nodeOwner, IGridNode node) {
            nodeOwner.saveChanges();
        }

        @Override
        public void onStateChanged(MeFactoryAeMachine nodeOwner, IGridNode node, State state) {
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

    public static final class AeBackedFactoryEnergyContainer<TILE extends TileEntityMekanism>
            extends MeNetworkEnergyHelper.NetworkBackedEnergyContainer<TILE> {
        private final TILE owner;

        public AeBackedFactoryEnergyContainer(TILE owner, IContentsListener listener) {
            super(owner, listener, () -> factoryGrid(owner), () -> factoryActionSource(owner));
            this.owner = owner;
        }
    }

    private static IGrid factoryGrid(TileEntityMekanism owner) {
        MeFactoryAeSupport support = owner instanceof MeFactoryAeMachine aeMachine ? aeMachine.getAeSupport() : null;
        return support == null ? null : support.getGrid();
    }

    private static IActionSource factoryActionSource(TileEntityMekanism owner) {
        MeFactoryAeSupport support = owner instanceof MeFactoryAeMachine aeMachine ? aeMachine.getAeSupport() : null;
        return support == null ? null : support.actionSource;
    }
}
