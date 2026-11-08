package com.beipuo.mekenergistics.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackChemicalToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.support.MeSmartPatternMultiplication;
import com.beipuo.mekenergistics.network.packet.RefundPendingPatternsPacket;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeType;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.security.SecurityMode;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@GameTestHolder(MekEnergistics.MODID)
@PrefixGameTestTemplate(false)
public final class MeSmartPatternRefundGameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos POWER = new BlockPos(1, 1, 0);
    private static final BlockPos STORAGE = new BlockPos(0, 1, 1);
    private static final AEItemKey ORE = AEItemKey.of(Items.IRON_ORE);

    @GameTest(template = "empty_3x3x3", timeoutTicks = 140)
    public static void separatorRejectsFortyBucketsOfBrineThenProcessesEightBucketsOfWater(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekenergistics:me_electrolytic_separator"));
        helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR, "ME electrolytic separator is not registered");
        helper.setBlock(MACHINE, block);
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        var separator = (MeElectrolyticSeparatorBlockEntity) helper.getBlockEntity(MACHINE);
        var brine = AEFluidKey.of(MekanismFluids.BRINE.asStack(1));
        var water = AEFluidKey.of(Fluids.WATER);
        separator.setAeOutputMode(AeOutputMode.NONE);
        separator.setSmartPatternMultiplicationEnabled(true);
        separator.getPatternSlots().get(0).setStack(PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(brine, 40_000)),
                List.of(new GenericStack(MekanismKey.of(MekanismChemicals.SODIUM.asStack(1)), 4_000),
                        new GenericStack(MekanismKey.of(MekanismChemicals.CHLORINE.asStack(1)), 4_000))));
        separator.getPatternSlots().get(1).setStack(PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(water, 8_000)),
                List.of(new GenericStack(MekanismKey.of(MekanismChemicals.HYDROGEN.asStack(1)), 8_000),
                        new GenericStack(MekanismKey.of(MekanismChemicals.OXYGEN.asStack(1)), 4_000))));
        helper.startSequence().thenExecuteAfter(40, () -> {
            var support = separator.getRecipeAeSupport();
            helper.assertTrue(separator.getMainNode().isActive(), "separator AE network is inactive");
            helper.assertTrue(separator.fluidTank.getCapacity() == 24_000, "unexpected separator tank capacity");
            helper.assertTrue(separator.fluidTank.isEmpty(), "separator input was not initially empty");
            helper.assertTrue(separator.getAvailablePatterns().size() == 2, "separator did not publish both fluid patterns");
            var brinePattern = separator.getAvailablePatterns().get(0);
            KeyCounter brineInput = new KeyCounter();
            brineInput.add(brine, 40_000);
            helper.assertTrue(!support.pushPatternWithAdapter(brinePattern, new KeyCounter[]{brineInput}),
                    "separator accepted a 40,000 mB single-craft brine request");
            helper.assertTrue(!support.enqueueSmartPattern(brinePattern, new KeyCounter[]{brineInput}),
                    "reflected enqueue accepted a 40,000 mB single-craft brine request");
            helper.assertTrue(!separator.isBusy(), "rejected brine request left the separator busy");
            helper.assertTrue(separator.fluidTank.isEmpty(), "rejected brine request changed the physical tank");
            helper.assertTrue(separator.isSmartPatternMultiplicationEnabled(), "brine capacity rejection disabled multiplication");
            var waterPattern = separator.getAvailablePatterns().get(1);
            KeyCounter waterInput = new KeyCounter();
            waterInput.add(water, 8_000);
            helper.assertTrue(support.pushPatternWithAdapter(waterPattern, new KeyCounter[]{waterInput}),
                    "normal 8,000 mB water pattern was blocked after brine rejection");
            helper.assertTrue(support.processPatternIo(separator.getAeOutputMode(), false),
                    "separator production pattern-I/O path did not feed the admitted water");
            helper.assertTrue(separator.fluidTank.getFluidAmount() == 8_000
                            && separator.fluidTank.getFluid().getFluid() == Fluids.WATER,
                    "separator did not physically receive exactly 8,000 mB water");
            helper.assertTrue(!separator.isBusy(), "fed water request remained in the smart queue");
        }).thenExecuteAfter(20, () -> {
            long consumed = 8_000L - separator.fluidTank.getFluidAmount();
            helper.assertTrue(consumed > 0 && consumed % 2 == 0, "separator did not process the physically fed water");
            helper.assertTrue(separator.leftTank.getStack().getChemical() == MekanismChemicals.HYDROGEN.asStack(1).getChemical(),
                    "separator did not produce hydrogen");
            helper.assertTrue(separator.rightTank.getStack().getChemical() == MekanismChemicals.OXYGEN.asStack(1).getChemical(),
                    "separator did not produce oxygen");
            helper.assertTrue(separator.leftTank.getStored() == consumed
                            && separator.rightTank.getStored() == consumed / 2,
                    "water consumption and retained hydrogen/oxygen outputs do not balance");
        }).thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 100)
    public static void ultimateInjectingFactoryRejectsGoldCrystalBatchExceedingHclTank(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekenergistics:me_ultimate_injecting_factory"));
        helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR, "ME ultimate injecting factory is not registered");
        helper.setBlock(MACHINE, block);
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        var factory = (MeItemStackChemicalToItemStackFactoryBlockEntity) helper.getBlockEntity(MACHINE);
        var crystalItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:crystal_gold"));
        var shardItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:shard_gold"));
        helper.assertTrue(crystalItem != Items.AIR && shardItem != Items.AIR, "gold processing items are not registered");
        var crystal = AEItemKey.of(crystalItem);
        var hcl = MekanismKey.of(MekanismChemicals.HYDROGEN_CHLORIDE.asStack(1));
        factory.setSmartPatternMultiplicationEnabled(true);
        factory.getPatternSlots().getFirst().setStack(PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(crystal, 32), new GenericStack(hcl, 6_400)),
                List.of(new GenericStack(AEItemKey.of(shardItem), 32))));
        helper.startSequence().thenExecuteAfter(40, () -> {
            var support = factory.getAeSupport();
            helper.assertTrue(factory.getMainNode().isActive(), "factory AE network is inactive");
            helper.assertTrue(factory.getChemicalTank().getCapacity() == 1_890, "unexpected ultimate injecting HCl capacity");
            helper.assertTrue(factory.getChemicalTank().isEmpty(), "factory chemical tank was not initially empty");
            helper.assertTrue(factory.getAvailablePatterns().size() == 1, "factory did not publish the gold/HCl pattern");
            var pattern = factory.getAvailablePatterns().getFirst();
            KeyCounter items = new KeyCounter();
            items.add(crystal, 32);
            KeyCounter chemicals = new KeyCounter();
            chemicals.add(hcl, 6_400);
            KeyCounter[] inputs = {items, chemicals};
            helper.assertTrue(!support.pushPatternWithAdapter(pattern, inputs),
                    "factory accepted 32 gold crystals plus 6,400 mB HCl despite its 1,890 mB tank");
            helper.assertTrue(!support.enqueueSmartPattern(pattern, inputs),
                    "reflected enqueue bypassed the factory HCl capacity check");
            helper.assertTrue(!factory.isBusy(), "rejected gold/HCl request left the factory busy");
            helper.assertTrue(factory.getChemicalTank().isEmpty()
                            && factory.meInputSlots().stream().allMatch(mekanism.api.inventory.IInventorySlot::isEmpty),
                    "rejected gold/HCl request changed physical inputs");
            helper.assertTrue(factory.isSmartPatternMultiplicationEnabled(), "HCl capacity rejection disabled multiplication");
        }).thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 400)
    public static void realNetworkRefundRetriesOfflinePartialAndFullStorageDespiteOutputBacklog(GameTestHelper helper) {
        setupUpgradedMachine(helper);
        long[] capacity = {0};
        helper.startSequence().thenExecuteAfter(1, () -> installProviderAndCell(helper))
                .thenExecuteAfter(40, () -> {
                    var machine = machine(helper);
                    var support = machine.getRecipeAeSupport();
                    helper.assertTrue(machine.getMainNode().isActive(), "network is inactive");
                    capacity[0] = storage(helper).insert(ORE, Long.MAX_VALUE, Actionable.MODULATE, source(helper));
                    helper.assertTrue(capacity[0] > 1_000, "real cell did not accept seed inventory");
                    helper.assertTrue(storage(helper).extract(ORE, 17, Actionable.MODULATE, source(helper)) == 17,
                            "could not leave exactly 17 items of free space");
                    helper.assertTrue(support.enqueueSmartPattern(machine.getAvailablePatterns().getFirst(), counters(1_000)),
                            "batched request exceeding physical capacity was not queued by its single-craft size");
                    outputSlot(helper).setStack(new ItemStack(Items.GOLD_INGOT));
                    helper.assertTrue(support.hasPatternOutputBacklog(machine.getAeOutputMode()), "missing output backlog");
                    machine.getMainNode().setExposedOnSides(Set.of());
                    machine.getMainNode().getNode().getGrid().getEnergyService()
                            .extractAEPower(Double.MAX_VALUE, Actionable.MODULATE, PowerMultiplier.ONE);
                    support.requestPendingPatternRefund();
                    helper.assertTrue(refundBalance(helper) == 1_000, "refund did not retain the exact queued balance");
                }).thenExecuteAfter(40, () -> {
                    helper.assertTrue(!machine(helper).getMainNode().isActive(), "isolated network did not go offline");
                    helper.assertTrue(refundBalance(helper) == 1_000, "offline refund lost inputs");
                    helper.assertTrue(outputSlot(helper).getCount() == 1, "offline output backlog changed");
                    machine(helper).getMainNode().setExposedOnSides(EnumSet.allOf(Direction.class));
                    machine(helper).getRecipeAeSupport().alertAeTicker();
                }).thenExecuteAfter(60, () -> {
                    helper.assertTrue(machine(helper).getMainNode().isActive(), "network did not reconnect");
                    helper.assertTrue(networkCount(helper) == capacity[0], "partial refund did not fill exactly 17 available spaces");
                    helper.assertTrue(refundBalance(helper) == 983, "partial refund did not retain 983 inputs");
                    helper.assertTrue(outputSlot(helper).getCount() == 1, "refund required clearing the output backlog");
                    var support = machine(helper).getRecipeAeSupport();
                    CompoundTag saved = new CompoundTag();
                    support.save(saved, helper.getLevel().registryAccess());
                    support.saveSlots(saved, helper.getLevel().registryAccess());
                    support.load(saved, helper.getLevel().registryAccess());
                    support.loadSlots(saved, helper.getLevel().registryAccess());
                    support.requestPendingPatternRefund();
                    helper.assertTrue(refundBalance(helper) == 983, "reload or repeated click duplicated the balance");
                }).thenExecuteAfter(40, () -> {
                    helper.assertTrue(networkCount(helper) == capacity[0], "full network received duplicate refunds");
                    helper.assertTrue(refundBalance(helper) == 983, "full network lost the refund remainder");
                    helper.assertTrue(machine(helper).isBusy(), "pending refund no longer reports busy");
                    helper.assertTrue(storage(helper).extract(ORE, 983, Actionable.MODULATE, source(helper)) == 983,
                            "could not free space for the remaining balance");
                }).thenExecuteAfter(40, () -> {
                    helper.assertTrue(networkCount(helper) == capacity[0], "remaining balance was lost or duplicated: stored="
                            + networkCount(helper) + ", capacity=" + capacity[0] + ", refund=" + refundBalance(helper)
                            + ", output=" + outputSlot(helper).getCount());
                    helper.assertTrue(refundBalance(helper) == 0, "refund remainder did not drain");
                    helper.assertTrue(!machine(helper).isBusy(), "completed refund did not release busy");
                    helper.assertTrue(outputSlot(helper).getCount() == 1, "output backlog was unexpectedly consumed");
                }).thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 100)
    public static void refundPacketRequiresMatchingOpenTargetRangeAndSecurity(GameTestHelper helper) {
        setupUpgradedMachine(helper);
        helper.startSequence().thenExecuteAfter(1, () -> installProviderAndCell(helper))
                .thenExecuteAfter(40, () -> {
                    var machine = machine(helper);
                    var support = machine.getRecipeAeSupport();
                    helper.assertTrue(support.enqueueSmartPattern(machine.getAvailablePatterns().getFirst(), counters(1_000)),
                            "could not queue refund inputs");
                    ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                            new GameProfile(UUID.randomUUID(), "refund-test"));
                    var target = helper.absolutePos(MACHINE);
                    player.setPos(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
                    sendRefund(player, target);
                    helper.assertTrue(refundBalance(helper) == 0, "packet accepted without an open machine menu");
                    var tile = (TileEntityMekanism) helper.getBlockEntity(MACHINE);
                    var menu = new MekanismTileContainer<>(MekanismContainerTypes.ENRICHMENT_CHAMBER,
                            1, player.getInventory(), tile);
                    player.containerMenu = menu;
                    try {
                        sendRefund(player, helper.absolutePos(STORAGE));
                        helper.assertTrue(refundBalance(helper) == 0, "packet accepted for a different target");
                        player.setPos(target.getX() + 20, target.getY(), target.getZ());
                        sendRefund(player, target);
                        helper.assertTrue(refundBalance(helper) == 0, "out-of-range packet accepted");
                        player.setPos(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
                        CompoundTag security = new CompoundTag();
                        security.putUUID(mekanism.api.SerializationConstants.OWNER_UUID, UUID.randomUUID());
                        mekanism.common.util.NBTUtils.writeEnum(security,
                                mekanism.api.SerializationConstants.SECURITY_MODE, SecurityMode.PRIVATE);
                        tile.getSecurity().deserialize(security, helper.getLevel().registryAccess());
                        player.containerMenu = menu;
                        helper.assertTrue(!menu.canPlayerAccess(player), "test player unexpectedly bypasses private security");
                        sendRefund(player, target);
                        helper.assertTrue(refundBalance(helper) == 0, "packet bypassed private machine security");
                        security.putUUID(mekanism.api.SerializationConstants.OWNER_UUID, player.getUUID());
                        tile.getSecurity().deserialize(security, helper.getLevel().registryAccess());
                        player.containerMenu = menu;
                        helper.assertTrue(menu.canPlayerAccess(player), "machine owner cannot access the menu");
                        sendRefund(player, target);
                        helper.assertTrue(refundBalance(helper) == 1_000, "authorized packet did not start refund");
                        sendRefund(player, target);
                        helper.assertTrue(refundBalance(helper) == 1_000, "repeated packet duplicated refund inputs");
                    } finally {
                        menu.removed(player);
                        player.containerMenu = player.inventoryMenu;
                    }
                }).thenSucceed();
    }

    private static void setupUpgradedMachine(GameTestHelper helper) {
        helper.setBlock(MACHINE, mekanism.common.registries.MekanismBlocks.ENRICHMENT_CHAMBER.value());
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(STORAGE, AEBlocks.ME_CHEST.block());
    }

    private static void installProviderAndCell(GameTestHelper helper) {
        var tile = (TileEntityMekanism) helper.getBlockEntity(MACHINE);
        helper.assertTrue(((MeUpgradeStateOwner) tile).getMeUpgradeContainer().install(MeUpgradeType.PATTERN_PROVIDER).successful(),
                "could not install ME pattern provider");
        var cell = AEItems.ITEM_CELL_1K.stack();
        appeng.items.contents.CellConfig.create(cell).setStack(0, new GenericStack(ORE, 1));
        ((MEChestBlockEntity) helper.getBlockEntity(STORAGE)).setCell(cell);
        machine(helper).getPatternSlots().getFirst().setStack(PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(ORE, 1)), List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1))));
        machine(helper).setSmartPatternMultiplicationEnabled(true);
        for (var energy : tile.getEnergyContainers(null)) energy.setEnergy(0);
    }

    private static MeAeMachine machine(GameTestHelper helper) {
        return (MeAeMachine) helper.getBlockEntity(MACHINE);
    }

    private static appeng.api.storage.MEStorage storage(GameTestHelper helper) {
        return machine(helper).getGrid().getStorageService().getInventory();
    }

    private static IActionSource source(GameTestHelper helper) {
        return IActionSource.ofMachine(machine(helper));
    }

    private static long networkCount(GameTestHelper helper) {
        return storage(helper).extract(ORE, Long.MAX_VALUE, Actionable.SIMULATE, source(helper));
    }

    private static mekanism.api.inventory.IInventorySlot outputSlot(GameTestHelper helper) {
        return ((TileEntityMekanism) helper.getBlockEntity(MACHINE)).getInventorySlots(null).stream()
                .filter(OutputInventorySlot.class::isInstance).findFirst().orElseThrow();
    }

    private static KeyCounter[] counters(long amount) {
        KeyCounter counter = new KeyCounter();
        counter.add(ORE, amount);
        return new KeyCounter[]{counter};
    }

    private static long refundBalance(GameTestHelper helper) {
        CompoundTag saved = new CompoundTag();
        machine(helper).getRecipeAeSupport().saveSlots(saved, helper.getLevel().registryAccess());
        ListTag balances = saved.getList("SmartPatternRefunds", net.minecraft.nbt.Tag.TAG_COMPOUND);
        long total = 0;
        for (int i = 0; i < balances.size(); i++) total += Long.parseLong(balances.getCompound(i).getString("Amount"));
        return total;
    }

    private static void sendRefund(ServerPlayer player, BlockPos target) {
        IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "player" -> player;
                    case "enqueueWork" -> {
                        ((Runnable) args[0]).run();
                        yield CompletableFuture.completedFuture(null);
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        new RefundPendingPatternsPacket(target).handle(context);
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 100)
    public static void oversizedSingleCraftIsRejectedBeforeQueueAdmission(GameTestHelper helper) {
        setupUpgradedMachine(helper);
        helper.startSequence().thenExecuteAfter(1, () -> {
            installProviderAndCell(helper);
            machine(helper).getPatternSlots().getFirst().setStack(PatternDetailsHelper.encodeProcessingPattern(
                    List.of(new GenericStack(ORE, 100_000)),
                    List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1))));
        }).thenExecuteAfter(40, () -> {
            var machine = machine(helper);
            var support = machine.getRecipeAeSupport();
            helper.assertTrue(machine.getMainNode().isActive(), "network is inactive");
            var details = machine.getAvailablePatterns().getFirst();
            helper.assertTrue(!support.pushPatternWithAdapter(details, counters(100_000)),
                    "oversized single craft was accepted through pushPatternWithAdapter");
            helper.assertTrue(!support.enqueueSmartPattern(details, counters(100_000)),
                    "oversized single craft was accepted through reflected enqueueSmartPattern");
            helper.assertTrue(!support.isPatternBusy(), "rejected request left a busy queue");
            helper.assertTrue(support.isSmartPatternMultiplicationEnabled(), "capacity rejection disabled multiplication");
        }).thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 100)
    public static void bothAdmissionPathsCheckOneCraftInsteadOfWholeBatch(GameTestHelper helper) {
        setupUpgradedMachine(helper);
        helper.startSequence().thenExecuteAfter(1, () -> installProviderAndCell(helper))
                .thenExecuteAfter(40, () -> {
                    var machine = machine(helper);
                    var support = machine.getRecipeAeSupport();
                    helper.assertTrue(machine.getMainNode().isActive(), "network is inactive");
                    var details = machine.getAvailablePatterns().getFirst();
                    helper.assertTrue(support.enqueueSmartPattern(details, counters(1_000)),
                            "reflected enqueue checked the full batch against physical capacity");
                    helper.assertTrue(support.pushPatternWithAdapter(details, counters(1_000)),
                            "adapter push checked the full batch against physical capacity");
                    support.requestPendingPatternRefund();
                    helper.assertTrue(refundBalance(helper) == 2_000, "admitted batch quantities were changed");
                }).thenSucceed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void legacyUnexecutableQueueRefundSurvivesReload(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var iron = AEItemKey.of(Items.IRON_INGOT);
        CompoundTag entry = new CompoundTag();
        entry.putLong("Remaining", 3);
        entry.put("Definition", GenericStack.writeTag(registries, new GenericStack(iron, 1)));
        CompoundTag input = new CompoundTag();
        input.put("Input", GenericStack.writeTag(registries, new GenericStack(iron, 100_000)));
        ListTag inputs = new ListTag();
        inputs.add(input);
        entry.put("Inputs", inputs);
        ListTag pending = new ListTag();
        pending.add(entry);
        CompoundTag legacy = new CompoundTag();
        legacy.put("SmartPatternMultiplicationPending", pending);
        var queue = new MeSmartPatternMultiplication();
        queue.loadPending(legacy, registries);
        helper.assertTrue(queue.hasPendingWork(), "legacy request was lost");
        helper.assertTrue(queue.requestRefund(), "legacy request cannot be recovered");
        helper.assertTrue(!queue.requestRefund(), "repeated request duplicated balance");
        helper.assertTrue(!queue.refundPending(stack -> 0), "offline network changed balance");
        helper.assertTrue(queue.refundPending(stack -> 17), "partial insert was not recorded");
        CompoundTag saved = new CompoundTag();
        queue.savePending(saved, registries);
        var restored = new MeSmartPatternMultiplication();
        restored.loadPending(saved, registries);
        long[] received = {17};
        helper.assertTrue(restored.refundPending(stack -> {
            received[0] += stack.amount();
            return stack.amount();
        }), "remaining balance was not returned");
        helper.assertTrue(received[0] == 300_000, "refund lost or duplicated inputs");
        helper.assertTrue(!restored.hasPendingWork(), "refund did not release busy state");
        helper.succeed();
    }
}
