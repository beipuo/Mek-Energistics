package com.beipuo.mekenergistics.item;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MeInstallerLifecycleTest {
    private static final Path STANDARD_INSTALLER =
            Path.of("src/main/java/com/beipuo/mekenergistics/item/MeTierInstallerItem.java");
    private static final Path COMPAT_INSTALLER =
            Path.of("src/main/java/com/beipuo/mekenergistics/item/MeInstallerUpgradeHandler.java");
    private static final Path AE_SUPPORT =
            Path.of("src/main/java/com/beipuo/mekenergistics/blockentity/support/AbstractMeAeSupport.java");

    /**
     * Both installers must share one replacement implementation, so every guard, rollback and
     * feedback message below is enforced for the factory installer as well.
     */
    @Test
    void factoryInstallerDelegatesToTheSharedReplacementPath() throws IOException {
        String source = Files.readString(STANDARD_INSTALLER);
        assertTrue(source.contains("MeInstallerUpgradeHandler.upgrade("),
                "MeTierInstallerItem must replace machines through MeInstallerUpgradeHandler.upgrade");
        assertTrue(!source.contains("setBlockAndUpdate"),
                "MeTierInstallerItem must not keep a second copy of the block swap");
    }

    @Test
    void everyInWorldInstallerRefreshesTheAeLifecycleAfterMutation() throws IOException {
        for (Path installer : new Path[] {COMPAT_INSTALLER}) {
            String source = Files.readString(installer);
            assertTrue(source.contains("refreshMeLifecycle(upgradedTile)"),
                    () -> installer.getFileName() + " must refresh the newly-created ME block entity");
            assertTrue(source.contains("refreshAfterWorldMutation()"),
                    () -> installer.getFileName() + " must schedule the post-node capability refresh");
        }
    }

    @Test
    void worldMutationRefreshRunsAfterTheManagedNodeIsReady() throws IOException {
        String source = Files.readString(AE_SUPPORT);
        int refresh = source.indexOf("public final void refreshAfterWorldMutation()");
        int firstTick = source.indexOf("GridHelper.onFirstTick", refresh);
        int create = source.indexOf("create();", firstTick);
        int invalidate = source.indexOf("level.invalidateCapabilities(pos);", create);
        int patterns = source.indexOf("rebuildPatternCache(false);", create);
        assertTrue(refresh >= 0 && firstTick > refresh && create > firstTick,
                "world mutation refresh must wait for AE2's first-tick lifecycle");
        assertTrue(patterns > create && invalidate > patterns,
                "node creation must precede pattern publication and capability invalidation");
    }

    @Test
    void everyInstallerCapturesOldStateBeforeTheSwapForRollback() throws IOException {
        for (Path installer : new Path[] {COMPAT_INSTALLER}) {
            String source = Files.readString(installer);
            int captureState = source.indexOf("BlockState oldState = level.getBlockState(pos)");
            int captureNbt = source.indexOf("saveWithoutMetadata", captureState);
            int swap = source.indexOf("setBlockAndUpdate(pos, upgradeState)", captureNbt);
            int restore = source.indexOf("rollbackUpgrade(", swap);
            assertTrue(captureState >= 0 && captureNbt > captureState && swap > captureNbt,
                    () -> installer.getFileName() + " must capture the old block state and tile NBT before the swap");
            assertTrue(restore > swap,
                    () -> installer.getFileName() + " must restore the old machine on post-swap failure");
        }
    }

    @Test
    void missingUpgradedTileRestoresTheOldMachine() throws IOException {
        String source = Files.readString(COMPAT_INSTALLER);
        int capture = source.indexOf("BlockState oldState = level.getBlockState(pos)");
        int nbt = source.indexOf("saveWithoutMetadata", capture);
        int swap = source.indexOf("setBlockAndUpdate(pos, upgradeState)", nbt);
        int missing = source.indexOf("upgraded tile missing", swap);
        int restore = source.indexOf("rollbackUpgrade(", missing);
        assertTrue(capture >= 0 && nbt > capture && swap > nbt,
                "the old block state and tile NBT must be captured before the block swap");
        assertTrue(missing > swap && restore > missing,
                "a missing upgraded tile must roll back to the old block and tile data");
    }

    @Test
    void failedUpgradeDataParseRestoresTheOldMachine() throws IOException {
        String source = Files.readString(COMPAT_INSTALLER);
        int swap = source.indexOf("setBlockAndUpdate(pos, upgradeState)");
        int parse = source.indexOf("upgradedTile.parseUpgradeData", swap);
        int load = source.indexOf("MePatternSlotTransfer.load", parse);
        int caught = source.indexOf("catch (RuntimeException failure)", load);
        int restore = source.indexOf("rollbackUpgrade(", caught);
        assertTrue(parse > swap && load > parse,
                "upgrade data must be parsed after the block swap");
        assertTrue(caught > load && restore > caught,
                "a failed upgrade-data parse must roll back to the old block and tile data");
    }

    @Test
    void rollbackReappliesOldTileStateAndRemovesNewBoundingBlocks() throws IOException {
        for (Path installer : new Path[] {COMPAT_INSTALLER}) {
            String source = Files.readString(installer);
            int helper = source.indexOf("private static InteractionResult rollbackUpgrade(");
            int removeBounding = source.indexOf("removeBoundingBlocks", helper);
            int setBack = source.indexOf("setBlockAndUpdate(pos, oldState)", removeBounding);
            int reload = source.indexOf("loadWithComponents", setBack);
            assertTrue(removeBounding > helper && setBack > removeBounding && reload > setBack,
                    () -> installer.getFileName()
                            + " must remove new bounding blocks, restore the old block, and reload the old tile data");
        }
    }
}