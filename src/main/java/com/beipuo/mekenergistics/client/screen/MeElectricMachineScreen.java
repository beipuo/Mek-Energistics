package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.network.CycleAeOutputModePacket;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.machine.GuiElectricMachine;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class MeElectricMachineScreen extends GuiElectricMachine<MeElectricMachineBlockEntity, MekanismTileContainer<MeElectricMachineBlockEntity>> {
    private MekanismButton aeOutputModeButton;

    public MeElectricMachineScreen(MekanismTileContainer<MeElectricMachineBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        this.aeOutputModeButton = addRenderableWidget(new MekanismButton(this, 7, 4, 58, 12,
                Component.literal(tile.getAeOutputMode().label()),
                (element, mouseX, mouseY) -> {
                    PacketDistributor.sendToServer(new CycleAeOutputModePacket(tile.getBlockPos()));
                    return true;
                }));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.aeOutputModeButton != null) {
            this.aeOutputModeButton.setMessage(Component.literal(tile.getAeOutputMode().label()));
        }
    }
}
