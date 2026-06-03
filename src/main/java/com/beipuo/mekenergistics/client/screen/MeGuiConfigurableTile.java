package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.client.screen.element.MeGuiSideConfigurationTab;
import java.util.function.Supplier;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.tab.window.GuiTransporterConfigTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class MeGuiConfigurableTile<TILE extends TileEntityMekanism & ISideConfiguration & MeAeMachine, CONTAINER extends MekanismTileContainer<TILE>>
        extends GuiMekanismTile<TILE, CONTAINER> {
    private MeGuiSideConfigurationTab<TILE> sideConfigTab;
    private GuiTransporterConfigTab<TILE> transporterConfigTab;

    protected MeGuiConfigurableTile(CONTAINER container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        Supplier<MeGuiSideConfigurationTab<TILE>> sideConfigSupplier = () -> this.sideConfigTab;
        this.sideConfigTab = addRenderableWidget(new MeGuiSideConfigurationTab<>(this, this.tile, sideConfigSupplier));
        this.transporterConfigTab = addRenderableWidget(new GuiTransporterConfigTab<>(this, this.tile, () -> this.transporterConfigTab));
    }
}
