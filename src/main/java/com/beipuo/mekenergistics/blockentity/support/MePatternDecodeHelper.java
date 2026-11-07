package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import com.beipuo.mekenergistics.MekEnergistics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MePatternDecodeHelper {
    private MePatternDecodeHelper() {
    }

    @Nullable
    public static IPatternDetails safeDecode(ItemStack stack, @Nullable Level level, BlockPos pos, String ownerName) {
        return safeDecode(stack, level, ownerName + " at " + pos);
    }

    @Nullable
    public static IPatternDetails safeDecode(ItemStack stack, @Nullable Level level, String ownerName) {
        try {
            return PatternDetailsHelper.decodePattern(stack, level);
        } catch (RuntimeException exception) {
            String patternName = stack.getHoverName().getString();
            String signature = ownerName + '|' + patternName + '|' + exception;
            if (firstReport(signature)) {
                // Full stack trace once; pattern caches rebuild on every slot change and the
                // pattern window decodes every frame, so repeating it would flood the log.
                MekEnergistics.LOGGER.warn("Skipping invalid encoded pattern in {}: {} (further identical failures are logged at debug level)",
                        ownerName, patternName, exception);
            } else {
                MekEnergistics.LOGGER.debug("Skipping invalid encoded pattern in {}: {}: {}", ownerName, patternName, exception.toString());
            }
            return null;
        }
    }

    private static final int MAX_REMEMBERED_FAILURES = 1024;
    private static final java.util.Set<String> REPORTED_FAILURES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static boolean firstReport(String signature) {
        if (REPORTED_FAILURES.size() >= MAX_REMEMBERED_FAILURES) {
            REPORTED_FAILURES.clear();
        }
        return REPORTED_FAILURES.add(signature);
    }
}
