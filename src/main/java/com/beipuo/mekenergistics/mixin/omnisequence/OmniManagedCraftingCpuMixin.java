package com.beipuo.mekenergistics.mixin.omnisequence;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.atir.molecularmanipulator.blockentity.OmniComputationCoreBlockEntity;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.compat.omnisequence.OmniBatchCompat;
import com.beipuo.mekenergistics.crafting.OmniManagedCpuGuard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Reports whether OmniSequence owns batch dispatch on this CPU. OmniSequence links CPUs to its omni
 * computation core through the same {@code ownerOf} probe its own CPU mixin consults, so matching
 * it exactly makes the deferral agree with the mod's dispatch decision on every present/absent
 * matrix.
 */
@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class OmniManagedCraftingCpuMixin implements OmniManagedCpuGuard {
    @Shadow
    @Final
    private CraftingCPUCluster cluster;

    /** Set once OmniSequence's probe proved binary-incompatible; never retried afterwards. */
    @Unique
    private static volatile boolean mekenergistics$omniProbeBroken;

    @Override
    public boolean mekenergistics$isOmniManagedCpu() {
        if (mekenergistics$omniProbeBroken) {
            return false;
        }
        try {
            return OmniBatchCompat.isOmniManagedCpu(
                    true, OmniComputationCoreBlockEntity.ownerOf(this.cluster) != null);
        } catch (LinkageError incompatible) {
            // The plugin gate only proves the classes exist. If an OmniSequence update renames or
            // re-signatures ownerOf, this runs inside AE2's crafting CPU tick, where an uncaught
            // NoSuchMethodError would crash the server. Fall back to plain AE2 dispatch instead.
            mekenergistics$omniProbeBroken = true;
            MekEnergistics.LOGGER.error("OmniSequence's CPU ownership probe is incompatible with this "
                    + "Mek Energistics build; batch deferral to OmniSequence is disabled", incompatible);
            return false;
        }
    }
}
