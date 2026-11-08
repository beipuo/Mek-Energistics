package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.ticking.IGridTickable;
import com.beipuo.mekenergistics.blockentity.api.MePatternIoOwner;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Owns the AE grid-node lifecycle of an {@link AbstractMeAeSupport} owner: the managed main node,
 * the large-machine port nodes, node NBT retention, and deferred crafting-provider updates.
 */
public class MeAeNodeLifecycle<O extends MePatternIoOwner> {
    private static final String TAG_NODE = "node";

    private final O owner;
    private final TileEntityMekanism ownerTile;
    private final Supplier<IGridTickable> tickerFactory;
    private final Runnable rebuildPatternCache;

    private IManagedGridNode mainNode;
    private NodeState nodeState = NodeState.NEW;
    private CompoundTag retainedNodeData;
    private final CraftingUpdateState craftingUpdateState = new CraftingUpdateState();
    private final Map<BlockPos, IManagedGridNode> largeMachinePortNodes = new HashMap<>();

    MeAeNodeLifecycle(O owner, TileEntityMekanism ownerTile, Supplier<IGridTickable> tickerFactory,
            Runnable rebuildPatternCache) {
        this.owner = owner;
        this.ownerTile = ownerTile;
        this.tickerFactory = tickerFactory;
        this.rebuildPatternCache = rebuildPatternCache;
        this.mainNode = createManagedNode();
    }

    private IManagedGridNode createManagedNode() {
        IManagedGridNode node = GridHelper.createManagedNode(this.owner, new NodeListener())
                .setTagName(TAG_NODE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this.owner)
                .addService(IGridTickable.class, this.tickerFactory.get());
        if (this.retainedNodeData != null) {
            node.loadFromNBT(this.retainedNodeData);
        }
        return node;
    }

    public IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    /**
     * @param side ignored — AE2 rejects any node that is not exposed on the side it asked for, and the
     *             node already carries the machine's real exposed faces.
     */
    public IGridNode getLargeMachineGridNode(BlockPos position, Direction side) {
        IManagedGridNode port = this.largeMachinePortNodes.get(position);
        return port == null ? null : port.getNode();
    }

    public void createOnFirstTick() {
        GridHelper.onFirstTick(this.ownerTile, tile -> create());
    }

    void create() {
        if (this.ownerTile.isRemoved() || this.ownerTile.getLevel() == null || this.ownerTile.getLevel().isClientSide()) {
            return;
        }
        create(this.ownerTile.getLevel(), this.owner.getGridNodePosition());
    }

    public void create(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || this.ownerTile.isRemoved()) {
            return;
        }
        if (this.nodeState == NodeState.DESTROYED) {
            this.mainNode = createManagedNode();
            this.nodeState = NodeState.NEW;
        }
        if (this.nodeState == NodeState.NEW) {
            this.mainNode.setInWorldNode(true);
            if (this.owner.getMachine().isMekmmLargeMachine()) {
                createLargeMachineNodes(level, pos);
            } else {
                this.mainNode.create(level, pos);
            }
            this.nodeState = NodeState.ACTIVE;
            this.rebuildPatternCache.run();
        }
    }

    /**
     * Places a node on every block the machine occupies so a cable can attach anywhere along its
     * surface. The controller keeps the main node — which carries the crafting and ticking services —
     * while the bounding blocks get bare nodes wired back to it.
     *
     * <p>Each node is exposed only on the faces that lead out of the machine, so two nodes of the same
     * machine never face each other and AE2's in-world scan cannot duplicate the direct connections
     * made here.
     */
    private void createLargeMachineNodes(Level level, BlockPos controllerPos) {
        this.largeMachinePortNodes.clear();
        MeLargeMachineFootprint footprint =
                MeLargeMachineFootprint.of(level, controllerPos, this.ownerTile.getBlockState());
        this.mainNode.setExposedOnSides(footprint.exposedFaces(controllerPos));
        this.mainNode.create(level, controllerPos);
        IGridNode mainGridNode = this.mainNode.getNode();
        if (mainGridNode == null) {
            return;
        }
        int owningPlayerId = mainGridNode.getOwningPlayerId();
        footprint.forEachExposedPosition((position, exposed) -> {
            if (position.equals(controllerPos)) {
                return;
            }
            IManagedGridNode portNode = GridHelper.createManagedNode(this.owner, new PortNodeListener())
                    .setInWorldNode(true)
                    .setExposedOnSides(exposed)
                    .setIdlePowerUsage(0);
            portNode.setOwningPlayerId(owningPlayerId);
            portNode.create(level, position);
            GridHelper.createConnection(mainGridNode, portNode.getNode());
            this.largeMachinePortNodes.put(position, portNode);
            // The bounding block had no grid node when its capability was last resolved.
            level.invalidateCapabilities(position);
        });
    }

    /** Keeps every node of the machine attributed to the same player for AE2's security checks. */
    public void setOwningPlayer(ServerPlayer player) {
        this.mainNode.setOwningPlayer(player);
        for (IManagedGridNode port : this.largeMachinePortNodes.values()) {
            port.setOwningPlayer(player);
        }
    }

    public void destroyNode() {
        if (this.nodeState == NodeState.DESTROYED) {
            return;
        }
        retainNodeData();
        this.craftingUpdateState.markPending();
        Level level = this.ownerTile.getLevel();
        for (Map.Entry<BlockPos, IManagedGridNode> port : this.largeMachinePortNodes.entrySet()) {
            port.getValue().destroy();
            if (level != null) {
                level.invalidateCapabilities(port.getKey());
            }
        }
        this.largeMachinePortNodes.clear();
        this.mainNode.destroy();
        this.nodeState = NodeState.DESTROYED;
    }

    public IGrid getGrid() {
        IGridNode node = this.mainNode.getNode();
        return node == null || !node.isActive() ? null : node.getGrid();
    }

    public void saveNode(CompoundTag tag) {
        boolean nodeMissing = this.nodeState == NodeState.DESTROYED || this.mainNode.getNode() == null;
        if (nodeMissing && this.retainedNodeData != null) {
            // Before the first tick (or after destruction) AE2 has no live node to serialize. A
            // chunk saved in that window must keep the loaded node data, including the owning
            // player, instead of writing nothing.
            if (this.retainedNodeData.contains(TAG_NODE)) {
                tag.put(TAG_NODE, this.retainedNodeData.getCompound(TAG_NODE).copy());
            }
            return;
        }
        this.mainNode.saveToNBT(tag);
    }

    public void loadNode(CompoundTag tag) {
        this.retainedNodeData = copyNodeData(tag);
        if (this.nodeState == NodeState.DESTROYED) {
            this.mainNode = createManagedNode();
            this.nodeState = NodeState.NEW;
        } else {
            this.mainNode.loadFromNBT(tag);
        }
    }

    private void retainNodeData() {
        if (this.mainNode.getNode() == null && this.retainedNodeData != null) {
            // Never created (destroyed before its first tick): keep what was loaded.
            return;
        }
        CompoundTag tag = new CompoundTag();
        this.mainNode.saveToNBT(tag);
        this.retainedNodeData = copyNodeData(tag);
    }

    private static CompoundTag copyNodeData(CompoundTag source) {
        CompoundTag result = new CompoundTag();
        if (source.contains(TAG_NODE)) {
            result.put(TAG_NODE, source.getCompound(TAG_NODE).copy());
        }
        return result;
    }

    void markCraftingUpdatePending() {
        this.craftingUpdateState.markPending();
    }

    void requestCraftingUpdate() {
        IGridNode node = this.mainNode.getNode();
        this.craftingUpdateState.request(node != null && node.isActive(),
                () -> ICraftingProvider.requestUpdate(this.mainNode));
    }

    void alertAeTicker() {
        this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    enum NodeState {
        NEW,
        ACTIVE,
        DESTROYED
    }

    private final class PortNodeListener implements IGridNodeListener<O> {
        @Override
        public void onSaveChanges(O nodeOwner, IGridNode node) {
            // Port nodes contain no persistent services or state of their own.
        }
    }

    private final class NodeListener implements IGridNodeListener<O> {
        @Override
        public void onSaveChanges(O nodeOwner, IGridNode node) {
            nodeOwner.saveChanges();
        }

        @Override
        public void onStateChanged(O nodeOwner, IGridNode node, State state) {
            if (node.isActive()) {
                craftingUpdateState.flush(() -> ICraftingProvider.requestUpdate(mainNode));
                node.getGrid().getTickManager().alertDevice(node);
            }
        }
    }

    static class CraftingUpdateState {
        private boolean pending;

        void request(boolean nodeActive, Runnable update) {
            if (nodeActive) {
                update.run();
                this.pending = false;
            } else {
                this.pending = true;
            }
        }

        void markPending() {
            this.pending = true;
        }

        void flush(Runnable update) {
            if (this.pending) {
                update.run();
                this.pending = false;
            }
        }

        boolean isPending() {
            return this.pending;
        }
    }
}
