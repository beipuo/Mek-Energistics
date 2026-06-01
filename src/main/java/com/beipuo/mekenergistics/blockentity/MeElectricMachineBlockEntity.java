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
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityElectricMachineAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.OneInputCachedRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeElectricMachineBlockEntity extends TileEntityElectricMachine
        implements ICraftingProvider, IInWorldGridNodeHost, IGridNodeListener<MeElectricMachineBlockEntity>, IActionHost, MeAeMachine {
    private static final int PATTERN_SLOTS_COUNT = 36;
    private static final String TAG_PATTERN_PRIORITY = "PatternPriority";
    private static final String TAG_AE_OUTPUT_MODE = "AeOutputMode";

    private final MeMekanismMachine machine;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private List<BasicInventorySlot> patternSlots;
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private int patternPriority;
    private MeMekanismMachineBlockEntity.AeOutputMode aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.BOTH;

    public MeElectricMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state, BASE_TICKS_REQUIRED);
        this.machine = machine;
        this.actionSource = IActionSource.ofMachine(this);
        this.mainNode = GridHelper.createManagedNode(this, this)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this);
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<SingleRecipeInput, ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> getRecipeType() {
        return switch (this.machine.factoryType()) {
            case ENRICHING -> MekanismRecipeType.ENRICHING;
            case CRUSHING -> MekanismRecipeType.CRUSHING;
            case SMELTING -> MekanismRecipeType.SMELTING;
            default -> MekanismRecipeType.ENRICHING;
        };
    }

    @Override
    public IRecipeViewerRecipeType<ItemStackToItemStackRecipe> recipeViewerType() {
        return switch (this.machine.factoryType()) {
            case ENRICHING -> RecipeViewerRecipeType.ENRICHING;
            case CRUSHING -> RecipeViewerRecipeType.CRUSHING;
            case SMELTING -> RecipeViewerRecipeType.SMELTING;
            default -> RecipeViewerRecipeType.ENRICHING;
        };
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        MachineEnergyContainer<TileEntityElectricMachine> energyContainer = new AeBackedEnergyContainer(this, recipeCacheUnpauseListener);
        ((TileEntityElectricMachineAccessor) this).mekenergistics$setEnergyContainer(energyContainer);
        builder.addContainer(energyContainer);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        IInventorySlotHolder original = super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener);
        InventorySlotHelper patternBuilder = InventorySlotHelper.readOnly();
        this.patternSlots = new ArrayList<>(PATTERN_SLOTS_COUNT);
        for (int i = 0; i < PATTERN_SLOTS_COUNT; i++) {
            this.patternSlots.add(patternBuilder.addSlot(MePatternInventorySlot.create(PatternDetailsHelper::isEncodedPattern, () -> {
                listener.onContentsChanged();
                updatePatterns();
            })));
        }
        IInventorySlotHolder patterns = patternBuilder.build();
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(patterns.getInventorySlots(side));
            return slots;
        };
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        insertOutputSlotIntoNetwork();
        return sendUpdatePacket;
    }

    @NotNull
    @Override
    public CachedRecipe<ItemStackToItemStackRecipe> createNewCachedRecipe(@NotNull ItemStackToItemStackRecipe recipe, int cacheIndex) {
        MachineEnergyContainer<TileEntityElectricMachine> energyContainer = getEnergyContainer();
        IEnergyContainer recipeEnergyView = energyContainer instanceof AeBackedEnergyContainer aeBackedEnergyContainer
                ? new RecipeEnergyView(aeBackedEnergyContainer)
                : energyContainer;
        return OneInputCachedRecipe.itemToItem(recipe, recheckAllRecipeErrors, inputHandler, outputHandler)
              .setErrorsChanged(this::onErrorsChanged)
              .setCanHolderFunction(this::canFunction)
              .setActive(this::setActive)
              .setEnergyRequirements(energyContainer::getEnergyPerTick, recipeEnergyView)
              .setRequiredTicks(this::getTicksRequired)
              .setOnFinish(this::markForSave)
              .setOperatingTicksChanged(this::setOperatingTicks);
    }

    private void insertOutputSlotIntoNetwork() {
        if (!this.aeOutputMode.items()) {
            return;
        }
        OutputInventorySlot outputSlot = ((TileEntityElectricMachineAccessor) this).mekenergistics$getOutputSlot();
        ItemStack output = outputSlot.getStack();
        if (output.isEmpty()) {
            return;
        }
        long inserted = insertIntoNetwork(output);
        if (inserted <= 0) {
            return;
        }
        output.shrink((int) inserted);
        outputSlot.setStack(output.isEmpty() ? ItemStack.EMPTY : output);
        setChanged();
    }

    private long insertIntoNetwork(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        MEStorage storage = getNetworkStorage();
        return storage == null ? 0 : storage.insert(key, stack.getCount(), Actionable.MODULATE, this.actionSource);
    }

    @Nullable
    private MEStorage getNetworkStorage() {
        IGrid grid = getGrid();
        IStorageService storageService = grid == null ? null : grid.getService(IStorageService.class);
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
    public void onSaveChanges(MeElectricMachineBlockEntity nodeOwner, IGridNode node) {
        setChanged();
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
        if (!this.mainNode.isActive() || !this.patterns.contains(patternDetails) || inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        ItemStack input = getSingleItemInput(inputHolder[0]);
        if (input.isEmpty()) {
            return false;
        }
        InputInventorySlot inputSlot = ((TileEntityElectricMachineAccessor) this).mekenergistics$getInputSlot();
        ItemStack simulated = inputSlot.insertItem(input.copy(), Action.SIMULATE, AutomationType.INTERNAL);
        if (!simulated.isEmpty()) {
            return false;
        }
        inputSlot.insertItem(input, Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
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

    @Override
    public boolean isBusy() {
        return false;
    }

    private void updatePatterns() {
        this.patterns.clear();
        for (BasicInventorySlot patternSlot : this.patternSlots) {
            ItemStack stack = patternSlot.getStack();
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
        this.mainNode.saveToNBT(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.patternPriority = tag.getInt(TAG_PATTERN_PRIORITY);
        this.aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.byId(tag.getInt(TAG_AE_OUTPUT_MODE));
        this.mainNode.loadFromNBT(tag);
        updatePatterns();
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> this.aeOutputMode.ordinal(),
                mode -> this.aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.byId(mode)));
    }

    @Override
    public MeMekanismMachineBlockEntity.AeOutputMode getAeOutputMode() {
        return this.aeOutputMode;
    }

    @Override
    public void cycleAeOutputMode() {
        this.aeOutputMode = this.aeOutputMode.next();
        setChanged();
    }

    @Override
    public void setOwner(ServerPlayer player) {
        this.mainNode.setOwningPlayer(player);
    }

    @Override
    public List<BasicInventorySlot> getPatternSlots() {
        return this.patternSlots == null ? Collections.emptyList() : Collections.unmodifiableList(this.patternSlots);
    }

    @Override
    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    @Override
    public ItemStack getTerminalIconStack() {
        return new ItemStack(ModBlocks.getMachineBlock(this.machine).get());
    }

    private static final class AeBackedEnergyContainer extends MachineEnergyContainer<TileEntityElectricMachine> {
        private final MeElectricMachineBlockEntity owner;

        private AeBackedEnergyContainer(MeElectricMachineBlockEntity owner, IContentsListener listener) {
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
            return localExtracted + extractAeAsFe(remaining, action);
        }

        private long extractAeAsFe(long requestedFe, Action action) {
            IGridNode node = this.owner.mainNode == null ? null : this.owner.mainNode.getNode();
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
    }

    private static final class RecipeEnergyView implements IEnergyContainer {
        private final AeBackedEnergyContainer energyContainer;

        private RecipeEnergyView(AeBackedEnergyContainer energyContainer) {
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
}
