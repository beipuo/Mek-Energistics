package com.beipuo.mekenergistics.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.beipuo.mekenergistics.compat.dataenergistics.DataAbiGate;
import com.beipuo.mekenergistics.compat.omnisequence.OmniBatchCompat;
import net.neoforged.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public class MekEnergisticsMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("mekenergistics-mixin");
    /**
     * Gate for an optional mixin. {@code modId} is the mod that must be loaded;
     * {@code targetClasses} are the binary names the mixin's bytecode actually binds to, or empty
     * when the mod being loaded already implies the target exists.
     *
     * <p>The two are not interchangeable. Some optional mods ship their mixin targets in a
     * conditional sub-module, so mod presence does not guarantee the class is there. The
     * {@code Large*} accessors target {@code com.jerry.meklm.*}, a sub-module of {@code mekmm} that
     * {@link com.beipuo.mekenergistics.compat.OptionalCompatClasses#hasMekmmLargeMachines()} probes
     * for at registration time for exactly this reason; gating them on the mod id alone left the
     * mixin enabled against a class that need not be present. The Data/Omni bridge mixins gate on
     * every ABI class their bytecode references so a missing member disables the whole bridge
     * instead of leaving a half-applied mixin that double-batches.
     */
    private record Gate(List<String> modIds, List<String> targetClasses) {
        static Gate mod(String modId) {
            return new Gate(List.of(modId), List.of());
        }

        static Gate target(String modId, String targetClass) {
            return new Gate(List.of(modId), List.of(targetClass));
        }

        static Gate target(String modId, String targetClass, String additionalModId) {
            return new Gate(List.of(modId, additionalModId), List.of(targetClass));
        }

        static Gate targets(String modId, String... targetClasses) {
            return new Gate(List.of(modId), List.of(targetClasses));
        }
    }

    private static final Set<Boolean> DATA_BRIDGE_STATE_REPORTED = ConcurrentHashMap.newKeySet();
    private static final Set<Boolean> OMNI_BRIDGE_STATE_REPORTED = ConcurrentHashMap.newKeySet();

    private static final String MEKLM_MACHINE = "com.jerry.meklm.common.tile.machine.";
    private static final String MEKEX_FACTORY = "com.jerry.mekextras.common.tile.factory.";
    private static final String MEKMM_FACTORY = "com.jerry.mekmm.common.tile.factory.";
    private static final String MEKEX_MEKMM_FACTORY =
            "com.jerry.mekextras.common.integration.mekmm.tile.factory.";
    private static final String MEKEX_PRESSING_FACTORY =
            MEKEX_MEKMM_FACTORY + "TileEntityExtraPressingFactory";
    private static final String EMEX_FACTORY = "io.github.masyumero.emextras.common.tile.factory.";
    private static final String EMEX_MEKMM_FACTORY =
            "io.github.masyumero.emextras.common.integration.mekmm.tile.factory.";
    private static final String MEKAF_FACTORY = "com.jerry.mekaf.common.tile.factory.";
    private static final String MEKEX_MEKAF_FACTORY =
            "com.jerry.mekextras.common.integration.mekaf.tile.factory.";
    private static final String EMEXTRAS_MEKAF_FACTORY =
            "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.";
    private static final String DATA_COUNTED_PROVIDER =
            "com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.provider.CountedCraftingProvider";
    private static final String DATA_COUNTED_ADMISSION =
            "com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingAdmission";
    private static final String DATA_COUNTED_PREPARATION =
            "com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.commit.CountedCraftingPreparation";
    private static final String DATA_TARGET_AVAILABILITY =
            "com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingDispatchTargetAvailability";
    private static final String DATA_DISPATCH_TARGET =
            "com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingDispatchTarget";
    private static final String OMNI_MOLECULAR_BATCH_PROVIDER =
            "com.atir.molecularmanipulator.integration.ae2.MolecularBatchCraftingProvider";
    private static final String OMNI_COMPUTATION_CORE =
            "com.atir.molecularmanipulator.blockentity.OmniComputationCoreBlockEntity";
    private static final String NEOECO_BATCH_BRIDGE =
            "cn.dancingsnow.neoecoae.integration.ae2lt.AE2LTBatchCraftingBridge";
    private static final String THUNDERBOLT_BATCH_PROVIDER =
            "com.moakiee.thunderbolt.ae2.api.crafting.IBatchCraftingProvider";

    private static final Map<String, Gate> OPTIONAL_MIXINS = Map.ofEntries(
            Map.entry(".TileEntityAlloyerAccessor", Gate.mod("evolvedmekanism")),
            Map.entry(".TileEntitySolidifierAccessor", Gate.mod("evolvedmekanism")),
            Map.entry(".TileEntityMelterAccessor", Gate.mod("evolvedmekanism")),
            Map.entry(".TileEntityChemixerAccessor", Gate.mod("evolvedmekanism")),
            Map.entry(".EvolvedRecipeMachineMeUpgradeMixin", Gate.mod("evolvedmekanism")),
            Map.entry(".EvolvedAlloyingFactoryMeUpgradeMixin", Gate.mod("evolvedmekanism")),
            Map.entry(".MekExtrasAlloyingFactoryMeUpgradeEnergyMixin",
                    Gate.target("mekanism_extras", MEKEX_FACTORY + "TileEntityExtraAlloyingFactory",
                            "evolvedmekanism")),
            Map.entry(".MekExtrasFactoryMeUpgradeMixin",
                    Gate.target("mekanism_extras", MEKEX_FACTORY + "TileEntityExtraFactory")),
            Map.entry(".MekExtrasFactoryMeUpgradeEnergyMixin",
                    Gate.target("mekanism_extras", MEKEX_FACTORY + "TileEntityExtraFactory")),
            Map.entry(".MekmmFactoryMeUpgradeMixin",
                    Gate.target("mekmm", MEKMM_FACTORY + "TileEntityMoreMachineFactory")),
            Map.entry(".MekmmFactoryMeUpgradeEnergyMixin",
                    Gate.target("mekmm", MEKMM_FACTORY + "TileEntityMoreMachineFactory")),
            Map.entry(".MekExtrasMoreMachineFactoryMeUpgradeMixin",
                    Gate.target("mekanism_extras", MEKEX_MEKMM_FACTORY + "TileEntityExtraMoreMachineFactory")),
            Map.entry(".MekExtrasMoreMachineFactoryMeUpgradeEnergyMixin",
                    Gate.target("mekanism_extras", MEKEX_MEKMM_FACTORY + "TileEntityExtraMoreMachineFactory")),
            Map.entry(".TileEntityExtraPressingFactoryAccessor",
                    Gate.target("mekanism_extras", MEKEX_PRESSING_FACTORY)),
            Map.entry(".MekanismExtrasMekmm140CompatMixin", new Gate(
                    List.of("mekanism_extras", "mekmm"),
                    List.of(
                            "com.jerry.mekextras.common.integration.mekmm.registries.ExtraMoreMachineBlockTypes",
                            "com.jerry.mekextras.common.integration.mekmm.registries.ExtraMoreMachineBlocks"))),
            Map.entry(".EvolvedMekanismExtrasMekmm140CompatMixin", new Gate(
                    List.of("emextras", "mekmm"),
                    List.of(
                            "io.github.masyumero.emextras.common.integration.mekmm.registries.EMExtraMoreMachineBlockTypes",
                            "io.github.masyumero.emextras.common.integration.mekmm.registries.EMExtraMoreMachineBlocks"))),
            Map.entry(".EmExtrasAlloyingFactoryAccessor",
                    Gate.target("emextras", EMEX_FACTORY + "TileEntityEMExtraAlloyingFactory")),
            Map.entry(".EmExtrasFactoryMeUpgradeMixin",
                    Gate.target("emextras", EMEX_FACTORY + "TileEntityEMExtraFactory")),
            Map.entry(".EmExtrasFactoryMeUpgradeEnergyMixin",
                    Gate.target("emextras", EMEX_FACTORY + "TileEntityEMExtraFactory")),
            Map.entry(".EmExtrasMoreMachineFactoryMeUpgradeMixin",
                    Gate.target("emextras", EMEX_MEKMM_FACTORY + "TileEntityEMExtraMoreMachineFactory")),
            Map.entry(".EmExtrasMoreMachineFactoryMeUpgradeEnergyMixin",
                    Gate.target("emextras", EMEX_MEKMM_FACTORY + "TileEntityEMExtraMoreMachineFactory")),
            Map.entry(".AdvancedFactoryMeUpgradeMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".MekExtrasAdvancedFactoryMeUpgradeMixin",
                    Gate.target("mekanism_extras", MEKEX_MEKAF_FACTORY + "base.TileEntityExtraAdvancedFactoryBase")),
            Map.entry(".EMExtrasAdvancedFactoryMeUpgradeMixin",
                    Gate.target("emextras", EMEXTRAS_MEKAF_FACTORY + "base.TileEntityEMExtraAdvancedFactoryBase")),
            Map.entry(".EMExtrasAdvancedFactoryDissolvingPortMixin",
                    Gate.target("emextras", EMEXTRAS_MEKAF_FACTORY + "TileEntityEMExtraDissolvingFactory")),
            Map.entry(".MekExtrasAdvancedFactoryDissolvingPortMixin",
                    Gate.target("mekanism_extras", MEKEX_MEKAF_FACTORY + "TileEntityExtraDissolvingFactory")),
            Map.entry(".AdvancedFactoryChemicalToChemicalPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryChemicalToItemPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryItemToChemicalPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryItemToItemPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryDissolvingPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "TileEntityDissolvingFactory")),
            Map.entry(".AdvancedFactoryLiquifyingPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryPaintingPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryPressurizedReactingPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryWashingPortMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".AdvancedFactoryMeUpgradeEnergyMixin",
                    Gate.target("mekmm", MEKAF_FACTORY + "base.TileEntityAdvancedFactoryBase")),
            Map.entry(".MekmmSimpleRecipeMachineMeUpgradeMixin", Gate.mod("mekmm")),
            Map.entry(".MekmmComplexRecipeMachineMeUpgradeMixin", Gate.mod("mekmm")),
            Map.entry(".MekanismMagicMachineMeUpgradeMixin",
                    Gate.target("mekanism_magic",
                            "com.example.mekanismmagic.blockentity.NativeMagicMachineBlockEntity")),
            Map.entry(".MeklmLargeRecipeMachineMeUpgradeMixin",
                    Gate.target("mekmm", MEKLM_MACHINE + "TileEntityLargeRotaryCondensentrator")),
            Map.entry(".TileEntityEMExtraDissolvingFactoryAccessor",
                    Gate.target("emextras", EMEXTRAS_MEKAF_FACTORY + "TileEntityEMExtraDissolvingFactory")),
            Map.entry(".TileEntityExtraDissolvingFactoryAccessor",
                    Gate.target("mekanism_extras", MEKEX_MEKAF_FACTORY + "TileEntityExtraDissolvingFactory")),
            Map.entry(".TileEntityChemicalReplicatorAccessor", Gate.mod("mekmm")),
            Map.entry(".TileEntityPlantingStationAccessor", Gate.mod("mekmm")),
            Map.entry(".TileEntityReplicatorAccessor", Gate.mod("mekmm")),
            Map.entry(".TileEntityFluidReplicatorAccessor", Gate.mod("mekmm")),
            Map.entry(".TileEntityLargeRotaryCondensentratorAccessor",
                    Gate.target("mekmm", MEKLM_MACHINE + "TileEntityLargeRotaryCondensentrator")),
            Map.entry(".TileEntityLargeChemicalInfuserAccessor",
                    Gate.target("mekmm", MEKLM_MACHINE + "TileEntityLargeChemicalInfuser")),
            Map.entry(".TileEntityLargeAntiprotonicNucleosynthesizerAccessor",
                    Gate.target("mekmm", MEKLM_MACHINE + "TileEntityLargeAntiprotonicNucleosynthesizer")),
            Map.entry(".extendedae.ContainerRenamerMixin", Gate.mod("extendedae")),
            Map.entry(".dataenergistics.PatternProviderSyncHelperMixin", Gate.mod("data_energistics")),
            Map.entry(".dataenergistics.PatternProviderNameHelperMixin", Gate.mod("data_energistics")),
            Map.entry(".dataenergistics.DataCountedCraftingProviderMixin",
                    Gate.targets("data_energistics", DATA_COUNTED_PROVIDER, DATA_COUNTED_ADMISSION,
                            DATA_COUNTED_PREPARATION, DATA_TARGET_AVAILABILITY, DATA_DISPATCH_TARGET)),
            Map.entry(".dataenergistics.DataCountedFactoryCraftingProviderMixin",
                    Gate.targets("data_energistics", DATA_COUNTED_PROVIDER, DATA_COUNTED_ADMISSION,
                            DATA_COUNTED_PREPARATION, DATA_TARGET_AVAILABILITY, DATA_DISPATCH_TARGET)),
            Map.entry(".omnisequence.OmniBatchCraftingProviderMixin",
                    Gate.targets("molecularmanipulator", OMNI_MOLECULAR_BATCH_PROVIDER, OMNI_COMPUTATION_CORE)),
            Map.entry(".omnisequence.OmniBatchFactoryCraftingProviderMixin",
                    Gate.targets("molecularmanipulator", OMNI_MOLECULAR_BATCH_PROVIDER, OMNI_COMPUTATION_CORE)),
            Map.entry(".omnisequence.OmniManagedCraftingCpuMixin",
                    Gate.targets("molecularmanipulator", OMNI_MOLECULAR_BATCH_PROVIDER, OMNI_COMPUTATION_CORE)),
            Map.entry(".neoecoae.NeoEcoBatchCraftingBridgeMixin",
                    Gate.target("neoecoae", NEOECO_BATCH_BRIDGE)),
            Map.entry(".thunderbolt.ThunderboltBatchCraftingProviderMixin",
                    Gate.target("thunderbolt", THUNDERBOLT_BATCH_PROVIDER)),
            Map.entry(".thunderbolt.ThunderboltBatchFactoryCraftingProviderMixin",
                    Gate.target("thunderbolt", THUNDERBOLT_BATCH_PROVIDER)));

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        for (var entry : OPTIONAL_MIXINS.entrySet()) {
            if (mixinClassName.endsWith(entry.getKey())) {
                Gate gate = entry.getValue();
                if (!gate.modIds().stream().allMatch(MekEnergisticsMixinPlugin::isModLoaded)) {
                    return false;
                }
                boolean targetsPresent = gate.targetClasses().stream()
                        .allMatch(MekEnergisticsMixinPlugin::isClassPresent);
                if (!targetsPresent) {
                    reportBridgeState(mixinClassName, false);
                    return false;
                }
                reportBridgeState(mixinClassName, true);
                return true;
            }
        }
        return true;
    }

    /**
     * Logs a bridge state transition once per boot. The Data and Omni counted-crafting bridges are
     * only safe as a whole: any half-applied mixin would either run both batching engines over the
     * same push or bind against a class that is not there. A bridge that is present but whose ABI
     * does not match must degrade loudly instead of silently skipping while still claiming
     * compatibility.
     */
    private static void reportBridgeState(String mixinClassName, boolean enabled) {
        if (mixinClassName.endsWith(".dataenergistics.DataCountedCraftingProviderMixin")
                || mixinClassName.endsWith(".dataenergistics.DataCountedFactoryCraftingProviderMixin")) {
            if (!enabled && DATA_BRIDGE_STATE_REPORTED.add(false)) {
                DataAbiGate.countedBridgeWarning(true,
                                isClassPresent(DATA_COUNTED_PROVIDER),
                                isClassPresent(DATA_COUNTED_ADMISSION))
                        .ifPresent(LOGGER::warn);
            }
        } else if (mixinClassName.endsWith(".omnisequence.OmniBatchCraftingProviderMixin")
                || mixinClassName.endsWith(".omnisequence.OmniBatchFactoryCraftingProviderMixin")
                || mixinClassName.endsWith(".omnisequence.OmniManagedCraftingCpuMixin")) {
            if (OMNI_BRIDGE_STATE_REPORTED.add(enabled)) {
                if (enabled) {
                    LOGGER.info("OmniSequence batch bridge enabled: MolecularBatchCraftingProvider"
                            + " and OmniComputationCoreBlockEntity present");
                } else {
                    OmniBatchCompat.bridgeWarning(true,
                                    isClassPresent(OMNI_MOLECULAR_BATCH_PROVIDER),
                                    isClassPresent(OMNI_COMPUTATION_CORE))
                            .ifPresent(LOGGER::warn);
                }
            }
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModLoaded(String modId) {
        return LoadingModList.get() != null && LoadingModList.get().getModFileById(modId) != null;
    }

    /**
     * Probes for a class without loading it. Uses the Mixin service rather than a {@link ClassLoader}
     * because this runs during mixin bootstrap, before the game class loader is usable.
     */
    private static boolean isClassPresent(String binaryName) {
        String resource = binaryName.replace('.', '/') + ".class";
        try (InputStream stream = MixinService.getService().getResourceAsStream(resource)) {
            return stream != null;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
