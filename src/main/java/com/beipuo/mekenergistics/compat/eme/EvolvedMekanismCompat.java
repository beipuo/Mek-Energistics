package com.beipuo.mekenergistics.compat.eme;

import fr.iglee42.evolvedmekanism.items.ItemMaxTierInstaller;
import fr.iglee42.evolvedmekanism.registries.EMFactoryType;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class EvolvedMekanismCompat {
    private EvolvedMekanismCompat() {
    }

    public static <TILE extends TileEntityMekanism> void withAlloyingFactoryType(
            BlockTypeTile.BlockTileBuilder<?, TILE, ?> builder) {
        builder.with(new AttributeFactoryType(EMFactoryType.ALLOYING));
    }

    public static VoxelShape[] alloyingFactoryShape(@Nullable FactoryTier tier) {
        return BlockShapes.getShape(tier, EMFactoryType.ALLOYING);
    }

    public static boolean isInstaller(ItemStack stack) {
        return stack.getItem() instanceof ItemMaxTierInstaller;
    }
}
