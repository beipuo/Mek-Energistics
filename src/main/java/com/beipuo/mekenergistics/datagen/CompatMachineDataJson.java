package com.beipuo.mekenergistics.datagen;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineSpec;
import com.beipuo.mekenergistics.compat.catalog.CompatRequirement;
import com.beipuo.mekenergistics.compat.catalog.CompatMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.ResourceLocation;

final class CompatMachineDataJson {
    private CompatMachineDataJson() {
    }

    static JsonObject factoryBlockState(CompatMachineSpec spec) {
        return blockState(spec, true);
    }

    static JsonObject machineBlockState(CompatMachineSpec spec) {
        return blockState(spec, CompatMachineResourceProfile.hasDedicatedActiveMachineModel(spec));
    }

    private static JsonObject blockState(CompatMachineSpec spec, boolean dedicatedActiveModel) {
        JsonObject variants = new JsonObject();
        addFacingVariants(variants, spec.meBlockId(), false);
        addFacingVariants(variants, dedicatedActiveModel ? activeModel(spec.meBlockId()) : spec.meBlockId(), true);
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    static JsonObject factoryModel(CompatMachineSpec spec, boolean active) {
        return switch (CompatMachineResourceProfile.factoryModelStyle(spec)) {
            case CENTRIFUGING -> centrifugingFactoryModel(spec, active);
            case PLANTING -> plantingFactoryModel(spec, active);
            case STANDARD -> standardFactoryModel(spec, active);
        };
    }

    private static JsonObject standardFactoryModel(CompatMachineSpec spec, boolean active) {
        FactoryModelParts parts = factoryModelParts(spec.machine());
        JsonObject root = new JsonObject();
        root.addProperty("loader", "neoforge:composite");
        root.addProperty("parent", "block/block");

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", parts.particle());
        root.add("textures", textures);

        JsonObject base = new JsonObject();
        base.addProperty("parent", parts.baseParent());
        if (active && parts.activeFront() != null) {
            JsonObject baseTextures = new JsonObject();
            baseTextures.addProperty("front", parts.activeFront());
            base.add("textures", baseTextures);
        }

        JsonObject led = new JsonObject();
        led.addProperty("parent", MekEnergistics.MODID + ":block/factory/front_led/"
                + (active ? "active/" : "") + spec.tierId());

        JsonObject children = new JsonObject();
        children.add("base", base);
        children.add("front_led", led);
        root.add("children", children);
        return root;
    }

    private static JsonObject centrifugingFactoryModel(CompatMachineSpec spec, boolean active) {
        JsonObject root = specialFactoryModelRoot();
        JsonObject children = new JsonObject();

        JsonObject base = new JsonObject();
        base.addProperty("parent", "mekmm:block/factory/centrifuging/base");
        children.add("base", base);

        JsonObject translucent = new JsonObject();
        translucent.addProperty("parent", MekEnergistics.MODID + ":block/factory/centrifuging_translucent");
        children.add("translucent", translucent);
        children.add("front_led", inlineCombinedLed(spec.tierId(), active, 28.99));
        root.add("children", children);
        return root;
    }

    private static JsonObject plantingFactoryModel(CompatMachineSpec spec, boolean active) {
        JsonObject root = specialFactoryModelRoot();
        JsonObject children = new JsonObject();

        JsonObject base = new JsonObject();
        base.addProperty("parent", "mekmm:block/factory/planting/base" + (active ? "_active" : ""));
        children.add("base", base);

        JsonObject led = inlineCombinedLed(spec.tierId(), active, 29.99);
        led.addProperty("render_type", "cutout");
        children.add("led", led);
        root.add("children", children);
        return root;
    }

    private static JsonObject specialFactoryModelRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("loader", "neoforge:composite");
        root.addProperty("gui_light", "side");
        root.addProperty("parent", "block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "mekanism:block/factory/factory_front_back");
        root.add("textures", textures);
        return root;
    }

    private static JsonObject inlineCombinedLed(String tierId, boolean active, double minY) {
        CombinedLedParts parts = combinedLedParts(tierId);
        JsonObject led = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("led", MekEnergistics.MODID + ":block/factory/" + parts.texture());
        led.add("textures", textures);

        JsonObject element = new JsonObject();
        element.addProperty("name", "front_panel_led");
        element.add("from", numberArray(4.98, minY, 0.01));
        element.add("to", numberArray(11.02, minY + 1, 1.01));
        JsonObject faces = new JsonObject();
        faces.add("north", ledFace(parts.uvRow(), false, active));
        faces.add("up", ledFace(parts.uvRow(), true, active));
        element.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(element);
        led.add("elements", elements);
        return led;
    }

    private static JsonObject ledFace(int uvRow, boolean rotate, boolean active) {
        JsonObject face = new JsonObject();
        face.add("uv", numberArray(0, uvRow, 6, uvRow + 1));
        face.addProperty("texture", "#led");
        face.addProperty("cullface", rotate ? "up" : "north");
        if (rotate) {
            face.addProperty("rotation", 180);
        }
        if (active) {
            JsonObject light = new JsonObject();
            light.addProperty("block_light", 15);
            light.addProperty("sky_light", 15);
            face.add("neoforge_data", light);
        }
        return face;
    }

    private static JsonArray numberArray(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(value);
        }
        return array;
    }

    private static CombinedLedParts combinedLedParts(String tierId) {
        return switch (tierId) {
            case "absolute_overclocked" -> new CombinedLedParts("emextra_led", 0);
            case "supreme_quantum" -> new CombinedLedParts("supreme_quantum_led", 1);
            case "cosmic_dense" -> new CombinedLedParts("cosmic_dense_led", 2);
            case "infinite_multiversal" -> new CombinedLedParts("infinite_multiversal_led", 3);
            default -> throw new IllegalArgumentException("No combined factory LED for tier " + tierId);
        };
    }

    static JsonObject itemModel(CompatMachineSpec spec) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", modelId(spec.meBlockId()));
        return root;
    }

    static JsonObject installerRecipe(CompatMachineSpec spec) {
        JsonObject root = new JsonObject();
        Set<String> requiredMods = requiredMods(spec);
        if (!requiredMods.isEmpty()) {
            JsonArray conditions = new JsonArray();
            for (String modId : requiredMods) {
                JsonObject condition = new JsonObject();
                condition.addProperty("type", "neoforge:mod_loaded");
                condition.addProperty("modid", modId);
                conditions.add(condition);
            }
            // A loaded mod is not enough: several compat machines are only registered when
            // specific classes or tiers exist (large machines, advanced factories, combined
            // tiers). Without these checks every datapack reload logs a parse error for each
            // recipe whose source or result item was never registered.
            conditions.add(itemExists(spec.sourceBlockId()));
            conditions.add(itemExists(spec.meBlockId()));
            root.add("neoforge:conditions", conditions);
        }
        root.addProperty("type", "minecraft:crafting_shapeless");

        JsonArray ingredients = new JsonArray();
        ingredients.add(itemIngredient(spec.sourceBlockId()));
        ingredients.add(itemIngredient(ResourceLocation.fromNamespaceAndPath(
                MekEnergistics.MODID, "me_factory_installer")));
        root.add("ingredients", ingredients);

        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", spec.meBlockId().toString());
        root.add("result", result);
        return root;
    }

    static JsonObject selfDropLootTable(CompatMachineSpec spec) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", spec.meBlockId().toString());
        JsonObject copyComponents = new JsonObject();
        copyComponents.addProperty("function", "minecraft:copy_components");
        copyComponents.addProperty("source", "block_entity");
        JsonArray functions = new JsonArray();
        functions.add(copyComponents);
        entry.add("functions", functions);
        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject pool = new JsonObject();
        boolean legacyRandomSequence = CompatMachineResourceProfile.usesLegacyRandomSequence(spec);
        if (legacyRandomSequence) {
            pool.addProperty("rolls", 1.0);
            pool.addProperty("bonus_rolls", 0.0);
        } else {
            pool.addProperty("rolls", 1);
        }
        pool.add("entries", entries);
        if (CompatMachineResourceProfile.survivesExplosion(spec)) {
            JsonObject survivesExplosion = new JsonObject();
            survivesExplosion.addProperty("condition", "minecraft:survives_explosion");
            JsonArray conditions = new JsonArray();
            conditions.add(survivesExplosion);
            pool.add("conditions", conditions);
        }
        JsonArray pools = new JsonArray();
        pools.add(pool);

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", pools);
        if (legacyRandomSequence) {
            root.addProperty("random_sequence", MekEnergistics.MODID + ":blocks/" + spec.meBlockId().getPath());
        }
        return root;
    }

    private static Set<String> requiredMods(CompatMachineSpec spec) {
        Set<String> modIds = new TreeSet<>();
        for (CompatRequirement requirement : spec.requirements()) {
            switch (requirement) {
                case MEKMM -> modIds.add(CompatMod.MEKMM.modId());
                case MEKE -> modIds.add(CompatMod.MEKE.modId());
                case EMEK -> modIds.add(CompatMod.EMEK.modId());
                case EMEKE -> modIds.add(CompatMod.EMEKE.modId());
                default -> {
                    // Feature requirements have no independent mod id.
                }
            }
        }
        return modIds;
    }

    private static JsonObject itemExists(ResourceLocation item) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:item_exists");
        condition.addProperty("item", item.toString());
        return condition;
    }

    private static JsonObject itemIngredient(ResourceLocation item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item.toString());
        return ingredient;
    }

    private static void addFacingVariants(JsonObject variants, ResourceLocation model, boolean active) {
        addVariant(variants, "facing=north,active=" + active, model, 0);
        addVariant(variants, "facing=south,active=" + active, model, 180);
        addVariant(variants, "facing=east,active=" + active, model, 90);
        addVariant(variants, "facing=west,active=" + active, model, -90);
    }

    private static void addVariant(JsonObject variants, String key, ResourceLocation model, int rotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelId(model));
        if (rotation != 0) {
            variant.addProperty("y", rotation);
        }
        variants.add(key, variant);
    }

    private static ResourceLocation activeModel(ResourceLocation id) {
        return id.withSuffix("_active");
    }

    private static String modelId(ResourceLocation id) {
        return id.getNamespace() + ":block/" + id.getPath();
    }

    private static FactoryModelParts factoryModelParts(MeMekanismMachine machine) {
        String type = machine.machineTypeId();
        if ("alloying".equals(type)) {
            String root = "evolvedmekanism:block/factory/alloying/";
            return new FactoryModelParts(root + "alloying_factory_front", root + "base",
                    root + "alloying_factory_front_active");
        }
        if (machine.factoryType() != null) {
            String root = "mekanism:block/factory/" + type + "/";
            return new FactoryModelParts(root + type + "_factory_front", root + "base",
                    root + type + "_factory_front_active");
        }
        return new FactoryModelParts("mekanism:block/factory/factory_front_back",
                "mekmm:block/factory/" + type + "/base", null);
    }

    private record FactoryModelParts(String particle, String baseParent, String activeFront) {
    }

    private record CombinedLedParts(String texture, int uvRow) {
    }
}
