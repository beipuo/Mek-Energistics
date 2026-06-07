package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineAdvancedCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineBaseCompat;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

final class MeInstallerTargetResolver {
    private MeInstallerTargetResolver() {
    }

    @Nullable
    static MeMekanismMachine resolve(BlockState state) {
        if (ModBlocks.getMachine(state.getBlock()) != null) {
            return null;
        }
        MeMekanismMachine directTarget = getTargetByRegistryName(state);
        if (directTarget != null) {
            return directTarget;
        }
        if (OptionalCompatClasses.hasMekmmAdvancedFactories()) {
            MeMekanismMachine advancedFactoryTarget = MekanismMoreMachineAdvancedCompat.getFactoryTarget(state);
            if (advancedFactoryTarget != null) {
                return advancedFactoryTarget;
            }
        }
        if (OptionalCompatClasses.hasMekmm()) {
            MeMekanismMachine baseFactoryTarget = MekanismMoreMachineBaseCompat.getFactoryTarget(state);
            if (baseFactoryTarget != null) {
                return baseFactoryTarget;
            }
        }
        if (OptionalCompatClasses.hasEvolvedMekanismExtras()) {
            MeMekanismMachine emExtraTarget = EvolvedMekanismExtrasCompat.getFactoryTarget(state);
            if (emExtraTarget != null) {
                return emExtraTarget;
            }
        }
        AttributeFactoryType attribute = Attribute.get(state, AttributeFactoryType.class);
        if (attribute == null) {
            return null;
        }
        FactoryType factoryType = attribute.getFactoryType();
        if (ModList.get().isLoaded("mekanism_extras")) {
            MeMekanismMachine extraTarget = MekanismExtrasCompat.getFactoryTarget(state, factoryType);
            if (extraTarget != null) {
                return extraTarget;
            }
        }
        AttributeTier<?> tier = Attribute.get(state, AttributeTier.class);
        if (tier == null) {
            return MeMekanismMachine.getBaseMachine(factoryType);
        }
        if (!(tier.tier() instanceof FactoryTier factoryTier)) {
            return null;
        }
        if ("alloying".equals(factoryType.getRegistryNameComponent())) {
            return MeMekanismMachine.getFactory(factoryTier, "alloying");
        }
        return MeMekanismMachine.getFactory(factoryTier, factoryType);
    }

    @Nullable
    private static MeMekanismMachine getTargetByRegistryName(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (path.startsWith("me_")) {
            return null;
        }
        return MeMekanismMachine.getByRegistryName("me_" + path);
    }
}
