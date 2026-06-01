package com.beipuo.mekenergistics.block;

import java.util.function.Supplier;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.states.BlockStateHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MeUpgradeableAttribute extends AttributeUpgradeable {
    private final Supplier<? extends Block> upgradeBlock;

    public MeUpgradeableAttribute(Supplier<? extends Block> upgradeBlock) {
        super(() -> null);
        this.upgradeBlock = upgradeBlock;
    }

    @NotNull
    @Override
    public BlockState upgradeResult(@NotNull BlockState current, @NotNull BaseTier tier) {
        return BlockStateHelper.copyStateData(current, this.upgradeBlock.get().defaultBlockState());
    }
}
