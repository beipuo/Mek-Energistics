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
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.settings.TickRates;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
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
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.util.MekanismUtils;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.BooleanSupplier;

public class MeMekanismMachineBlockEntity extends TileEntityConfigurableMachine
        implements ICraftingProvider, MeSmartCableConnection, IGridNodeListener<MeMekanismMachineBlockEntity>, IActionHost, IHasDumpButton, MeAeMachine {
    private static final int BASE_TICKS_REQUIRED = 10 * 20;
    public static final int INPUT_SLOT = 0;
    public static final int SECONDARY_INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SECONDARY_OUTPUT_SLOT = 3;
    public static final int PATTERN_SLOTS_START = 4;

    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_PATTERN_PRIORITY = "PatternPriority";
    private static final String TAG_CHEMICAL = "Chemical";
    private static final String TAG_AE_OUTPUT_MODE = "AeOutputMode";

    private IInventorySlot[] inventorySlots;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private final List<PendingCraftingOutput> pendingCraftingOutputs = new ArrayList<>();
    private final List<PendingKeyOutput> pendingKeyOutputs = new ArrayList<>();
    private final MeMekanismMachine machine;
    private IChemicalTank chemicalTank;
    private MachineEnergyContainer<MeMekanismMachineBlockEntity> energyContainer;
    private int operatingTicks;
    private int ticksRequired = BASE_TICKS_REQUIRED;
    private int patternPriority = 0;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeMekanismMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
        this.actionSource = IActionSource.ofMachine(this);
        this.mainNode = GridHelper.createManagedNode(this, this)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this)
                .addService(IGridTickable.class, new AeTicker());
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
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        if (this.inventorySlots[energySlot()] instanceof EnergyInventorySlot energySlot) {
            energySlot.fillContainerOrConvert();
        }
        fillChemicalFromConversionSlot();
        if (canProcessRecipe()) {
            this.energyContainer.extract(this.energyContainer.getEnergyPerTick(), Action.EXECUTE, AutomationType.INTERNAL);
            this.operatingTicks++;
            setActive(true);
            if (this.operatingTicks >= this.ticksRequired) {
                processRecipe();
                this.operatingTicks = 0;
            }
        } else {
            this.operatingTicks = 0;
            setActive(false);
        }
        processAeNetworkOutputs();
        return sendUpdatePacket;
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(this.energyContainer = new AeBackedEnergyContainer(this, listener));
        return builder.build();
    }

    @Nullable
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        if (!getMachineEarly().hasChemicalInput()) {
            return null;
        }
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(this.chemicalTank = BasicChemicalTank.createModern(getChemicalCapacity(), ConstantPredicates.alwaysTrue(), listener));
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
        } else switch (machine.factoryType()) {
            case INFUSING -> {
                inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 17, 35));
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 51, 43));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 109, 43));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 143, 35));
            }
            case COMPRESSING, INJECTING, PURIFYING -> {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
                inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 53));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 39, 35));
            }
            case COMBINING -> {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
                inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 53));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 39, 35));
            }
            case SAWING -> {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 56, 17));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[SECONDARY_OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 132, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 56, 53));
            }
            default -> {
                inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 64, 17));
                inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 116, 35));
                inventorySlots[energySlot()] = builder.addSlot(EnergyInventorySlot.fillOrConvert(this.energyContainer, this::getLevel, listener, 64, 53));
            }
        }
        for (int slot = PATTERN_SLOTS_START; slot <= patternSlotsEnd(); slot++) {
            inventorySlots[slot] = builder.addSlot(MePatternInventorySlot.create(PatternDetailsHelper::isEncodedPattern, () -> {
                listener.onContentsChanged();
                updatePatterns();
            }));
        }
        return builder.build();
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
        if (this.mainNode.isOnline()) {
            return Component.translatable("message.mekenergistics.machine.online");
        }
        return Component.translatable("message.mekenergistics.machine.offline");
    }

    public int getChemicalAmount() {
        return this.chemicalTank == null ? 0 : clampNeeded(this.chemicalTank.getStack().getAmount());
    }

    public int getChemicalCapacityInt() {
        return clampNeeded(getChemicalCapacity());
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

    public double getScaledProgress() {
        return this.ticksRequired <= 0 ? 0 : this.operatingTicks / (double) this.ticksRequired;
    }

    public BooleanSupplier getWarningCheck(RecipeError error) {
        return () -> false;
    }

    public void setOwner(ServerPlayer player) {
        MeOwnerHelper.setOwner(this, this.mainNode, player);
    }

    @Override
    public List<BasicInventorySlot> getPatternSlots() {
        List<BasicInventorySlot> patternSlots = new ArrayList<>(MekEnergisticsConfig.patternSlots());
        IInventorySlot[] slots = slots();
        for (int slot = PATTERN_SLOTS_START; slot <= patternSlotsEnd(); slot++) {
            if (slots[slot] instanceof BasicInventorySlot patternSlot) {
                patternSlots.add(patternSlot);
            }
        }
        return Collections.unmodifiableList(patternSlots);
    }

    public Component getDisplayName() {
        return Component.translatable(this.machine.translationKey());
    }

    private long insertIntoNetwork(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }

        return insertIntoNetwork(key, stack.getCount());
    }

    private long insertIntoNetwork(AEKey key, long amount) {
        MEStorage storage = this.getNetworkStorage();
        if (storage == null || key == null || amount <= 0) {
            return 0;
        }
        return storage.insert(key, amount, Actionable.MODULATE, this.actionSource);
    }

    private void insertOutputSlotIntoNetwork(int slot) {
        if (!this.aeOutputMode.items()) {
            return;
        }
        ItemStack output = getStack(slot);
        if (output.isEmpty()) {
            return;
        }

        long inserted = this.insertIntoNetwork(output);
        if (inserted <= 0) {
            return;
        }

        output.shrink((int) inserted);
        setStack(slot, output.isEmpty() ? ItemStack.EMPTY : output);
    }

    private void insertChemicalTankIntoNetwork() {
        if (!this.aeOutputMode.chemicals()) {
            return;
        }
        ChemicalStack stack = getChemicalStack();
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
        this.chemicalTank.shrinkStack(inserted, Action.EXECUTE);
        setChanged();
    }

    private boolean processAeNetworkOutputs() {
        boolean hadWork = hasAeNetworkOutputWork();
        processPendingCraftingOutputs();
        processPendingKeyOutputs();
        insertOutputSlotIntoNetwork(OUTPUT_SLOT);
        insertOutputSlotIntoNetwork(SECONDARY_OUTPUT_SLOT);
        insertChemicalTankIntoNetwork();
        boolean hasWork = hasAeNetworkOutputWork();
        if (hasWork) {
            alertAeTicker();
        }
        return hadWork && !hasWork;
    }

    private boolean hasAeNetworkOutputWork() {
        if (!this.pendingCraftingOutputs.isEmpty() || !this.pendingKeyOutputs.isEmpty()) {
            return true;
        }
        if (this.aeOutputMode.items() && (!getStack(OUTPUT_SLOT).isEmpty() || !getStack(SECONDARY_OUTPUT_SLOT).isEmpty())) {
            return true;
        }
        return this.aeOutputMode.chemicals() && this.chemicalTank != null && !this.chemicalTank.isEmpty();
    }

    private void alertAeTicker() {
        this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    private void processRecipe() {
        if (this.level == null || !this.machine.hasRecipeLogic()) {
            return;
        }
        switch (this.machine.slotLayout()) {
            case SINGLE_ITEM -> processSingleItemRecipe();
            case DOUBLE_ITEM -> processCombinerRecipe();
            case SAWING -> processSawingRecipe();
            case ITEM_CHEMICAL -> processItemChemicalRecipe();
        }
    }

    private boolean canProcessRecipe() {
        if (this.level == null || !this.machine.hasRecipeLogic()) {
            return false;
        }
        if (!hasEnergyForRecipe()) {
            return false;
        }
        return switch (this.machine.slotLayout()) {
            case SINGLE_ITEM -> canProcessSingleItemRecipe();
            case DOUBLE_ITEM -> canProcessCombinerRecipe();
            case SAWING -> canProcessSawingRecipe();
            case ITEM_CHEMICAL -> canProcessItemChemicalRecipe();
        };
    }

    private boolean hasEnergyForRecipe() {
        long energyPerTick = this.energyContainer.getEnergyPerTick();
        return energyPerTick <= 0 || this.energyContainer.getEnergy() >= energyPerTick
                || extractAeAsFe(energyPerTick - this.energyContainer.getEnergy(), Action.SIMULATE) >= energyPerTick - this.energyContainer.getEnergy();
    }

    private long extractAeAsFe(long requestedFe, Action action) {
        if (requestedFe <= 0) {
            return 0;
        }
        IGridNode node = this.mainNode == null ? null : this.mainNode.getNode();
        if (node == null || !node.isActive()) {
            return 0;
        }
        IGrid grid = node.getGrid();
        if (grid == null) {
            return 0;
        }
        double requestedAe = PowerUnit.FE.convertTo(PowerUnit.AE, requestedFe);
        double extractedAe = grid.getEnergyService().extractAEPower(requestedAe,
                action.execute() ? Actionable.MODULATE : Actionable.SIMULATE,
                appeng.api.config.PowerMultiplier.ONE);
        return Math.min(requestedFe, (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, extractedAe)));
    }

    private boolean canProcessSingleItemRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        ItemStackToItemStackRecipe recipe = switch (this.machine.factoryType()) {
            case ENRICHING -> MekanismRecipeType.ENRICHING.getInputCache().findFirstRecipe(this.level, input);
            case CRUSHING -> MekanismRecipeType.CRUSHING.getInputCache().findFirstRecipe(this.level, input);
            case SMELTING -> MekanismRecipeType.SMELTING.getInputCache().findFirstRecipe(this.level, input);
            default -> null;
        };
        if (recipe == null) {
            return false;
        }
        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        ItemStack output = recipe.getOutput(input);
        return needed > 0 && !output.isEmpty() && canFitOutput(OUTPUT_SLOT, output);
    }

    private boolean canProcessCombinerRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        ItemStack secondary = getStack(SECONDARY_INPUT_SLOT);
        if (input.isEmpty() || secondary.isEmpty()) {
            return false;
        }
        CombinerRecipe recipe = MekanismRecipeType.COMBINING.getInputCache().findFirstRecipe(this.level, input, secondary);
        if (recipe == null) {
            return false;
        }
        int neededInput = clampNeeded(recipe.getMainInput().getNeededAmount(input));
        int neededSecondary = clampNeeded(recipe.getExtraInput().getNeededAmount(secondary));
        ItemStack output = recipe.getOutput(input, secondary);
        return neededInput > 0 && neededSecondary > 0 && !output.isEmpty() && canFitOutput(OUTPUT_SLOT, output);
    }

    private boolean canProcessItemChemicalRecipe() {
        ChemicalStack chemicalStack = getChemicalStack();
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty() || chemicalStack.isEmpty()) {
            return false;
        }
        ItemStackChemicalToItemStackRecipe recipe = switch (this.machine.factoryType()) {
            case COMPRESSING -> MekanismRecipeType.COMPRESSING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            case INFUSING -> MekanismRecipeType.METALLURGIC_INFUSING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            case INJECTING -> MekanismRecipeType.INJECTING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            case PURIFYING -> MekanismRecipeType.PURIFYING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            default -> null;
        };
        if (recipe == null) {
            return false;
        }
        int neededInput = clampNeeded(recipe.getItemInput().getNeededAmount(input));
        long neededChemical = recipe.getChemicalInput().getNeededAmount(chemicalStack);
        ItemStack output = recipe.getOutput(input, chemicalStack);
        return neededInput > 0 && neededChemical > 0 && !output.isEmpty() && canFitOutput(OUTPUT_SLOT, output);
    }

    private boolean canProcessSawingRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        SawmillRecipe recipe = MekanismRecipeType.SAWING.getInputCache().findFirstRecipe(this.level, input);
        if (recipe == null) {
            return false;
        }
        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        SawmillRecipe.ChanceOutput output = recipe.getOutput(input);
        ItemStack mainOutput = output.getMainOutput();
        ItemStack secondaryOutput = output.getSecondaryOutput();
        return needed > 0
                && (!mainOutput.isEmpty() || !secondaryOutput.isEmpty())
                && (mainOutput.isEmpty() || canFitOutput(OUTPUT_SLOT, mainOutput))
                && (secondaryOutput.isEmpty() || canFitOutput(SECONDARY_OUTPUT_SLOT, secondaryOutput));
    }

    private void processSingleItemRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        ItemStackToItemStackRecipe recipe = switch (this.machine.factoryType()) {
            case ENRICHING -> MekanismRecipeType.ENRICHING.getInputCache().findFirstRecipe(this.level, input);
            case CRUSHING -> MekanismRecipeType.CRUSHING.getInputCache().findFirstRecipe(this.level, input);
            case SMELTING -> MekanismRecipeType.SMELTING.getInputCache().findFirstRecipe(this.level, input);
            default -> null;
        };
        if (recipe == null) {
            return;
        }

        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        ItemStack output = recipe.getOutput(input);
        if (needed <= 0 || output.isEmpty() || !canFitOutput(OUTPUT_SLOT, output)) {
            return;
        }
        input.shrink(needed);
        setStack(INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);
        addToOutput(OUTPUT_SLOT, output);
        setChanged();
    }

    private void processCombinerRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        ItemStack secondary = getStack(SECONDARY_INPUT_SLOT);
        if (input.isEmpty() || secondary.isEmpty()) {
            return;
        }

        CombinerRecipe recipe = MekanismRecipeType.COMBINING.getInputCache().findFirstRecipe(this.level, input, secondary);
        if (recipe == null) {
            return;
        }

        int neededInput = clampNeeded(recipe.getMainInput().getNeededAmount(input));
        int neededSecondary = clampNeeded(recipe.getExtraInput().getNeededAmount(secondary));
        ItemStack output = recipe.getOutput(input, secondary);
        if (neededInput <= 0 || neededSecondary <= 0 || output.isEmpty() || !canFitOutput(OUTPUT_SLOT, output)) {
            return;
        }
        input.shrink(neededInput);
        secondary.shrink(neededSecondary);
        setStack(INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);
        setStack(SECONDARY_INPUT_SLOT, secondary.isEmpty() ? ItemStack.EMPTY : secondary);
        addToOutput(OUTPUT_SLOT, output);
        setChanged();
    }

    private void processItemChemicalRecipe() {
        ChemicalStack chemicalStack = getChemicalStack();
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty() || chemicalStack.isEmpty()) {
            return;
        }

        ItemStackChemicalToItemStackRecipe recipe = switch (this.machine.factoryType()) {
            case COMPRESSING -> MekanismRecipeType.COMPRESSING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            case INFUSING -> MekanismRecipeType.METALLURGIC_INFUSING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            case INJECTING -> MekanismRecipeType.INJECTING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            case PURIFYING -> MekanismRecipeType.PURIFYING.getInputCache().findFirstRecipe(this.level, input, chemicalStack);
            default -> null;
        };
        if (recipe == null) {
            return;
        }

        int neededInput = clampNeeded(recipe.getItemInput().getNeededAmount(input));
        long neededChemical = recipe.getChemicalInput().getNeededAmount(chemicalStack);
        ItemStack output = recipe.getOutput(input, chemicalStack);
        if (neededInput <= 0 || neededChemical <= 0 || output.isEmpty() || !canFitOutput(OUTPUT_SLOT, output)) {
            return;
        }

        input.shrink(neededInput);
        setStack(INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);
        this.chemicalTank.shrinkStack(neededChemical, Action.EXECUTE);
        addToOutput(OUTPUT_SLOT, output);
        setChanged();
    }

    private void fillChemicalFromConversionSlot() {
        ItemStack conversionInput = getStack(SECONDARY_INPUT_SLOT);
        if (conversionInput.isEmpty() || this.level == null) {
            return;
        }

        ItemStack singleInput = conversionInput.copyWithCount(1);
        ItemStackToChemicalRecipe conversion = MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findTypeBasedRecipe(this.level, singleInput);
        if (conversion == null) {
            return;
        }

        ChemicalStack converted = conversion.getOutput(singleInput);
        if (converted.isEmpty() || !canAddChemical(converted)) {
            return;
        }

        this.chemicalTank.insert(converted, Action.EXECUTE, AutomationType.INTERNAL);
        conversionInput.shrink(1);
        setStack(SECONDARY_INPUT_SLOT, conversionInput.isEmpty() ? ItemStack.EMPTY : conversionInput);
        setChanged();
    }

    private boolean canAddChemical(ChemicalStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ChemicalStack current = getChemicalStack();
        if (!current.isEmpty() && !current.is(stack.getChemicalHolder())) {
            return false;
        }
        return current.getAmount() + stack.getAmount() <= getChemicalCapacity();
    }

    private long getChemicalCapacity() {
        return getMachineEarly().factoryType() == mekanism.common.content.blocktype.FactoryType.INFUSING
                ? TileEntityMetallurgicInfuser.MAX_INFUSE
                : TileEntityAdvancedElectricMachine.MAX_GAS;
    }

    private void processSawingRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        SawmillRecipe recipe = MekanismRecipeType.SAWING.getInputCache().findFirstRecipe(this.level, input);
        if (recipe == null) {
            return;
        }

        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        SawmillRecipe.ChanceOutput output = recipe.getOutput(input);
        ItemStack mainOutput = output.getMainOutput();
        ItemStack secondaryOutput = output.getSecondaryOutput();
        if (needed <= 0 || mainOutput.isEmpty() && secondaryOutput.isEmpty()) {
            return;
        }
        if (!mainOutput.isEmpty() && !canFitOutput(OUTPUT_SLOT, mainOutput)) {
            return;
        }
        if (!secondaryOutput.isEmpty() && !canFitOutput(SECONDARY_OUTPUT_SLOT, secondaryOutput)) {
            return;
        }
        input.shrink(needed);
        setStack(INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);
        addToOutput(OUTPUT_SLOT, mainOutput);
        addToOutput(SECONDARY_OUTPUT_SLOT, secondaryOutput);
        setChanged();
    }

    private static int clampNeeded(long needed) {
        return needed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) needed;
    }

    private boolean canFitOutput(int slot, ItemStack stack) {
        ItemStack existing = getStack(slot);
        if (existing.isEmpty()) {
            return stack.getCount() <= getSlotLimit(slot, stack);
        }
        return ItemStack.isSameItemSameComponents(existing, stack)
                && existing.getCount() + stack.getCount() <= Math.min(existing.getMaxStackSize(), getSlotLimit(slot, existing));
    }

    private void addToOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack existing = getStack(slot);
        if (existing.isEmpty()) {
            setStack(slot, stack.copy());
            return;
        }
        existing.grow(stack.getCount());
        setStack(slot, existing);
    }

    @Nullable
    private MEStorage getNetworkStorage() {
        IGrid grid = getGrid();
        if (grid == null) {
            return null;
        }

        IStorageService storageService = grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
    }

    @Nullable
    @Override
    public IGrid getGrid() {
        IGridNode node = this.mainNode.getNode();
        return node == null || !node.isActive() ? null : node.getGrid();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, blockEntity -> {
            blockEntity.mainNode.create(blockEntity.getLevel(), blockEntity.getBlockPos());
            blockEntity.updatePatterns();
        });
    }

    @Override
    public void setRemoved() {
        this.mainNode.destroy();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        this.mainNode.destroy();
        super.onChunkUnloaded();
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return this.mainNode.getNode();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return this.mainNode.getNode();
    }

    @Override
    public void onSaveChanges(MeMekanismMachineBlockEntity nodeOwner, IGridNode node) {
        this.setChanged();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    @Override
    public int getPatternPriority() {
        return this.patternPriority;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!this.mainNode.isActive() || !this.patterns.contains(patternDetails)) {
            return false;
        }

        if (tryInsertItemChemicalPatternInputs(inputHolder)) {
            return true;
        }

        if (tryInsertPatternInputs(inputHolder)) {
            return true;
        }

        return tryInsertChemicalConversionInput(patternDetails, inputHolder);
    }

    private ItemStack getSingleItemInput(KeyCounter counter) {
        if (counter == null || counter.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack input = ItemStack.EMPTY;
        for (var entry : counter) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (!(key instanceof AEItemKey itemKey) || amount <= 0 || amount > Integer.MAX_VALUE || !input.isEmpty()) {
                return ItemStack.EMPTY;
            }
            input = itemKey.toStack((int) amount);
        }
        return input;
    }

    private boolean tryInsertChemicalConversionInput(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (this.machine.slotLayout() != MeMekanismMachine.SlotLayout.ITEM_CHEMICAL || this.level == null || inputHolder == null || inputHolder.length != 1) {
            return false;
        }

        ItemStack input = getSingleItemInput(inputHolder[0]);
        if (input.isEmpty()) {
            return false;
        }

        ItemStack singleInput = input.copyWithCount(1);
        ItemStackToChemicalRecipe recipe = MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findTypeBasedRecipe(this.level, singleInput);
        if (recipe == null) {
            return false;
        }

        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        if (needed <= 0 || input.getCount() < needed) {
            return false;
        }

        ChemicalStack output = recipe.getOutput(singleInput);
        if (output.isEmpty() || !matchesChemicalOutputs(patternDetails, output, output.getAmount() * needed)) {
            return false;
        }

        ItemStack remainder = insertItem(SECONDARY_INPUT_SLOT, input.copyWithCount(needed));
        if (!remainder.isEmpty()) {
            return false;
        }
        setChanged();
        return true;
    }

    private boolean tryInsertItemChemicalPatternInputs(KeyCounter[] inputHolder) {
        if (this.machine.slotLayout() != MeMekanismMachine.SlotLayout.ITEM_CHEMICAL || inputHolder == null || inputHolder.length != 2 || this.chemicalTank == null) {
            return false;
        }

        ItemStack itemInput = ItemStack.EMPTY;
        ChemicalStack chemicalInput = ChemicalStack.EMPTY;
        for (KeyCounter counter : inputHolder) {
            PatternInput input = getSinglePatternInput(counter);
            if (input == null) {
                return false;
            }
            if (!input.item().isEmpty()) {
                if (!itemInput.isEmpty()) {
                    return false;
                }
                itemInput = input.item();
            } else if (!input.chemical().isEmpty()) {
                if (!chemicalInput.isEmpty()) {
                    return false;
                }
                chemicalInput = input.chemical();
            }
        }

        if (itemInput.isEmpty() || chemicalInput.isEmpty() || !canAddChemical(chemicalInput)) {
            return false;
        }

        ItemStack itemRemainder = insertItem(INPUT_SLOT, itemInput);
        if (!itemRemainder.isEmpty()) {
            return false;
        }
        this.chemicalTank.insert(chemicalInput, Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    @Nullable
    private PatternInput getSinglePatternInput(KeyCounter counter) {
        if (counter == null || counter.isEmpty()) {
            return null;
        }

        PatternInput input = null;
        for (var entry : counter) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            PatternInput next;
            if (key instanceof AEItemKey itemKey && amount > 0 && amount <= Integer.MAX_VALUE) {
                next = new PatternInput(itemKey.toStack((int) amount), ChemicalStack.EMPTY);
            } else if (key instanceof MekanismKey chemicalKey && amount > 0) {
                next = new PatternInput(ItemStack.EMPTY, chemicalKey.getStack().copyWithAmount(amount));
            } else {
                return null;
            }
            if (input != null) {
                return null;
            }
            input = next;
        }
        return input;
    }

    private boolean matchesChemicalOutputs(IPatternDetails patternDetails, ChemicalStack expectedOutput, long expectedAmount) {
        boolean matched = false;
        for (var output : patternDetails.getOutputs()) {
            if (output.what() instanceof MekanismKey chemicalKey) {
                ChemicalStack chemicalOutput = chemicalKey.getStack();
                if (matched || !chemicalOutput.is(expectedOutput.getChemicalHolder()) || output.amount() != expectedAmount) {
                    return false;
                }
                matched = true;
            } else {
                return false;
            }
        }
        return matched;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private boolean storeInOutputSlot(ItemStack stack) {
        ItemStack existing = getStack(OUTPUT_SLOT);
        if (existing.isEmpty()) {
            setStack(OUTPUT_SLOT, stack);
            return true;
        }
        if (ItemStack.isSameItemSameComponents(existing, stack)) {
            existing.grow(stack.getCount());
            setStack(OUTPUT_SLOT, existing);
            return true;
        }
        return false;
    }

    private boolean tryInsertPatternInputs(KeyCounter[] inputHolder) {
        int[] slots = getPatternInputSlots(inputHolder);
        if (slots == null) {
            return false;
        }

        ItemStack[] simulated = new ItemStack[totalSlots()];
        for (int i = 0; i < totalSlots(); i++) {
            simulated[i] = getStack(i).copy();
        }

        for (int i = 0; i < inputHolder.length; i++) {
            KeyCounter counter = inputHolder[i];
            if (counter == null || counter.isEmpty()) {
                return false;
            }

            int slot = slots[i];
            for (var entry : counter) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (!(key instanceof AEItemKey itemKey) || amount <= 0 || amount > Integer.MAX_VALUE) {
                    return false;
                }

                ItemStack stack = itemKey.toStack((int) amount);
                if (!insertIntoSimulatedSlot(simulated, slot, stack)) {
                    return false;
                }
            }
        }

        for (int slot : slots) {
            setStack(slot, simulated[slot]);
        }
        setChanged();
        return true;
    }

    @Nullable
    private int[] getPatternInputSlots(KeyCounter[] inputHolder) {
        if (inputHolder == null) {
            return null;
        }

        return switch (this.machine.slotLayout()) {
            case SINGLE_ITEM, SAWING -> inputHolder.length == 1 ? new int[] { INPUT_SLOT } : null;
            case DOUBLE_ITEM -> inputHolder.length == 2 ? new int[] { INPUT_SLOT, SECONDARY_INPUT_SLOT } : null;
            case ITEM_CHEMICAL -> inputHolder.length == 2 ? new int[] { INPUT_SLOT, SECONDARY_INPUT_SLOT } : null;
        };
    }

    private boolean insertIntoSimulatedSlot(ItemStack[] simulated, int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= simulated.length) {
            return false;
        }

        ItemStack existing = simulated[slot];
        int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot, stack));
        if (existing.isEmpty()) {
            if (stack.getCount() > limit) {
                return false;
            }
            simulated[slot] = stack.copy();
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return false;
        }
        int existingLimit = Math.min(existing.getMaxStackSize(), getSlotLimit(slot, existing));
        if (existing.getCount() + stack.getCount() > existingLimit) {
            return false;
        }
        existing.grow(stack.getCount());
        simulated[slot] = existing;
        return true;
    }

    private void processPendingCraftingOutputs() {
        for (int i = this.pendingCraftingOutputs.size() - 1; i >= 0; i--) {
            PendingCraftingOutput pending = this.pendingCraftingOutputs.get(i);
            if (pending.delayTicks-- > 0) {
                continue;
            }

            long inserted = this.insertIntoNetwork(pending.stack);
            if (inserted > 0) {
                pending.stack.shrink((int) inserted);
            }
            if (!pending.stack.isEmpty()) {
                pending.stack = insertItem(OUTPUT_SLOT, pending.stack);
            }
            if (!pending.stack.isEmpty()) {
                pending.stack = insertItem(SECONDARY_OUTPUT_SLOT, pending.stack);
            }
            if (pending.stack.isEmpty()) {
                this.pendingCraftingOutputs.remove(i);
                setChanged();
            }
        }
    }

    private void processPendingKeyOutputs() {
        for (int i = this.pendingKeyOutputs.size() - 1; i >= 0; i--) {
            PendingKeyOutput pending = this.pendingKeyOutputs.get(i);
            if (pending.delayTicks-- > 0) {
                continue;
            }

            long inserted = this.insertIntoNetwork(pending.key, pending.amount);
            if (inserted > 0) {
                pending.amount -= inserted;
            }
            if (pending.amount <= 0) {
                this.pendingKeyOutputs.remove(i);
                setChanged();
            }
        }
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

    private final class AeTicker implements IGridTickable {
        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(TickRates.Interface, !hasAeNetworkOutputWork());
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!mainNode.isActive()) {
                return TickRateModulation.SLEEP;
            }
            boolean hadWork = hasAeNetworkOutputWork();
            boolean finished = processAeNetworkOutputs();
            if (!hasAeNetworkOutputWork()) {
                return TickRateModulation.SLEEP;
            }
            return finished || hadWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }

    private void updatePatterns() {
        this.patterns.clear();
        for (int i = PATTERN_SLOTS_START; i <= patternSlotsEnd(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                IPatternDetails pattern = PatternDetailsHelper.decodePattern(stack, this.level);
                if (pattern != null) {
                    this.patterns.add(pattern);
                }
            }
        }
        if (this.mainNode.getNode() != null) {
            ICraftingProvider.requestUpdate(this.mainNode);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_PATTERN_PRIORITY, this.patternPriority);
        tag.putInt(TAG_AE_OUTPUT_MODE, this.aeOutputMode.ordinal());
        tag.putInt(SerializationConstants.PROGRESS, this.operatingTicks);
        this.mainNode.saveToNBT(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_CHEMICAL) && this.chemicalTank != null && this.chemicalTank.isEmpty()) {
            this.chemicalTank.setStack(ChemicalStack.parseOptional(registries, tag.getCompound(TAG_CHEMICAL)));
        }
        this.patternPriority = tag.getInt(TAG_PATTERN_PRIORITY);
        this.aeOutputMode = AeOutputMode.byId(tag.getInt(TAG_AE_OUTPUT_MODE));
        this.operatingTicks = tag.getInt(SerializationConstants.PROGRESS);
        this.mainNode.loadFromNBT(tag);
        this.updatePatterns();
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            this.ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
        }
    }

    private ChemicalStack getChemicalStack() {
        return this.chemicalTank == null ? ChemicalStack.EMPTY : this.chemicalTank.getStack();
    }

    private MeMekanismMachine getMachineEarly() {
        return this.machine == null ? ModBlocks.getMachine(getBlockState().getBlock()) : this.machine;
    }

    private ItemStack getStack(int slot) {
        return slots()[slot] == null ? ItemStack.EMPTY : slots()[slot].getStack();
    }

    private void setStack(int slot, ItemStack stack) {
        if (slots()[slot] != null) {
            slots()[slot].setStack(stack);
        }
    }

    private int getSlotLimit(int slot, ItemStack stack) {
        return slots()[slot] == null ? 0 : slots()[slot].getLimit(stack);
    }

    private ItemStack insertItem(int slot, ItemStack stack) {
        return slots()[slot] == null ? stack : slots()[slot].insertItem(stack, Action.EXECUTE, AutomationType.INTERNAL);
    }

    private IInventorySlot[] slots() {
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

    private static int totalSlots() {
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
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> this.operatingTicks, ticks -> this.operatingTicks = ticks));
        container.track(SyncableInt.create(() -> this.ticksRequired, ticks -> this.ticksRequired = ticks));
        container.track(SyncableInt.create(() -> this.aeOutputMode.ordinal(), mode -> this.aeOutputMode = AeOutputMode.byId(mode)));
        container.track(SyncableLong.create(() -> getChemicalStack().getAmount(), amount -> {
            if (this.chemicalTank != null && !this.chemicalTank.isEmpty()) {
                ChemicalStack stack = this.chemicalTank.getStack().copy();
                stack.setAmount(amount);
                this.chemicalTank.setStack(stack);
            }
        }));
    }

    private record PatternInput(ItemStack item, ChemicalStack chemical) {
    }

    private static final class AeBackedEnergyContainer extends MachineEnergyContainer<MeMekanismMachineBlockEntity> {
        private final MeMekanismMachineBlockEntity owner;

        private AeBackedEnergyContainer(MeMekanismMachineBlockEntity owner, IContentsListener listener) {
            super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                    BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
            this.owner = owner;
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            long localExtracted = super.extract(amount, action, automationType);
            long remaining = amount - localExtracted;
            if (remaining <= 0 || automationType != AutomationType.INTERNAL) {
                return localExtracted;
            }
            return localExtracted + this.owner.extractAeAsFe(remaining, action);
        }
    }

    public enum AeOutputMode {
        BOTH("AE: All", true, true),
        ITEMS("AE: Item", true, false),
        CHEMICALS("AE: Chem", false, true),
        NONE("AE: Off", false, false);

        private static final AeOutputMode[] VALUES = values();
        private final String label;
        private final boolean items;
        private final boolean chemicals;

        AeOutputMode(String label, boolean items, boolean chemicals) {
            this.label = label;
            this.items = items;
            this.chemicals = chemicals;
        }

        public String label() {
            return this.label;
        }

        public boolean items() {
            return this.items;
        }

        public boolean chemicals() {
            return this.chemicals;
        }

        public AeOutputMode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public AeOutputMode toggle(mekanism.common.lib.transmitter.TransmissionType type) {
            return switch (type) {
                case ITEM -> byFlags(!this.items, this.chemicals);
                case CHEMICAL -> byFlags(this.items, !this.chemicals);
                default -> this;
            };
        }

        private static AeOutputMode byFlags(boolean items, boolean chemicals) {
            for (AeOutputMode mode : VALUES) {
                if (mode.items == items && mode.chemicals == chemicals) {
                    return mode;
                }
            }
            return BOTH;
        }

        public static AeOutputMode byId(int id) {
            return id < 0 || id >= VALUES.length ? BOTH : VALUES[id];
        }
    }
}

