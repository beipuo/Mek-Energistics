package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import net.neoforged.fml.ModList;

final class AppliedFluxEnergyBridge {
    private static final String MODID = "appflux";

    private AppliedFluxEnergyBridge() {
    }

    static long extractFe(IGrid grid, long requestedFe, Actionable action, IActionSource source) {
        if (requestedFe <= 0 || grid == null || source == null || !ModList.get().isLoaded(MODID)) {
            return 0;
        }
        IStorageService storageService = grid.getService(IStorageService.class);
        if (storageService == null) {
            return 0;
        }
        return storageService.getInventory().extract(FluxKey.of(EnergyType.FE), requestedFe, action, source);
    }
}
