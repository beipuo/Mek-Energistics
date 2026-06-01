package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = TileEntityUpdateable.class, remap = false)
public abstract class TileEntityUpdateableMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            ),
            index = 0
    )
    private static BlockEntityType<?> mekenergistics$useMeBlockEntityType(
            BlockEntityType<?> original,
            BlockPos pos,
            BlockState state
    ) {
        if (state.getBlock() instanceof MeMekanismMachineBlock block) {
            return ModBlockEntities.getMachineBlockEntity(block.getMachine()).get();
        }
        return original;
    }
}
