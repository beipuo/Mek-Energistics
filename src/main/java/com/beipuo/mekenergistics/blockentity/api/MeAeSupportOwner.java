package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;

public interface MeAeSupportOwner extends ICraftingProvider, IActionHost {
    TileEntityMekanism getAeOwnerTile();

    MeMekanismMachine getMachine();

    default BlockPos getGridNodePosition() {
        return getAeOwnerTile().getBlockPos();
    }

    void saveChanges();

    boolean isSmartPatternMultiplicationEnabled();

    AbstractMeAeSupport<?> getPatternAeSupport();

    /** Maximum complete copies of the supplied one-craft input that can be accepted now. */
    long maxAcceptedPatternCopies(KeyCounter[] oneCraftInputs);

    /** Conservative provider-level parallelism when the provider contract has no input shape. */
    default int getAvailableParallelSlots() {
        return getPatternAeSupport().getAvailableParallelSlots();
    }

}
