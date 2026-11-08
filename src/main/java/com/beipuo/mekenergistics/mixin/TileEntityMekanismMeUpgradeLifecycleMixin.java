package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.upgrade.MePatternAutomationRuntimeHolder;
import com.beipuo.mekenergistics.upgrade.MePatternAutomationRuntimes;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class TileEntityMekanismMeUpgradeLifecycleMixin implements MePatternAutomationRuntimeHolder {
    /** Runtime of a pattern-automation SPI host; null for every other tile. */
    @Unique
    private MeUpgradeRecipeMachineRuntime mekenergistics$patternAutomationRuntime;

    @Override
    public MeUpgradeRecipeMachineRuntime mekenergistics$getPatternAutomationRuntime() {
        return this.mekenergistics$patternAutomationRuntime;
    }

    @Override
    public void mekenergistics$setPatternAutomationRuntime(MeUpgradeRecipeMachineRuntime runtime) {
        this.mekenergistics$patternAutomationRuntime = runtime;
    }

    /**
     * A concrete class method takes precedence over AE2 and optional integration
     * interface defaults when this Mixin is applied to third-party Mekanism tiles.
     */
    public IGridNode getGridNode(Direction side) {
        if ((Object) this instanceof MeAeMachine machine) {
            IManagedGridNode node = machine.getMainNode();
            return node == null ? null : node.getNode();
        }
        return null;
    }

    /**
     * Same rationale as getGridNode: a concrete class method takes precedence over AE2 and optional
     * integration interface defaults. Productive Bees Genesis' IAe2OutputHost supplies its own
     * default getCableConnectionType, so every concrete tile must resolve the method in a class
     * rather than between two interface defaults (IncompatibleClassChangeError).
     */
    public AECableType getCableConnectionType(Direction side) {
        return AECableType.SMART;
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lmekanism/common/tile/base/TileEntityMekanism;onUpdateServer()Z"))
    private static void mekenergistics$refillLocalEnergyFromMe(Level level, BlockPos pos, BlockState state,
            TileEntityMekanism tile, CallbackInfo ci) {
        if (!(tile instanceof MeAeMachine machine)) {
            return;
        }
        if (!(state.getBlock() instanceof MeMekanismMachineBlock)) {
            if (!(tile instanceof MeUpgradeableMachine upgradeable)
                    || !upgradeable.isMeUpgradeTarget() || !upgradeable.isMeUpgradeActive()) {
                return;
            }
        }
        machine.getRecipeAeSupport().refillLocalEnergyBuffers();
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void mekenergistics$destroyMeNode(CallbackInfo ci) {
        if ((Object) this instanceof MeUpgradeableMachine machine) {
            machine.destroyMeNode();
        }
        MePatternAutomationRuntimes.forget((TileEntityMekanism) (Object) this);
    }

    @Inject(method = "addContainerTrackers", at = @At("RETURN"))
    private void mekenergistics$addMeTrackers(MekanismContainer container, CallbackInfo ci) {
        if ((Object) this instanceof MeUpgradeableMachine machine) {
            machine.addMeTrackers(container);
        }
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void mekenergistics$saveMeState(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if ((Object) this instanceof MeUpgradeableMachine machine) {
            machine.saveMeState(tag, registries);
        }
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void mekenergistics$loadMeState(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if ((Object) this instanceof MeUpgradeableMachine machine) {
            machine.loadMeState(tag, registries);
            if (machine instanceof MeUpgradeStateOwner owner) {
                owner.getMeUpgradeContainer().migrateToNativeComponent();
            }
        }
    }
}
