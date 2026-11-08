package com.beipuo.mekenergistics.gametest;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.service.CraftingService;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MePatternDecodeHelper;
import com.beipuo.mekenergistics.mixin.TileEntityMetallurgicInfuserAccessor;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeType;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.registries.MekanismChemicals;
import com.moakiee.thunderbolt.core.crafting.batch.BatchCpuAccounting;
import com.moakiee.thunderbolt.core.crafting.batch.BatchExecutor;
import com.moakiee.thunderbolt.api.crafting.batch.BatchJobView;
import com.moakiee.thunderbolt.api.crafting.batch.BatchTaskHandle;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@GameTestHolder(MekEnergistics.MODID)
@PrefixGameTestTemplate(false)
public final class MeInterfaceModeGameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos POWER = new BlockPos(1, 1, 0);
    private static final BlockPos STORAGE = new BlockPos(0, 1, 1);

    private MeInterfaceModeGameTests() {
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceSupplyRespectsMachineCapacityAndRefundsRemainder(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    seedNetwork(helper, 100);
                    MeAeMachine machine = machine(helper);
                    inputSlot(helper).setStack(new ItemStack(Items.IRON_ORE, 54));
                    machine.getRecipeAeSupport().getInterfaceConfig()
                            .setStack(0, new GenericStack(AEItemKey.of(Items.IRON_ORE), 64));
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(inputSlot(helper).getCount() == 64,
                            "machine input was not filled to 64: " + inputSlot(helper).getCount());
                    helper.assertTrue(interfaceStock(helper, 0) == 64,
                            "interface buffer was not restocked to 64: " + interfaceStock(helper, 0));
                    helper.assertTrue(networkCount(helper, Items.IRON_ORE) == 26,
                            "AE should lose the 10 accepted by the machine plus the 64 kept in the buffer: "
                                    + networkCount(helper, Items.IRON_ORE));
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void thunderboltBatchConsumesCpuInventoryAndPhysicallyFeedsMachine(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    TileEntityMekanism tile = requiredTile(helper, MACHINE);
                    MeUpgradeStateOwner owner = (MeUpgradeStateOwner) tile;
                    helper.assertTrue(owner.getMeUpgradeContainer().install(MeUpgradeType.PATTERN_PROVIDER).successful(),
                            "could not install the ME pattern provider upgrade");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                            List.of(new GenericStack(AEItemKey.of(Items.IRON_ORE), 1)),
                            List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)));
                    machine.getPatternSlots().getFirst().setStack(encoded);
                    // This scenario exercises the smart queue; do not depend on the server config default.
                    machine.setSmartPatternMultiplicationEnabled(true);
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    helper.assertTrue(machine.getMainNode().isActive(), "pattern machine is not active");
                    helper.assertTrue(machine.getAvailablePatterns().size() == 1,
                            "pattern machine did not publish exactly one pattern");
                    IPatternDetails pattern = machine.getAvailablePatterns().getFirst();

                    ListCraftingInventory cpuInventory = new ListCraftingInventory(key -> { });
                    cpuInventory.insert(AEItemKey.of(Items.IRON_ORE), 3_000, Actionable.MODULATE);
                    TestBatchJob job = new TestBatchJob(helper.getLevel(), pattern, 3_000);
                    IGrid grid = machine.getGrid();
                    BatchExecutor.BatchRunResult result = BatchExecutor.runBatchOnly(
                            100,
                            BatchCpuAccounting.Mode.SUCCESSFUL_DISPATCH,
                            (CraftingService) grid.getCraftingService(),
                            grid.getEnergyService(),
                            job,
                            cpuInventory,
                            new HashMap<>(),
                            () -> { });

                    helper.assertTrue(result.dispatchedCopies() == 64,
                            "batch dispatched " + result.dispatchedCopies() + " copies instead of physical capacity 64");
                    helper.assertTrue(cpuInventory.list.get(AEItemKey.of(Items.IRON_ORE)) == 2_936,
                            "CPU inventory did not consume exactly 64 inputs: "
                                    + cpuInventory.list.get(AEItemKey.of(Items.IRON_ORE)));
                    helper.assertTrue(inputSlot(helper).getCount() == 64,
                            "machine did not physically receive 64 inputs: " + inputSlot(helper).getCount());
                    helper.assertTrue(outputSlot(helper).isEmpty(),
                            "machine produced output before its physical input could be processed");
                    helper.assertTrue(job.waitingFor.list.get(AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "crafting job did not register exactly the accepted outputs");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void thunderboltInfuserBatchConsumesBothInputsAndPhysicallyFeedsMachine(GameTestHelper helper) {
        setupMetallurgicInfuser(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    TileEntityMekanism tile = requiredTile(helper, MACHINE);
                    MeUpgradeStateOwner owner = (MeUpgradeStateOwner) tile;
                    helper.assertTrue(owner.getMeUpgradeContainer().install(MeUpgradeType.PATTERN_PROVIDER).successful(),
                            "could not install the ME pattern provider upgrade");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                            List.of(
                                    new GenericStack(AEItemKey.of(requiredItem("mekanism:ingot_osmium")), 1),
                                    new GenericStack(MekanismKey.of(MekanismChemicals.REDSTONE.asStack(10)), 10)),
                            List.of(new GenericStack(
                                    AEItemKey.of(requiredItem("mekanism:basic_control_circuit")), 1)));
                    machine.getPatternSlots().getFirst().setStack(encoded);
                    // This scenario exercises the smart queue; do not depend on the server config default.
                    machine.setSmartPatternMultiplicationEnabled(true);
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    helper.assertTrue(machine.getMainNode().isActive(), "infuser pattern machine is not active");
                    helper.assertTrue(machine.getAvailablePatterns().size() == 1,
                            "infuser did not publish exactly one pattern");
                    IPatternDetails pattern = machine.getAvailablePatterns().getFirst();
                    AEItemKey osmium = AEItemKey.of(requiredItem("mekanism:ingot_osmium"));
                    AEItemKey circuit = AEItemKey.of(requiredItem("mekanism:basic_control_circuit"));
                    MekanismKey redstone = MekanismKey.of(MekanismChemicals.REDSTONE.asStack(1));
                    ListCraftingInventory cpuInventory = new ListCraftingInventory(key -> { });
                    cpuInventory.insert(osmium, 3_000, Actionable.MODULATE);
                    cpuInventory.insert(redstone, 30_000, Actionable.MODULATE);
                    TestBatchJob job = new TestBatchJob(helper.getLevel(), pattern, 3_000);
                    IGrid grid = machine.getGrid();
                    BatchExecutor.BatchRunResult result = BatchExecutor.runBatchOnly(
                            100, BatchCpuAccounting.Mode.SUCCESSFUL_DISPATCH,
                            (CraftingService) grid.getCraftingService(), grid.getEnergyService(),
                            job, cpuInventory, new HashMap<>(), () -> { });
                    TileEntityMetallurgicInfuser infuser = (TileEntityMetallurgicInfuser) requiredTile(helper, MACHINE);
                    TileEntityMetallurgicInfuserAccessor slots = (TileEntityMetallurgicInfuserAccessor) infuser;
                    helper.assertTrue(result.dispatchedCopies() == 64,
                            "infuser batch dispatched " + result.dispatchedCopies() + " copies instead of 64");
                    helper.assertTrue(cpuInventory.list.get(osmium) == 2_936,
                            "CPU inventory did not consume 64 osmium: " + cpuInventory.list.get(osmium));
                    helper.assertTrue(cpuInventory.list.get(redstone) == 29_360,
                            "CPU inventory did not consume 640 redstone: " + cpuInventory.list.get(redstone));
                    helper.assertTrue(slots.mekenergistics$getInputSlot().getCount() == 64,
                            "infuser item slot did not receive 64 osmium: " + slots.mekenergistics$getInputSlot().getCount());
                    helper.assertTrue(infuser.infusionTank.getStored() == 640,
                            "infuser tank did not receive 640 redstone: " + infuser.infusionTank.getStored());
                    helper.assertTrue(slots.mekenergistics$getOutputSlot().isEmpty(),
                            "infuser produced output before its physical inputs could be processed");
                    helper.assertTrue(job.waitingFor.list.get(circuit) == 64,
                            "crafting job did not register exactly 64 control circuits");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 10 * SharedConstants.TICKS_PER_SECOND)
    public static void infuserSmartQueueEventuallyFeedsBothPhysicalInputs(GameTestHelper helper) {
        setupMetallurgicInfuser(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    TileEntityMekanism tile = requiredTile(helper, MACHINE);
                    MeUpgradeStateOwner owner = (MeUpgradeStateOwner) tile;
                    helper.assertTrue(owner.getMeUpgradeContainer().install(MeUpgradeType.PATTERN_PROVIDER).successful(),
                            "could not install the ME pattern provider upgrade");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    machine.setAeOutputMode(AeOutputMode.BOTH);
                    machine.getRecipeAeSupport().invalidatePatternIoCache();
                    helper.assertTrue(machine.getAeOutputMode() == AeOutputMode.BOTH,
                            "infuser did not enter the legacy BOTH output mode");
                    ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                            List.of(
                                    new GenericStack(AEItemKey.of(requiredItem("mekanism:ingot_osmium")), 1),
                                    new GenericStack(MekanismKey.of(MekanismChemicals.REDSTONE.asStack(10)), 10)),
                            List.of(new GenericStack(AEItemKey.of(requiredItem("mekanism:basic_control_circuit")), 1)));
                    machine.getPatternSlots().getFirst().setStack(encoded);
                    // This scenario exercises the smart queue; do not depend on the server config default.
                    machine.setSmartPatternMultiplicationEnabled(true);
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    IPatternDetails pattern = machine.getAvailablePatterns().getFirst();
                    KeyCounter[] inputs = new KeyCounter[] {new KeyCounter(), new KeyCounter()};
                    inputs[0].add(AEItemKey.of(requiredItem("mekanism:ingot_osmium")), 64);
                    inputs[1].add(MekanismKey.of(MekanismChemicals.REDSTONE.asStack(10)), 640);
                    helper.assertTrue(machine.pushPattern(pattern, inputs),
                            "machine rejected the 64-copy smart queue submission");
                    TileEntityMetallurgicInfuserAccessor slots = (TileEntityMetallurgicInfuserAccessor)
                            requiredTile(helper, MACHINE);
                    helper.assertTrue(slots.mekenergistics$getInputSlot().isEmpty(),
                            "smart queue unexpectedly fed the item slot synchronously");
                })
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    TileEntityMetallurgicInfuser infuser = (TileEntityMetallurgicInfuser) requiredTile(helper, MACHINE);
                    TileEntityMetallurgicInfuserAccessor slots = (TileEntityMetallurgicInfuserAccessor) infuser;
                    helper.assertTrue(slots.mekenergistics$getInputSlot().getCount() == 64,
                            "smart queue did not physically feed 64 osmium: "
                                    + slots.mekenergistics$getInputSlot().getCount());
                    helper.assertTrue(infuser.infusionTank.getStored() == 640,
                            "smart queue did not physically feed 640 redstone: " + infuser.infusionTank.getStored());
                    helper.assertTrue(slots.mekenergistics$getOutputSlot().isEmpty(),
                            "smart queue produced output before the test's physical input check");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceDoesNotExtractWhenMachineCannotReceive(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    seedNetwork(helper, 100);
                    inputSlot(helper).setStack(new ItemStack(Items.IRON_ORE, 64));
                    machine(helper).getRecipeAeSupport().getInterfaceConfig()
                            .setStack(0, new GenericStack(AEItemKey.of(Items.IRON_ORE), 64));
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(inputSlot(helper).getCount() == 64,
                            "machine input changed while full: " + inputSlot(helper).getCount());
                    helper.assertTrue(interfaceStock(helper, 0) == 64,
                            "interface buffer was not stocked to 64 while the machine was full: "
                                    + interfaceStock(helper, 0));
                    helper.assertTrue(networkCount(helper, Items.IRON_ORE) == 36,
                            "AE should only decrease by the 64 kept in the interface buffer: "
                                    + networkCount(helper, Items.IRON_ORE));
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfacePartialStockInsertsOnlyActual(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    seedNetwork(helper, 5);
                    machine(helper).getRecipeAeSupport().getInterfaceConfig()
                            .setStack(0, new GenericStack(AEItemKey.of(Items.IRON_ORE), 64));
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(inputSlot(helper).getCount() == 5,
                            "machine input did not receive exactly the available 5: " + inputSlot(helper).getCount());
                    helper.assertTrue(networkCount(helper, Items.IRON_ORE) == 0,
                            "AE was not drained to zero: " + networkCount(helper, Items.IRON_ORE));
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceDuplicateSlotsAccumulateBatches(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    seedNetwork(helper, 200);
                    var config = machine(helper).getRecipeAeSupport().getInterfaceConfig();
                    config.setStack(0, new GenericStack(AEItemKey.of(Items.IRON_ORE), 64));
                    config.setStack(1, new GenericStack(AEItemKey.of(Items.IRON_ORE), 64));
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(inputSlot(helper).getCount() == 64,
                            "machine input did not reach one full batch: " + inputSlot(helper).getCount());
                    helper.assertTrue(interfaceStock(helper, 0) == 64 && interfaceStock(helper, 1) == 64,
                            "both interface columns should stay stocked at 64");
                    helper.assertTrue(networkCount(helper, Items.IRON_ORE) == 8,
                            "AE should lose one machine batch plus two stocked columns: "
                                    + networkCount(helper, Items.IRON_ORE));
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceModeIsolatesPatternServices(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    helper.assertTrue(machine.getMainNode().getNode() != null
                                    && machine.getMainNode().getNode().isActive(),
                            "interface mode dropped off the AE network");
                    helper.assertTrue(MeAeMachine.modeOf(machine).isOutputInterface(),
                            "machine is not running interface mode");
                    helper.assertTrue(machine.getAvailablePatterns().isEmpty(),
                            "interface mode exposed patterns");
                    ItemStack patternStack = PatternDetailsHelper.encodeProcessingPattern(
                            java.util.List.of(new GenericStack(AEItemKey.of(Items.IRON_ORE), 1)),
                            java.util.List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)));
                    IPatternDetails pattern = MePatternDecodeHelper.safeDecode(patternStack, helper.getLevel(),
                            "interface isolation test");
                    machine.getPatternSlots().getFirst().setStack(patternStack);
                    helper.assertTrue(machine.getAvailablePatterns().isEmpty(),
                            "interface mode accepted a pattern into its published list");
                    KeyCounter[] inputs = new KeyCounter[]{new KeyCounter()};
                    inputs[0].add(AEItemKey.of(Items.IRON_ORE), 1);
                    helper.assertTrue(pattern != null && !machine.pushPattern(pattern, inputs),
                            "interface mode accepted a pattern push");
                    helper.assertTrue(machine.maxAcceptedPatternCopies(inputs) == 0,
                            "interface mode accepted pattern copies");
                    helper.assertTrue(machine.isBusy(),
                            "interface mode did not report busy");
                    helper.assertTrue(!machine.isVisibleInTerminal(),
                            "interface mode stayed visible in the Pattern Access Terminal");
                    helper.assertTrue(!machine.hasPassiveCraftingUpgrade(),
                            "interface mode kept passive crafting active");
                    helper.assertTrue(!machine.isSmartPatternMultiplicationEnabled(),
                            "interface mode kept smart pattern multiplication active");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceOutputReturnsToAeByActualReceived(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    outputSlot(helper).setStack(new ItemStack(Items.IRON_INGOT, 10));
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(networkCount(helper, Items.IRON_INGOT) == 10,
                            "AE did not receive exactly the 10 returned items: "
                                    + networkCount(helper, Items.IRON_INGOT));
                    helper.assertTrue(outputSlot(helper).isEmpty(),
                            "machine output was not cleared after AE accepted it");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceOutputStaysWhenNetworkDrops(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    outputSlot(helper).setStack(new ItemStack(Items.IRON_INGOT, 10));
                    helper.setBlock(POWER, Blocks.AIR);
                })
                .thenExecuteAfter(3 * SharedConstants.TICKS_PER_SECOND, () -> {
                    helper.assertTrue(outputSlot(helper).getCount() == 10,
                            "machine output was lost while the AE network was down");
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                                    net.minecraft.world.entity.item.ItemEntity.class,
                                    new net.minecraft.world.phys.AABB(helper.absolutePos(MACHINE)).inflate(4.0D))
                                    .isEmpty(),
                            "output items were dropped instead of preserved");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 8 * SharedConstants.TICKS_PER_SECOND)
    public static void interfaceConfigSurvivesBlockEntityReload(GameTestHelper helper) {
        setupInterfaceMachine(helper);
        helper.startSequence()
                .thenExecuteAfter(1, () -> installInterfaceUpgrade(helper))
                .thenExecuteAfter(2 * SharedConstants.TICKS_PER_SECOND, () -> {
                    MeAeMachine machine = machine(helper);
                    AbstractMeAeSupport<?> support = machine.getRecipeAeSupport();
                    support.getInterfaceConfig()
                            .setStack(0, new GenericStack(AEItemKey.of(Items.IRON_ORE), 64));
                    support.getInterfaceInventory()
                            .insert(0, AEItemKey.of(Items.IRON_ORE), 32, Actionable.MODULATE);
                    CompoundTag saved = new CompoundTag();
                    support.save(saved, helper.getLevel().registryAccess());
                    support.getInterfaceConfig().setStack(0, null);
                    support.getInterfaceInventory().extract(0, AEItemKey.of(Items.IRON_ORE), 32, Actionable.MODULATE);
                    support.load(saved, helper.getLevel().registryAccess());
                    var configured = support.getInterfaceConfig().getStack(0);
                    var stocked = support.getInterfaceInventory().getStack(0);
                    helper.assertTrue(configured != null && configured.amount() == 64,
                            "interface config did not survive save/load");
                    helper.assertTrue(stocked != null && stocked.amount() == 32,
                            "interface inventory did not survive save/load");
                    MeUpgradeStateOwner owner = (MeUpgradeStateOwner) helper.getBlockEntity(MACHINE);
                    helper.assertTrue(owner.getMeUpgradeContainer().isInstalled(MeUpgradeType.OUTPUT_INTERFACE),
                            "interface upgrade did not survive save/load");
                })
                .thenSucceed();
    }

    private static void setupInterfaceMachine(GameTestHelper helper) {
        helper.setBlock(MACHINE, requiredBlock("mekanism:enrichment_chamber"));
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(STORAGE, AEBlocks.ME_CHEST.block());
    }

    private static void setupMetallurgicInfuser(GameTestHelper helper) {
        helper.setBlock(MACHINE, requiredBlock("mekanism:metallurgic_infuser"));
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(STORAGE, AEBlocks.ME_CHEST.block());
    }

    private static void installInterfaceUpgrade(GameTestHelper helper) {
        MEChestBlockEntity chest = (MEChestBlockEntity) helper.getBlockEntity(STORAGE);
        chest.setCell(AEItems.ITEM_CELL_1K.stack());
        TileEntityMekanism tile = requiredTile(helper, MACHINE);
        drainLocalEnergy(helper, tile, "enrichment chamber before interface test");
        MeUpgradeStateOwner owner = (MeUpgradeStateOwner) tile;
        helper.assertTrue(owner.getMeUpgradeContainer().install(MeUpgradeType.OUTPUT_INTERFACE).successful(),
                "could not install the ME output interface upgrade");
    }

    private static MeAeMachine machine(GameTestHelper helper) {
        TileEntityMekanism tile = requiredTile(helper, MACHINE);
        if (!(tile instanceof MeAeMachine machine)) {
            throw new GameTestAssertException("machine does not implement MeAeMachine");
        }
        return machine;
    }

    private static void seedNetwork(GameTestHelper helper, int amount) {
        MeAeMachine machine = machine(helper);
        IGrid grid = machine.getGrid();
        if (grid == null) {
            throw new GameTestAssertException("machine has no AE grid to seed");
        }
        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = IActionSource.ofMachine((IActionHost) machine);
        long inserted = storage.insert(AEItemKey.of(Items.IRON_ORE), amount, Actionable.MODULATE, source);
        if (inserted != amount) {
            throw new GameTestAssertException("seeded " + inserted + " of " + amount + " iron ore");
        }
    }

    private static long interfaceStock(GameTestHelper helper, int slot) {
        var stack = machine(helper).getRecipeAeSupport().getInterfaceInventory().getStack(slot);
        return stack == null ? 0 : stack.amount();
    }

    private static long networkCount(GameTestHelper helper, net.minecraft.world.level.ItemLike item) {
        MeAeMachine machine = machine(helper);
        IGrid grid = machine.getGrid();
        if (grid == null) {
            return -1;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        return storage.extract(AEItemKey.of(item), Long.MAX_VALUE, Actionable.SIMULATE,
                IActionSource.ofMachine((IActionHost) machine));
    }

    private static mekanism.api.inventory.IInventorySlot inputSlot(GameTestHelper helper) {
        return requiredTile(helper, MACHINE).getInventorySlots(null).stream()
                .filter(slot -> slot instanceof mekanism.common.inventory.slot.InputInventorySlot)
                .findFirst()
                .orElseThrow(() -> new GameTestAssertException("machine has no input slot"));
    }

    private static mekanism.api.inventory.IInventorySlot outputSlot(GameTestHelper helper) {
        return requiredTile(helper, MACHINE).getInventorySlots(null).stream()
                .filter(slot -> slot instanceof mekanism.common.inventory.slot.OutputInventorySlot)
                .findFirst()
                .orElseThrow(() -> new GameTestAssertException("machine has no output slot"));
    }

    private static void drainLocalEnergy(GameTestHelper helper, TileEntityMekanism tile, String description) {
        var containers = tile.getEnergyContainers(null);
        helper.assertTrue(!containers.isEmpty(), description + " has no local energy container");
        for (IEnergyContainer container : containers) {
            container.setEnergy(0);
        }
    }

    private static TileEntityMekanism requiredTile(GameTestHelper helper, BlockPos position) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (blockEntity instanceof TileEntityMekanism tile) {
            return tile;
        }
        throw new GameTestAssertException("position " + position + " did not create a Mekanism block entity");
    }

    private static Block requiredBlock(String id) {
        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Missing required block " + id);
        }
        return block;
    }

    private static net.minecraft.world.item.Item requiredItem(String id) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.parse(id));
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing required item " + id);
        }
        return item;
    }

    private static final class TestBatchJob implements BatchJobView {
        private final List<BatchTaskHandle> tasks = new ArrayList<>();
        private final ListCraftingInventory waitingFor = new ListCraftingInventory(key -> { });
        private final net.minecraft.world.level.Level level;
        private final java.util.UUID craftingId = java.util.UUID.randomUUID();

        @Override
        public net.minecraft.world.level.Level level() {
            return this.level;
        }

        @Override
        public java.util.UUID craftingId() {
            return this.craftingId;
        }

        private TestBatchJob(net.minecraft.world.level.Level level, IPatternDetails pattern, long copies) {
            this.level = level;
            this.tasks.add(new BatchTaskHandle() {
                private long remaining = copies;

                @Override
                public IPatternDetails details() {
                    return pattern;
                }

                @Override
                public long getValue() {
                    return this.remaining;
                }

                @Override
                public void setValue(long value) {
                    this.remaining = value;
                }
            });
        }

        @Override
        public Iterator<BatchTaskHandle> taskIterator() {
            return this.tasks.iterator();
        }

        @Override
        public ListCraftingInventory waitingFor() {
            return this.waitingFor;
        }

        @Override
        public void addContainerMaxItems(long count, appeng.api.stacks.AEKeyType type) {
        }
    }
}
