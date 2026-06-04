package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeNetworkEnergyHelper;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.slot.MePatternInventorySlot;
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
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.jerry.mekmm.api.recipes.RecyclerRecipe;
import com.jerry.mekmm.api.recipes.StamperRecipe;
import com.jerry.mekmm.common.recipe.MoreMachineRecipeType;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.SawmillRecipe;
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
import mekanism.common.recipe.MekanismRecipeType;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
    private static final String TAG_PATTERN_TERMINAL_NAME = "PatternTerminalName";

    private IInventorySlot[] inventorySlots;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private final MeMekanismMachineAeOutput aeOutput;
    private final MeMekanismMachine machine;
    private IChemicalTank chemicalTank;
    private MachineEnergyContainer<MeMekanismMachineBlockEntity> energyContainer;
    private int operatingTicks;
    private int ticksRequired = BASE_TICKS_REQUIRED;
    private int patternPriority = 0;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;
    private String patternTerminalName = "";

    public MeMekanismMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
        this.actionSource = IActionSource.ofMachine(this);
        this.aeOutput = new MeMekanismMachineAeOutput(this);
        this.mainNode = GridHelper.createManagedNode(this, this)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this)
                .addService(appeng.api.networking.ticking.IGridTickable.class, this.aeOutput);
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
        this.aeOutput.process();
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

    long extractAeAsFe(long requestedFe, Action action) {
        if (requestedFe <= 0) {
            return 0;
        }
        return MeNetworkEnergyHelper.extractNetworkFe(getGrid(), this.actionSource, requestedFe, action);
    }

    private boolean canProcessSingleItemRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        if (this.machine == MeMekanismMachine.RECYCLER) {
            RecyclerRecipe recipe = MoreMachineRecipeType.RECYCLING.getInputCache().findFirstRecipe(this.level, input);
            if (recipe == null) {
                return false;
            }
            int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
            ItemStack output = recipe.getOutput(input).getMaxChanceOutput();
            return needed > 0 && !output.isEmpty() && canFitOutput(OUTPUT_SLOT, output);
        }
        ItemStackToItemStackRecipe recipe = getSingleItemRecipe(input);
        if (recipe == null) {
            return false;
        }
        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        ItemStack output = recipe.getOutput(input);
        return needed > 0 && !output.isEmpty() && canFitOutput(OUTPUT_SLOT, output);
    }

    @Nullable
    private ItemStackToItemStackRecipe getSingleItemRecipe(ItemStack input) {
        if (this.machine == MeMekanismMachine.CNC_LATHE) {
            return MoreMachineRecipeType.LATHING.getInputCache().findFirstRecipe(this.level, input);
        }
        if (this.machine == MeMekanismMachine.CNC_ROLLING_MILL) {
            return MoreMachineRecipeType.ROLLING_MILL.getInputCache().findFirstRecipe(this.level, input);
        }
        return switch (this.machine.factoryType()) {
            case ENRICHING -> MekanismRecipeType.ENRICHING.getInputCache().findFirstRecipe(this.level, input);
            case CRUSHING -> MekanismRecipeType.CRUSHING.getInputCache().findFirstRecipe(this.level, input);
            case SMELTING -> MekanismRecipeType.SMELTING.getInputCache().findFirstRecipe(this.level, input);
            case null, default -> null;
        };
    }

    private boolean canProcessCombinerRecipe() {
        ItemStack input = getStack(INPUT_SLOT);
        ItemStack secondary = getStack(SECONDARY_INPUT_SLOT);
        if (input.isEmpty() || secondary.isEmpty()) {
            return false;
        }
        if (this.machine == MeMekanismMachine.CNC_STAMPER) {
            StamperRecipe recipe = MoreMachineRecipeType.STAMPING.getInputCache().findFirstRecipe(this.level, input, secondary);
            if (recipe == null) {
                return false;
            }
            int neededInput = clampNeeded(recipe.getInput().getNeededAmount(input));
            int neededSecondary = clampNeeded(recipe.getMold().getNeededAmount(secondary));
            ItemStack output = recipe.getOutput(input, secondary);
            return neededInput > 0 && neededSecondary > 0 && !output.isEmpty() && canFitOutput(OUTPUT_SLOT, output);
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
        if (this.machine == MeMekanismMachine.RECYCLER) {
            RecyclerRecipe recipe = MoreMachineRecipeType.RECYCLING.getInputCache().findFirstRecipe(this.level, input);
            if (recipe == null) {
                return;
            }
            int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
            ItemStack output = recipe.getOutput(input).getChanceOutput();
            if (needed <= 0 || output.isEmpty() || !canFitOutput(OUTPUT_SLOT, output)) {
                return;
            }
            input.shrink(needed);
            setStack(INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);
            addToOutput(OUTPUT_SLOT, output);
            setChanged();
            return;
        }

        ItemStackToItemStackRecipe recipe = getSingleItemRecipe(input);
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
        if (this.machine == MeMekanismMachine.CNC_STAMPER) {
            StamperRecipe recipe = MoreMachineRecipeType.STAMPING.getInputCache().findFirstRecipe(this.level, input, secondary);
            if (recipe == null) {
                return;
            }
            int neededInput = clampNeeded(recipe.getInput().getNeededAmount(input));
            int neededSecondary = clampNeeded(recipe.getMold().getNeededAmount(secondary));
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
        var conversion = MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findTypeBasedRecipe(this.level, singleInput);
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

    boolean canAddChemical(ChemicalStack stack) {
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

    static int clampNeeded(long needed) {
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

    IManagedGridNode getManagedNode() {
        return this.mainNode;
    }

    public IActionSource getActionSource() {
        return this.actionSource;
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
    public String getCustomPatternTerminalName() {
        return this.patternTerminalName;
    }

    @Override
    public void setCustomPatternTerminalName(String name) {
        String sanitized = MeAeMachine.sanitizePatternTerminalName(name);
        if (this.patternTerminalName.equals(sanitized)) {
            return;
        }
        this.patternTerminalName = sanitized;
        if (this.mainNode.getNode() != null) {
            ICraftingProvider.requestUpdate(this.mainNode);
        }
        setChanged();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!this.mainNode.isActive() || !this.patterns.contains(patternDetails)) {
            return false;
        }

        return MeMekanismMachinePatternInput.push(this, patternDetails, inputHolder);
    }

    @Override
    public boolean isBusy() {
        return false;
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
        if (this.patternTerminalName.isEmpty()) {
            tag.remove(TAG_PATTERN_TERMINAL_NAME);
        } else {
            tag.putString(TAG_PATTERN_TERMINAL_NAME, this.patternTerminalName);
        }
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
        this.patternTerminalName = MeAeMachine.sanitizePatternTerminalName(tag.getString(TAG_PATTERN_TERMINAL_NAME));
        this.operatingTicks = tag.getInt(SerializationConstants.PROGRESS);
        this.mainNode.loadFromNBT(tag);
        this.updatePatterns();
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

    private MeMekanismMachine getMachineEarly() {
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

}

