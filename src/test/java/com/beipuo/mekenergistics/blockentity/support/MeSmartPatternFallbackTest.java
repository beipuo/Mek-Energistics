package com.beipuo.mekenergistics.blockentity.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.testfixture.FakeKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class MeSmartPatternFallbackTest {
    @Test
    void validCpuInputsStayOnSmartMultiplicationPath() {
        FakeKey input = new FakeKey("normal_cpu_input");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        multiplication.setEnabled(true);
        AtomicBoolean disabled = new AtomicBoolean();
        AtomicBoolean directlyDispatched = new AtomicBoolean();
        TestPattern pattern = new TestPattern(input);

        boolean accepted = AbstractMeAeSupport.dispatchWithSmartPatternFallback(
                true,
                true,
                multiplication,
                pattern,
                counters(input),
                () -> disabled.set(true),
                () -> {
                    directlyDispatched.set(true);
                    return true;
                });

        assertTrue(accepted);
        assertTrue(multiplication.isEnabled());
        assertTrue(multiplication.hasPendingWork());
        assertFalse(disabled.get());
        assertFalse(directlyDispatched.get());
    }

    @Test
    void incompatibleAddonCpuBatchDisablesMultiplicationAndFallsBack() {
        FakeKey primary = new FakeKey("addon_cpu_primary");
        FakeKey substitute = new FakeKey("addon_cpu_substitute");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        AtomicBoolean directlyDispatched = new AtomicBoolean();
        KeyCounter mixedCpuBatch = counters(primary)[0];
        mixedCpuBatch.add(substitute, 1);
        TestPattern pattern = new TestPattern(primary, substitute);

        boolean accepted = AbstractMeAeSupport.dispatchWithSmartPatternFallback(
                true,
                true,
                multiplication,
                pattern,
                new KeyCounter[] {mixedCpuBatch},
                () -> multiplication.setEnabled(false),
                () -> {
                    directlyDispatched.set(true);
                    return true;
                });

        assertTrue(accepted);
        assertFalse(multiplication.isEnabled());
        assertFalse(multiplication.hasPendingWork());
        assertTrue(directlyDispatched.get());
    }

    @Test
    void disabledMultiplicationKeepsUsingDirectCpuDispatch() {
        FakeKey input = new FakeKey("disabled_multiplier_input");
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        multiplication.setEnabled(false);
        AtomicBoolean disableCalledAgain = new AtomicBoolean();
        TestPattern pattern = new TestPattern(input);

        boolean accepted = AbstractMeAeSupport.dispatchWithSmartPatternFallback(
                true,
                true,
                multiplication,
                pattern,
                counters(input),
                () -> disableCalledAgain.set(true),
                () -> true);

        assertTrue(accepted);
        assertFalse(multiplication.isEnabled());
        assertFalse(disableCalledAgain.get());
    }

    @Test
    void sameDefinitionWrapperFromAddonCpuDisablesLocalMultiplication() {
        FakeKey input = new FakeKey("scaled_wrapper_input");
        TestPattern addonWrapper = new TestPattern(input);
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        AtomicBoolean directlyDispatched = new AtomicBoolean();

        boolean accepted = AbstractMeAeSupport.dispatchWithSmartPatternFallback(
                false,
                true,
                multiplication,
                addonWrapper,
                counters(input),
                () -> multiplication.setEnabled(false),
                () -> {
                    directlyDispatched.set(true);
                    return true;
                });

        assertTrue(accepted);
        assertFalse(multiplication.isEnabled());
        assertFalse(multiplication.hasPendingWork());
        assertTrue(directlyDispatched.get());
    }

    @Test
    void unrelatedPatternIsStillRejected() {
        FakeKey input = new FakeKey("unrelated_pattern_input");
        TestPattern unrelated = new TestPattern(input);
        MeSmartPatternMultiplication multiplication = new MeSmartPatternMultiplication();
        multiplication.setEnabled(true);
        AtomicBoolean disabled = new AtomicBoolean();
        AtomicBoolean directlyDispatched = new AtomicBoolean();

        boolean accepted = AbstractMeAeSupport.dispatchWithSmartPatternFallback(
                false,
                false,
                multiplication,
                unrelated,
                counters(input),
                () -> disabled.set(true),
                () -> {
                    directlyDispatched.set(true);
                    return true;
                });

        assertFalse(accepted);
        assertTrue(multiplication.isEnabled());
        assertFalse(disabled.get());
        assertFalse(directlyDispatched.get());
    }

    private static KeyCounter[] counters(AEKey key) {
        KeyCounter counter = new KeyCounter();
        counter.add(key, 1);
        return new KeyCounter[] {counter};
    }

    private static final class TestPattern implements IPatternDetails {
        private final IInput[] inputs;

        private TestPattern(AEKey... inputs) {
            GenericStack[] possibleInputs = new GenericStack[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                possibleInputs[i] = new GenericStack(inputs[i], 1);
            }
            this.inputs = new IInput[] {new TestInput(possibleInputs)};
        }

        @Override
        public appeng.api.stacks.AEItemKey getDefinition() {
            return null;
        }

        @Override
        public IInput[] getInputs() {
            return this.inputs;
        }

        @Override
        public List<GenericStack> getOutputs() {
            return List.of();
        }
    }

    private record TestInput(GenericStack[] possible) implements IPatternDetails.IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return this.possible;
        }

        @Override
        public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            for (GenericStack candidate : this.possible) {
                if (candidate.what().equals(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
