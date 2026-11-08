package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Shared AE support for the native configurable Mekanism machine family. */
public final class MeMekanismMachineAeSupport extends AbstractMeAeSupport<MeMekanismMachineBlockEntity> {
    public MeMekanismMachineAeSupport(MeMekanismMachineBlockEntity owner) {
        super(owner);
    }

    public void saveAeState(CompoundTag tag, HolderLookup.Provider registries, AeOutputMode mode) {
        tag.putInt("AeOutputMode", mode.ordinal());
        saveCommon(tag, registries);
    }

    public AeOutputMode loadAeState(CompoundTag tag, HolderLookup.Provider registries) {
        loadCommon(tag, registries);
        return AeOutputMode.byId(tag.getInt("AeOutputMode"));
    }

    @Override
    protected String patternOwnerName() {
        return this.owner.getMachine().name();
    }

    @Override
    protected boolean hasAeOutputWork() {
        if (isInterfaceMode()) {
            return hasInterfaceWork();
        }
        return this.smartPatternMultiplication.hasPendingWork()
                || hasPatternOutputBacklog(this.owner.getAeOutputMode());
    }

    @Override
    protected boolean processAeOutputWork() {
        if (isInterfaceMode()) {
            return processInterfaceMode();
        }
        AeOutputMode mode = this.owner.getAeOutputMode();
        boolean changed = drainPatternOutputs(mode);
        return hasPatternOutputBacklog(mode) ? changed : processSmartPatternViaAdapter() || changed;
    }

    /** Called from the block entity tick: this machine drains outputs through the AE ticker only. */
    public void wakeOutputTicker() {
        wakeAeTickerIfIdle();
    }
}
