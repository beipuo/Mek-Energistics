package com.beipuo.mekenergistics.blockentity.support;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.network.chat.Component;

public final class MePatternTerminalNames {
    private MePatternTerminalNames() {
    }

    public static String get(TileEntityMekanism owner, String legacyName) {
        Component customName = owner.getCustomName();
        if (customName != null) {
            return MeAeMachine.sanitizePatternTerminalName(customName.getString());
        }
        return MeAeMachine.sanitizePatternTerminalName(legacyName);
    }

    public static boolean set(TileEntityMekanism owner, String name, String legacyName) {
        String sanitized = MeAeMachine.sanitizePatternTerminalName(name);
        if (get(owner, legacyName).equals(sanitized)) {
            return false;
        }
        owner.setCustomName(sanitized.isBlank() ? null : Component.literal(sanitized));
        owner.setChanged();
        return true;
    }

    public static String migrateLegacy(TileEntityMekanism owner, String legacyName) {
        String sanitized = MeAeMachine.sanitizePatternTerminalName(legacyName);
        if (!sanitized.isEmpty() && owner.getCustomName() == null) {
            owner.setCustomName(Component.literal(sanitized));
        }
        return sanitized;
    }
}
