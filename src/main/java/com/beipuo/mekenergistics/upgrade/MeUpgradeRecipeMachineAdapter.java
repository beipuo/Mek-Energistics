package com.beipuo.mekenergistics.upgrade;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;

/**
 * Shared ME-upgrade boundary for recipe-machine Mixins.
 *
 * <p>An adapter supplies only its tile, lazy runtime, profile, and injection-specific recipe hooks.
 * Node lifecycle, activation, trackers, persistence, pattern I/O, and output mode behavior remain
 * centralized in {@link MeUpgradeRecipeMachineRuntime}.</p>
 */
public interface MeUpgradeRecipeMachineAdapter extends MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    MeUpgradeRecipeMachineRuntime getOrCreateMeUpgradeRuntime();

    MeUpgradeRecipeMachineRuntime getExistingMeUpgradeRuntime();

    @Override
    default MeUpgradeContainer getMeUpgradeContainer() {
        return getOrCreateMeUpgradeRuntime().upgrades();
    }

    @Override
    default boolean supportsNativePatternProvider() {
        return getOrCreateMeUpgradeRuntime().supportsNativePatternProvider();
    }

    @Override
    default boolean isPatternInventoryEmpty() {
        return getOrCreateMeUpgradeRuntime().isPatternInventoryEmpty();
    }

    @Override
    default void onMeUpgradeStateChanged() {
        getOrCreateMeUpgradeRuntime().onMeUpgradeStateChanged();
    }

    @Override
    default boolean isMeUpgradeTarget() {
        return getOrCreateMeUpgradeRuntime().matches(getMeUpgradeProfile());
    }

    @Override
    default boolean isMeUpgradeActive() {
        return getOrCreateMeUpgradeRuntime().active(isMeUpgradeTarget());
    }

    @Override
    default AbstractMeAeSupport<?> getRecipeAeSupport() {
        return getOrCreateMeUpgradeRuntime().support();
    }

    @Override
    default AeOutputMode getAeOutputMode() {
        return getOrCreateMeUpgradeRuntime().outputMode();
    }

    @Override
    default void cycleAeOutputMode() {
        getOrCreateMeUpgradeRuntime().cycleOutputMode();
    }

    @Override
    default MeInputLayout getPatternInputLayout() {
        return getOrCreateMeUpgradeRuntime().inputLayout(getMeUpgradeProfile());
    }

    @Override
    default List<? extends MeOutputPort> getPatternOutputPorts() {
        return getOrCreateMeUpgradeRuntime().outputPorts(getMeUpgradeProfile());
    }

    default IInventorySlotHolder addMePatternSlots(
            IInventorySlotHolder holder, IContentsListener recipeCacheListener) {
        return isMeUpgradeTarget()
                ? getOrCreateMeUpgradeRuntime().withPatternSlots(holder, recipeCacheListener)
                : holder;
    }

    default boolean processMePatternIo(boolean changed) {
        return getOrCreateMeUpgradeRuntime().tick(isMeUpgradeActive(), changed);
    }

    @Override
    default void createMeNodeIfActive() {
        getOrCreateMeUpgradeRuntime().createNodeIfActive(isMeUpgradeActive());
    }

    @Override
    default void destroyMeNode() {
        MeUpgradeRecipeMachineRuntime runtime = getExistingMeUpgradeRuntime();
        if (runtime != null) {
            runtime.destroyNode();
        }
    }

    @Override
    default void onChunkUnloaded() {
        destroyMeNode();
    }

    @Override
    default void addMeTrackers(MekanismContainer container) {
        getOrCreateMeUpgradeRuntime().addTrackers(container, isMeUpgradeTarget());
    }

    @Override
    default void saveMeState(CompoundTag tag, HolderLookup.Provider registries) {
        MeUpgradeRecipeMachineRuntime runtime = getExistingMeUpgradeRuntime();
        if (runtime != null) {
            runtime.save(tag, registries);
        }
    }

    @Override
    default void loadMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.isEmpty()) {
            getOrCreateMeUpgradeRuntime().load(tag, registries);
        }
    }
}
