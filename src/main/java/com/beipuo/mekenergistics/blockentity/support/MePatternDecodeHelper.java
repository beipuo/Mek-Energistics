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
            MekEnergistics.LOGGER.warn("Skipping invalid encoded pattern in {}: {}", ownerName, stack.getHoverName().getString(), exception);
            return null;
        }
    }
}
