package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeMekanismMachineAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeNetworkEnergyHelper;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputPort;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.blockentity.api.MePatternIoOwner;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import appeng.api.networking.security.IActionSource;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeDataMigration;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwnerSupport;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.BooleanSupplier;

public class MeMekanismMachineBlockEntity extends TileEntityConfigurableMachine
        implements ICraftingProvider, MeSmartCableConnection, IActionHost, IHasDumpButton, MeAeMachine, MePatternIoOwner,
        MeUpgradeStateOwner {
    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    private static final int BASE_TICKS_REQUIRED = 10 * 20;
    public static final int INPUT_SLOT = 0;
    public static final int SECONDARY_INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SECONDARY_OUTPUT_SLOT = 3;
    public static final int PATTERN_SLOTS_START = 4;

    private static final String TAG_CHEMICAL = "Chemical";
    private static final String TAG_AE_OUTPUT_MODE = "AeOutputMode";

    private IInventorySlot[] inventorySlots;
    private final IActionSource actionSource;
    private MeMekanismMachineAeSupport aeSupport;
    private final MeMekanismMachine machine;
    private IChemicalTank chemicalTank;
    private MachineEnergyContainer<MeMekanismMachineBlockEntity> energyContainer;
    private int operatingTicks;
    private int ticksRequired = BASE_TICKS_REQUIRED;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;
    private final MeUpgradeStateOwnerSupport meUpgradeOwner = new MeUpgradeStateOwnerSupport(
            () -> true,
            () -> getAeSupport().getPatternSlots().stream().allMatch(slot -> slot.getStack().isEmpty()),
            () -> getAeSupport().canRemoveInterfaceUpgrade(),
            this::onMeUpgradeStateChangedInternal,
            () -> supportsUpgrades() ? getComponent() : null);
    private final MeMachineRecipeProcessor recipeProcessor = new MeMachineRecipeProcessor(this);

    public MeMekanismMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
        this.actionSource = IActionSource.ofMachine(this);
        IInventorySlot[] inventorySlots = slots();
        List<IInventorySlot> inputSlots = new ArrayList<>();
        inputSlots.add(inventorySlots[INPUT_SLOT]);
        if (machine.hasSecondaryItemInput() || machine.hasChemicalInput()) {
            inputSlots.add(inventorySlots[SECONDARY_INPUT_SLOT]);
        }
        for (int slot = PATTERN_SLOTS_START; slot <= patternSlotsEnd(); slot++) {
            inputSlots.add(inventorySlots[slot]);
        }
        List<IInventorySlot> outputSlots = new ArrayList<>();
        outputSlots.add(inventorySlots[OUTPUT_SLOT]);
        if (machine.hasSecondaryOutput()) {
            outputSlots.add(inventorySlots[SECONDARY_OUTPUT_SLOT]);
        }
        this.configComponent.setupItemIOConfig(inputSlots, outputSlots, inventorySlots[energySlot()], false);
        this.configComponent.setupInputConfig(TransmissionType.ENERGY, this.energyContainer);
        if (this.chemicalTank != null) {
            this.configComponent.setupIOConfig(TransmissionType.CHEMICAL, this.chemicalTank, RelativeSide.RIGHT).setCanEject(false);
        }
        this.ejectorComponent = new TileComponentEjector(this);
        this.ejectorComponent.setOutputData(this.configComponent, TransmissionType.ITEM);
        getAeSupport();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        if (this.inventorySlots[energySlot()] instanceof EnergyInventorySlot energySlot) {
            energySlot.fillContainerOrConvert();
        }
        this.recipeProcessor.fillChemicalFromConversionSlot();
        if (this.recipeProcessor.canProcessRecipe()) {
            long energyPerTick = this.energyContainer.getEnergyPerTick();
            if (this.energyContainer.extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL) < energyPerTick) {
                // Never make progress without paying for it (e.g. the simulated check counted
                // network energy that was gone by the time the real extraction ran). Progress is
                // paused, not reset, matching Mekanism's behaviour when a machine runs dry.
                setActive(false);
                return sendUpdatePacket;
            }
            this.energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
            this.operatingTicks++;
            setActive(true);
            if (this.operatingTicks >= this.ticksRequired) {
                this.recipeProcessor.processRecipe();
                this.operatingTicks = 0;
            }
        } else {
            this.operatingTicks = 0;
            setActive(false);
        }
        // Outputs of this machine are returned by the AE ticker, which sleeps while idle.
        getAeSupport().wakeOutputTicker();
        return sendUpdatePacket;
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(this.energyContainer = new MeAeBackedEnergyContainer(this, listener));
        return builder.build();
    }

    @Nullable
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        if (!getMachineEarly().hasChemicalInput()) {
            return null;
        }
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(this.chemicalTank = BasicChemicalTank.createModern(MeMachineRecipeProcessor.getChemicalCapacity(this), ConstantPredicates.alwaysTrue(), listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        MeMekanismMachine machine = getMachineEarly();
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        IInventorySlot[] inventorySlots = slots();
        if (!machine.hasRecipeLogic()) {
            inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
            inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
            inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 64, 53));
        } else if (machine.factoryType() == mekanism.common.content.blocktype.FactoryType.INFUSING) {
                inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 17, 35));
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 51, 43));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 109, 43));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 143, 35));
        } else if (machine.hasChemicalInput()) {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
                inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 53));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 39, 35));
        } else if (machine.hasSecondaryItemInput()) {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
                inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 53));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 39, 35));
        } else if (machine.hasSecondaryOutput()) {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 56, 17));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[SECONDARY_OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 132, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 56, 53));
        } else {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 64, 53));
        }
        // The AE support owns the canonical pattern slots. Append those same slot instances to
        // Mekanism's holder so menus, sided inventory and AE pattern routing address one list.
        List<BasicInventorySlot> patternSlots = getAeSupport().getPatternSlots();
        for (int i = 0; i < patternSlots.size(); i++) {
            inventorySlots[PATTERN_SLOTS_START + i] = patternSlots.get(i);
        }
        return withPatternSlots(builder.build());
    }

    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    @Override
    public ItemStack getTerminalIconStack() {
        return new ItemStack(ModBlocks.getMachineBlock(this.machine).get());
    }

    public Component getStatusMessage() {
        if (this.machine.hasChemicalInput() && this.chemicalTank != null && !this.chemicalTank.isEmpty()) {
            ChemicalStack stack = this.chemicalTank.getStack();
            return Component.literal(stack.getAmount() + " mB " + stack.getTextComponent().getString());
        }
        if (getMainNode().isOnline()) {
            return Component.translatable("message.mekenergistics.machine.online");
        }
        return Component.translatable("message.mekenergistics.machine.offline");
    }

    public int getChemicalAmount() {
        return this.chemicalTank == null ? 0 : this.recipeProcessor.clampNeeded(this.chemicalTank.getStack().getAmount());
    }

    public int getChemicalCapacityInt() {
        return this.recipeProcessor.clampNeeded(this.recipeProcessor.getChemicalCapacity());
    }

    public void dumpChemical() {
        if (this.chemicalTank != null && !this.chemicalTank.isEmpty()) {
            this.chemicalTank.setEmpty();
            setChanged();
        }
    }

    @Override
    public void dump() {
        dumpChemical();
    }

    public MachineEnergyContainer<MeMekanismMachineBlockEntity> getEnergyContainer() {
        return this.energyContainer;
    }

    public IChemicalTank getChemicalTank() {
        return this.chemicalTank;
    }

    /** Secondary physical input used by the generic item+chemical layout's conversion slot. */
    public IInventorySlot getAePrimaryInputSlot() {
        return slots()[INPUT_SLOT];
    }

    public IInventorySlot getAeConversionSlot() {
        return slots()[SECONDARY_INPUT_SLOT];
    }

    public double getScaledProgress() {
        return this.ticksRequired <= 0 ? 0 : this.operatingTicks / (double) this.ticksRequired;
    }

    public BooleanSupplier getWarningCheck(RecipeError error) {
        return () -> false;
    }

    @Override
    public List<BasicInventorySlot> getPatternSlots() {
        return getAeSupport().getPatternSlots();
    }

    public Component getDisplayName() {
        return Component.translatable(this.machine.translationKey());
    }

    long extractAeAsFe(long requestedFe, Action action) {
        if (requestedFe <= 0) {
            return 0;
        }
        return MeNetworkEnergyHelper.extractNetworkFe(getGrid(), this.actionSource, requestedFe, action);
    }

    boolean canAddChemical(ChemicalStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ChemicalStack current = getChemicalStack();
        if (!current.isEmpty() && !current.is(stack.getChemicalHolder())) {
            return false;
        }
        return current.getAmount() + stack.getAmount() <= this.recipeProcessor.getChemicalCapacity();
    }
    @Nullable
    @Override
    public IGrid getGrid() {
        IGridNode node = getMainNode().getNode();
        return node == null || !node.isActive() ? null : node.getGrid();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        getAeSupport().createOnFirstTick();
    }

    public MeMekanismMachineAeSupport getAeSupport() {
        if (this.aeSupport == null) {
            this.aeSupport = new MeMekanismMachineAeSupport(this);
        }
        return this.aeSupport;
    }

    @Override
    public MeMekanismMachineAeSupport getRecipeAeSupport() {
        return getAeSupport();
    }

    @Override
    public List<BasicInventorySlot> getExternalPatternSlots() {
        List<BasicInventorySlot> result = new ArrayList<>();
        for (int slot = PATTERN_SLOTS_START; slot <= patternSlotsEnd(); slot++) {
            if (slots()[slot] instanceof BasicInventorySlot patternSlot) {
                result.add(patternSlot);
            }
        }
        return result;
    }

    @Override
    public int getLegacyPatternSlotOffset() {
        return PATTERN_SLOTS_START;
    }

    @Override
    public void setRemoved() {
        getAeSupport().destroyNode();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        getAeSupport().destroyNode();
        super.onChunkUnloaded();
    }

    @Override
    public IManagedGridNode getMainNode() {
        return getAeSupport().getMainNode();
    }

    IManagedGridNode getManagedNode() {
        return getMainNode();
    }

    public IActionSource getActionSource() {
        return this.actionSource;
    }

    @Override
    public MeInputLayout getPatternInputLayout() {
        List<MeInputPort> ports = new ArrayList<>();
        if (this.chemicalTank != null && this.aeOutputMode.chemicals()) {
            if (slots()[SECONDARY_INPUT_SLOT] != null) {
                ports.add(MeMachineIoAdapter.itemInput(slots()[SECONDARY_INPUT_SLOT]));
            }
            return MeInputLayout.unordered(ports);
        }
        if (slots()[INPUT_SLOT] != null) {
            ports.add(MeMachineIoAdapter.itemInput(slots()[INPUT_SLOT]));
        }
        if (slots()[SECONDARY_INPUT_SLOT] != null) {
            ports.add(MeMachineIoAdapter.itemInput(slots()[SECONDARY_INPUT_SLOT]));
        }
        if (this.chemicalTank != null) {
            ports.add(MeMachineIoAdapter.chemicalInput(this.chemicalTank));
        }
        return MeInputLayout.unordered(ports);
    }

    public List<MeOutputPort> getAeOutputPorts() {
        List<MeOutputPort> ports = new ArrayList<>();
        if (slots()[OUTPUT_SLOT] instanceof OutputInventorySlot output) {
            ports.add(MeMachineIoAdapter.itemOutput(output));
        }
        if (slots()[SECONDARY_OUTPUT_SLOT] instanceof OutputInventorySlot output) {
            ports.add(MeMachineIoAdapter.itemOutput(output));
        }
        if (this.chemicalTank != null) {
            ports.add(MeMachineIoAdapter.chemicalOutput(this.chemicalTank));
        }
        return ports;
    }

    @Override
    public List<? extends MeOutputPort> getPatternOutputPorts() {
        return getAeOutputPorts();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return getAeSupport().getAvailablePatterns();
    }

    @Override
    public int getPatternPriority() {
        return getAeSupport().getPatternPriority();
    }

    @Override
    public String getCustomPatternTerminalName() {
        return getAeSupport().getPatternTerminalName();
    }

    @Override
    public void setCustomPatternTerminalName(String name) {
        getAeSupport().setPatternTerminalName(name);
    }

    @Override
    public boolean isSmartPatternMultiplicationEnabled() {
        return getAeSupport().isSmartPatternMultiplicationEnabled();
    }

    @Override
    public void setSmartPatternMultiplicationEnabled(boolean enabled) {
        getAeSupport().setSmartPatternMultiplicationEnabled(enabled);
    }

    private void updatePatterns() {
        getAeSupport().updatePatterns();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_AE_OUTPUT_MODE, this.aeOutputMode.ordinal());
        tag.putInt(SerializationConstants.PROGRESS, this.operatingTicks);
        getAeSupport().saveAeState(tag, registries, this.aeOutputMode);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        getMeUpgradeContainer().setData(MeUpgradeDataMigration.migrate(tag).data());
        getMeUpgradeContainer().migrateToNativeComponent();
        if (tag.contains(TAG_CHEMICAL) && this.chemicalTank != null && this.chemicalTank.isEmpty()) {
            this.chemicalTank.setStack(ChemicalStack.parseOptional(registries, tag.getCompound(TAG_CHEMICAL)));
        }
        this.aeOutputMode = AeOutputMode.byId(tag.getInt(TAG_AE_OUTPUT_MODE));
        this.operatingTicks = tag.getInt(SerializationConstants.PROGRESS);
        this.aeOutputMode = getAeSupport().loadAeState(tag, registries);
    }

    @Override
    public CompoundTag getConfigurationData(HolderLookup.Provider provider, Player player) {
        CompoundTag data = super.getConfigurationData(provider, player);
        data.putInt(TAG_AE_OUTPUT_MODE, this.aeOutputMode.ordinal());
        return data;
    }

    @Override
    public void setConfigurationData(HolderLookup.Provider provider, Player player, CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        this.aeOutputMode = AeOutputMode.byId(data.getInt(TAG_AE_OUTPUT_MODE));
    }

    @Override
    public boolean isConfigurationDataCompatible(Block blockType) {
        MeMekanismMachine sourceMachine = ModBlocks.getMachine(blockType);
        return sourceMachine != null && sourceMachine == this.machine || super.isConfigurationDataCompatible(blockType);
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            this.ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
        }
    }

    ChemicalStack getChemicalStack() {
        return this.chemicalTank == null ? ChemicalStack.EMPTY : this.chemicalTank.getStack();
    }

    MeMekanismMachine getMachineEarly() {
        return this.machine == null ? ModBlocks.getMachine(getBlockState().getBlock()) : this.machine;
    }

    ItemStack getStack(int slot) {
        return slots()[slot] == null ? ItemStack.EMPTY : slots()[slot].getStack();
    }

    void setStack(int slot, ItemStack stack) {
        if (slots()[slot] != null) {
            slots()[slot].setStack(stack);
        }
    }

    int getSlotLimit(int slot, ItemStack stack) {
        return slots()[slot] == null ? 0 : slots()[slot].getLimit(stack);
    }

    ItemStack insertItem(int slot, ItemStack stack) {
        return insertItem(slot, stack, Action.EXECUTE);
    }

    ItemStack insertItem(int slot, ItemStack stack, Action action) {
        return slots()[slot] == null ? stack : slots()[slot].insertItem(stack, action, AutomationType.INTERNAL);
    }

    IInventorySlot[] slots() {
        if (this.inventorySlots == null) {
            this.inventorySlots = new IInventorySlot[totalSlots()];
        }
        return this.inventorySlots;
    }

    private static int patternSlotsEnd() {
        return PATTERN_SLOTS_START + MekEnergisticsConfig.patternSlots() - 1;
    }

    private static int energySlot() {
        return PATTERN_SLOTS_START + MekEnergisticsConfig.patternSlots();
    }

    static int totalSlots() {
        return energySlot() + 1;
    }

    public AeOutputMode getAeOutputMode() {
        return this.aeOutputMode;
    }

    public void cycleAeOutputMode() {
        this.aeOutputMode = this.aeOutputMode.next();
        setChanged();
    }

    @Override
    public MeUpgradeContainer getMeUpgradeContainer() {
        return this.meUpgradeOwner.getMeUpgradeContainer();
    }

    @Override
    public void onMeUpgradeStateChanged() {
        this.meUpgradeOwner.onMeUpgradeStateChanged();
    }

    private void onMeUpgradeStateChangedInternal() {
        setChanged();
        getAeSupport().alertAeTicker();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
        }
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> this.operatingTicks, ticks -> this.operatingTicks = ticks));
        container.track(SyncableInt.create(() -> this.ticksRequired, ticks -> this.ticksRequired = ticks));
        container.track(SyncableInt.create(() -> this.aeOutputMode.ordinal(), mode -> this.aeOutputMode = AeOutputMode.byId(mode)));
        container.track(mekanism.common.inventory.container.sync.SyncableBoolean.create(this::hasPassiveCraftingUpgrade,
                getAeSupport()::setClientPassiveCraftingEnabled));
        container.track(mekanism.common.inventory.container.sync.SyncableBoolean.create(
                getAeSupport()::isInterfaceMode, getAeSupport()::setClientInterfaceMode));
        container.track(mekanism.common.inventory.container.sync.SyncableBoolean.create(this::isSmartPatternMultiplicationEnabled, this::setSmartPatternMultiplicationEnabled));
        container.track(mekanism.common.inventory.container.sync.SyncableBoolean.create(this::isVisibleInTerminal, this::setVisibleInPatternAccessTerminal));
        container.track(SyncableInt.create(() -> getPassiveCraftingSettings().intervalTicks(), value -> getPassiveCraftingSettings().set(value, getPassiveCraftingSettings().multiplier())));
        container.track(SyncableLong.create(() -> getPassiveCraftingSettings().multiplier(), value -> getPassiveCraftingSettings().set(getPassiveCraftingSettings().intervalTicks(), value)));
        container.track(SyncableLong.create(() -> getChemicalStack().getAmount(), amount -> {
            if (this.chemicalTank != null && !this.chemicalTank.isEmpty()) {
                ChemicalStack stack = this.chemicalTank.getStack().copy();
                stack.setAmount(amount);
                this.chemicalTank.setStack(stack);
            }
        }));
    }

}
