package com.beipuo.mekenergistics.compat.omnisequence;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSequence Transfinite {@code MolecularBatchCraftingProvider} bridge decisions.
 *
 * <p>OmniSequence's CPU mixin owns batch dispatch on CPUs linked to an omni computation core and
 * treats any {@code ICraftingProvider} implementing its batch interface as a scaled-push target.
 * This class answers the two interface questions for ME machines and never references OmniSequence
 * classes, so it stays loadable while the mod is absent.</p>
 */
public final class OmniBatchCompat {
    private OmniBatchCompat() {
    }

    /**
     * Whether an ME machine can accept one physical push carrying several identical crafts. Only
     * processing patterns can be pushed as bulk inputs; the machine must hold the pattern, must not
     * be mid-batch, and must physically fit at least two copies, otherwise claiming batch support
     * would make OmniSequence extract more than the machine can ever consume.
     */
    public static boolean supportsBatching(AbstractMeAeSupport<?> support, IPatternDetails pattern) {
        return support != null && pattern != null
                && pattern.supportsPushInputsToExternalInventory()
                && !support.isPatternBusy()
                && support.hasRegisteredPattern(pattern)
                && getBatchLimit(support, pattern) >= 2;
    }

    /**
     * Physical batch limit for the machine and pattern, derived from the first possible input of
     * every slot. OmniSequence's adaptive controller treats this as the maximum window and halves
     * on rejection, so an over-estimate converges safely and an under-estimate only shrinks the
     * batch.
     */
    public static long getBatchLimit(AbstractMeAeSupport<?> support, IPatternDetails pattern) {
        KeyCounter[] oneCraftPrototype = oneCraftPrototype(pattern);
        if (support == null || oneCraftPrototype == null) {
            return 0;
        }
        return Math.max(0, support.maxAcceptedCopies(oneCraftPrototype));
    }

    /**
     * Single batch owner for ME machines. OmniSequence dispatches CPUs it owns; everywhere else
     * Mek-Energistics' own CPU batching is the owner. The guard mixin uses this on every CPU, so
     * the present/absent matrices never run both batching engines over the same push.
     */
    public static boolean isOmniManagedCpu(boolean omniBatchIntegrationLoaded, boolean coreOwnsCpu) {
        return omniBatchIntegrationLoaded && coreOwnsCpu;
    }

    /**
     * Retrievable warning when the mod is loaded but the ABI the bridge compiles against is not
     * present, so the bridge is explicitly disabled instead of silently skipping.
     */
    public static Optional<String> bridgeWarning(boolean omniLoaded,
            boolean batchProviderPresent, boolean corePresent) {
        if (!omniLoaded || (batchProviderPresent && corePresent)) {
            return Optional.empty();
        }
        String missing = !batchProviderPresent
                ? "MolecularBatchCraftingProvider"
                : "OmniComputationCoreBlockEntity";
        return Optional.of("OmniSequence batch bridge disabled: expected ABI class missing from the"
                + " loaded mod: " + missing);
    }

    @Nullable
    private static KeyCounter[] oneCraftPrototype(IPatternDetails pattern) {
        if (pattern == null) {
            return null;
        }
        IPatternDetails.IInput[] inputs = pattern.getInputs();
        if (inputs == null || inputs.length == 0) {
            return null;
        }
        KeyCounter[] prototype = new KeyCounter[inputs.length];
        for (int slot = 0; slot < inputs.length; slot++) {
            prototype[slot] = new KeyCounter();
            GenericStack first = firstPossibleInput(inputs[slot]);
            if (first == null) {
                return null;
            }
            long multiplier = Math.max(1, inputs[slot].getMultiplier());
            if (first.amount() > Long.MAX_VALUE / multiplier) {
                return null;
            }
            long amount = first.amount() * multiplier;
            if (amount <= 0) {
                return null;
            }
            prototype[slot].add(first.what(), amount);
        }
        return prototype;
    }

    @Nullable
    private static GenericStack firstPossibleInput(IPatternDetails.IInput input) {
        if (input == null) {
            return null;
        }
        for (GenericStack possible : input.getPossibleInputs()) {
            if (possible != null && possible.what() != null && possible.amount() > 0) {
                return possible;
            }
        }
        return null;
    }
}
