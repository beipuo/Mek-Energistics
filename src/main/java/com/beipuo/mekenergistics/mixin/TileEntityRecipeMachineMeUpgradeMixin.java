package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeInfusionModePolicy;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeDataMigration;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwnerSupport;
import com.beipuo.mekenergistics.upgrade.MeUpgradeType;
import com.beipuo.mekenergistics.upgrade.MekanismRecipeUpgradeProfiles;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRuntimeState;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityCombiner;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityAdvancedElectricMachine.class, TileEntityCombiner.class,
        TileEntityPrecisionSawmill.class, TileEntityMetallurgicInfuser.class}, remap = false)
public abstract class TileEntityRecipeMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    @Unique private MeRecipeMachineAeSupport<?> mekenergistics$aeSupport;
    @Unique private AeOutputMode mekenergistics$aeOutputMode;
    @Unique private MeUpgradeRuntimeState mekenergistics$runtimeState;
    @Unique private MeUpgradeStateOwnerSupport mekenergistics$upgradeOwner;

    @Unique
    private MeUpgradeRuntimeState mekenergistics$runtimeState() {
        if (this.mekenergistics$runtimeState == null) {
            this.mekenergistics$runtimeState = new MeUpgradeRuntimeState();
        }
        return this.mekenergistics$runtimeState;
    }

    @Unique
    private TileEntityMekanism mekenergistics$tile() {
        return (TileEntityMekanism) (Object) this;
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
                    () -> mekenergistics$tile().supportsUpgrades() ? mekenergistics$tile().getComponent() : null);
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
        TileEntityMekanism tile = mekenergistics$tile();
        tile.setChanged();
        mekenergistics$invalidateCapabilities();
    }

    @Unique
    private AeOutputMode mekenergistics$defaultOutputMode() {
        return mekenergistics$tile() instanceof TileEntityMetallurgicInfuser ? AeOutputMode.ITEMS : AeOutputMode.BOTH;
    }

    @Unique
    private MeUpgradeMachineProfile<?> mekenergistics$profile() {
        return MekanismRecipeUpgradeProfiles.forTile(mekenergistics$tile());
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private MeRecipeMachineAeSupport<?> mekenergistics$support() {
        if (this.mekenergistics$aeSupport == null) {
            this.mekenergistics$aeSupport = new MeRecipeMachineAeSupport(mekenergistics$tile());
        }
        return this.mekenergistics$aeSupport;
    }

    @Override public MeUpgradeMachineProfile<?> getMeUpgradeProfile() { return mekenergistics$profile(); }
    @Override public boolean isMeUpgradeTarget() { return mekenergistics$profile() != null; }

    @Override
    public boolean isMeUpgradeActive() {
        TileEntityMekanism tile = mekenergistics$tile();
        return mekenergistics$runtimeState().activeFor(tile.getLevel(), isMeUpgradeTarget(),
                mekenergistics$isInstalled());
    }

        @Unique
    private boolean mekenergistics$isInstalled() {
        return isMeUpgradeTarget() && getMeUpgradeContainer() != null
                && (getMeUpgradeContainer().isInstalled(MeUpgradeType.PATTERN_PROVIDER)
                    || getMeUpgradeContainer().isInstalled(MeUpgradeType.OUTPUT_INTERFACE));
    }

    @Override public AbstractMeAeSupport<?> getRecipeAeSupport() { return mekenergistics$support(); }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public MeInputLayout getPatternInputLayout() {
        return ((MeUpgradeMachineProfile) mekenergistics$profile()).inputLayoutFor(mekenergistics$tile());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<? extends MeOutputPort> getPatternOutputPorts() {
        return ((MeUpgradeMachineProfile) mekenergistics$profile()).outputPortsFor(mekenergistics$tile());
    }

    @Override
    public AeOutputMode getAeOutputMode() {
        return this.mekenergistics$aeOutputMode == null ? mekenergistics$defaultOutputMode() : this.mekenergistics$aeOutputMode;
    }

    @Override
    public void cycleAeOutputMode() {
        this.mekenergistics$aeOutputMode = getAeOutputMode().next();
        mekenergistics$support().invalidatePatternIoCache();
        saveChanges();
    }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener,
            CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        mekenergistics$runtimeState().setRecipeCacheListener(recipeCacheListener);
        if (isMeUpgradeTarget()) {
            cir.setReturnValue(mekenergistics$support().withPatternSlots(cir.getReturnValue()));
        }
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackChemicalToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemicalRecipe(ItemStackChemicalToItemStackRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackChemicalToItemStackRecipe>> cir) {
        mekenergistics$wrapEnergy(cir);
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/CombinerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapCombinerRecipe(CombinerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<CombinerRecipe>> cir) {
        mekenergistics$wrapEnergy(cir);
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/SawmillRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapSawmillRecipe(SawmillRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<SawmillRecipe>> cir) {
        mekenergistics$wrapEnergy(cir);
    }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrapEnergy(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        if (isMeUpgradeTarget()) {
            cir.setReturnValue(mekenergistics$support().wrapRecipeEnergy(mekenergistics$energyContainer(), cir.getReturnValue()));
        }
    }

    @Unique
    private MachineEnergyContainer<?> mekenergistics$energyContainer() {
        Object tile = this;
        if (tile instanceof TileEntityAdvancedElectricMachine advanced) return advanced.getEnergyContainer();
        if (tile instanceof TileEntityCombiner combiner) return combiner.getEnergyContainer();
        if (tile instanceof TileEntityPrecisionSawmill sawmill) return sawmill.getEnergyContainer();
        return ((TileEntityMetallurgicInfuser) tile).getEnergyContainer();
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$processPatternIo(CallbackInfoReturnable<Boolean> cir) {
        boolean active = isMeUpgradeActive();
        switch (mekenergistics$runtimeState().transitionTo(active)) {
            case ACTIVATE -> {
                mekenergistics$syncOwner();
                TileEntityMekanism tile = mekenergistics$tile();
                mekenergistics$support().create(tile.getLevel(), tile.getBlockPos());
                mekenergistics$invalidateCapabilities();
                mekenergistics$runtimeState().refreshRecipeCache();
            }
            case DEACTIVATE -> {
                mekenergistics$support().destroyNode();
                mekenergistics$invalidateCapabilities();
                mekenergistics$runtimeState().refreshRecipeCache();
            }
            case NONE -> { }
        }
        if (active) {
            AeOutputMode outputMode = mekenergistics$tile() instanceof TileEntityMetallurgicInfuser
                    ? MeInfusionModePolicy.effectiveOutputMode(getAeOutputMode())
                    : getAeOutputMode();
            cir.setReturnValue(mekenergistics$support().processPatternIo(outputMode, cir.getReturnValue()));
        }
    }

    @Unique
    private void mekenergistics$invalidateCapabilities() {
        TileEntityMekanism tile = mekenergistics$tile();
        if (tile.getLevel() != null) tile.getLevel().invalidateCapabilities(tile.getBlockPos());
    }

    @Unique
    private void mekenergistics$syncOwner() {
        TileEntityMekanism tile = mekenergistics$tile();
        if (!(tile.getLevel() instanceof ServerLevel level) || tile.getOwnerUUID() == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(tile.getOwnerUUID());
        if (player != null) mekenergistics$support().setOwningPlayer(player);
    }

    @Override
    public void createMeNodeIfActive() {
        if (isMeUpgradeActive()) {
            mekenergistics$syncOwner();
            mekenergistics$support().createOnFirstTick();
        }
    }

    @Override
    public void destroyMeNode() {
        if (this.mekenergistics$aeSupport != null) this.mekenergistics$aeSupport.destroyNode();
        mekenergistics$runtimeState().markInactive();
    }

    @Override public void onChunkUnloaded() { destroyMeNode(); }

    @Override
    public void addMeTrackers(MekanismContainer container) {
        if (!isMeUpgradeTarget()) return;
        container.track(SyncableBoolean.create(this::mekenergistics$isInstalled,
                active -> mekenergistics$runtimeState().acceptClientActive(active)));
        mekenergistics$support().addAeTrackers(container, this::getAeOutputMode,
                mode -> this.mekenergistics$aeOutputMode = mode, true);
    }

    @Override
    public void saveMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.mekenergistics$aeSupport != null) {
            this.mekenergistics$aeSupport.saveAeState(tag, registries, getAeOutputMode());
        }
    }

    @Override
    public void loadMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (isMeUpgradeTarget()) {
            getMeUpgradeContainer().setData(MeUpgradeDataMigration.migrate(tag).data());
            getMeUpgradeContainer().migrateToNativeComponent();
            AeOutputMode loaded = mekenergistics$support().loadAeState(tag, registries);
            this.mekenergistics$aeOutputMode = tag.contains("AeOutputMode") ? loaded : mekenergistics$defaultOutputMode();
        }
    }
}
