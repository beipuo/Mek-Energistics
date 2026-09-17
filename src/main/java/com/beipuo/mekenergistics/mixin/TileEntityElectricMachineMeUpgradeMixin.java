package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeDataMigration;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfiles;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRuntimeState;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwnerSupport;
import com.beipuo.mekenergistics.upgrade.MeUpgradeType;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityElectricMachine.class, remap = false)
public abstract class TileEntityElectricMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    @Unique
    private MeRecipeMachineAeSupport<?> mekenergistics$aeSupport;
    @Unique
    private AeOutputMode mekenergistics$aeOutputMode = AeOutputMode.BOTH;
    @Unique
    private MeUpgradeRuntimeState mekenergistics$runtimeState;
    @Unique
    private MeUpgradeStateOwnerSupport mekenergistics$upgradeOwner;

    @Unique
    private MeUpgradeRuntimeState mekenergistics$runtimeState() {
        if (this.mekenergistics$runtimeState == null) {
            this.mekenergistics$runtimeState = new MeUpgradeRuntimeState();
        }
        return this.mekenergistics$runtimeState;
    }

    @Unique
    private MeUpgradeStateOwnerSupport mekenergistics$upgradeOwner() {
        if (this.mekenergistics$upgradeOwner == null) {
            this.mekenergistics$upgradeOwner = new MeUpgradeStateOwnerSupport(
                    () -> false,
                    () -> mekenergistics$support().getPatternSlots().stream()
                            .allMatch(slot -> slot.getStack().isEmpty()),
                    () -> mekenergistics$support().canRemoveInterfaceUpgrade(),
                    this::mekenergistics$onMeUpgradeStateChanged,
                    () -> {
                        TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
                        return tile.supportsUpgrades() ? tile.getComponent() : null;
                    });
        }
        return this.mekenergistics$upgradeOwner;
    }

    @Override
    public MeUpgradeContainer getMeUpgradeContainer() {
        return mekenergistics$upgradeOwner().getMeUpgradeContainer();
    }

    @Override
    public boolean isPatternInventoryEmpty() {
        return mekenergistics$upgradeOwner().isPatternInventoryEmpty();
    }

    @Override
    public void onMeUpgradeStateChanged() {
        mekenergistics$upgradeOwner().onMeUpgradeStateChanged();
    }

    @Unique
    private void mekenergistics$onMeUpgradeStateChanged() {
        TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
        tile.setChanged();
        mekenergistics$invalidateCapabilities();
    }

    @Unique
    private MeUpgradeMachineProfile<TileEntityElectricMachine> mekenergistics$profile() {
        MeUpgradeMachineProfile<TileEntityElectricMachine> resolved =
                MeUpgradeMachineProfiles.forTile((TileEntityElectricMachine) (Object) this);
        return resolved;
    }

    @Override
    public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        return mekenergistics$profile();
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private MeRecipeMachineAeSupport<?> mekenergistics$support() {
        if (this.mekenergistics$aeSupport == null) {
            this.mekenergistics$aeSupport = new MeRecipeMachineAeSupport(
                    (TileEntityElectricMachine) (Object) this);
        }
        return this.mekenergistics$aeSupport;
    }

    @Override
    public boolean isMeUpgradeTarget() {
        TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
        MeUpgradeMachineProfile<TileEntityElectricMachine> profile = mekenergistics$profile();
        return profile != null && profile.matches(tile);
    }

    @Override
    public boolean isMeUpgradeActive() {
        TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
        return mekenergistics$runtimeState().activeFor(tile.getLevel(), isMeUpgradeTarget(),
                mekenergistics$isMeUpgradeInstalled());
    }

    @Unique
    private boolean mekenergistics$isMeUpgradeInstalled() {
        return isMeUpgradeTarget() && getMeUpgradeContainer() != null
                && (getMeUpgradeContainer().isInstalled(MeUpgradeType.PATTERN_PROVIDER)
                    || getMeUpgradeContainer().isInstalled(MeUpgradeType.OUTPUT_INTERFACE));
    }

    @Override
    public AbstractMeAeSupport<?> getRecipeAeSupport() {
        return mekenergistics$support();
    }

    @Override
    public MeInputLayout getPatternInputLayout() {
        return mekenergistics$profile().inputLayoutFor((TileEntityElectricMachine) (Object) this);
    }

    @Override
    public List<? extends MeOutputPort> getPatternOutputPorts() {
        return mekenergistics$profile().outputPortsFor((TileEntityElectricMachine) (Object) this);
    }

    @Override
    public AeOutputMode getAeOutputMode() {
        return this.mekenergistics$aeOutputMode;
    }

    @Override
    public void cycleAeOutputMode() {
        this.mekenergistics$aeOutputMode = this.mekenergistics$aeOutputMode.next();
        saveChanges();
    }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener,
          IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener,
          CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        mekenergistics$runtimeState().setRecipeCacheListener(recipeCacheListener);
        if (isMeUpgradeTarget()) {
            cir.setReturnValue(mekenergistics$support().withPatternSlots(cir.getReturnValue()));
        }
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true)
    private void mekenergistics$wrapRecipeEnergy(ItemStackToItemStackRecipe recipe, int cacheIndex,
          CallbackInfoReturnable<CachedRecipe<ItemStackToItemStackRecipe>> cir) {
        if (isMeUpgradeTarget()) {
            TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
            cir.setReturnValue(mekenergistics$support().wrapRecipeEnergy(
                    tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$processPatternIo(CallbackInfoReturnable<Boolean> cir) {
        boolean active = isMeUpgradeActive();
        switch (mekenergistics$runtimeState().transitionTo(active)) {
            case ACTIVATE -> {
                TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
                mekenergistics$syncMekanismOwner(tile);
                mekenergistics$support().create(tile.getLevel(), tile.getBlockPos());
                mekenergistics$invalidateCapabilities();
                mekenergistics$runtimeState().refreshRecipeCache();
            }
            case DEACTIVATE -> {
                mekenergistics$support().destroyNode();
                mekenergistics$invalidateCapabilities();
                mekenergistics$runtimeState().refreshRecipeCache();
            }
            case NONE -> {
            }
        }
        if (active) {
            cir.setReturnValue(mekenergistics$support().processPatternIo(
                    this.mekenergistics$aeOutputMode, cir.getReturnValue()));
        }
    }

    @Unique
    private void mekenergistics$invalidateCapabilities() {
        TileEntityElectricMachine tile = (TileEntityElectricMachine) (Object) this;
        if (tile.getLevel() != null) {
            tile.getLevel().invalidateCapabilities(tile.getBlockPos());
        }
    }

    @Unique
    @Override
    public void createMeNodeIfActive() {
        if (isMeUpgradeActive()) {
            mekenergistics$syncMekanismOwner((TileEntityElectricMachine) (Object) this);
            mekenergistics$support().createOnFirstTick();
        }
    }

    @Unique
    private void mekenergistics$syncMekanismOwner(TileEntityElectricMachine tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || tile.getOwnerUUID() == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(tile.getOwnerUUID());
        if (player != null) {
            mekenergistics$support().setOwningPlayer(player);
        }
    }

    @Unique
    @Override
    public void destroyMeNode() {
        if (this.mekenergistics$aeSupport != null) {
            this.mekenergistics$aeSupport.destroyNode();
        }
        mekenergistics$runtimeState().markInactive();
    }

    @Override
    public void onChunkUnloaded() {
        destroyMeNode();
    }

    @Unique
    @Override
    public void addMeTrackers(mekanism.common.inventory.container.MekanismContainer container) {
        if (isMeUpgradeTarget()) {
            container.track(SyncableBoolean.create(
                    this::mekenergistics$isMeUpgradeInstalled,
                    active -> {
                        mekenergistics$runtimeState().acceptClientActive(active);
                    }));
            mekenergistics$support().addAeTrackers(container, this::getAeOutputMode,
                    mode -> this.mekenergistics$aeOutputMode = mode, true);
        }
    }

    @Unique
    @Override
    public void saveMeState(net.minecraft.nbt.CompoundTag tag,
          net.minecraft.core.HolderLookup.Provider registries) {
        if (this.mekenergistics$aeSupport != null) {
            this.mekenergistics$aeSupport.saveAeState(tag, registries, this.mekenergistics$aeOutputMode);
        }
    }

    @Unique
    @Override
    public void loadMeState(net.minecraft.nbt.CompoundTag tag,
          net.minecraft.core.HolderLookup.Provider registries) {
        if (isMeUpgradeTarget()) {
            getMeUpgradeContainer().setData(MeUpgradeDataMigration.migrate(tag).data());
            getMeUpgradeContainer().migrateToNativeComponent();
            this.mekenergistics$aeOutputMode = mekenergistics$support().loadAeState(tag, registries);
        }
    }
}
