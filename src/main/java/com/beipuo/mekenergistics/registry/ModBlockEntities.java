package com.beipuo.mekenergistics.registry;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.provider.CompatMachineProviders;
import com.beipuo.mekenergistics.registry.machine.MachineFactory;
import com.beipuo.mekenergistics.upgrade.MeUpgradeSupportRegistrar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModBlockEntities {
    private static final TileEntityTypeDeferredRegister BLOCK_ENTITIES =
            new TileEntityTypeDeferredRegister(MekEnergistics.MODID);
    private static final Map<MeMekanismMachine, TileEntityTypeRegistryObject<? extends TileEntityMekanism>> MACHINES =
            new LinkedHashMap<>();

    static {
        CompatMachineCatalog.available().forEach(spec -> MACHINES.put(
                spec.machine(),
                CompatMachineProviders.get(spec.provider())
                        .registerBlockEntity(spec, ModBlockEntities::registerMachine)));
    }

    public static <TILE extends TileEntityMekanism> TileEntityTypeRegistryObject<TILE> registerMachine(
            MeMekanismMachine machine, MachineFactory<TILE> factory) {
        return BLOCK_ENTITIES.mekBuilder(
                        ModBlocks.getMachineBlock(machine),
                        (pos, state) -> factory.create(machine, pos, state))
                .serverTicker((level, pos, state, tile) -> TileEntityMekanism.tickServer(level, pos, state, tile))
                .clientTicker((level, pos, state, tile) -> TileEntityMekanism.tickClient(level, pos, state, tile))
                .withSimple(Capabilities.CONFIG_CARD)
                .build();
    }

    public static final TileEntityTypeRegistryObject<? extends TileEntityMekanism> ME_METALLURGIC_INFUSER =
            getMachineBlockEntity(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModBlockEntities() {
    }

    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> getMachineBlockEntity(
            MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    /**
     * Identity set of every ME machine block entity type, built on first use (after registration).
     * Capability lookups on bounding blocks call {@link #isMachineBlockEntity} frequently; a linear
     * scan over every registered machine type per call was wasted work.
     */
    private static volatile java.util.Set<BlockEntityType<?>> machineTypes;

    public static boolean isMachineBlockEntity(BlockEntityType<?> type) {
        java.util.Set<BlockEntityType<?>> types = machineTypes;
        if (types == null) {
            java.util.Set<BlockEntityType<?>> built =
                    java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder : MACHINES.values()) {
                built.add(holder.get());
            }
            machineTypes = types = java.util.Collections.unmodifiableSet(built);
        }
        return types.contains(type);
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(ModBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // NeoForge AE2 resolves cable hosts only via AECapabilities.IN_WORLD_GRID_NODE_HOST
        // (GridHelper.getNodeHost). Mixin-implemented IInWorldGridNodeHost is not enough.
        List<Block> upgradeTargetList = new ArrayList<>();
        CompatMachineCatalog.available()
                .map(spec -> BuiltInRegistries.BLOCK.get(spec.sourceBlockId()))
                .filter(block -> block != null && block != net.minecraft.world.level.block.Blocks.AIR)
                .forEach(upgradeTargetList::add);
        upgradeTargetList.addAll(MeUpgradeSupportRegistrar.externalPatternUpgradeBlocks());
        Block[] upgradeTargets = upgradeTargetList.toArray(Block[]::new);
        if (upgradeTargets.length > 0) {
            event.registerBlock(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    (level, pos, state, blockEntity, context) ->
                            blockEntity instanceof MeUpgradeableMachine machine
                                    && machine.isMeUpgradeActive() ? machine : null,
                    upgradeTargets);
        }
        CompatMachineCatalog.available().forEach(spec -> CompatMachineProviders.get(spec.provider())
                .registerGridNodeHost(spec, event, MACHINES.get(spec.machine())));
        if (CompatMachineCatalog.available().anyMatch(spec -> spec.machine().isMekmmLargeMachine())) {
            event.registerBlock(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    (level, pos, state, blockEntity, context) -> {
                        if (!(blockEntity instanceof TileEntityBoundingBlock bounding)) {
                            return null;
                        }
                        if (bounding.getMainTile(pos) instanceof MeAeMachine machine
                                && (!(machine instanceof MeUpgradeableMachine upgradeable)
                                        || isMachineBlockEntity(machine.getAeOwnerTile().getType())
                                        || upgradeable.isMeUpgradeTarget())
                                && machine.getMachine().isMekmmLargeMachine()) {
                            // Callers may hold on to the host, and the position they hand us is often a
                            // shared mutable one that the caller re-targets between sides.
                            BlockPos nodePos = pos.immutable();
                            return new IInWorldGridNodeHost() {
                                @Override
                                public IGridNode getGridNode(Direction side) {
                                    return machine.getRecipeAeSupport().getLargeMachineGridNode(nodePos, side);
                                }

                                @Override
                                public AECableType getCableConnectionType(Direction side) {
                                    IGridNode node = getGridNode(side);
                                    return node == null ? AECableType.NONE : machine.getCableConnectionType(side);
                                }
                            };
                        }
                        return null;
                    }, MekanismBlocks.BOUNDING_BLOCK.value());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder,
            Class<? extends IInWorldGridNodeHost> tileClass) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                (BlockEntityType) holder.get(),
                (blockEntity, context) -> tileClass.isInstance(blockEntity)
                        ? tileClass.cast(blockEntity) : null);
    }
}
