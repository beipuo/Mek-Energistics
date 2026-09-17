package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.jerry.mekmm.api.recipes.RecyclerRecipe;
import com.jerry.mekmm.api.recipes.StamperRecipe;
import com.jerry.mekmm.common.tile.machine.TileEntityRecycler;
import com.jerry.mekmm.common.tile.machine.TileEntityStamper;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityRecycler.class, TileEntityStamper.class}, remap = false)
public abstract class MekmmSimpleRecipeMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    @Unique private MeUpgradeRecipeMachineRuntime mekenergistics$runtime;
    @Unique private InputInventorySlot mekenergistics$inputSlot;
    @Unique private InputInventorySlot mekenergistics$secondaryInputSlot;
    @Unique private OutputInventorySlot mekenergistics$outputSlot;

    @Unique private TileEntityMekanism mekenergistics$tile() { return (TileEntityMekanism) (Object) this; }

    @Unique
    private MeUpgradeRecipeMachineRuntime mekenergistics$runtime() {
        if (this.mekenergistics$runtime == null) {
            this.mekenergistics$runtime = new MeUpgradeRecipeMachineRuntime(mekenergistics$tile(), AeOutputMode.BOTH);
        }
        return this.mekenergistics$runtime;
    }

    @Override
    public MeUpgradeContainer getMeUpgradeContainer() {
        return mekenergistics$runtime().upgrades();
    }

    @Override
    public boolean supportsNativePatternProvider() {
        return mekenergistics$runtime().supportsNativePatternProvider();
    }

    @Override
    public boolean isPatternInventoryEmpty() {
        return mekenergistics$runtime().isPatternInventoryEmpty();
    }

    @Override
    public void onMeUpgradeStateChanged() {
        mekenergistics$runtime().onMeUpgradeStateChanged();
    }

    @Unique
    private MeMekanismMachine mekenergistics$machine() {
        var id = BuiltInRegistries.BLOCK.getKey(mekenergistics$tile().getBlockState().getBlock());
        return CompatMachineCatalog.findBySourceBlockId(id)
                .map(spec -> spec.machine() == MeMekanismMachine.RECYCLER
                        || spec.machine() == MeMekanismMachine.CNC_STAMPER ? spec.machine() : null)
                .orElse(null);
    }

    @Override
    public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        MeMekanismMachine machine = mekenergistics$machine();
        if (machine == null) return null;
        return new MeUpgradeMachineProfile<>(candidate -> candidate == mekenergistics$tile(),
                candidate -> this.mekenergistics$inputSlot == null ? MeInputLayout.empty()
                        : MeInputLayout.unordered(List.of(MeMachineIoAdapter.itemInput(this.mekenergistics$inputSlot))),
                candidate -> this.mekenergistics$outputSlot == null ? List.of()
                        : List.of(MeMachineIoAdapter.itemOutput(this.mekenergistics$outputSlot)),
                machine,
                candidate -> new net.minecraft.world.item.ItemStack(candidate.getBlockState().getBlock()),
                candidate -> candidate.getBlockState().getBlock().getName());
    }

    @Override public boolean isMeUpgradeTarget() { return mekenergistics$machine() != null; }
    @Override public boolean isMeUpgradeActive() { return mekenergistics$runtime().active(isMeUpgradeTarget()); }
    @Override public AbstractMeAeSupport<?> getRecipeAeSupport() { return mekenergistics$runtime().support(); }
    @Override public AeOutputMode getAeOutputMode() { return mekenergistics$runtime().outputMode(); }
    @Override public void cycleAeOutputMode() { mekenergistics$runtime().cycleOutputMode(); }

    @Override public MeInputLayout getPatternInputLayout() {
        return this.mekenergistics$inputSlot == null ? MeInputLayout.empty()
                : MeInputLayout.unordered(List.of(MeMachineIoAdapter.itemInput(this.mekenergistics$inputSlot)));
    }
    @Override public List<? extends MeOutputPort> getPatternOutputPorts() {
        return this.mekenergistics$outputSlot == null ? List.of()
                : List.of(MeMachineIoAdapter.itemOutput(this.mekenergistics$outputSlot));
    }

    @Override
    public boolean isPatternBusy() {
        return (Object) this instanceof TileEntityStamper
                && (this.mekenergistics$secondaryInputSlot == null || this.mekenergistics$secondaryInputSlot.isEmpty());
    }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener,
            CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        if (!isMeUpgradeTarget()) return;
        List<InputInventorySlot> inputs = new ArrayList<>();
        for (IInventorySlot slot : cir.getReturnValue().getInventorySlots(null)) {
            if (slot instanceof InputInventorySlot input) inputs.add(input);
            else if (slot instanceof OutputInventorySlot output && this.mekenergistics$outputSlot == null) {
                this.mekenergistics$outputSlot = output;
            }
        }
        if (!inputs.isEmpty()) this.mekenergistics$inputSlot = inputs.getFirst();
        if (inputs.size() > 1) this.mekenergistics$secondaryInputSlot = inputs.get(1);
        cir.setReturnValue(mekenergistics$runtime().withPatternSlots(cir.getReturnValue(), recipeCacheListener));
    }

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/RecyclerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapRecycler(RecyclerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<RecyclerRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/StamperRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapStamper(StamperRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<StamperRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrapEnergy(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        cir.setReturnValue(mekenergistics$runtime().wrapEnergy(
                mekenergistics$energyContainer(), cir.getReturnValue()));
    }

    @Unique
    private MachineEnergyContainer<?> mekenergistics$energyContainer() {
        return (Object) this instanceof TileEntityRecycler recycler
                ? recycler.getEnergyContainer() : ((TileEntityStamper) (Object) this).getEnergyContainer();
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$processPatternIo(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(mekenergistics$runtime().tick(isMeUpgradeActive(), cir.getReturnValue()));
    }

    @Override public void createMeNodeIfActive() { mekenergistics$runtime().createNodeIfActive(isMeUpgradeActive()); }
    @Override public void destroyMeNode() { if (this.mekenergistics$runtime != null) this.mekenergistics$runtime.destroyNode(); }
    @Override public void onChunkUnloaded() { destroyMeNode(); }
    @Override public void addMeTrackers(MekanismContainer container) { mekenergistics$runtime().addTrackers(container, isMeUpgradeTarget()); }
    @Override public void saveMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.mekenergistics$runtime != null) this.mekenergistics$runtime.save(tag, registries);
    }
    @Override public void loadMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (isMeUpgradeTarget()) mekenergistics$runtime().load(tag, registries);
    }
}
