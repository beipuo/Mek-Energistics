package com.beipuo.mekenergistics.gametest;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.item.MeInstallerUpgradeHandler;
import com.beipuo.mekenergistics.blockentity.support.MeNetworkEnergyHelper;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MeUpgradeType;
import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.api.recipes.cache.CachedRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MekEnergistics.MODID)
@PrefixGameTestTemplate(false)
public final class MeUpgradeRegressionGameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos POWER = new BlockPos(1, 1, 0);

    @GameTest(template = "empty_3x3x3", timeoutTicks = 120)
    public static void nativeTierInstallersPreservePatterns(GameTestHelper helper) {
        tierUpgrade(helper, false);
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 120)
    public static void meTierInstallersPreservePatterns(GameTestHelper helper) {
        tierUpgrade(helper, true);
    }

    private static void tierUpgrade(GameTestHelper helper, boolean nativeMe) {
        place(helper, nativeMe ? "mekenergistics:me_basic_enriching_factory" : "mekanism:basic_enriching_factory");
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        ItemStack pattern = pattern(Items.IRON_ORE, Items.IRON_INGOT);
        helper.startSequence().thenExecuteAfter(1, () -> {
            if (!nativeMe) install(helper);
            var slots = machine(helper).getPatternSlots();
            slots.getFirst().setStack(pattern.copy());
            slots.getLast().setStack(pattern(Items.GOLD_ORE, Items.GOLD_INGOT));
            machine(helper).getRecipeAeSupport().setSmartPatternMultiplicationEnabled(false);
        }).thenExecuteAfter(40, () -> {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            for (String tier : List.of("advanced", "elite", "ultimate")) {
                ItemStack installer = new ItemStack(item("mekanism:" + tier + "_tier_installer"));
                var pos = helper.absolutePos(MACHINE);
                boolean upgraded = nativeMe
                        ? MeInstallerUpgradeHandler.tryUpgrade(installer, helper.getBlockState(MACHINE),
                                helper.getLevel(), pos, player).consumesAction()
                        : installer.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                                installer, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))).consumesAction();
                helper.assertTrue(upgraded, tier + " installer failed");
                helper.assertTrue(installer.isEmpty(), "installer was not consumed exactly once");
                var expected = ResourceLocation.parse((nativeMe ? "mekenergistics:me_" : "mekanism:") + tier + "_enriching_factory");
                helper.assertTrue(BuiltInRegistries.BLOCK.getKey(helper.getBlockState(MACHINE).getBlock()).equals(expected),
                        "wrong tier after installer: " + expected);
                assertPatterns(helper, pattern);
            }
        }).thenExecuteAfter(40, () -> {
            assertPatterns(helper, pattern);
            helper.assertTrue(machine(helper).getMainNode().isActive(), "upgraded factory did not reconnect");
            helper.assertTrue(machine(helper).getAvailablePatterns().size() == 2, "upgraded patterns were not published");
            helper.assertTrue(!machine(helper).isSmartPatternMultiplicationEnabled(), "ME configuration was reset");
        }).thenSucceed();
    }

    private static void assertPatterns(GameTestHelper helper, ItemStack pattern) {
        var slots = machine(helper).getPatternSlots();
        helper.assertTrue(ItemStack.matches(slots.getFirst().getStack(), pattern), "first pattern was lost or changed");
        helper.assertTrue(ItemStack.matches(slots.getLast().getStack(), pattern(Items.GOLD_ORE, Items.GOLD_INGOT)),
                "last pattern was lost or reordered");
        helper.assertTrue(slots.stream().filter(slot -> !slot.isEmpty()).count() == 2, "patterns were duplicated");
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 400)
    public static void upgradedInfuserAcceptsTwoItems(GameTestHelper helper) {
        infuse(helper, "mekanism:metallurgic_infuser", true);
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 400)
    public static void meInfuserAcceptsTwoItems(GameTestHelper helper) {
        infuse(helper, "mekenergistics:me_metallurgic_infuser", false);
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 400)
    public static void upgradedInfusingFactoryAcceptsTwoItems(GameTestHelper helper) {
        infuse(helper, "mekanism:basic_infusing_factory", true);
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 400)
    public static void meInfusingFactoryAcceptsTwoItems(GameTestHelper helper) {
        infuse(helper, "mekenergistics:me_basic_infusing_factory", false);
    }

    private static void infuse(GameTestHelper helper, String block, boolean upgraded) {
        place(helper, block);
        helper.setBlock(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block());
        Item osmium = item("mekanism:ingot_osmium");
        Item circuit = item("mekanism:basic_control_circuit");
        helper.startSequence().thenExecuteAfter(1, () -> {
            if (upgraded) install(helper);
            machine(helper).getRecipeAeSupport().setSmartPatternMultiplicationEnabled(false);
            machine(helper).getPatternSlots().getFirst().setStack(PatternDetailsHelper.encodeProcessingPattern(
                    List.of(new GenericStack(AEItemKey.of(osmium), 1), new GenericStack(AEItemKey.of(Items.REDSTONE), 2)),
                    List.of(new GenericStack(AEItemKey.of(circuit), 1))));
        }).thenExecuteAfter(40, () -> {
            MeAeMachine machine = machine(helper);
            helper.assertTrue(machine.getAvailablePatterns().size() == 1, block + " did not publish pattern");
            KeyCounter main = new KeyCounter();
            main.add(AEItemKey.of(osmium), 1);
            KeyCounter extra = new KeyCounter();
            extra.add(AEItemKey.of(Items.REDSTONE), 2);
            helper.assertTrue(machine.pushPattern(machine.getAvailablePatterns().getFirst(), new KeyCounter[]{main, extra}),
                    block + " rejected item + item inputs");
        }).thenExecuteAfter(250, () -> {
            long produced = tile(helper).getInventorySlots(null).stream().filter(slot -> slot.getStack().is(circuit))
                    .mapToLong(slot -> slot.getCount()).sum();
            helper.assertTrue(produced == 1, block + " did not produce exactly one circuit: " + produced);
        }).thenSucceed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 700)
    public static void installingProviderDoesNotContinuouslyDrainIdleEnergy(GameTestHelper helper) {
        energy(helper, false);
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 700)
    public static void upgradingFactoryDoesNotContinuouslyDrainIdleEnergy(GameTestHelper helper) {
        energy(helper, true);
    }

    private static void energy(GameTestHelper helper, boolean tierUpgrade) {
        place(helper, "mekanism:basic_smelting_factory");
        helper.setBlock(POWER, AEBlocks.DENSE_ENERGY_CELL.block());
        double[] before = new double[1];
        CachedRecipe<?>[] originalRecipe = new CachedRecipe<?>[1];
        helper.startSequence().thenExecuteAfter(1, () -> {
            cell(helper).injectAEPower(1_000_000, Actionable.MODULATE);
            // Create a paused recipe BEFORE installing the provider.
            var slot = tile(helper).getInventorySlots(null).stream()
                    .filter(s -> s instanceof mekanism.common.inventory.slot.FactoryInputInventorySlot).findFirst().orElseThrow();
            slot.setStack(new ItemStack(Items.IRON_ORE));
        }).thenExecuteAfter(10, () -> {
                    originalRecipe[0] = cachedRecipe(helper);
                    helper.assertTrue(originalRecipe[0] != null, "test did not create a recipe before installation");
                    install(helper);
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(cachedRecipe(helper) == originalRecipe[0], "upgrade unexpectedly replaced the cached recipe");
                    assertLiveEnergySwitch(helper, originalRecipe[0]);
                    if (tierUpgrade) {
                        ItemStack installer = new ItemStack(item("mekanism:advanced_tier_installer"));
                        var player = helper.makeMockPlayer(GameType.SURVIVAL);
                        var pos = helper.absolutePos(MACHINE);
                        helper.assertTrue(installer.getItem().useOn(new UseOnContext(helper.getLevel(), player,
                                InteractionHand.MAIN_HAND, installer,
                                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))).consumesAction(),
                                "factory tier upgrade failed");
                    }
                }).thenExecuteAfter(250, () -> {
                    long produced = tile(helper).getInventorySlots(null).stream().filter(s -> s.getStack().is(Items.IRON_INGOT))
                            .mapToLong(s -> s.getCount()).sum();
                    helper.assertTrue(produced == 1, "recipe did not finish without replacing the machine: " + produced + ", progress=" + ((TileEntityFactory<?>) tile(helper)).getProgress(0) + ", state=" + energyState(helper));
                    for (IEnergyContainer energy : tile(helper).getEnergyContainers(null)) {
                        helper.assertTrue(energy.getEnergy() == energy.getMaxEnergy(), "idle local buffer is not full");
                    }
                    before[0] = cell(helper).getAECurrentPower();
                }).thenExecuteAfter(200, () -> {
                    double spent = before[0] - cell(helper).getAECurrentPower();
                    helper.assertTrue(spent >= 0 && spent < 1_000, "idle factory continuously drained AE power: " + spent);
                    // Repeated refill in the same tick must not debit a full buffer at all.
                    double stored = cell(helper).getAECurrentPower();
                    for (int i = 0; i < 100; i++) machine(helper).getRecipeAeSupport().refillLocalEnergyBuffers();
                    helper.assertTrue(cell(helper).getAECurrentPower() == stored, "full buffer was charged twice");
                    for (var slot : tile(helper).getInventorySlots(null)) {
                        if (slot instanceof mekanism.common.inventory.slot.OutputInventorySlot) {
                            slot.setStack(new ItemStack(Items.IRON_INGOT, 64));
                        }
                    }
                    tile(helper).getInventorySlots(null).stream()
                            .filter(slot -> slot instanceof mekanism.common.inventory.slot.FactoryInputInventorySlot)
                            .findFirst().orElseThrow().setStack(new ItemStack(Items.IRON_ORE));
                    before[0] = stored;
                }).thenExecuteAfter(100, () -> {
                    double spent = before[0] - cell(helper).getAECurrentPower();
                    helper.assertTrue(spent >= 0 && spent < 500, "blocked output drained processing energy: " + spent);
                }).thenSucceed();
    }

    private static String energyState(GameTestHelper helper) {
        var factory = (TileEntityFactory<?>) tile(helper);
        var recipe = cachedRecipe(helper);
        try {
            var paused = CachedRecipe.class.getDeclaredField("pausedForErrors");
            var errors = CachedRecipe.class.getDeclaredField("errors");
            paused.setAccessible(true);
            errors.setAccessible(true);
            return "cell=" + cell(helper).getAECurrentPower() + ", local=" + factory.getEnergyContainer().getEnergy()
                    + ", required=" + factory.getTicksRequired() + ", progress=" + factory.getProgress(0)
                    + ", active=" + machine(helper).getMainNode().isActive() + ", paused=" + paused.get(recipe)
                    + ", errors=" + errors.get(recipe);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertLiveEnergySwitch(GameTestHelper helper, CachedRecipe<?> recipe) {
        var factory = (TileEntityFactory<?>) tile(helper);
        var upgrades = ((MeUpgradeStateOwner) factory).getMeUpgradeContainer();
        factory.getEnergyContainer().setEnergy(0);
        int progress = factory.getProgress(0);
        double stored = cell(helper).getAECurrentPower();
        for (int i = 0; i < 100; i++) {
            MeNetworkEnergyHelper.availableWithLocalBuffer(factory.getEnergyContainer(), machine(helper).getGrid(),
                    IActionSource.ofMachine(machine(helper)));
        }
        helper.assertTrue(cell(helper).getAECurrentPower() == stored, "energy simulation consumed AE power");
        recipe.unpauseErrors();
        recipe.process();
        helper.assertTrue(factory.getProgress(0) > progress, "pre-install recipe did not switch to AE power");
        helper.assertTrue(cell(helper).getAECurrentPower() < stored, "processing did not debit AE power");
        helper.assertTrue(factory.getEnergyContainer().getEnergy() == 0, "direct AE extraction modified local storage");

        helper.assertTrue(upgrades.uninstall(MeUpgradeType.PATTERN_PROVIDER), "could not remove provider");
        stored = cell(helper).getAECurrentPower();
        progress = factory.getProgress(0);
        recipe.process();
        helper.assertTrue(cell(helper).getAECurrentPower() == stored, "cached recipe consumed AE power after removal");
        helper.assertTrue(factory.getProgress(0) == progress, "unpowered recipe progressed after removal");
        install(helper);
    }

    private static CachedRecipe<?> cachedRecipe(GameTestHelper helper) {
        try {
            var field = TileEntityFactory.class.getDeclaredField("recipeCacheLookupMonitors");
            field.setAccessible(true);
            var monitors = (FactoryRecipeCacheLookupMonitor<?>[]) field.get(tile(helper));
            return monitors[0].getCachedRecipe(0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect factory recipe for the regression test", exception);
        }
    }

    private static void place(GameTestHelper helper, String id) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR, "missing test block " + id);
        helper.setBlock(MACHINE, block);
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalArgumentException("missing test item " + id);
        return item;
    }

    private static void install(GameTestHelper helper) {
        helper.assertTrue(((MeUpgradeStateOwner) tile(helper)).getMeUpgradeContainer()
                .install(MeUpgradeType.PATTERN_PROVIDER).successful(), "provider installation failed");
    }

    private static ItemStack pattern(Item input, Item output) {
        return PatternDetailsHelper.encodeProcessingPattern(List.of(new GenericStack(AEItemKey.of(input), 1)),
                List.of(new GenericStack(AEItemKey.of(output), 1)));
    }

    private static TileEntityMekanism tile(GameTestHelper helper) {
        return (TileEntityMekanism) helper.getBlockEntity(MACHINE);
    }

    private static MeAeMachine machine(GameTestHelper helper) {
        return (MeAeMachine) tile(helper);
    }

    private static EnergyCellBlockEntity cell(GameTestHelper helper) {
        return (EnergyCellBlockEntity) helper.getBlockEntity(POWER);
    }
}
