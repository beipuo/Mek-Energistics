package com.beipuo.mekenergistics.gametest;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeType;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingDispatchTargetAvailability;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingProviderId;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.provider.CountedCraftingProviderAdapters;
import java.util.List;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MekEnergistics.MODID)
@PrefixGameTestTemplate(false)
public final class MeOptionalBatchIntegrationGameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);

    private MeOptionalBatchIntegrationGameTests() {
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void neoEcoNativeBatchEnqueuesAndFeedsMachine(GameTestHelper helper) {
        setup(helper)
                .thenExecute(() -> {
                    MeAeMachine machine = machine(helper);
                    IPatternDetails pattern = machine.getAvailablePatterns().getFirst();
                    KeyCounter[] prototype = prototype();
                    Object session = openNeoEcoSession(machine);
                    helper.assertTrue(invokeNeoEco(session, "inspect", pattern, prototype, 8) == 8,
                            "NeoECO did not inspect eight smart copies");
                    helper.assertTrue(invokeNeoEco(session, "submit", pattern, prototype, 8) == 0,
                            "NeoECO native submission did not transfer all eight copies");
                    helper.assertTrue(prototype[0].get(AEItemKey.of(Items.IRON_ORE)) == 1,
                            "NeoECO mutated the borrowed one-copy template");
                    helper.assertTrue(inputSlot(helper).isEmpty(),
                            "NeoECO bypassed the persistent smart queue");
                    helper.assertTrue(machine.getRecipeAeSupport().isPatternBusy(),
                            "NeoECO accepted the inputs without creating pending work");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(inputSlot(helper).getCount() == 8,
                            "NeoECO's native smart enqueue did not physically feed exactly eight inputs");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void neoEcoNativeBatchRejectsCapacityLostAfterInspection(GameTestHelper helper) {
        setup(helper)
                .thenExecute(() -> {
                    MeAeMachine machine = machine(helper);
                    IPatternDetails pattern = machine.getAvailablePatterns().getFirst();
                    KeyCounter[] prototype = prototype();
                    Object session = openNeoEcoSession(machine);
                    helper.assertTrue(invokeNeoEco(session, "inspect", pattern, prototype, 8) == 8,
                            "NeoECO did not inspect the initially available batch");
                    inputSlot(helper).setStack(new ItemStack(Items.IRON_ORE, 64));
                    helper.assertTrue(invokeNeoEco(session, "submit", pattern, prototype, 8) == 8,
                            "NeoECO transferred ownership despite zero live machine capacity");
                    helper.assertTrue(prototype[0].get(AEItemKey.of(Items.IRON_ORE)) == 1,
                            "rejected NeoECO submission consumed its template");
                    inputSlot(helper).setStack(ItemStack.EMPTY);
                    helper.assertTrue(!machine.getRecipeAeSupport().isPatternBusy(),
                            "rejected NeoECO submission left pending work behind");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(inputSlot(helper).isEmpty(),
                            "rejected NeoECO inputs were fed later");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void dataNativeCapacityCaptureAndAdmissionFeedOnlyAcceptedCopies(GameTestHelper helper) {
        setup(helper)
                .thenExecute(() -> {
                    MeAeMachine machine = machine(helper);
                    IPatternDetails pattern = machine.getAvailablePatterns().getFirst();
                    KeyCounter[] prototype = prototype();
                    helper.assertTrue(CountedCraftingProviderAdapters.supportsCountedDispatch(machine),
                            "DataEnergistics did not recognize the machine's counted provider mixin");
                    var snapshots = CountedCraftingProviderAdapters.captureCapacity(machine,
                            new CraftingProviderId(1, 1), pattern, prototype, 96,
                            "mekenergistics:batch_gametest", 1, 1, helper.getLevel().getGameTime());
                    helper.assertTrue(snapshots.size() == 1,
                            "DataEnergistics did not capture the machine's aggregate capacity");
                    var snapshot = snapshots.getFirst();
                    var blocked = CountedCraftingProviderAdapters.prepare(machine, pattern, prototype,
                            96, snapshot, target -> false);
                    helper.assertTrue(!blocked.accepted() && inputSlot(helper).isEmpty(),
                            "DataEnergistics ignored a blocked dispatch target");
                    var prepared = CountedCraftingProviderAdapters.prepare(machine, pattern, prototype,
                            96, snapshot, CraftingDispatchTargetAvailability.all());
                    helper.assertTrue(prepared.accepted(),
                            "DataEnergistics rejected a live aggregate batch");
                    var admission = prepared.admission();
                    helper.assertTrue(CountedCraftingProviderAdapters.validatedAdmissionCount(
                                    machine, admission, 96) == 64,
                            "DataEnergistics did not limit the offer to physical capacity");
                    helper.assertTrue(!admission.hasTransferredInputOwnership(),
                            "DataEnergistics transferred inputs during read-only preparation");
                    helper.assertTrue(admission.commit(prototype) && admission.hasTransferredInputOwnership(),
                            "DataEnergistics did not commit counted input ownership");
                    helper.assertTrue(inputSlot(helper).getCount() == 64,
                            "DataEnergistics did not physically deliver exactly 64 accepted inputs");
                    helper.assertTrue(prototype[0].get(AEItemKey.of(Items.IRON_ORE)) == 1,
                            "DataEnergistics mutated its per-craft template");
                    helper.assertTrue(CountedCraftingProviderAdapters.captureCapacity(machine,
                                    new CraftingProviderId(1, 1), pattern, prototype, 96,
                                    "mekenergistics:batch_gametest", 1, 2, helper.getLevel().getGameTime()).isEmpty(),
                            "DataEnergistics still advertised capacity after the machine filled");
                })
                .thenSucceed();
    }

    private static GameTestSequence setup(GameTestHelper helper) {
        helper.setBlock(MACHINE, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("mekanism:enrichment_chamber")));
        helper.setBlock(new BlockPos(1, 1, 0), AEBlocks.CREATIVE_ENERGY_CELL.block());
        return helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    TileEntityMekanism tile = (TileEntityMekanism) helper.getBlockEntity(MACHINE);
                    for (var container : tile.getEnergyContainers(null)) {
                        container.setEnergy(0);
                    }
                    helper.assertTrue(((MeUpgradeStateOwner) tile).getMeUpgradeContainer()
                                    .install(MeUpgradeType.PATTERN_PROVIDER).successful(),
                            "could not install the pattern provider upgrade");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    machine.getPatternSlots().getFirst().setStack(PatternDetailsHelper.encodeProcessingPattern(
                            List.of(new GenericStack(AEItemKey.of(Items.IRON_ORE), 1)),
                            List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1))));
                    machine.setSmartPatternMultiplicationEnabled(true);
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(machine(helper).getMainNode().isActive(), "pattern machine is not active");
                    helper.assertTrue(machine(helper).getAvailablePatterns().size() == 1,
                            "pattern machine did not publish exactly one pattern");
                });
    }

    private static MeAeMachine machine(GameTestHelper helper) {
        return (MeAeMachine) helper.getBlockEntity(MACHINE);
    }

    private static IInventorySlot inputSlot(GameTestHelper helper) {
        return ((TileEntityMekanism) helper.getBlockEntity(MACHINE)).getInventorySlots(null).stream()
                .filter(slot -> slot instanceof InputInventorySlot)
                .findFirst().orElseThrow(() -> new GameTestAssertException("machine has no input slot"));
    }

    private static KeyCounter[] prototype() {
        KeyCounter counter = new KeyCounter();
        counter.add(AEItemKey.of(Items.IRON_ORE), 1);
        return new KeyCounter[] {counter};
    }

    private static Object openNeoEcoSession(ICraftingProvider provider) {
        try {
            Object session = Class.forName(
                    "cn.dancingsnow.neoecoae.compat.mekenergistics.ECOMekEnergisticsBatchCapability")
                    .getMethod("open", ICraftingProvider.class).invoke(null, provider);
            if (session == null) {
                throw new GameTestAssertException("NeoECO did not open its native Mek-Energistics session");
            }
            return session;
        } catch (ReflectiveOperationException exception) {
            throw new GameTestAssertException("NeoECO native session failed: " + exception);
        }
    }

    private static long invokeNeoEco(Object session, String operation, IPatternDetails pattern,
            KeyCounter[] prototype, long count) {
        try {
            return (long) session.getClass().getMethod(operation,
                    IPatternDetails.class, KeyCounter[].class, long.class)
                    .invoke(session, pattern, prototype, count);
        } catch (ReflectiveOperationException exception) {
            throw new GameTestAssertException("NeoECO native " + operation + " failed: " + exception);
        }
    }
}
