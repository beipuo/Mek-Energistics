package com.beipuo.mekenergistics.client.jei;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.client.jei.compat.MekanismMoreMachineJeiCompat;
import com.beipuo.mekenergistics.client.overlay.MePatternWindowOverlay;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.registry.ModItems;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRuntimeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class MekEnergisticsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(GuiMekanism.class, new PatternButtonExclusionHandler());
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registerCatalysts(registration, RecipeViewerRecipeType.SMELTING, FactoryType.SMELTING);
        registerCatalysts(registration, RecipeViewerRecipeType.ENRICHING, FactoryType.ENRICHING);
        registerCatalysts(registration, RecipeViewerRecipeType.CRUSHING, FactoryType.CRUSHING);
        registerCatalysts(registration, RecipeViewerRecipeType.COMPRESSING, FactoryType.COMPRESSING);
        registerCatalysts(registration, RecipeViewerRecipeType.COMBINING, FactoryType.COMBINING);
        registerCatalysts(registration, RecipeViewerRecipeType.PURIFYING, FactoryType.PURIFYING);
        registerCatalysts(registration, RecipeViewerRecipeType.INJECTING, FactoryType.INJECTING);
        registerCatalysts(registration, RecipeViewerRecipeType.METALLURGIC_INFUSING, FactoryType.INFUSING);
        registerCatalysts(registration, RecipeViewerRecipeType.SAWING, FactoryType.SAWING);
        registerMachines(registration, RecipeViewerRecipeType.CRYSTALLIZING, MeMekanismMachine.CHEMICAL_CRYSTALLIZER);
        registerMachines(registration, RecipeViewerRecipeType.DISSOLUTION, MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER);
        registerMachines(registration, RecipeViewerRecipeType.CHEMICAL_INFUSING, MeMekanismMachine.CHEMICAL_INFUSER);
        registerMachines(registration, RecipeViewerRecipeType.OXIDIZING, MeMekanismMachine.CHEMICAL_OXIDIZER);
        registerMachines(registration, RecipeViewerRecipeType.WASHING, MeMekanismMachine.CHEMICAL_WASHER);
        registerMachines(registration, RecipeViewerRecipeType.SEPARATING, MeMekanismMachine.ELECTROLYTIC_SEPARATOR);
        registerMachines(registration, RecipeViewerRecipeType.ACTIVATING, MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR);
        registerMachines(registration, RecipeViewerRecipeType.CENTRIFUGING, MeMekanismMachine.ISOTOPIC_CENTRIFUGE);
        registerMachines(registration, RecipeViewerRecipeType.REACTION, MeMekanismMachine.PRESSURIZED_REACTION_CHAMBER);
        registerMachines(registration, RecipeViewerRecipeType.NUCLEOSYNTHESIZING, MeMekanismMachine.ANTIPROTONIC_NUCLEOSYNTHESIZER);
        registerMachines(registration, RecipeViewerRecipeType.PIGMENT_EXTRACTING, MeMekanismMachine.PIGMENT_EXTRACTOR);
        registerMachines(registration, RecipeViewerRecipeType.PIGMENT_MIXING, MeMekanismMachine.PIGMENT_MIXER);
        registerMachines(registration, RecipeViewerRecipeType.PAINTING, MeMekanismMachine.PAINTING_MACHINE);
        registerMachines(registration, RecipeViewerRecipeType.NUTRITIONAL_LIQUIFICATION, MeMekanismMachine.NUTRITIONAL_LIQUIFIER);
        registerMachines(registration, RecipeViewerRecipeType.CONDENSENTRATING, MeMekanismMachine.ROTARY_CONDENSENTRATOR);
        registerMachines(registration, RecipeViewerRecipeType.DECONDENSENTRATING, MeMekanismMachine.ROTARY_CONDENSENTRATOR);
        registerEvolvedMekanismCatalysts(registration);
        registerMachines(registration, RecipeViewerRecipeType.CHEMICAL_CONVERSION,
                MeMekanismMachine.PURIFICATION_CHAMBER,
                MeMekanismMachine.METALLURGIC_INFUSER,
                MeMekanismMachine.OSMIUM_COMPRESSOR,
                MeMekanismMachine.CHEMICAL_INJECTION_CHAMBER,
                MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER,
                MeMekanismMachine.ANTIPROTONIC_NUCLEOSYNTHESIZER);
        registerVanillaCatalysts(registration);
        registerMoreMachineCatalysts(registration);
    }

    @Override
    public void registerRuntime(@NotNull IRuntimeRegistration registration) {
        if (!MekEnergisticsConfig.hideJeiMachineVariants()) {
            return;
        }
        registration.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenStacks());
    }

    private static void registerCatalysts(IRecipeCatalystRegistration registration, IRecipeViewerRecipeType<?> recipeType, FactoryType factoryType) {
        MeMekanismMachine baseMachine = MeMekanismMachine.getBaseMachine(factoryType);
        ItemLike[] catalysts = baseMachine == null ? new ItemLike[0] : catalysts(baseMachine);
        if (catalysts.length > 0) {
            registration.addRecipeCatalysts(MekanismJEI.genericRecipeType(recipeType), catalysts);
        }
    }

    public static void registerMachines(IRecipeCatalystRegistration registration, IRecipeViewerRecipeType<?> recipeType, MeMekanismMachine... machines) {
        ItemLike[] catalysts = catalysts(machines);
        if (catalysts.length > 0) {
            registration.addRecipeCatalysts(MekanismJEI.genericRecipeType(recipeType), catalysts);
        }
    }

    private static void registerVanillaCatalysts(IRecipeCatalystRegistration registration) {
        ItemLike[] smeltingCatalysts = catalysts(MeMekanismMachine.ENERGIZED_SMELTER);
        if (smeltingCatalysts.length > 0) {
            registration.addRecipeCatalysts(RecipeTypes.SMELTING, smeltingCatalysts);
        }
        ItemLike[] craftingCatalysts = catalysts(MeMekanismMachine.FORMULAIC_ASSEMBLICATOR);
        if (craftingCatalysts.length > 0) {
            registration.addRecipeCatalysts(RecipeTypes.CRAFTING, craftingCatalysts);
        }
    }

    private static void registerMoreMachineCatalysts(IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("mekmm")) {
            return;
        }
        MekanismMoreMachineJeiCompat.registerCatalysts(registration, MekEnergisticsJeiPlugin::registerMoreMachineFactories);
    }

    private static void registerEvolvedMekanismCatalysts(IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("evolvedmekanism")) {
            return;
        }
        try {
            Class.forName("com.beipuo.mekenergistics.client.jei.compat.EvolvedMekanismJeiCompat")
                    .getMethod("registerCatalysts", IRecipeCatalystRegistration.class)
                    .invoke(null, registration);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    private static void registerMoreMachineFactories(IRecipeCatalystRegistration registration, IRecipeViewerRecipeType<?> recipeType, String factoryTypeName) {
        MeMekanismMachine basicFactory = MeMekanismMachine.getMoreMachineFactory(FactoryTier.BASIC, factoryTypeName);
        ItemLike[] catalysts = basicFactory == null ? new ItemLike[0] : catalysts(basicFactory);
        if (catalysts.length > 0) {
            registration.addRecipeCatalysts(MekanismJEI.genericRecipeType(recipeType), catalysts);
        }
    }

    private static ItemLike[] catalysts(MeMekanismMachine... machines) {
        List<ItemLike> catalysts = new ArrayList<>();
        for (MeMekanismMachine machine : machines) {
            if (!machine.isAvailable()) {
                continue;
            }
            var item = ModItems.getMachineItem(machine);
            if (item != null) {
                catalysts.add(item.get());
            }
        }
        return catalysts.toArray(ItemLike[]::new);
    }

    private static List<ItemStack> hiddenStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        addHiddenStack(stacks, MeMekanismMachine.SEISMIC_VIBRATOR);
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (machine.isFactory()) {
                addHiddenStack(stacks, machine);
            }
        }
        return stacks;
    }

    private static void addHiddenStack(List<ItemStack> stacks, MeMekanismMachine machine) {
        if (!machine.isAvailable()) {
            return;
        }
        var item = ModItems.getMachineItem(machine);
        if (item != null) {
            stacks.add(item.get().getDefaultInstance());
        }
    }

    private static final class PatternButtonExclusionHandler implements IGuiContainerHandler<GuiMekanism<?>> {
        @Override
        public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull GuiMekanism<?> gui) {
            if (!MePatternWindowOverlay.hasPatternTarget(gui)) {
                return List.of();
            }
            return List.of(MePatternWindowOverlay.jeiButtonArea(gui));
        }
    }
}
