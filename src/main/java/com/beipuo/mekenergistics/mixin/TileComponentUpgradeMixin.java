package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.upgrade.MeMekanismUpgrades;
import com.beipuo.mekenergistics.upgrade.MeUpgradeConflictPolicy;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeComponentState;
import com.beipuo.mekenergistics.blockentity.support.MePatternSlotTransfer;
import mekanism.api.Upgrade;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forwards native Mekanism upgrade changes into the existing AE lifecycle. */
@Mixin(value = TileComponentUpgrade.class, remap = false)
public abstract class TileComponentUpgradeMixin implements MeUpgradeComponentState {
    @Shadow
    @Final
    private mekanism.common.tile.base.TileEntityMekanism tile;

    @Override
    public CompoundTag mekenergistics$captureTierState(HolderLookup.Provider registries) {
        var state = MePatternSlotTransfer.saveMeState(this.tile, registries);
        state.merge(MePatternSlotTransfer.save(this.tile, registries));
        return state;
    }

    @Inject(method = "deserialize", at = @At("RETURN"))
    private void mekenergistics$restoreTierState(CompoundTag tag,
            HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains(TRANSFER_TAG, Tag.TAG_COMPOUND)) {
            var state = tag.getCompound(TRANSFER_TAG);
            MePatternSlotTransfer.loadMeState(this.tile, registries, state);
            MePatternSlotTransfer.load(this.tile, registries, state);
        }
    }

    @Inject(method = "addUpgrades(Lmekanism/api/Upgrade;I)I", at = @At("RETURN"))
    private void mekenergistics$upgradeInstalled(Upgrade upgrade, int maxAvailable,
            CallbackInfoReturnable<Integer> cir) {
        notifyMeUpgradeChange(upgrade);
    }

    @Inject(method = "addUpgrades(Lmekanism/api/Upgrade;II)I", at = @At("HEAD"), cancellable = true)
    private void mekenergistics$rejectConflictingMeUpgrade(Upgrade upgrade, int installed, int maxAvailable,
            CallbackInfoReturnable<Integer> cir) {
        if (this.tile instanceof MeUpgradeStateOwner owner
                && MeUpgradeConflictPolicy.blocksNativeInstall(MeMekanismUpgrades.toType(upgrade), owner)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "addUpgrades(Lmekanism/api/Upgrade;II)I", at = @At("RETURN"))
    private void mekenergistics$upgradeInstalledFromSlot(Upgrade upgrade, int installed, int maxAvailable,
            CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() > 0) {
            notifyMeUpgradeChange(upgrade);
        }
    }

    @Inject(method = "removeUpgrade", at = @At("RETURN"))
    private void mekenergistics$upgradeRemoved(Upgrade upgrade, boolean removeAll, CallbackInfo ci) {
        notifyMeUpgradeChange(upgrade);
    }

    @Inject(method = "removeUpgrade", at = @At("HEAD"), cancellable = true)
    private void mekenergistics$guardInterfaceInventory(Upgrade upgrade, boolean removeAll, CallbackInfo ci) {
        if (MeMekanismUpgrades.toType(upgrade) == com.beipuo.mekenergistics.upgrade.MeUpgradeType.OUTPUT_INTERFACE
                && this.tile instanceof MeUpgradeStateOwner owner && !owner.isInterfaceInventoryEmpty()) {
            ci.cancel();
        }
    }

    private void notifyMeUpgradeChange(Upgrade upgrade) {
        if (MeMekanismUpgrades.toType(upgrade) != null && this.tile instanceof MeUpgradeStateOwner owner) {
            owner.onMeUpgradeStateChanged();
        }
    }
}
