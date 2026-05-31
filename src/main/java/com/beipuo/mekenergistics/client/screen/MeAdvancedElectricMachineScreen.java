package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeAdvancedElectricMachineBlockEntity;
import mekanism.client.gui.element.bar.GuiChemicalBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MeAdvancedElectricMachineScreen extends MeMekanismMachineScreen<MeAdvancedElectricMachineBlockEntity> {
    public MeAdvancedElectricMachineScreen(MekanismTileContainer<MeAdvancedElectricMachineBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 16));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), () -> 0L));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.BAR, this, 86, 38));
        if (tile.getChemicalTank() != null) {
            addRenderableWidget(new GuiChemicalBar(this, GuiChemicalBar.getProvider(tile.getChemicalTank(), tile.getChemicalTanks(null)), 68, 36, 6, 12, false));
        }
    }
}
