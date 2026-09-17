package com.beipuo.mekenergistics.upgrade;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Snapshot carried only by tier-upgrade data, not by ordinary component saves. */
public interface MeUpgradeComponentState {
    String TRANSFER_TAG = "mekenergistics:tier_upgrade_state";

    CompoundTag mekenergistics$captureTierState(HolderLookup.Provider registries);
}
