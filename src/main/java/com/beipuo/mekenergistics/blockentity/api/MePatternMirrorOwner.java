package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import com.beipuo.mekenergistics.blockentity.support.MePatternMirrorSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.List;
import net.minecraft.world.level.Level;

public interface MePatternMirrorOwner {
    MePatternMirrorSupport getPatternMirrorSupport();

    IManagedGridNode getMainNode();

    MeMekanismMachine getMachine();

    Level getOwnerLevel();

    List<IPatternDetails> getLocalAvailablePatterns();

    void requestPatternMirrorUpdate();

    void saveChanges();
}
