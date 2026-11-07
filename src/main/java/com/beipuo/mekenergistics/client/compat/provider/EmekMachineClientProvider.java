package com.beipuo.mekenergistics.client.compat.provider;

import com.beipuo.mekenergistics.client.compat.eme.EvolvedMekanismMachineClientScreens;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineFamily;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client screens for the Evolved Mekanism machine family. These menus are registered whenever
 * Evolved Mekanism is present, so their screens must not depend on Evolved Mekanism Extras being
 * installed as well. Evolved Mekanism factories reuse the core {@code ME_FACTORY} menu, which
 * {@code ClientSetup} already binds.
 */
public final class EmekMachineClientProvider implements CompatMachineClientProvider {
    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        if (CompatMachineCatalog.hasAvailableFamily(CompatMachineFamily.EMEK_MACHINE)) {
            EvolvedMekanismMachineClientScreens.register(event);
        }
    }
}
