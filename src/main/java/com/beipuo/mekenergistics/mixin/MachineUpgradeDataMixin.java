package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.upgrade.MeUpgradeComponentState;
import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.upgrade.MachineUpgradeData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MachineUpgradeData.class, remap = false)
public abstract class MachineUpgradeDataMixin {
    @Shadow @Final public CompoundTag components;

    @Inject(method = "<init>(Lnet/minecraft/core/HolderLookup$Provider;ZLmekanism/common/tile/interfaces/IRedstoneControl$RedstoneControl;Lmekanism/api/energy/IEnergyContainer;[ILmekanism/common/inventory/slot/EnergyInventorySlot;Ljava/util/List;Ljava/util/List;ZLjava/util/List;)V",
            at = @At("RETURN"))
    private void mekenergistics$captureMeState(HolderLookup.Provider registries, boolean redstone,
            RedstoneControl control, IEnergyContainer energy, int[] progress, EnergyInventorySlot energySlot,
            List<IInventorySlot> inputs, List<IInventorySlot> outputs, boolean sorting,
            List<ITileComponent> sourceComponents, CallbackInfo ci) {
        for (ITileComponent component : sourceComponents) {
            if (!(component instanceof MeUpgradeComponentState state)) {
                continue;
            }
            CompoundTag snapshot = state.mekenergistics$captureTierState(registries);
            if (!snapshot.isEmpty()) {
                CompoundTag componentTag = this.components.getCompound(component.getComponentKey());
                componentTag.put(MeUpgradeComponentState.TRANSFER_TAG, snapshot);
                this.components.put(component.getComponentKey(), componentTag);
            }
        }
    }
}
