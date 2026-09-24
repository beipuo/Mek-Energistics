package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MePatternIoOwner;
import com.beipuo.mekenergistics.blockentity.slot.MePatternInventorySlot;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeCountedInputAdmission;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.blockentity.support.io.MePatternIoAdapter;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.upgrade.MeInterfaceConfig;
import com.beipuo.mekenergistics.upgrade.MeInterfaceInventory;
import com.beipuo.mekenergistics.upgrade.MeMachineMode;
import com.beipuo.mekenergistics.upgrade.MePassiveCraftingDispatcher;
import com.beipuo.mekenergistics.upgrade.MePassiveCraftingSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractMeAeSupport<O extends MePatternIoOwner> {
    public static final int AE_PATTERN_SCHEMA = 2;
    private static final String TAG_PATTERN_SCHEMA = "AePatternSchema";
    private static final String TAG_PATTERN_TERMINAL_NAME = "PatternTerminalName";
    private static final String TAG_TERMINAL_VISIBLE = "PatternTerminalVisible";

    protected final O owner;
    protected final TileEntityMekanism ownerTile;
    protected final IActionSource actionSource;
    protected final List<BasicInventorySlot> patternSlots;
    private final List<BasicInventorySlot> patternSlotsView;
    protected final List<IPatternDetails> patterns = new ArrayList<>();
    /** Deferred cache recovery for block entities whose slots loaded before their Level was attached. */
    private boolean patternCacheNeedsRebuild;
    private boolean rebuildingPatternCache;
    protected final MeSmartPatternMultiplication smartPatternMultiplication = new MeSmartPatternMultiplication();
    private final MePassiveCraftingSettings passiveCraftingSettings = new MePassiveCraftingSettings();
    private Boolean clientPassiveCraftingEnabled;
    private final MeInterfaceConfig interfaceConfig;
    private final MeInterfaceInventory interfaceInventory;
    private final List<GenericStack> interfaceRecoveryBuffer = new ArrayList<>();
    private Boolean clientInterfaceMode;

    protected int patternPriority;
    protected String patternTerminalName = "";
    protected boolean visibleInPatternAccessTerminal = true;

    private final MeAeNodeLifecycle<O> nodeLifecycle;

    protected AbstractMeAeSupport(O owner) {
        this.owner = owner;
        this.ownerTile = owner.getAeOwnerTile();
        this.actionSource = IActionSource.ofMachine(owner);
        this.nodeLifecycle = new MeAeNodeLifecycle<>(owner, this.ownerTile, () -> new AeTicker(), () -> rebuildPatternCache(false));
        List<BasicInventorySlot> externalSlots = owner.getExternalPatternSlots();
        if (!externalSlots.isEmpty()) {
            this.patternSlots = externalSlots;
        } else {
            int slotCount = MekEnergisticsConfig.patternSlots();
            this.patternSlots = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                this.patternSlots.add(MePatternInventorySlot.create(PatternDetailsHelper::isEncodedPattern, this::updatePatterns));
            }
        }
        this.patternSlotsView = Collections.unmodifiableList(this.patternSlots);
        this.interfaceConfig = new MeInterfaceConfig(() -> {
            this.owner.saveChanges();
            alertAeTicker();
        });
        this.interfaceInventory = new MeInterfaceInventory(() -> {
            this.owner.saveChanges();
            alertAeTicker();
        });
    }

    public final IManagedGridNode getMainNode() {
        return this.nodeLifecycle.getMainNode();
    }

    /**
     * @param side ignored — AE2 rejects any node that is not exposed on the side it asked for, and the
     *             node already carries the machine's real exposed faces.
     */
    public final IGridNode getLargeMachineGridNode(BlockPos position, Direction side) {
        return this.nodeLifecycle.getLargeMachineGridNode(position, side);
    }

    /** Wrapped once: the backing list is final, and the inventory queries this on every call. */
    public final List<BasicInventorySlot> getPatternSlots() {
        return this.patternSlotsView;
    }

    /**
     * Physical pattern I/O is structural: every port is a stateless wrapper holding a final
     * reference to a slot or tank that the owner creates once and never replaces. Rebuilding it
     * allocates a port per slot/tank plus the enclosing lists, and the tick path queries it about
     * nine times per machine, so it is built once and reused.
     *
     * <p>Only the structure is cached. {@link MePatternIoOwner#isPatternBusy()} is per-tick state
     * and is always read live.
     */
    private MeInputLayout cachedInputLayout;
    private List<? extends MeOutputPort> cachedOutputPorts;
    private Boolean lastPatternBusy;

    private MeInputLayout patternInputLayout() {
        if (this.cachedInputLayout == null) {
            this.cachedInputLayout = this.owner.getPatternInputLayout();
        }
        return this.cachedInputLayout;
    }

    private List<? extends MeOutputPort> patternOutputPorts() {
        if (this.cachedOutputPorts == null) {
            this.cachedOutputPorts = List.copyOf(this.owner.getPatternOutputPorts());
        }
        return this.cachedOutputPorts;
    }

    /** Drops the cached I/O structure; call if an owner ever swaps a slot or tank instance. */
    public final void invalidatePatternIoCache() {
        this.cachedInputLayout = null;
        this.cachedOutputPorts = null;
    }

    public final MePatternIoAdapter getPatternIoAdapter() {
        return new MePatternIoAdapter(patternInputLayout(), patternOutputPorts(), this.owner.isPatternBusy());
    }

    public final boolean isPatternBusy() {
        return this.smartPatternMultiplication.hasPendingWork() || this.owner.isPatternBusy();
    }

    /** Returns whether the exact pattern or a same-definition registered pattern is available. */
    public final boolean hasRegisteredPattern(IPatternDetails patternDetails) {
        return patternDetails != null && (this.patterns.contains(patternDetails)
                || hasMatchingPatternDefinition(this.patterns, patternDetails));
    }

    /** Returns the physical input capacity available for a counted CPU submission. */
    public final long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
        if (oneCraftInputs == null || !this.nodeLifecycle.getMainNode().isActive() || isPatternBusy()) {
            return 0;
        }
        return patternInputLayout().maxAcceptedCopies(oneCraftInputs);
    }

    /**
     * Returns a conservative provider-level parallelism value when no concrete pattern inputs are
     * available. The counted admission path remains authoritative for the actual batch size.
     */
    public final int getAvailableParallelSlots() {
        return this.nodeLifecycle.getMainNode().isActive() && !isPatternBusy() ? 1 : 0;
    }

    /** Routes pre-scaled counted inputs without invoking Mek-Energistics smart multiplication. */
    public final boolean routeDataPatternInputs(KeyCounter[] scaledInputs) {
        if (scaledInputs == null || !this.nodeLifecycle.getMainNode().isActive() || isPatternBusy()) {
            return false;
        }
        return routePatternInputs(scaledInputs);
    }

    public final boolean pushPatternWithAdapter(IPatternDetails patternDetails, KeyCounter[] inputs) {
        if (!this.nodeLifecycle.getMainNode().isActive() || patternDetails == null || inputs == null
                || this.owner.isPatternBusy()) {
            return false;
        }
        boolean exactPattern = this.patterns.contains(patternDetails);
        boolean registeredPattern = exactPattern || hasMatchingPatternDefinition(this.patterns, patternDetails);
        return dispatchWithSmartPatternFallback(
                exactPattern,
                registeredPattern,
                this.smartPatternMultiplication,
                patternDetails,
                inputs,
                () -> {
                    MekEnergistics.LOGGER.warn(
                            "Disabling smart pattern multiplication for {} after an incompatible crafting CPU batch",
                            this.owner.getGridNodePosition());
                    setSmartPatternMultiplicationEnabled(false);
                },
                () -> routePatternInputs(inputs));
    }

    /**
     * Compatibility fallback for CPU paths without the DataEnergistics counted-provider contract:
     * some addon CPUs pre-batch pattern inputs, but their multiplier cannot be coordinated with our
     * smart queue. If their input shape cannot be represented, disable this machine's multiplier
     * and accept the CPU-provided batch directly. Counted CPU integrations use their dedicated
     * admission contracts instead of this fallback.
     */
    static boolean dispatchWithSmartPatternFallback(boolean exactPattern, boolean registeredPattern,
            MeSmartPatternMultiplication multiplication, IPatternDetails patternDetails, KeyCounter[] inputs,
            Runnable disableMultiplication, BooleanSupplier directDispatch) {
        if (!registeredPattern) {
            return false;
        }
        if (multiplication.isEnabled() && exactPattern) {
            if (multiplication.enqueue(patternDetails, inputs)) {
                return true;
            }
        }
        if (multiplication.isEnabled()) {
            disableMultiplication.run();
        }
        return directDispatch.getAsBoolean();
    }

    private static boolean hasMatchingPatternDefinition(List<IPatternDetails> registeredPatterns,
            IPatternDetails candidate) {
        AEItemKey definition = candidate.getDefinition();
        if (definition == null) {
            return false;
        }
        for (IPatternDetails registered : registeredPatterns) {
            if (definition.equals(registered.getDefinition())) {
                return true;
            }
        }
        return false;
    }

    private boolean routePatternInputs(KeyCounter[] inputs) {
        boolean changed = patternInputLayout().route(inputs);
        if (changed) {
            this.owner.saveChanges();
        }
        return changed;
    }

    protected final boolean processSmartPatternViaAdapter() {
        MeInputLayout layout = patternInputLayout();
        boolean busy = this.owner.isPatternBusy();
        if (this.lastPatternBusy == null || this.lastPatternBusy != busy) {
            this.smartPatternMultiplication.wake();
            this.lastPatternBusy = busy;
        }
        if (layout.isEmpty() || busy) {
            return false;
        }
        return processSmartPattern(new MeSmartPatternMultiplication.CapacityAwareFeeder() {
            @Override
            public boolean feed(KeyCounter[] oneCraftInputs) {
                return layout.route(oneCraftInputs);
            }

            @Override
            public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
                return layout.maxAcceptedCopies(oneCraftInputs);
            }
        });
    }

    public final boolean hasPatternOutputBacklog(
            com.beipuo.mekenergistics.blockentity.api.AeOutputMode mode) {
        for (MeOutputPort output : patternOutputPorts()) {
            AEKey key = output.key();
            if (key != null && output.amount() > 0 && outputModeAllows(mode, key)) {
                return true;
            }
        }
        return false;
    }

    public final boolean drainPatternOutputs(
            com.beipuo.mekenergistics.blockentity.api.AeOutputMode mode) {
        return drainOutputPorts(mode, patternOutputPorts());
    }

    public final IInventorySlotHolder withPatternSlots(IInventorySlotHolder original) {
        if (original instanceof MePatternSlotInventoryHolder) {
            return original;
        }
        return new PatternSlotHolder(original, this.patternSlots);
    }

    private static final class PatternSlotHolder implements MePatternSlotInventoryHolder {
        private final IInventorySlotHolder original;
        private final List<? extends mekanism.api.inventory.IInventorySlot> patternSlots;

        private PatternSlotHolder(IInventorySlotHolder original,
                List<? extends mekanism.api.inventory.IInventorySlot> patternSlots) {
            this.original = original;
            this.patternSlots = patternSlots;
        }

        @Override
        public List<mekanism.api.inventory.IInventorySlot> getInventorySlots(net.minecraft.core.Direction side) {
            List<mekanism.api.inventory.IInventorySlot> slots = new ArrayList<>(this.original.getInventorySlots(side));
            slots.addAll(this.patternSlots);
            return slots;
        }
    }

    public final List<IPatternDetails> getAvailablePatterns() {
        ensurePatternCacheReady();
        return Collections.unmodifiableList(this.patterns);
    }

    /**
     * Pattern slots can be restored before a Mekanism block entity has a Level. AE2's pattern
     * decoder needs that Level, so an early rebuild would otherwise clear the cache permanently.
     * Rebuild lazily once the owner is attached to a live level and keep the provider update
     * inside the same guard to avoid recursion through AE2's crafting service.
     */
    private void ensurePatternCacheReady() {
        if (!this.patternCacheNeedsRebuild || this.rebuildingPatternCache || this.ownerTile.getLevel() == null) {
            return;
        }
        this.rebuildingPatternCache = true;
        try {
            rebuildPatternCache(false);
        } finally {
            this.rebuildingPatternCache = false;
        }
    }

    public final int getPatternPriority() {
        return this.patternPriority;
    }

    public final String getPatternTerminalName() {
        return MePatternTerminalNames.get(this.ownerTile, this.patternTerminalName);
    }

    public final void setPatternTerminalName(String name) {
        if (!MePatternTerminalNames.set(this.ownerTile, name, this.patternTerminalName)) {
            return;
        }
        this.patternTerminalName = com.beipuo.mekenergistics.blockentity.api.MeAeMachine.sanitizePatternTerminalName(name);
        requestCraftingUpdate();
    }

    public final boolean isVisibleInPatternAccessTerminal() {
        return this.visibleInPatternAccessTerminal;
    }

    public final void setVisibleInPatternAccessTerminal(boolean visible) {
        if (this.visibleInPatternAccessTerminal == visible) {
            return;
        }
        this.visibleInPatternAccessTerminal = visible;
        requestCraftingUpdate();
        this.owner.saveChanges();
    }

    public final void createOnFirstTick() {
        this.nodeLifecycle.createOnFirstTick();
    }

    public final void refreshAfterWorldMutation() {
        GridHelper.onFirstTick(this.ownerTile, tile -> {
            this.nodeLifecycle.create();
            rebuildPatternCache(false);
            net.minecraft.world.level.Level level = this.ownerTile.getLevel();
            if (level == null || level.isClientSide()) {
                return;
            }
            BlockPos pos = this.ownerTile.getBlockPos();
            level.invalidateCapabilities(pos);
            for (Direction direction : Direction.values()) {
                level.invalidateCapabilities(pos.relative(direction));
            }
            level.updateNeighborsAt(pos, this.ownerTile.getBlockState().getBlock());
        });
    }

    public final void create(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        this.nodeLifecycle.create(level, pos);
    }

    /** Keeps every node of the machine attributed to the same player for AE2's security checks. */
    public final void setOwningPlayer(net.minecraft.server.level.ServerPlayer player) {
        this.nodeLifecycle.setOwningPlayer(player);
    }

    public final void destroyNode() {
        this.nodeLifecycle.destroyNode();
    }

    public final IGrid getGrid() {
        return this.nodeLifecycle.getGrid();
    }

    /** Refills every local FE buffer before Mekanism evaluates machine work for this tick. */
    public final void refillLocalEnergyBuffers() {
        if (isInterfaceMode()) {
            return;
        }
        IGrid grid = getGrid();
        if (grid == null) {
            return;
        }
        for (IEnergyContainer energyContainer : this.ownerTile.getEnergyContainers(null)) {
            MeNetworkEnergyHelper.refillLocalEnergy(energyContainer, grid, this.actionSource);
        }
    }

    public final boolean isSmartPatternMultiplicationEnabled() {
        return !isInterfaceMode() && this.smartPatternMultiplication.isEnabled();
    }

    public final void setSmartPatternMultiplicationEnabled(boolean enabled) {
        if (this.smartPatternMultiplication.isEnabled() == enabled) {
            return;
        }
        this.smartPatternMultiplication.setEnabled(enabled);
        this.owner.saveChanges();
        alertAeTicker();
    }

    public final MePassiveCraftingSettings getPassiveCraftingSettings() {
        return passiveCraftingSettings;
    }

    public final Boolean getClientPassiveCraftingEnabled() {
        return clientPassiveCraftingEnabled;
    }

    public final void setClientPassiveCraftingEnabled(boolean enabled) {
        clientPassiveCraftingEnabled = enabled;
    }

    /** True when the ME output interface upgrade drives this machine instead of pattern mode. */
    public final boolean isInterfaceMode() {
        return this.owner instanceof MeAeMachine aeMachine && MeAeMachine.modeOf(aeMachine).isOutputInterface();
    }

    public final Boolean getClientInterfaceMode() {
        return this.clientInterfaceMode;
    }

    public final void setClientInterfaceMode(boolean enabled) {
        this.clientInterfaceMode = enabled;
    }

    public final MeInterfaceConfig getInterfaceConfig() {
        return this.interfaceConfig;
    }

    public final MeInterfaceInventory getInterfaceInventory() {
        return this.interfaceInventory;
    }

    public final boolean hasInterfaceRecovery() {
        return !this.interfaceRecoveryBuffer.isEmpty();
    }

    /** True when uninstalling the interface upgrade cannot orphan stocked or buffered items. */
    public final boolean canRemoveInterfaceUpgrade() {
        return this.interfaceInventory.isEmpty() && !hasInterfaceRecovery();
    }

    /** Interface mode has work when there is an output backlog, buffered remainders, or any
     * configured supply slot worth retrying. */
    public final boolean hasInterfaceWork() {
        return isInterfaceMode() && (hasInterfaceRecovery()
                || this.interfaceConfig.hasConfiguredSlots()
                || !this.interfaceInventory.isEmpty()
                || hasPatternOutputBacklog(com.beipuo.mekenergistics.blockentity.api.AeOutputMode.ALL));
    }

    /**
     * Runs one interface-mode tick: retry buffered remainders, move the stocked interface row into
     * machine inputs, refill that row to its configured targets, then return machine outputs to ME.
     */
    public final boolean processInterfaceMode() {
        if (!isInterfaceMode()) {
            return false;
        }
        boolean changed = false;
        changed |= flushInterfaceRecovery();
        changed |= drainInterfaceInventoryToMachine();
        changed |= restockInterfaceInventory();
        changed |= drainPatternOutputs(com.beipuo.mekenergistics.blockentity.api.AeOutputMode.ALL);
        return changed;
    }

    private boolean drainInterfaceInventoryToMachine() {
        if (this.ownerTile.getLevel() == null || this.ownerTile.getLevel().isClientSide) {
            return false;
        }
        MeInputLayout layout = patternInputLayout();
        if (layout.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < this.interfaceInventory.size(); i++) {
            GenericStack configured = this.interfaceConfig.getStack(i);
            GenericStack stocked = this.interfaceInventory.getStack(i);
            if (stocked == null || !(stocked.what() instanceof AEItemKey key) || stocked.amount() <= 0) {
                continue;
            }
            if (configured == null || !configured.what().equals(key)) {
                long removed = this.interfaceInventory.extract(i, key, stocked.amount(), Actionable.MODULATE);
                refundToNetworkOrBuffer(key, removed);
                changed |= removed > 0;
                continue;
            }
            long accepted = simulateAccepted(key, stocked.amount(), layout);
            if (accepted <= 0) {
                continue;
            }
            long extracted = this.interfaceInventory.extract(i, key, accepted, Actionable.MODULATE);
            if (extracted <= 0) {
                continue;
            }
            if (layout.routeInterface(key, extracted)) {
                changed = true;
            } else {
                long restored = this.interfaceInventory.insert(i, key, extracted, Actionable.MODULATE);
                if (restored < extracted) {
                    refundToNetworkOrBuffer(key, extracted - restored);
                }
                changed = true;
            }
        }
        return changed;
    }

    private boolean restockInterfaceInventory() {
        IGrid grid = getGrid();
        MEStorage network = getNetworkStorage(grid);
        if (this.ownerTile.getLevel() == null || this.ownerTile.getLevel().isClientSide
                || grid == null || network == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < this.interfaceConfig.size(); i++) {
            GenericStack configured = this.interfaceConfig.getStack(i);
            GenericStack stocked = this.interfaceInventory.getStack(i);
            if (configured == null || !(configured.what() instanceof AEItemKey key) || configured.amount() <= 0) {
                if (stocked != null && stocked.amount() > 0) {
                    long removed = this.interfaceInventory.extract(i, stocked.what(), stocked.amount(), Actionable.MODULATE);
                    refundToNetworkOrBuffer(stocked.what(), removed);
                    changed |= removed > 0;
                }
                continue;
            }
            if (stocked != null && !stocked.what().equals(key)) {
                long removed = this.interfaceInventory.extract(i, stocked.what(), stocked.amount(), Actionable.MODULATE);
                refundToNetworkOrBuffer(stocked.what(), removed);
                changed |= removed > 0;
                stocked = null;
            }
            long current = stocked == null ? 0 : stocked.amount();
            long missing = Math.max(0, configured.amount() - current);
            long capacity = this.interfaceInventory.insert(i, key, missing, Actionable.SIMULATE);
            if (capacity <= 0) {
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(grid.getEnergyService(), network, key, capacity, this.actionSource);
            if (extracted <= 0) {
                continue;
            }
            long inserted = this.interfaceInventory.insert(i, key, extracted, Actionable.MODULATE);
            if (inserted < extracted) {
                refundToNetworkOrBuffer(key, extracted - inserted);
            }
            changed = true;
        }
        return changed;
    }

    /** Returns how many units of {@code key} the machine input layout accepts for one request. */
    private static long simulateAccepted(AEKey key, long requested, MeInputLayout layout) {
        return Math.max(0, layout.maxAcceptedInterfaceAmount(key, requested));
    }

    /** Returns unexpected interface supplies to the AE network; buffers what the network rejects. */
    private void refundToNetworkOrBuffer(AEKey key, long amount) {
        MEStorage storage = getNetworkStorage(getGrid());
        if (storage == null) {
            bufferInterfaceRemainder(key, amount);
            return;
        }
        long inserted = storage.insert(key, amount, Actionable.MODULATE, this.actionSource);
        long remainder = amount - inserted;
        if (remainder > 0) {
            bufferInterfaceRemainder(key, remainder);
        }
    }

    private void bufferInterfaceRemainder(AEKey key, long amount) {
        if (key == null || amount <= 0) {
            return;
        }
        for (int i = 0; i < this.interfaceRecoveryBuffer.size(); i++) {
            GenericStack entry = this.interfaceRecoveryBuffer.get(i);
            if (entry.what().equals(key)) {
                long merged = entry.amount() > Long.MAX_VALUE - amount ? Long.MAX_VALUE : entry.amount() + amount;
                this.interfaceRecoveryBuffer.set(i, new GenericStack(key, merged));
                return;
            }
        }
        this.interfaceRecoveryBuffer.add(new GenericStack(key, amount));
    }

    /** Returns buffered interface remainders to the AE network when space becomes available. */
    private boolean flushInterfaceRecovery() {
        if (this.interfaceRecoveryBuffer.isEmpty()) {
            return false;
        }
        MEStorage storage = getNetworkStorage(getGrid());
        if (storage == null) {
            return false;
        }
        boolean changed = false;
        List<GenericStack> remaining = new ArrayList<>();
        for (GenericStack entry : this.interfaceRecoveryBuffer) {
            long inserted = storage.insert(entry.what(), entry.amount(), Actionable.MODULATE, this.actionSource);
            long leftover = entry.amount() - inserted;
            changed |= inserted > 0;
            if (leftover > 0) {
                remaining.add(new GenericStack(entry.what(), leftover));
            }
        }
        this.interfaceRecoveryBuffer.clear();
        this.interfaceRecoveryBuffer.addAll(remaining);
        if (changed) {
            this.owner.saveChanges();
        }
        return changed;
    }

    private static final String TAG_INTERFACE_RECOVERY = "MeInterfaceRecovery";

    private void saveInterfaceRecovery(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.interfaceRecoveryBuffer.isEmpty()) {
            tag.remove(TAG_INTERFACE_RECOVERY);
            return;
        }
        ListTag list = new ListTag();
        for (GenericStack entry : this.interfaceRecoveryBuffer) {
            list.add(GenericStack.writeTag(registries, entry));
        }
        tag.put(TAG_INTERFACE_RECOVERY, list);
    }

    private void loadInterfaceRecovery(CompoundTag tag, HolderLookup.Provider registries) {
        this.interfaceRecoveryBuffer.clear();
        ListTag list = tag.getList(TAG_INTERFACE_RECOVERY, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                GenericStack stack = GenericStack.readTag(registries, list.getCompound(i));
                if (stack != null && stack.what() != null && stack.amount() > 0) {
                    this.interfaceRecoveryBuffer.add(stack);
                }
            } catch (RuntimeException ignored) {
                // A corrupt recovery entry must never fail the machine load; skip it.
            }
        }
    }

    public final void setPassiveCraftingSettings(int intervalTicks, long multiplier) {
        passiveCraftingSettings.set(intervalTicks, multiplier);
        this.owner.saveChanges();
    }

    protected final boolean processPassiveCrafting(boolean enabled) {
        if (!enabled) {
            return false;
        }
        if (this.smartPatternMultiplication.isEnabled()) {
            setSmartPatternMultiplicationEnabled(false);
        }
        if (!passiveCraftingSettings.tick() || this.ownerTile.getLevel() == null
                || this.ownerTile.getLevel().isClientSide || isPatternBusy()) {
            return false;
        }
        IGrid grid = getGrid();
        MEStorage storage = getNetworkStorage(grid);
        if (storage == null) {
            return false;
        }
        boolean changed = MePassiveCraftingDispatcher.submitAvailable(
                this.patterns, passiveCraftingSettings.multiplier(), this.ownerTile.getLevel(), storage,
                this.actionSource, this::routeDataPatternInputs, this.passiveCraftingSettings);
        if (changed) {
            this.owner.saveChanges();
        }
        return changed;
    }

    public final boolean enqueueSmartPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!this.smartPatternMultiplication.enqueue(patternDetails, inputHolder)) {
            return false;
        }
        this.owner.saveChanges();
        alertAeTicker();
        return true;
    }

    public final boolean drainOutputPorts(com.beipuo.mekenergistics.blockentity.api.AeOutputMode mode,
            List<? extends MeOutputPort> outputPorts) {
        boolean changed = drainOutputPorts(mode, outputPorts, this::insertIntoNetwork);
        if (changed) {
            this.owner.saveChanges();
        }
        return changed;
    }

    static boolean drainOutputPorts(com.beipuo.mekenergistics.blockentity.api.AeOutputMode mode,
            List<? extends MeOutputPort> outputPorts, BiFunction<AEKey, Long, Long> networkInserter) {
        boolean changed = false;
        for (MeOutputPort output : outputPorts) {
            AEKey key = output.key();
            long available = output.amount();
            if (key == null || available <= 0 || !outputModeAllows(mode, key)) {
                continue;
            }
            long inserted = Math.min(available, Math.max(0, networkInserter.apply(key, available)));
            if (inserted > 0) {
                changed |= output.extract(inserted, mekanism.api.Action.EXECUTE) > 0;
            }
        }
        return changed;
    }

    private static boolean outputModeAllows(
            com.beipuo.mekenergistics.blockentity.api.AeOutputMode mode, AEKey key) {
        return key instanceof AEItemKey ? mode.items()
                : key instanceof AEFluidKey ? mode.fluids() : mode.chemicals();
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

    public final void alertAeTicker() {
        this.nodeLifecycle.alertAeTicker();
    }

    protected final long insertIntoNetwork(AEKey key, long amount) {
        IGrid grid = getGrid();
        MEStorage storage = getNetworkStorage(grid);
        return storage == null || key == null || amount <= 0 ? 0
                : StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, amount, this.actionSource);
    }

    protected final MEStorage getNetworkStorage(IGrid grid) {
        IStorageService storageService = grid == null ? null : grid.getService(IStorageService.class);
        return storageService == null ? null : storageService.getInventory();
    }

    /**
     * Returns a quarantined smart-multiplication balance to the AE network. Returns 0 when the
     * machine has no active grid yet (normal during deserialization), so the caller keeps the
     * balance in the quarantined NBT instead of losing it.
     */
    private long refundPendingBalance(List<GenericStack> inputs, long remaining) {
        if (inputs == null || inputs.isEmpty() || remaining <= 0) {
            return 0;
        }
        MEStorage storage = getNetworkStorage(getGrid());
        if (storage == null) {
            return 0;
        }
        long refunded = 0;
        for (GenericStack input : inputs) {
            if (input == null || input.what() == null) {
                continue;
            }
            long amount = MePendingPatternStore.scaleAmountClamped(input.amount(), remaining);
            if (amount <= 0) {
                continue;
            }
            long inserted = storage.insert(input.what(), amount, Actionable.MODULATE, this.actionSource);
            refunded = refunded > Long.MAX_VALUE - inserted ? Long.MAX_VALUE : refunded + inserted;
        }
        return refunded;
    }

    public final void updatePatterns() {
        rebuildPatternCache(true);
    }

    private void rebuildPatternCache(boolean saveChanges) {
        if (this.ownerTile.getLevel() == null) {
            this.patternCacheNeedsRebuild = true;
            this.nodeLifecycle.markCraftingUpdatePending();
            return;
        }
        this.patterns.clear();
        for (BasicInventorySlot patternSlot : this.patternSlots) {
            ItemStack stack = patternSlot.getStack();
            if (!stack.isEmpty()) {
                IPatternDetails pattern = MePatternDecodeHelper.safeDecode(stack, this.ownerTile.getLevel(),
                        this.ownerTile.getBlockPos(), patternOwnerName());
                if (pattern != null) {
                    this.patterns.add(pattern);
                }
            }
        }
        this.patternCacheNeedsRebuild = false;
        requestCraftingUpdate();
        if (saveChanges) {
            this.owner.saveChanges();
        }
    }

    protected abstract String patternOwnerName();

    protected abstract boolean hasAeOutputWork();

    protected abstract boolean processAeOutputWork();

    private void requestCraftingUpdate() {
        this.nodeLifecycle.requestCraftingUpdate();
    }

    protected final void saveCommon(CompoundTag tag, HolderLookup.Provider registries) {
        save(tag, registries);
        saveSlots(tag, registries);
    }

    public final void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("PatternPriority", this.patternPriority);
        this.smartPatternMultiplication.saveConfig(tag);
        this.passiveCraftingSettings.save(tag, registries);
        this.interfaceConfig.save(tag, registries);
        this.interfaceInventory.save(tag, registries);
        saveInterfaceRecovery(tag, registries);
        tag.putBoolean(TAG_TERMINAL_VISIBLE, this.visibleInPatternAccessTerminal);
        tag.remove(TAG_PATTERN_TERMINAL_NAME);
        saveNode(tag);
    }

    public final void saveSlots(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(TAG_PATTERN_SCHEMA, AE_PATTERN_SCHEMA);
        for (int i = 0; i < this.patternSlots.size(); i++) {
            tag.put("MePatternSlot" + i, this.patternSlots.get(i).serializeNBT(registries));
        }
        this.smartPatternMultiplication.savePending(tag, registries);
    }

    protected final void loadCommon(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, registries);
        loadSlots(tag, registries);
    }

    public final void load(CompoundTag tag, HolderLookup.Provider registries) {
        this.patternPriority = tag.getInt("PatternPriority");
        this.smartPatternMultiplication.loadConfig(tag);
        this.passiveCraftingSettings.load(tag, registries);
        this.interfaceConfig.load(tag, registries);
        this.interfaceInventory.load(tag, registries);
        loadInterfaceRecovery(tag, registries);
        this.visibleInPatternAccessTerminal = !tag.contains(TAG_TERMINAL_VISIBLE) || tag.getBoolean(TAG_TERMINAL_VISIBLE);
        this.patternTerminalName = MePatternTerminalNames.migrateLegacy(this.ownerTile, tag.getString(TAG_PATTERN_TERMINAL_NAME));
        loadNode(tag);
    }

    public final void loadSlots(CompoundTag tag, HolderLookup.Provider registries) {
        boolean migrated = tag.getInt(TAG_PATTERN_SCHEMA) < AE_PATTERN_SCHEMA;
        for (int i = 0; i < this.patternSlots.size(); i++) {
            if (tag.contains("MePatternSlot" + i)) {
                this.patternSlots.get(i).deserializeNBT(registries, tag.getCompound("MePatternSlot" + i));
            }
        }
        if (migrated && !hasPatternSlotTags(tag)) {
            int offset = this.owner instanceof MePatternIoOwner io ? Math.max(0, io.getLegacyPatternSlotOffset()) : 0;
            loadLegacyInventory(this.patternSlots, tag, registries, offset);
        }
        this.smartPatternMultiplication.loadPending(tag, registries, this::refundPendingBalance);
        // Block entities can be deserialized before they have a level. Decode the
        // restored patterns after the managed node is created on the first tick.
        this.patterns.clear();
        this.patternCacheNeedsRebuild = true;
        if (this.ownerTile.getLevel() != null && !this.ownerTile.getLevel().isClientSide()) {
            rebuildPatternCache(false);
        } else {
            this.nodeLifecycle.markCraftingUpdatePending();
        }
    }

    private boolean hasPatternSlotTags(CompoundTag tag) {
        for (int i = 0; i < this.patternSlots.size(); i++) {
            if (tag.contains("MePatternSlot" + i)) {
                return true;
            }
        }
        return false;
    }

    static void loadLegacyInventory(List<BasicInventorySlot> slots, CompoundTag tag, HolderLookup.Provider registries, int offset) {
        if (!tag.contains("Inventory", CompoundTag.TAG_LIST)) {
            return;
        }
        ListTag inventory = tag.getList("Inventory", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < slots.size() && i + offset < inventory.size(); i++) {
            slots.get(i).deserializeNBT(registries, inventory.getCompound(i + offset));
        }
    }

    static void savePatternSlots(List<BasicInventorySlot> slots, CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(TAG_PATTERN_SCHEMA, AE_PATTERN_SCHEMA);
        for (int i = 0; i < slots.size(); i++) {
            tag.put("MePatternSlot" + i, slots.get(i).serializeNBT(registries));
        }
    }

    protected final void saveNode(CompoundTag tag) {
        this.nodeLifecycle.saveNode(tag);
    }

    protected final void loadNode(CompoundTag tag) {
        this.nodeLifecycle.loadNode(tag);
    }

    static final class CraftingUpdateState extends MeAeNodeLifecycle.CraftingUpdateState {
    }

    private final class AeTicker implements IGridTickable {
        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(1, 1, !hasAeOutputWork());
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!nodeLifecycle.getMainNode().isActive()) {
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
