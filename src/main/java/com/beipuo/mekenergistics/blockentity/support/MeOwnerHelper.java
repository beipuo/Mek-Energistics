package com.beipuo.mekenergistics.blockentity.support;

import java.util.UUID;
import mekanism.common.network.to_client.security.PacketSyncSecurity;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MeOwnerHelper {
    private MeOwnerHelper() {
    }

    /**
     * Whether a player who just upgraded/converted a machine may become its ME owner. A player who
     * is merely trusted with (or can access) someone else's machine must not take over the
     * owner's AE identity just by applying an installer.
     */
    public static boolean mayClaimOwnership(TileEntityMekanism tile, ServerPlayer player) {
        UUID owner = tile.hasSecurity() ? tile.getOwnerUUID() : null;
        return owner == null || owner.equals(player.getUUID());
    }

    public static void claimMekanismOwnerIfMissing(TileEntityMekanism tile, ServerPlayer player) {
        if (tile.hasSecurity() && tile.getOwnerUUID() == null) {
            tile.setOwnerUUID(player.getUUID());
            PacketDistributor.sendToAllPlayers(new PacketSyncSecurity(player.getUUID()));
        }
    }
}
