package com.beipuo.mekenergistics.mixin.extendedae;

import java.util.function.Consumer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.glodblock.github.extendedae.container.ContainerRenamer.class, remap = false)
public abstract class ContainerRenamerMixin {
    @Inject(method = "setter", at = @At("HEAD"), cancellable = true)
    private static void mekenergistics$setterForMekanismTile(Object target, CallbackInfoReturnable<Consumer<String>> cir) {
        if (target instanceof TileEntityMekanism tile) {
            cir.setReturnValue(name -> {
                Component customName = name.isBlank() ? null : Component.literal(name);
                tile.setCustomName(customName);
            });
        }
    }
}
