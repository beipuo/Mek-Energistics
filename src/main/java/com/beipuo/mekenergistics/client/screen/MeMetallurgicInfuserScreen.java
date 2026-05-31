package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeMetallurgicInfuserBlockEntity;
import mekanism.client.gui.element.GuiDumpButton;
import mekanism.client.gui.element.bar.GuiChemicalBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MeMetallurgicInfuserScreen extends MeMekanismMachineScreen<MeMetallurgicInfuserBlockEntity> {
    public MeMetallurgicInfuserScreen(MekanismTileContainer<MeMetallurgicInfuserBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 15));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), () -> 0L));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.RIGHT, this, 72, 47));
        if (tile.getChemicalTank() != null) {
            addRenderableWidget(new GuiChemicalBar(this, GuiChemicalBar.getProvider(tile.getChemicalTank(), tile.getChemicalTanks(null)), 7, 15, 4, 52, false));
            addRenderableWidget(new GuiDumpButton<>(this, tile, 16, 59));
        }
    }
}
