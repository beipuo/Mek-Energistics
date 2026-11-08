package com.beipuo.mekenergistics.upgrade;

import java.util.Objects;
import java.util.function.Supplier;
import mekanism.common.tile.component.TileComponentUpgrade;
import org.jetbrains.annotations.NotNull;

/**
 * Single source of truth for one machine's ME upgrade state. Handles queries, install, uninstall,
 * count limits, prerequisites, conflicts and change notifications.
 *
 * <p>All mutations go through this container; the owning machine reacts through
 * {@link MeUpgradeStateOwner#onMeUpgradeStateChanged()}.</p>
 */
public final class MeUpgradeContainer {
    private MeUpgradeData data;
    private final MeUpgradeStateOwner owner;
    private final Runnable changeNotifier;
    private final Supplier<TileComponentUpgrade> nativeComponent;

    public MeUpgradeContainer(@NotNull MeUpgradeStateOwner owner, @NotNull Runnable changeNotifier) {
        this(owner, changeNotifier, () -> null);
    }

    public MeUpgradeContainer(@NotNull MeUpgradeStateOwner owner, @NotNull Runnable changeNotifier,
            @NotNull Supplier<TileComponentUpgrade> nativeComponent) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.changeNotifier = Objects.requireNonNull(changeNotifier, "changeNotifier");
        this.nativeComponent = Objects.requireNonNull(nativeComponent, "nativeComponent");
        this.data = MeUpgradeData.EMPTY;
    }

    public MeUpgradeData data() {
        return this.data;
    }

    public int count(MeUpgradeType type) {
        TileComponentUpgrade component = nativeComponent();
        if (component != null) {
            return component.getUpgrades(MeMekanismUpgrades.forType(type));
        }
        return this.data.count(type);
    }

    public boolean isInstalled(MeUpgradeType type) {
        return count(type) > 0;
    }

    public boolean isEmpty() {
        for (MeUpgradeType type : MeUpgradeType.values()) {
            if (isInstalled(type)) {
                return false;
            }
        }
        return this.data.preserved().isEmpty();
    }

    /** Derives the machine running mode from the installed upgrades. */
    public MeMachineMode mode() {
        if (isInstalled(MeUpgradeType.OUTPUT_INTERFACE)) {
            return MeMachineMode.OUTPUT_INTERFACE;
        }
        if (isInstalled(MeUpgradeType.PATTERN_PROVIDER)) {
            return MeMachineMode.PATTERN_PROVIDER;
        }
        return MeMachineMode.NONE;
    }

    /** Installs one unit of {@code type} after validating support, prerequisites, limits and
     * conflicts. Never partially modifies state. */
    public MeUpgradeConflictPolicy.Result install(MeUpgradeType type) {
        return install(type, 1);
    }

    public MeUpgradeConflictPolicy.Result install(MeUpgradeType type, int amount) {
        if (type == null || amount <= 0) {
            return MeUpgradeConflictPolicy.Result.blocked();
        }
        MeUpgradeConflictPolicy.Result precheck = canInstall(type, amount);
        if (!precheck.successful()) {
            return precheck;
        }
        TileComponentUpgrade component = nativeComponent();
        if (component != null) {
            int added = component.addUpgrades(MeMekanismUpgrades.forType(type), amount);
            return added == amount ? MeUpgradeConflictPolicy.Result.ok()
                    : MeUpgradeConflictPolicy.Result.blocked();
        }
        this.data = this.data.with(type, this.data.count(type) + amount);
        notifyChanged();
        return MeUpgradeConflictPolicy.Result.ok();
    }

    /** Validates an install without modifying state. */
    public MeUpgradeConflictPolicy.Result canInstall(MeUpgradeType type, int amount) {
        if (type == null || amount <= 0) {
            return MeUpgradeConflictPolicy.Result.blocked();
        }
        if (!this.owner.supportsUpgrade(type)) {
            return MeUpgradeConflictPolicy.Result.unsupported();
        }
        int target = count(type) + amount;
        if (target > type.getMaxCount()) {
            return MeUpgradeConflictPolicy.Result.limitReached();
        }
        MeUpgradeType conflict = MeUpgradeConflictPolicy.conflictWith(type, this::isInstalled).orElse(null);
        if (conflict != null) {
            return MeUpgradeConflictPolicy.Result.conflict(conflict);
        }
        if (type == MeUpgradeType.PASSIVE_CRAFTING && !this.owner.supportsNativePatternProvider()
                && !isInstalled(MeUpgradeType.PATTERN_PROVIDER)) {
            return MeUpgradeConflictPolicy.Result.missingPrerequisite();
        }
        return MeUpgradeConflictPolicy.Result.ok();
    }

    /** Removes one unit of {@code type} after machine-specific uninstall guards. */
    public boolean uninstall(MeUpgradeType type) {
        return uninstall(type, 1);
    }

    public boolean uninstall(MeUpgradeType type, int amount) {
        if (type == null || amount <= 0 || !isInstalled(type)) {
            return false;
        }
        if (type == MeUpgradeType.PATTERN_PROVIDER) {
            if (!this.owner.isPatternInventoryEmpty()) {
                return false;
            }
            if (isInstalled(MeUpgradeType.PASSIVE_CRAFTING)) {
                return false;
            }
        }
        if (type == MeUpgradeType.OUTPUT_INTERFACE && !this.owner.isInterfaceInventoryEmpty()) {
            return false;
        }
        TileComponentUpgrade component = nativeComponent();
        if (component != null) {
            int before = component.getUpgrades(MeMekanismUpgrades.forType(type));
            for (int i = 0; i < Math.min(amount, before); i++) {
                component.removeUpgrade(MeMekanismUpgrades.forType(type), false);
            }
            return component.getUpgrades(MeMekanismUpgrades.forType(type)) < before;
        }
        this.data = this.data.with(type, this.data.count(type) - amount);
        notifyChanged();
        return true;
    }

    /** Replaces state wholesale (load, migration, reset). Normalizes via {@link MeUpgradeData}. */
    public void setData(MeUpgradeData newData) {
        MeUpgradeData next = newData == null ? MeUpgradeData.EMPTY : newData;
        if (next.equals(this.data)) {
            return;
        }
        this.data = next;
        notifyChanged();
    }

    public void markChanged() {
        notifyChanged();
    }

    public void refreshOnWorldLoad() {
        this.owner.onMeUpgradeStateChanged();
    }

    /** Moves pre-native save data into Mekanism's component once, then clears the legacy counts. */
    public void migrateToNativeComponent() {
        TileComponentUpgrade component = nativeComponent();
        if (component == null || this.data.counts().isEmpty()) {
            return;
        }
        java.util.Map<MeUpgradeType, Integer> unmigrated = new java.util.EnumMap<>(MeUpgradeType.class);
        for (MeUpgradeType type : MeUpgradeType.values()) {
            int legacy = this.data.count(type);
            if (legacy <= 0) {
                continue;
            }
            int missing = legacy - component.getUpgrades(MeMekanismUpgrades.forType(type));
            if (missing > 0 && component.supports(MeMekanismUpgrades.forType(type))) {
                component.addUpgrades(MeMekanismUpgrades.forType(type), missing);
                missing = legacy - component.getUpgrades(MeMekanismUpgrades.forType(type));
            }
            if (missing > 0) {
                unmigrated.put(type, missing);
            }
        }
        if (!unmigrated.isEmpty()) {
            // The native component refused some legacy cards (unsupported block, conflict, full).
            // Legacy counts are not written back on save, so make the loss visible instead of
            // silent; server owners can then restore the cards by hand.
            com.beipuo.mekenergistics.MekEnergistics.LOGGER.warn(
                    "Could not migrate legacy ME upgrades {} into the native upgrade component; they were dropped",
                    unmigrated);
        }
        this.data = new MeUpgradeData(java.util.Map.of(), this.data.preserved());
        notifyChanged();
    }

    private TileComponentUpgrade nativeComponent() {
        return this.nativeComponent.get();
    }

    private void notifyChanged() {
        this.changeNotifier.run();
        this.owner.onMeUpgradeStateChanged();
    }
}




