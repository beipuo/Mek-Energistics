package com.beipuo.mekenergistics.block;

import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeable;
import java.util.function.Supplier;
import mekanism.common.block.states.BlockStateHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MeExtraUpgradeableAttribute extends ExtraAttributeUpgradeable {
    private final Supplier<? extends Block> upgradeBlock;

    public MeExtraUpgradeableAttribute(Supplier<? extends Block> upgradeBlock) {
        super(() -> null);
        this.upgradeBlock = upgradeBlock;
    }

    @NotNull
    @Override
    public BlockState upgradeResult(@NotNull BlockState current, @NotNull AdvancedTier tier) {
        return BlockStateHelper.copyStateData(current, this.upgradeBlock.get().defaultBlockState());
    }
}
