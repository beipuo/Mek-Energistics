package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.client.screen.MeElectricMachineScreen;
import com.beipuo.mekenergistics.client.screen.MeGenericMachineScreen;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiAdvancedElectricMachine;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiChemicalInfuser;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiFactory;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiAdvancedFactory;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraAdvancedFactory;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraFactory;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraMoreMachineFactory;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiMoreMachineFactory;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiMetallurgicInfuser;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiPRC;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiCombiner;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiPrecisionSawmill;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiChemicalCrystallizer;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiChemicalDissolutionChamber;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiChemicalOxidizer;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiChemicalWasher;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiElectrolyticSeparator;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiRotaryCondensentrator;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiIsotopicCentrifuge;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiPaintingMachine;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiPigmentExtractor;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiPigmentMixer;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiSolarNeutronActivator;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiNutritionalLiquifier;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiAntiprotonicNucleosynthesizer;
import com.beipuo.mekenergistics.blockentity.machine.process.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeAntiprotonicNucleosynthesizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalCrystallizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalDissolutionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalOxidizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalWasherBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDigitalMinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDimensionalStabilizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeElectricPumpBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeFluidicPlenisherBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeLogisticalSorterBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeModificationStationBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeOredictionificatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeResistiveHeaterBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeTeleporterBlockEntity;
import com.beipuo.mekenergistics.menu.MePatternMachineContainer;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekaf.common.tile.factory.base.TileEntityAdvancedFactoryBase;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineFactory;
import mekanism.client.gui.GuiDimensionalStabilizer;
import mekanism.client.gui.GuiLogisticalSorter;
import mekanism.client.gui.GuiModificationStation;
import mekanism.client.gui.GuiTeleporter;
import mekanism.client.gui.machine.GuiDigitalMiner;
import mekanism.client.gui.machine.GuiElectricPump;
import mekanism.client.gui.machine.GuiFluidicPlenisher;
import mekanism.client.gui.machine.GuiFormulaicAssemblicator;
import mekanism.client.gui.machine.GuiOredictionificator;
import mekanism.client.gui.machine.GuiResistiveHeater;
import mekanism.client.gui.machine.GuiSeismicVibrator;
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
import mekanism.common.tile.factory.TileEntityFactory;
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
                        new MeGuiAdvancedElectricMachine<>(menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_METALLURGIC_INFUSER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiMetallurgicInfuser((MekanismTileContainer<MeMetallurgicInfuserBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_COMBINER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiCombiner((MekanismTileContainer<MeCombinerBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PRECISION_SAWMILL.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiPrecisionSawmill((MekanismTileContainer<MePrecisionSawmillBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
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
                        new MeGuiPRC((MekanismTileContainer<MePressurizedReactionChamberBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_CRYSTALLIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiChemicalCrystallizer((MekanismTileContainer<MeChemicalCrystallizerBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_DISSOLUTION_CHAMBER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiChemicalDissolutionChamber((MekanismTileContainer<MeChemicalDissolutionChamberBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_INFUSER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiChemicalInfuser((MekanismTileContainer<MeChemicalInfuserBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_OXIDIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiChemicalOxidizer((MekanismTileContainer<MeChemicalOxidizerBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_CHEMICAL_WASHER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiChemicalWasher((MekanismTileContainer<MeChemicalWasherBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ROTARY_CONDENSENTRATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiRotaryCondensentrator((MekanismTileContainer<MeRotaryCondensentratorBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ELECTROLYTIC_SEPARATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiElectrolyticSeparator((MekanismTileContainer<MeElectrolyticSeparatorBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_SOLAR_NEUTRON_ACTIVATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiSolarNeutronActivator((MekanismTileContainer<MeSolarNeutronActivatorBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ISOTOPIC_CENTRIFUGE.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiIsotopicCentrifuge((MekanismTileContainer<MeIsotopicCentrifugeBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_NUTRITIONAL_LIQUIFIER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiNutritionalLiquifier((MekanismTileContainer<MeNutritionalLiquifierBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiAntiprotonicNucleosynthesizer((MekanismTileContainer<MeAntiprotonicNucleosynthesizerBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PIGMENT_EXTRACTOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiPigmentExtractor((MekanismTileContainer<MePigmentExtractorBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PIGMENT_MIXER.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiPigmentMixer((MekanismTileContainer<MePigmentMixerBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_PAINTING_MACHINE.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiPaintingMachine((MekanismTileContainer<MePaintingMachineBlockEntity>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiFactory((MekanismTileContainer<TileEntityFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_MORE_MACHINE_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiMoreMachineFactory((MekanismTileContainer<TileEntityMoreMachineFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_ADVANCED_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiAdvancedFactory((MekanismTileContainer<TileEntityAdvancedFactoryBase<?>>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_EXTRA_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiExtraFactory((MekanismTileContainer<TileEntityExtraFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_EXTRA_MORE_MACHINE_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiExtraMoreMachineFactory((MekanismTileContainer<TileEntityExtraMoreMachineFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
        event.register((MenuType) ModMenuTypes.ME_EXTRA_ADVANCED_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiExtraAdvancedFactory((MekanismTileContainer<TileEntityExtraAdvancedFactoryBase<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
