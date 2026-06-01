package com.beipuo.mekenergistics.blockentity.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.MeFactoryAeSupport;
import com.beipuo.mekenergistics.blockentity.MeFactoryPatternInput;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class MeExternalFactorySupport {
    private MeExternalFactorySupport() {
    }

    public interface Owner extends MeFactoryAeMachine {
        List<IInventorySlot> meInputSlots();

        List<IInventorySlot> meOutputSlots();

        void unpauseRecipeMonitors();
    }

    public static <TILE extends TileEntityMekanism & ISideConfiguration> IEnergyContainerHolder energyContainers(
            TILE tile, IContentsListener listener, Runnable unpauseRecipeMonitors,
            java.util.function.Consumer<MeFactoryAeSupport.AeBackedFactoryEnergyContainer<TILE>> containerSetter) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(tile);
        MeFactoryAeSupport.AeBackedFactoryEnergyContainer<TILE> container = new MeFactoryAeSupport.AeBackedFactoryEnergyContainer<>(tile, () -> {
            listener.onContentsChanged();
            unpauseRecipeMonitors.run();
        });
        containerSetter.accept(container);
        builder.addContainer(container);
        return builder.build();
    }

    public static IInventorySlotHolder withPatternSlots(IInventorySlotHolder original, Owner owner) {
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(owner.getAeSupport().getPatternSlots());
            return slots;
        };
    }

    public static boolean pushSingleItem(Owner owner, KeyCounter[] inputHolder) {
        if (inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        return input != null && input.isItem() && insertItem(owner, input.item());
    }

    public static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        ItemStack itemInput = ItemStack.EMPTY;
        ChemicalStack chemicalInput = ChemicalStack.EMPTY;
        for (KeyCounter counter : inputHolder) {
            MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
            if (input == null) {
                return false;
            }
            if (input.isItem()) {
                if (!itemInput.isEmpty()) {
                    return false;
                }
                itemInput = input.item();
            } else if (input.isChemical()) {
                if (!chemicalInput.isEmpty()) {
                    return false;
                }
                chemicalInput = input.chemical();
            }
        }
        if (itemInput.isEmpty() || chemicalInput.isEmpty()) {
            return false;
        }
        for (IInventorySlot inputSlot : owner.meInputSlots()) {
            if (inputSlot.insertItem(itemInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                    && chemicalTank.insert(chemicalInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
                inputSlot.insertItem(itemInput, Action.EXECUTE, AutomationType.INTERNAL);
                chemicalTank.insert(chemicalInput, Action.EXECUTE, AutomationType.INTERNAL);
                owner.saveChanges();
                return true;
            }
        }
        return false;
    }

    public static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput first = MeFactoryPatternInput.single(inputHolder[0]);
        MeFactoryPatternInput second = MeFactoryPatternInput.single(inputHolder[1]);
        if (first == null || second == null || !first.isItem() || !second.isItem()) {
            return false;
        }
        for (IInventorySlot inputSlot : owner.meInputSlots()) {
            if (inputSlot.insertItem(first.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                    && extraSlot.insertItem(second.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
                inputSlot.insertItem(first.item(), Action.EXECUTE, AutomationType.INTERNAL);
                extraSlot.insertItem(second.item(), Action.EXECUTE, AutomationType.INTERNAL);
                owner.saveChanges();
                return true;
            }
        }
        return false;
    }

    public static boolean pushThreeItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        if (inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        ItemStack first = getItem(inputHolder[0]);
        ItemStack second = getItem(inputHolder[1]);
        ItemStack third = getItem(inputHolder[2]);
        if (first.isEmpty() || second.isEmpty() || third.isEmpty()) {
            return false;
        }
        for (IInventorySlot inputSlot : owner.meInputSlots()) {
            if (inputSlot.insertItem(first.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                    && secondSlot.insertItem(second.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                    && thirdSlot.insertItem(third.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
                inputSlot.insertItem(first, Action.EXECUTE, AutomationType.INTERNAL);
                secondSlot.insertItem(second, Action.EXECUTE, AutomationType.INTERNAL);
                thirdSlot.insertItem(third, Action.EXECUTE, AutomationType.INTERNAL);
                owner.saveChanges();
                return true;
            }
        }
        return false;
    }

    public static boolean insertItem(Owner owner, ItemStack input) {
        if (input.isEmpty()) {
            return false;
        }
        for (IInventorySlot inputSlot : owner.meInputSlots()) {
            if (inputSlot.insertItem(input.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
                inputSlot.insertItem(input, Action.EXECUTE, AutomationType.INTERNAL);
                owner.saveChanges();
                return true;
            }
        }
        return false;
    }

    private static ItemStack getItem(KeyCounter counter) {
        MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
        return input != null && input.isItem() ? input.item() : ItemStack.EMPTY;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket) {
        return owner.getAeSupport().insertOutputSlotsIntoNetwork(owner.meOutputSlots()) || sendUpdatePacket;
    }

    public static void createNodeOnFirstTick(TileEntityMekanism tile, MeFactoryAeSupport support, Level level, BlockPos pos) {
        GridHelper.onFirstTick(tile, ignored -> support.create(level, pos));
    }

    public static List<IPatternDetails> getAvailablePatterns(MeFactoryAeSupport support) {
        return support.getAvailablePatterns();
    }

    public static void save(MeFactoryAeSupport support, CompoundTag tag, HolderLookup.Provider registries) {
        support.save(tag);
        support.saveSlots(tag, registries);
    }

    public static void load(MeFactoryAeSupport support, CompoundTag tag, HolderLookup.Provider registries) {
        support.load(tag);
        support.loadSlots(tag, registries);
    }
}
