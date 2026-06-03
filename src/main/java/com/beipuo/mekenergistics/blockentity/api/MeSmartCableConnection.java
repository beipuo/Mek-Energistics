package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.util.AECableType;
import net.minecraft.core.Direction;

public interface MeSmartCableConnection extends IInWorldGridNodeHost {
    @Override
    default AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }
}
