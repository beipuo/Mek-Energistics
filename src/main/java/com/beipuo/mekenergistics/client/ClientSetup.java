package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.client.screen.MeElectricMachineScreen;
import com.beipuo.mekenergistics.client.screen.MeGenericMachineScreen;
import com.beipuo.mekenergistics.blockentity.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeAntiprotonicNucleosynthesizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalCrystallizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalDissolutionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalOxidizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalWasherBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeDigitalMinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeDimensionalStabilizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectricPumpBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeFluidicPlenisherBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeLogisticalSorterBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeModificationStationBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeOredictionificatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeResistiveHeaterBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeTeleporterBlockEntity;
import com.beipuo.mekenergistics.menu.MePatternMachineContainer;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import mekanism.client.gui.GuiDimensionalStabilizer;
import mekanism.client.gui.GuiLogisticalSorter;
import mekanism.client.gui.GuiModificationStation;
import mekanism.client.gui.GuiTeleporter;
import mekanism.client.gui.machine.GuiAdvancedElectricMachine;
import mekanism.client.gui.machine.GuiAntiprotonicNucleosynthesizer;
import mekanism.client.gui.machine.GuiChemicalCrystallizer;
import mekanism.client.gui.machine.GuiChemicalDissolutionChamber;
import mekanism.client.gui.machine.GuiChemicalInfuser;
import mekanism.client.gui.machine.GuiChemicalOxidizer;
import mekanism.client.gui.machine.GuiChemicalWasher;
import mekanism.client.gui.machine.GuiCombiner;
import mekanism.client.gui.machine.GuiDigitalMiner;
import mekanism.client.gui.machine.GuiElectricPump;
import mekanism.client.gui.machine.GuiElectrolyticSeparator;
import mekanism.client.gui.machine.GuiFactory;
import mekanism.client.gui.machine.GuiFluidicPlenisher;
import mekanism.client.gui.machine.GuiFormulaicAssemblicator;
import mekanism.client.gui.machine.GuiIsotopicCentrifuge;
import mekanism.client.gui.machine.GuiMetallurgicInfuser;
import mekanism.client.gui.machine.GuiNutritionalLiquifier;
import mekanism.client.gui.machine.GuiOredictionificator;
import mekanism.client.gui.machine.GuiPaintingMachine;
import mekanism.client.gui.machine.GuiPigmentExtractor;
import mekanism.client.gui.machine.GuiPigmentMixer;
import mekanism.client.gui.machine.GuiPrecisionSawmill;
import mekanism.client.gui.machine.GuiPRC;
import mekanism.client.gui.machine.GuiResistiveHeater;
import mekanism.client.gui.machine.GuiRotaryCondensentrator;
import mekanism.client.gui.machine.GuiSeismicVibrator;
import mekanism.client.gui.machine.GuiSolarNeutronActivator;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.tile.TileEntityModificationStation;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.machine.TileEntityCombiner;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import mekanism.common.tile.machine.TileEntityElectricPump;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import mekanism.common.tile.machine.TileEntityFluidicPlenisher;
import mekanism.common.tile.machine.TileEntityAntiprotonicNucleosynthesizer;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.machine.TileEntityChemicalInfuser;
import mekanism.common.tile.machine.TileEntityChemicalOxidizer;
import mekanism.common.tile.machine.TileEntityChemicalWasher;
import mekanism.common.tile.machine.TileEntityIsotopicCentrifuge;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import mekanism.common.tile.machine.TileEntityOredictionificator;
import mekanism.common.tile.machine.TileEntityPaintingMachine;
import mekanism.common.tile.machine.TileEntityPigmentExtractor;
import mekanism.common.tile.machine.TileEntityPigmentMixer;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.tile.machine.TileEntityPressurizedReactionChamber;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import mekanism.common.tile.machine.TileEntitySeismicVibrator;
import mekanism.common.tile.machine.TileEntitySolarNeutronActivator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;

@EventBusSubscriber(modid = MekEnergistics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ME_ELECTRIC_MACHINE.get(), MeElectricMachineScreen::new);
        event.register(ModMenuTypes.ME_GENERIC_MACHINE.get(), MeGenericMachineScreen::new);
        event.register(ModMenuTypes.ME_ADVANCED_ELECTRIC_MACHINE.get(),
                (MePatternMachineContainer<MeAdvancedElectricMachineBlockEntity> menu, net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.chat.Component title) ->
                        new GuiAdvancedElectricMachine<>(menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_METALLURGIC_INFUSER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiMetallurgicInfuser((MekanismTileContainer<TileEntityMetallurgicInfuser>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_COMBINER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiCombiner((MekanismTileContainer<TileEntityCombiner>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PRECISION_SAWMILL.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiPrecisionSawmill((MekanismTileContainer<TileEntityPrecisionSawmill>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ELECTRIC_PUMP.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiElectricPump((MekanismTileContainer<TileEntityElectricPump>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_FLUIDIC_PLENISHER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiFluidicPlenisher((MekanismTileContainer<TileEntityFluidicPlenisher>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_RESISTIVE_HEATER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiResistiveHeater((MekanismTileContainer<TileEntityResistiveHeater>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_SEISMIC_VIBRATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiSeismicVibrator((MekanismTileContainer<TileEntitySeismicVibrator>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_TELEPORTER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiTeleporter((MekanismTileContainer<TileEntityTeleporter>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_OREDICTIONIFICATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiOredictionificator((MekanismTileContainer<TileEntityOredictionificator>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_MODIFICATION_STATION.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiModificationStation((MekanismTileContainer<TileEntityModificationStation>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_DIGITAL_MINER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiDigitalMiner((MekanismTileContainer<TileEntityDigitalMiner>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_LOGISTICAL_SORTER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiLogisticalSorter((MekanismTileContainer<TileEntityLogisticalSorter>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_DIMENSIONAL_STABILIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiDimensionalStabilizer((MekanismTileContainer<TileEntityDimensionalStabilizer>) (MekanismTileContainer<?>) menu, inv, title));
        event.register(ModMenuTypes.ME_FORMULAIC_ASSEMBLICATOR.get(), GuiFormulaicAssemblicator::new);
        event.register((MenuType) ModMenuTypes.ME_PRESSURIZED_REACTION_CHAMBER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiPRC((MekanismTileContainer<TileEntityPressurizedReactionChamber>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_CRYSTALLIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiChemicalCrystallizer((MekanismTileContainer<TileEntityChemicalCrystallizer>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_DISSOLUTION_CHAMBER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiChemicalDissolutionChamber((MekanismTileContainer<TileEntityChemicalDissolutionChamber>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_INFUSER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiChemicalInfuser((MekanismTileContainer<TileEntityChemicalInfuser>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_OXIDIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiChemicalOxidizer((MekanismTileContainer<TileEntityChemicalOxidizer>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_WASHER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiChemicalWasher((MekanismTileContainer<TileEntityChemicalWasher>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ROTARY_CONDENSENTRATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiRotaryCondensentrator((MekanismTileContainer<TileEntityRotaryCondensentrator>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ELECTROLYTIC_SEPARATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiElectrolyticSeparator((MekanismTileContainer<TileEntityElectrolyticSeparator>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_SOLAR_NEUTRON_ACTIVATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiSolarNeutronActivator((MekanismTileContainer<TileEntitySolarNeutronActivator>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ISOTOPIC_CENTRIFUGE.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiIsotopicCentrifuge((MekanismTileContainer<TileEntityIsotopicCentrifuge>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_NUTRITIONAL_LIQUIFIER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiNutritionalLiquifier((MekanismTileContainer<TileEntityNutritionalLiquifier>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiAntiprotonicNucleosynthesizer((MekanismTileContainer<TileEntityAntiprotonicNucleosynthesizer>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PIGMENT_EXTRACTOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiPigmentExtractor((MekanismTileContainer<TileEntityPigmentExtractor>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PIGMENT_MIXER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiPigmentMixer((MekanismTileContainer<TileEntityPigmentMixer>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PAINTING_MACHINE.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiPaintingMachine((MekanismTileContainer<TileEntityPaintingMachine>) (MekanismTileContainer<?>) menu, inv, title));
        event.register(ModMenuTypes.ME_FACTORY.get(), GuiFactory::new);
    }
}
