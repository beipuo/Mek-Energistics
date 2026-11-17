package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class MeNetworkEnergyHelper {
    /**
     * How long a captured "local + network" availability may be reused for recipe checks. Mekanism
     * asks for the available energy every server tick, and a single simulated network drain can
     * walk every AE storage, so a machine sitting on a full network must not pay that cost per
     * tick. One second is longer than any single operation and short enough that a network drained
     * by something else is noticed before a machine can fabricate energy out of stale numbers.
     */
    static final int AVAILABILITY_CACHE_TICKS = 20;

    /**
     * Refill the local FE buffer only once it can no longer cover this many ticks of operation
     * energy. Refilling every tick turns a full buffer into a per-tick network extraction, and AE
     * extractions are far more expensive than the Mekanism container bookkeeping they replace.
     */
    static final int REFILL_SAFETY_TICKS = 4;

    private MeNetworkEnergyHelper() {
    }

    public interface LocalEnergyBuffer {
        long getLocalEnergy();

        long extractLocal(long amount, Action action, AutomationType automationType);
    }

    public static long extractWithLocalBuffer(MachineEnergyContainer<?> localEnergy, IGrid grid, IActionSource source, long amount,
            Action action, AutomationType automationType) {
        if (amount <= 0) {
            return 0;
        }
        if (automationType != AutomationType.INTERNAL || grid == null) {
            return extractLocal(localEnergy, amount, action, automationType);
        }
        if (!MekEnergisticsConfig.preferNetworkEnergy()) {
            long localExtracted = extractLocal(localEnergy, amount, action, automationType);
            long networkExtracted = extractNetworkFe(grid, source, amount - localExtracted, action);
            notifyNetworkExtraction(localEnergy, networkExtracted, action);
            return localExtracted + networkExtracted;
        }
        long networkExtracted = extractNetworkFe(grid, source, amount, action);
        notifyNetworkExtraction(localEnergy, networkExtracted, action);
        return networkExtracted + extractLocal(localEnergy, amount - networkExtracted, action, automationType);
    }

    private static void notifyNetworkExtraction(MachineEnergyContainer<?> localEnergy, long extracted, Action action) {
        if (action.execute() && extracted > 0) {
            // Mekanism's recipe monitor relies on the energy listener to unpause
            // after an energy error clears. Direct AE extraction bypasses setEnergy,
            // so a full local buffer otherwise leaves the recipe paused indefinitely.
            localEnergy.onContentsChanged();
        }
    }

    private static long extractLocal(MachineEnergyContainer<?> localEnergy, long amount, Action action, AutomationType automationType) {
        if (amount <= 0) {
            return 0;
        }
        return localEnergy instanceof LocalEnergyBuffer buffer ? buffer.extractLocal(amount, action, automationType)
                : localEnergy.extract(amount, action, automationType);
    }

    public static long availableWithLocalBuffer(MachineEnergyContainer<?> localEnergy, IGrid grid, IActionSource source) {
        long local = localEnergy instanceof LocalEnergyBuffer buffer ? buffer.getLocalEnergy() : localEnergy.getEnergy();
        if (grid == null) {
            return local;
        }
        return availableWithNetwork(local, availabilityCap(localEnergy),
                requested -> extractNetworkFe(grid, source, requested, Action.SIMULATE));
    }

    /**
     * How much energy a recipe check ever needs to see. Mekanism only compares the result against
     * the energy of the operations it may run this tick, so simulating a drain of Long.MAX_VALUE FE
     * (which walks every AE energy provider and, with Applied Flux, the grid's FE storage) once per
     * process per tick bought nothing. The cap still exceeds the local buffer whenever one
     * operation costs more than the buffer holds.
     */
    static final long AVAILABILITY_OPERATIONS_MARGIN = 1024;

    static long availabilityCap(MachineEnergyContainer<?> localEnergy) {
        long perTick = Math.max(0, localEnergy.getEnergyPerTick());
        long operations = perTick > Long.MAX_VALUE / AVAILABILITY_OPERATIONS_MARGIN
                ? Long.MAX_VALUE : perTick * AVAILABILITY_OPERATIONS_MARGIN;
        return Math.max(Math.max(0, localEnergy.getMaxEnergy()), operations);
    }

    /** Whether a captured availability is still fresh. Game time {@code 0} means the source has no tick, so never cache. */
    static boolean reuseAvailability(long now, long cachedAtTick) {
        return now > 0 && now - cachedAtTick < AVAILABILITY_CACHE_TICKS;
    }

    static long availableWithNetwork(long local, LongUnaryOperator simulatedNetworkExtraction) {
        return availableWithNetwork(local, Long.MAX_VALUE, simulatedNetworkExtraction);
    }

    /** Local plus simulated network energy, querying the network for at most {@code cap - local}. */
    static long availableWithNetwork(long local, long cap, LongUnaryOperator simulatedNetworkExtraction) {
        if (local >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        long normalizedLocal = Math.max(0, local);
        if (normalizedLocal >= cap) {
            return local;
        }
        return totalAvailableEnergy(local, simulatedNetworkExtraction.applyAsLong(cap - normalizedLocal));
    }

    static long totalAvailableEnergy(long local, long network) {
        if (local <= 0) {
            return Math.max(0, network);
        }
        if (network <= 0) {
            return local;
        }
        return local > Long.MAX_VALUE - network ? Long.MAX_VALUE : local + network;
    }

    public static long extractNetworkFe(IGrid grid, IActionSource source, long requestedFe, Action action) {
        if (requestedFe <= 0 || grid == null) {
            return 0;
        }
        Actionable actionable = action.execute() ? Actionable.MODULATE : Actionable.SIMULATE;
        long extracted = 0;
        boolean hasAppliedFlux = OptionalCompatClasses.hasAppliedFlux();
        boolean appliedFluxFirst = hasAppliedFlux && MekEnergisticsConfig.preferAppliedFluxNetworkFe();
        if (appliedFluxFirst) {
            extracted = AppliedFluxEnergyBridge.extractFe(grid, requestedFe, actionable, source);
            requestedFe -= extracted;
        }
        long aeExtracted = extractAeEnergyAsFe(grid, requestedFe, actionable);
        extracted += aeExtracted;
        requestedFe -= aeExtracted;
        if (hasAppliedFlux && !appliedFluxFirst) {
            extracted += AppliedFluxEnergyBridge.extractFe(grid, requestedFe, actionable, source);
        }
        return extracted;
    }

    public static long refillLocalEnergy(IEnergyContainer localEnergy, IGrid grid, IActionSource source) {
        if (localEnergy == null || grid == null) {
            return 0;
        }
        // Containers that know their per-tick usage only refill once the buffer runs dry; anything
        // else keeps the old "top up whenever it is not full" behaviour.
        long lowWaterMark = localEnergy instanceof MachineEnergyContainer<?> machine
                ? lowWaterMark(machine.getMaxEnergy(), machine.getEnergyPerTick())
                : localEnergy.getMaxEnergy();
        return refillEnergyBuffer(localEnergy.getEnergy(), localEnergy.getMaxEnergy(), lowWaterMark,
                requested -> extractNetworkFe(grid, source, requested, Action.EXECUTE), localEnergy::setEnergy);
    }

    /**
     * Energy level at which a buffer must be refilled. The margin is the larger of
     * {@link #REFILL_SAFETY_TICKS} ticks of operation energy and half the buffer, so a machine
     * sips from its own buffer for a while before the network is walked again, while a buffer
     * too small to hold the safety margin still refills before it runs dry.
     */
    static long lowWaterMark(long maxEnergy, long energyPerTick) {
        if (maxEnergy <= 0) {
            return 0;
        }
        long reserve = energyPerTick <= 0 ? 0
                : energyPerTick > Long.MAX_VALUE / REFILL_SAFETY_TICKS ? Long.MAX_VALUE : energyPerTick * REFILL_SAFETY_TICKS;
        return maxEnergy - Math.min(maxEnergy, Math.max(reserve, maxEnergy / 2));
    }

    static long refillEnergyBuffer(long currentEnergy, long maxEnergy, LongUnaryOperator networkExtraction,
            LongConsumer energySetter) {
        return refillEnergyBuffer(currentEnergy, maxEnergy, maxEnergy, networkExtraction, energySetter);
    }

    static long refillEnergyBuffer(long currentEnergy, long maxEnergy, long lowWaterMark, LongUnaryOperator networkExtraction,
            LongConsumer energySetter) {
        if (maxEnergy <= 0 || currentEnergy >= maxEnergy || currentEnergy >= lowWaterMark) {
            return 0;
        }
        long normalizedCurrent = Math.max(0, currentEnergy);
        long remainingCapacity = maxEnergy - normalizedCurrent;
        long extracted = networkExtraction.applyAsLong(remainingCapacity);
        long injected = Math.min(remainingCapacity, Math.max(0, extracted));
        if (injected > 0) {
            energySetter.accept(normalizedCurrent + injected);
        }
        return injected;
    }

    public static IEnergyContainer recipeEnergyView(MachineEnergyContainer<?> energyContainer, Supplier<IGrid> gridSupplier, IActionSource source) {
        return recipeEnergyView(energyContainer, gridSupplier, source, () -> 0L);
    }

    /**
     * @param gameTimeSupplier current world game time, or a constant zero when no tick source is available. A
     *                         constant zero disables availability caching so the network is queried every call.
     */
    public static IEnergyContainer recipeEnergyView(MachineEnergyContainer<?> energyContainer, Supplier<IGrid> gridSupplier,
            IActionSource source, LongSupplier gameTimeSupplier) {
        return new NetworkRecipeEnergyView(energyContainer, gridSupplier, source, gameTimeSupplier);
    }

    public static class NetworkBackedEnergyContainer<TILE extends TileEntityMekanism> extends MachineEnergyContainer<TILE>
            implements LocalEnergyBuffer {
        private final Supplier<IGrid> gridSupplier;
        private final Supplier<IActionSource> actionSourceSupplier;

        public NetworkBackedEnergyContainer(TILE owner, IContentsListener listener, Supplier<IGrid> gridSupplier, IActionSource actionSource) {
            this(owner, listener, gridSupplier, () -> actionSource);
        }

        public NetworkBackedEnergyContainer(TILE owner, IContentsListener listener, Supplier<IGrid> gridSupplier, Supplier<IActionSource> actionSourceSupplier) {
            super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                    BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
            this.gridSupplier = gridSupplier;
            this.actionSourceSupplier = actionSourceSupplier;
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            IGrid grid = this.gridSupplier == null ? null : this.gridSupplier.get();
            IActionSource actionSource = this.actionSourceSupplier == null ? null : this.actionSourceSupplier.get();
            return extractWithLocalBuffer(this, grid, actionSource, amount, action, automationType);
        }

        @Override
        public long getLocalEnergy() {
            return super.getEnergy();
        }

        @Override
        public long extractLocal(long amount, Action action, AutomationType automationType) {
            return super.extract(amount, action, automationType);
        }
    }

    private static long extractAeEnergyAsFe(IGrid grid, long requestedFe, Actionable action) {
        if (requestedFe <= 0) {
            return 0;
        }
        double requestedAe = PowerUnit.FE.convertTo(PowerUnit.AE, requestedFe);
        double extractedAe = grid.getEnergyService().extractAEPower(requestedAe, action, PowerMultiplier.ONE);
        return Math.min(requestedFe, (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, extractedAe)));
    }

    private static final class NetworkRecipeEnergyView implements IEnergyContainer {
        /** Sentinel for "no cached availability"; cached numbers can never be negative. */
        private static final long UNSET_AVAILABILITY = Long.MIN_VALUE;

        private final MachineEnergyContainer<?> energyContainer;
        private final Supplier<IGrid> gridSupplier;
        private final IActionSource actionSource;
        private final LongSupplier gameTimeSupplier;
        private long cachedAvailable = UNSET_AVAILABILITY;
        private long cachedAtTick;

        private NetworkRecipeEnergyView(MachineEnergyContainer<?> energyContainer, Supplier<IGrid> gridSupplier, IActionSource actionSource,
                LongSupplier gameTimeSupplier) {
            this.energyContainer = energyContainer;
            this.gridSupplier = gridSupplier;
            this.actionSource = actionSource;
            this.gameTimeSupplier = gameTimeSupplier == null ? () -> 0L : gameTimeSupplier;
        }

        @Override
        public long getEnergy() {
            long local = localEnergy();
            long now = this.gameTimeSupplier.getAsLong();
            IGrid grid = grid();
            if (grid == null) {
                this.cachedAvailable = UNSET_AVAILABILITY;
                return local;
            }
            if (this.cachedAvailable != UNSET_AVAILABILITY && reuseAvailability(now, this.cachedAtTick)) {
                return Math.max(local, this.cachedAvailable);
            }
            long cap = availabilityCap(this.energyContainer);
            if (local >= cap) {
                // The local buffer alone already covers the request, so nothing has to be captured.
                // Caching local-only numbers here would let a later drain over-report availability.
                this.cachedAvailable = UNSET_AVAILABILITY;
                return local;
            }
            long requested = cap - Math.max(0, local);
            long network = extractNetworkFe(grid, this.actionSource, requested, Action.SIMULATE);
            long available = totalAvailableEnergy(local, network);
            if (network > 0) {
                // Any network contribution may be reused. A network that answered with zero is
                // re-checked every tick so a machine stops as soon as it is truly out of energy.
                this.cachedAvailable = available;
                this.cachedAtTick = now;
            } else {
                this.cachedAvailable = UNSET_AVAILABILITY;
            }
            return available;
        }

        private long localEnergy() {
            return this.energyContainer instanceof LocalEnergyBuffer buffer ? buffer.getLocalEnergy()
                    : this.energyContainer.getEnergy();
        }

        private IGrid grid() {
            return this.gridSupplier == null ? null : this.gridSupplier.get();
        }

        @Override
        public void setEnergy(long energy) {
            this.energyContainer.setEnergy(energy);
        }

        @Override
        public long extract(long amount, Action action, AutomationType automationType) {
            if (amount <= 0) {
                return 0;
            }
            if (!action.execute()) {
                // Simulated extractions still need the complete local + network picture.
                return extractWithLocalBuffer(this.energyContainer, grid(), this.actionSource, amount, action, automationType);
            }
            long extracted;
            if (localEnergy() >= amount) {
                // The buffer already holds enough, so this tick never looks at the network. The
                // refill pass keeps it stocked in batches instead of touching AE every tick.
                extracted = extractLocal(this.energyContainer, amount, action, automationType);
            } else {
                IGrid grid = grid();
                extracted = extractLocal(this.energyContainer, amount, action, automationType);
                if (grid != null) {
                    if (extracted < amount) {
                        long networkExtracted = extractNetworkFe(grid, this.actionSource, amount - extracted, Action.EXECUTE);
                        if (networkExtracted > 0) {
                            this.energyContainer.onContentsChanged();
                        }
                        extracted += networkExtracted;
                    }
                }
            }
            if (this.cachedAvailable != UNSET_AVAILABILITY) {
                if (extracted < amount) {
                    // The network could not cover the request after all; re-check next tick.
                    this.cachedAvailable = UNSET_AVAILABILITY;
                } else {
                    this.cachedAvailable = Math.max(0, this.cachedAvailable - extracted);
                }
            }
            return extracted;
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
