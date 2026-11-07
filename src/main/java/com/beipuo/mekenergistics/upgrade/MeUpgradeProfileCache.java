package com.beipuo.mekenergistics.upgrade;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Per-tile memo of a machine's ME upgrade profile.
 *
 * <p>Resolving a profile costs a block registry lookup, a map lookup and a fresh profile object
 * with several lambdas, and the mixins resolve it on every {@code isMeUpgradeTarget()} /
 * {@code isMeUpgradeActive()} call, i.e. several times per machine per tick even when no ME card
 * is installed. Catalog profiles depend only on the tile's block, which never changes for a block
 * entity, so they are resolved once and reused while the block stays the same.
 *
 * <p>Profiles provided through the pattern automation SPI are never cached: whether such a host
 * participates is decided at runtime by {@code meSupportsPatternAutomation()}.
 */
public final class MeUpgradeProfileCache {
    @Nullable
    private Block block;
    @Nullable
    private MeUpgradeMachineProfile<?> profile;
    private boolean resolved;

    /**
     * @param cacheMissing whether a {@code null} result is also determined by the block alone. Pass
     *                     false when the resolver may fall back to an SPI host for unknown blocks.
     */
    @SuppressWarnings("unchecked")
    public <P extends MeUpgradeMachineProfile<?>> P get(BlockEntity tile, Supplier<P> resolver, boolean cacheMissing) {
        Block current = tile.getBlockState().getBlock();
        if (this.resolved && current == this.block) {
            return (P) this.profile;
        }
        P fresh = resolver.get();
        if (isBlockDetermined(fresh, cacheMissing)) {
            this.block = current;
            this.profile = fresh;
            this.resolved = true;
        } else {
            this.resolved = false;
            this.profile = null;
            this.block = null;
        }
        return fresh;
    }

    private static boolean isBlockDetermined(@Nullable MeUpgradeMachineProfile<?> profile, boolean cacheMissing) {
        if (profile == null) {
            return cacheMissing;
        }
        return profile.machine() != MeMekanismMachine.MEKANISM_MAGIC_AUTOMATION;
    }
}
