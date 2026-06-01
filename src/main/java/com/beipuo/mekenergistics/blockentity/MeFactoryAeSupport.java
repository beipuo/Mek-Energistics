package com.beipuo.mekenergistics.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
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
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MeFactoryAeSupport {
    public static final int PATTERN_SLOTS = 36;

    private final MeFactoryAeMachine owner;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final List<BasicInventorySlot> patternSlots = new ArrayList<>(PATTERN_SLOTS);
    private final InternalInventory terminalPatternInventory = new PatternSlotInternalInventory(new PatternSlotOwner());
    private final List<IPatternDetails> patterns = new ArrayList<>();
    private int patternPriority;

    public MeFactoryAeSupport(MeFactoryAeMachine owner) {
        this.owner = owner;
        this.actionSource = IActionSource.ofMachine(owner);
        this.mainNode = GridHelper.createManagedNode(owner, NodeListener.INSTANCE)
                .setInWorldNode(true)
                .setTagName("node")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, owner);
        for (int i = 0; i < PATTERN_SLOTS; i++) {
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

    public InternalInventory getTerminalPatternInventory() {
        return this.terminalPatternInventory;
    }

    public PatternContainerGroup getTerminalGroup() {
        ItemStack iconStack = new ItemStack(ModBlocks.getMachineBlock(this.owner.getMachine()).get());
        AEItemKey icon = iconStack.isEmpty() ? null : AEItemKey.of(iconStack);
        return new PatternContainerGroup(icon, Component.translatable(this.owner.getMachine().translationKey()), List.of());
    }

    public IGrid getGrid() {
        IGridNode node = this.mainNode.getNode();
        return node == null || !node.isActive() ? null : node.getGrid();
    }

    public boolean insertOutputSlotsIntoNetwork(List<IInventorySlot> outputSlots) {
        MEStorage storage = getNetworkStorage();
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
            long inserted = storage.insert(key, output.getCount(), Actionable.MODULATE, this.actionSource);
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

    private MEStorage getNetworkStorage() {
        IGrid grid = getGrid();
        IStorageService storageService = grid == null ? null : grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
    }

    public void create(Level level, BlockPos pos) {
        this.mainNode.create(level, pos);
        updatePatterns();
    }

    public void destroy() {
        this.mainNode.destroy();
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

    public void updatePatterns() {
        this.patterns.clear();
        Level level = this.owner.getOwnerLevel();
        for (BasicInventorySlot patternSlot : this.patternSlots) {
            ItemStack stack = patternSlot.getStack();
            if (!stack.isEmpty()) {
                IPatternDetails pattern = PatternDetailsHelper.decodePattern(stack, level);
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
    }

    public void loadSlots(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            if (tag.contains("MePatternSlot" + i)) {
                this.patternSlots.get(i).deserializeNBT(registries, tag.getCompound("MePatternSlot" + i));
            }
        }
        updatePatterns();
    }

    private final class PatternSlotOwner implements MeAeMachine {
        @Override
        public MeMekanismMachineBlockEntity.AeOutputMode getAeOutputMode() {
            return MeMekanismMachineBlockEntity.AeOutputMode.BOTH;
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
    }

    private enum NodeListener implements IGridNodeListener<MeFactoryAeMachine> {
        INSTANCE;

        @Override
        public void onSaveChanges(MeFactoryAeMachine nodeOwner, IGridNode node) {
            nodeOwner.saveChanges();
        }
    }

    public static final class AeBackedFactoryEnergyContainer extends MachineEnergyContainer<TileEntityFactory<?>> {
        private final TileEntityFactory<?> owner;

        public AeBackedFactoryEnergyContainer(TileEntityFactory<?> owner, IContentsListener listener) {
            super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                    BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
            this.owner = owner;
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            long localExtracted = super.extract(amount, action, automationType);
            long remaining = amount - localExtracted;
            if (remaining <= 0 || automationType != AutomationType.INTERNAL || !(this.owner instanceof MeFactoryAeMachine aeMachine)) {
                return localExtracted;
            }
            return localExtracted + extractAeAsFe(aeMachine, remaining, action);
        }

        private long extractAeAsFe(MeFactoryAeMachine aeMachine, long requestedFe, Action action) {
            IGrid grid = aeMachine.getAeSupport().getGrid();
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
}
