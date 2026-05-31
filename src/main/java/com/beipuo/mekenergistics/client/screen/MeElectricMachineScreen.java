package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeElectricMachineBlockEntity;
import mekanism.client.gui.element.GuiUpArrow;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MeElectricMachineScreen extends MeMekanismMachineScreen<MeElectricMachineBlockEntity> {
    public MeElectricMachineScreen(MekanismTileContainer<MeElectricMachineBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiUpArrow(this, 68, 38));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 16));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), () -> 0L));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.BAR, this, 86, 38));
    }
}
