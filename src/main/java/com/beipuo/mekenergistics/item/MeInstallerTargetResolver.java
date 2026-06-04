package com.beipuo.mekenergistics.item;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineBaseCompat;
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
        MeMekanismMachine directTarget = getTargetByRegistryName(state);
        if (directTarget != null) {
            return directTarget;
        }
        if (ModList.get().isLoaded("mekmm")) {
            MeMekanismMachine moreMachineTarget = MekanismMoreMachineBaseCompat.getFactoryTarget(state);
            if (moreMachineTarget != null) {
                return moreMachineTarget;
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
        return tier.tier() instanceof FactoryTier factoryTier
                ? MeMekanismMachine.getFactory(factoryTier, factoryType)
                : null;
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
