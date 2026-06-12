package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.client.compat.eme.EvolvedMekanismExtrasClientScreens;
import com.beipuo.mekenergistics.client.compat.meke.MekanismExtrasAdvancedClientScreens;
import com.beipuo.mekenergistics.client.compat.meke.MekanismExtrasClientScreens;
import com.beipuo.mekenergistics.client.compat.meke.MekanismExtrasMoreMachineClientScreens;
import com.beipuo.mekenergistics.client.compat.mekmm.MekanismMoreMachineAdvancedClientScreens;
import com.beipuo.mekenergistics.client.compat.mekmm.MekanismMoreMachineClientScreens;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import com.beipuo.mekenergistics.client.screen.MeElectricMachineScreen;
import com.beipuo.mekenergistics.client.screen.MeGenericMachineScreen;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiAdvancedElectricMachine;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiChemicalInfuser;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiFactory;
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
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.menu.MePatternMachineContainer;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import mekanism.client.gui.machine.GuiFormulaicAssemblicator;
import mekanism.client.gui.machine.GuiSeismicVibrator;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.tile.machine.TileEntityCombiner;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import mekanism.common.tile.machine.TileEntityAntiprotonicNucleosynthesizer;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.machine.TileEntityChemicalInfuser;
import mekanism.common.tile.machine.TileEntityChemicalOxidizer;
import mekanism.common.tile.machine.TileEntityChemicalWasher;
import mekanism.common.tile.machine.TileEntityIsotopicCentrifuge;
import mekanism.common.tile.machine.TileEntityMetallurgicInfuser;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import mekanism.common.tile.machine.TileEntityPaintingMachine;
import mekanism.common.tile.machine.TileEntityPigmentExtractor;
import mekanism.common.tile.machine.TileEntityPigmentMixer;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.tile.machine.TileEntityPressurizedReactionChamber;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import mekanism.common.tile.machine.TileEntitySeismicVibrator;
import mekanism.common.tile.machine.TileEntitySolarNeutronActivator;
import mekanism.common.tile.factory.TileEntityFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerScreens);
    }

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
        event.register((MenuType) ModMenuTypes.ME_SEISMIC_VIBRATOR.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new GuiSeismicVibrator((MekanismTileContainer<TileEntitySeismicVibrator>) (MekanismTileContainer<?>) menu, inv, title));
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
        if (ModList.get().isLoaded("mekmm")) {
            MekanismMoreMachineClientScreens.register(event);
            if (OptionalCompatClasses.hasMekmmAdvancedFactories()) {
                MekanismMoreMachineAdvancedClientScreens.register(event);
            }
        }
        if (ModList.get().isLoaded("mekanism_extras")) {
            MekanismExtrasClientScreens.register(event);
            if (OptionalCompatClasses.hasMekanismExtrasMoreMachineFactories()) {
                MekanismExtrasMoreMachineClientScreens.register(event);
            }
            if (OptionalCompatClasses.hasMekanismExtrasAdvancedFactories()) {
                MekanismExtrasAdvancedClientScreens.register(event);
            }
        }
        if (ModList.get().isLoaded("emextras")) {
            EvolvedMekanismExtrasClientScreens.register(event);
        }
    }
}
