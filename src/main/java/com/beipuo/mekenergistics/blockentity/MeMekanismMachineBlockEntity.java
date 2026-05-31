package com.beipuo.mekenergistics.blockentity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.BasicChemicalTank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeMekanismMachineBlockEntity extends TileEntityConfigurableMachine
        implements ICraftingProvider, IInWorldGridNodeHost, IGridNodeListener<MeMekanismMachineBlockEntity>, IActionHost, IHasDumpButton {
    public static final int INPUT_SLOT = 0;
    public static final int SECONDARY_INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SECONDARY_OUTPUT_SLOT = 3;
    public static final int PATTERN_SLOTS_START = 4;
    public static final int PATTERN_SLOTS_COUNT = 9;
    public static final int PATTERN_SLOTS_END = PATTERN_SLOTS_START + PATTERN_SLOTS_COUNT - 1;
    public static final int TOTAL_SLOTS = PATTERN_SLOTS_START + PATTERN_SLOTS_COUNT;

    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_PATTERN_PRIORITY = "PatternPriority";
    private static final String TAG_CHEMICAL = "Chemical";

    private final IInventorySlot[] inventorySlots = new IInventorySlot[TOTAL_SLOTS];
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private final List<PendingCraftingOutput> pendingCraftingOutputs = new ArrayList<>();
    private final MeMekanismMachine machine;
    private IChemicalTank chemicalTank;
    private MachineEnergyContainer<MeMekanismMachineBlockEntity> energyContainer;
    private int workCooldown;
    private int patternPriority = 0;

    public MeMekanismMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
        this.actionSource = IActionSource.ofMachine(this);
        this.mainNode = GridHelper.createManagedNode(this, this)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this);
        this.configComponent.setupItemIOConfig(
                inventorySlots[INPUT_SLOT],
                inventorySlots[OUTPUT_SLOT],
                inventorySlots[SECONDARY_INPUT_SLOT],
                inventorySlots[SECONDARY_OUTPUT_SLOT],
                inventorySlots[PATTERN_SLOTS_START],
                inventorySlots[PATTERN_SLOTS_START + 1],
                inventorySlots[PATTERN_SLOTS_START + 2],
                inventorySlots[PATTERN_SLOTS_START + 3],
                inventorySlots[PATTERN_SLOTS_START + 4],
                inventorySlots[PATTERN_SLOTS_START + 5],
                inventorySlots[PATTERN_SLOTS_START + 6],
                inventorySlots[PATTERN_SLOTS_START + 7],
                inventorySlots[PATTERN_SLOTS_START + 8],
                inventorySlots[SECONDARY_INPUT_SLOT] instanceof EnergyInventorySlot ? inventorySlots[SECONDARY_INPUT_SLOT] : null
        );
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
        if (this.workCooldown-- <= 0) {
            this.workCooldown = 20;
            processRecipe();
        }
        processPendingCraftingOutputs();
        insertOutputSlotIntoNetwork(OUTPUT_SLOT);
        insertOutputSlotIntoNetwork(SECONDARY_OUTPUT_SLOT);
        return sendUpdatePacket;
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(this.energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @Nullable
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        if (!this.machine.hasChemicalInput()) {
            return null;
        }
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(this.chemicalTank = BasicChemicalTank.createModern(getChemicalCapacity(), ConstantPredicates.alwaysTrueBi(), listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        inventorySlots[INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, 51, 43));
        inventorySlots[SECONDARY_INPUT_SLOT] = builder.addSlot(BasicInventorySlot.at(listener, this.machine.hasChemicalInput() ? 17 : 17, this.machine.hasChemicalInput() ? 35 : 43));
        inventorySlots[OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 109, 43));
        inventorySlots[SECONDARY_OUTPUT_SLOT] = builder.addSlot(OutputInventorySlot.at(listener, 133, 43));
        for (int slot = PATTERN_SLOTS_START; slot <= PATTERN_SLOTS_END; slot++) {
            int index = slot - PATTERN_SLOTS_START;
            int x = -54 + index % 3 * 18;
            int y = 17 + index / 3 * 18;
            int patternSlot = slot;
            inventorySlots[slot] = builder.addSlot(BasicInventorySlot.at(PatternDetailsHelper::isEncodedPattern, () -> {
                listener.onContentsChanged();
                updatePatterns();
            }, x, y, 1));
        }
        return builder.build();
    }

    public MeMekanismMachine getMachine() {
        return this.machine;
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

    public IEnergyContainer getEnergyContainer() {
        return this.energyContainer;
    }

    public IChemicalTank getChemicalTank() {
        return this.chemicalTank;
    }

    public double getScaledProgress() {
        return this.workCooldown <= 0 ? 0 : 1 - this.workCooldown / 20.0;
    }

    public void setOwner(ServerPlayer player) {
        this.mainNode.setOwningPlayer(player);
    }

    public Component getDisplayName() {
        return Component.translatable(this.machine.translationKey());
    }

    private long insertIntoNetwork(ItemStack stack) {
        MEStorage storage = this.getNetworkStorage();
        if (storage == null || stack.isEmpty()) {
            return 0;
        }

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }

        return storage.insert(key, stack.getCount(), Actionable.MODULATE, this.actionSource);
    }

    private void insertOutputSlotIntoNetwork(int slot) {
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

    private void processRecipe() {
        if (this.level == null) {
            return;
        }
        switch (this.machine.slotLayout()) {
            case SINGLE_ITEM -> processSingleItemRecipe();
            case DOUBLE_ITEM -> processCombinerRecipe();
            case SAWING -> processSawingRecipe();
            case ITEM_CHEMICAL -> processItemChemicalRecipe();
        }
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
        fillChemicalFromConversionSlot();

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
        return this.machine.factoryType() == mekanism.common.content.blocktype.FactoryType.INFUSING
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
        IGridNode node = this.mainNode.getNode();
        if (node == null || !node.isActive()) {
            return null;
        }

        IGrid grid = node.getGrid();
        if (grid == null) {
            return null;
        }

        IStorageService storageService = grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
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

        if (tryInsertPatternInputs(inputHolder)) {
            return true;
        }

        if (this.machine.slotLayout() != MeMekanismMachine.SlotLayout.ITEM_CHEMICAL) {
            return false;
        }

        for (var output : patternDetails.getOutputs()) {
            if (output.what() instanceof AEItemKey itemKey) {
                if (output.amount() > Integer.MAX_VALUE) {
                    return false;
                }
                ItemStack outputStack = itemKey.toStack((int) output.amount());
                this.pendingCraftingOutputs.add(new PendingCraftingOutput(outputStack, 1));
            } else {
                return false;
            }
        }
        return true;
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

        ItemStack[] simulated = new ItemStack[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
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

    private static final class PendingCraftingOutput {
        private ItemStack stack;
        private int delayTicks;

        private PendingCraftingOutput(ItemStack stack, int delayTicks) {
            this.stack = stack;
            this.delayTicks = delayTicks;
        }
    }

    private void updatePatterns() {
        this.patterns.clear();
        for (int i = PATTERN_SLOTS_START; i <= PATTERN_SLOTS_END; i++) {
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_PATTERN_PRIORITY, this.patternPriority);
        this.mainNode.saveToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_CHEMICAL) && this.chemicalTank != null && this.chemicalTank.isEmpty()) {
            this.chemicalTank.setStack(ChemicalStack.parseOptional(registries, tag.getCompound(TAG_CHEMICAL)));
        }
        this.patternPriority = tag.getInt(TAG_PATTERN_PRIORITY);
        this.mainNode.loadFromNBT(tag);
        this.updatePatterns();
    }

    private ChemicalStack getChemicalStack() {
        return this.chemicalTank == null ? ChemicalStack.EMPTY : this.chemicalTank.getStack();
    }

    private ItemStack getStack(int slot) {
        return inventorySlots[slot] == null ? ItemStack.EMPTY : inventorySlots[slot].getStack();
    }

    private void setStack(int slot, ItemStack stack) {
        if (inventorySlots[slot] != null) {
            inventorySlots[slot].setStack(stack);
        }
    }

    private int getSlotLimit(int slot, ItemStack stack) {
        return inventorySlots[slot] == null ? 0 : inventorySlots[slot].getLimit(stack);
    }

    private ItemStack insertItem(int slot, ItemStack stack) {
        return inventorySlots[slot] == null ? stack : inventorySlots[slot].insertItem(stack, Action.EXECUTE, AutomationType.INTERNAL);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(() -> getChemicalStack().getAmount(), amount -> {
            if (this.chemicalTank != null && !this.chemicalTank.isEmpty()) {
                ChemicalStack stack = this.chemicalTank.getStack().copy();
                stack.setAmount(amount);
                this.chemicalTank.setStack(stack);
            }
        }));
    }
}
