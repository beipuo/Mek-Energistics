package com.beipuo.mekenergistics.blockentity;

import appeng.api.networking.IManagedGridNode;
import mekanism.common.network.to_client.security.PacketSyncSecurity;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MeOwnerHelper {
    private MeOwnerHelper() {
    }

    public static void setOwner(TileEntityMekanism tile, IManagedGridNode node, ServerPlayer player) {
        node.setOwningPlayer(player);
        claimMekanismOwnerIfMissing(tile, player);
    }

    public static void claimMekanismOwnerIfMissing(TileEntityMekanism tile, ServerPlayer player) {
        if (tile.hasSecurity() && tile.getOwnerUUID() == null) {
            tile.setOwnerUUID(player.getUUID());
            PacketDistributor.sendToAllPlayers(new PacketSyncSecurity(player.getUUID()));
        }
    }
}
