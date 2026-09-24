package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class SmartPatternRequest {
    private final AEKey definition;
    private final List<GenericStack> inputs;
    private final SmartPatternRequestKey key;
    private final KeyCounter[] oneCopyInputs;
    private final long maxBatchCopies;
    private long remaining;
    private long preferredBatch = -1;

    SmartPatternRequest(AEKey definition, List<GenericStack> inputs, long remaining) {
        this.definition = definition;
        this.inputs = List.copyOf(inputs);
        this.key = new SmartPatternRequestKey(definition, this.inputs);
        this.oneCopyInputs = createKeyCounters(inputs, 1);
        this.maxBatchCopies = calculateMaxBatchCopies(this.inputs);
        this.remaining = remaining;
    }

    @Nullable
    static SmartPatternRequest create(IPatternDetails details, KeyCounter[] holder) {
        if (details == null || holder == null || holder.length == 0 || details.getInputs().length != holder.length) return null;
        List<GenericStack> oneCraft = new ArrayList<>(holder.length);
        long copies = -1;
        for (int i = 0; i < holder.length; i++) {
            Object2LongMap.Entry<AEKey> pushed = singleEntry(holder[i]);
            if (pushed == null || pushed.getLongValue() <= 0) return null;
            long perCraft = inputAmountPerCraft(details.getInputs()[i], pushed.getKey());
            if (perCraft <= 0 || pushed.getLongValue() % perCraft != 0) return null;
            long inputCopies = pushed.getLongValue() / perCraft;
            if (copies < 0) copies = inputCopies; else if (copies != inputCopies) return null;
            oneCraft.add(new GenericStack(pushed.getKey(), perCraft));
        }
        return copies <= 0 ? null : new SmartPatternRequest(details.getDefinition(), oneCraft, copies);
    }

    SmartPatternRequestKey key() { return key; }
    AEKey definition() { return definition; }
    List<GenericStack> inputs() { return inputs; }
    long remaining() { return remaining; }
    boolean merge(SmartPatternRequest other) { if (other.remaining > Long.MAX_VALUE - remaining) return false; remaining += other.remaining; return true; }
    void remove(long copies) { remaining -= copies; }
    Iterable<AEKey> inputKeys() { Set<AEKey> keys = new LinkedHashSet<>(); for (GenericStack input : inputs) keys.add(input.what()); return keys; }
    KeyCounter[] toKeyCounters(long copies) { return copies == 1 ? oneCopyInputs : createKeyCounters(inputs, copies); }
    long maxAcceptedBy(MeSmartPatternMultiplication.Feeder feeder) { if (!(feeder instanceof MeSmartPatternMultiplication.CapacityAwareFeeder c)) return Long.MAX_VALUE; long accepted = c.maxAcceptedCopies(oneCopyInputs); return accepted <= 0 ? 0 : accepted; }
    long nextBatchAttempt() { long max = Math.min(remaining, maxBatchCopies); return preferredBatch <= 0 ? max : Math.max(1, Math.min(max, preferredBatch)); }
    void recordSuccessfulBatch(long copies) { if (copies <= 0) return; if (copies >= maxBatchCopies || copies > Long.MAX_VALUE / 2) preferredBatch = maxBatchCopies; else preferredBatch = Math.max(copies + 1, copies * 2); }
    void recordFailedBatch(long copies) { if (copies <= 1) { preferredBatch = 1; return; } long reduced = copies / 2; if (preferredBatch <= 0 || preferredBatch >= copies) preferredBatch = Math.max(1, reduced); }

    private static KeyCounter[] createKeyCounters(List<GenericStack> inputs, long copies) { KeyCounter[] result = new KeyCounter[inputs.size()]; for (int i=0;i<inputs.size();i++) { GenericStack input=inputs.get(i); KeyCounter counter=new KeyCounter(); counter.add(input.what(), MePendingPatternStore.scaleAmountClamped(input.amount(), copies)); result[i]=counter; } return result; }
    private static long calculateMaxBatchCopies(List<GenericStack> inputs) { long max=Long.MAX_VALUE; for (GenericStack input:inputs) { if (input.amount()<=0) return 1; if (input.what() instanceof AEItemKey || input.what() instanceof AEFluidKey) max=Math.min(max,Integer.MAX_VALUE/input.amount()); max=Math.min(max,Long.MAX_VALUE/input.amount()); } return Math.max(1,max); }
    @Nullable private static Object2LongMap.Entry<AEKey> singleEntry(KeyCounter counter) { return counter == null || counter.size()!=1 ? null : counter.getFirstEntry(); }
    private static long inputAmountPerCraft(IPatternDetails.IInput input, AEKey key) { long multiplier=Math.max(1,input.getMultiplier()); for (GenericStack possible:input.getPossibleInputs()) if (possible!=null && possible.what().equals(key) && possible.amount()>0) return possible.amount()*multiplier; return 0; }
}

record SmartPatternRequestKey(AEKey definition, List<GenericStack> inputs) { SmartPatternRequestKey { inputs=List.copyOf(inputs); } }
