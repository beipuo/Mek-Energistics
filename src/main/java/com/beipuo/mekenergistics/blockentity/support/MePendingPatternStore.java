package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.stacks.GenericStack;
import com.beipuo.mekenergistics.MekEnergistics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class MePendingPatternStore {
    private static final String TAG_QUARANTINED = "SmartPatternMultiplicationQuarantined";
    private static final String TAG_QUARANTINE_ENTRY = "Entry";
    private static final String TAG_QUARANTINE_INDEX = "Index";
    private static final String TAG_QUARANTINE_REASON = "Reason";
    private static final String TAG_INPUT = "Input";

    private final List<CompoundTag> quarantinedPending = new ArrayList<>();

    public void saveQuarantined(CompoundTag tag) {
        if (this.quarantinedPending.isEmpty()) {
            tag.remove(TAG_QUARANTINED);
        } else {
            ListTag quarantined = new ListTag();
            for (CompoundTag entry : this.quarantinedPending) {
                quarantined.add(entry);
            }
            tag.put(TAG_QUARANTINED, quarantined);
        }
    }

    public void loadQuarantined(CompoundTag tag) {
        this.quarantinedPending.clear();
        ListTag quarantined = tag.getList(TAG_QUARANTINED, CompoundTag.TAG_COMPOUND);
        for (int q = 0; q < quarantined.size(); q++) {
            this.quarantinedPending.add(quarantined.getCompound(q).copy());
        }
    }

    public void quarantine(int index, String reason, CompoundTag pendingTag) {
        CompoundTag quarantinedTag = new CompoundTag();
        quarantinedTag.putInt(TAG_QUARANTINE_INDEX, index);
        quarantinedTag.putString(TAG_QUARANTINE_REASON, reason);
        quarantinedTag.put(TAG_QUARANTINE_ENTRY, pendingTag.copy());
        this.quarantinedPending.add(quarantinedTag);
    }

    public int quarantinedCount() {
        return this.quarantinedPending.size();
    }

    /**
     * Decodes as many inputs as possible. A partial result is still useful: the decodable portion
     * can be refunded to the network while the raw entry is quarantined for recovery.
     */
    public static List<GenericStack> decodeInputs(HolderLookup.Provider registries, ListTag inputTags) {
        List<GenericStack> stacks = new ArrayList<>(inputTags.size());
        for (int j = 0; j < inputTags.size(); j++) {
            try {
                GenericStack stack = GenericStack.readTag(registries, inputTags.getCompound(j).getCompound(TAG_INPUT));
                if (stack != null && stack.amount() > 0) {
                    stacks.add(stack);
                }
            } catch (RuntimeException ignored) {
                // treated as an undecodable input; the caller decides the disposition
            }
        }
        return stacks;
    }

    public static long scaleAmountClamped(long amount, long copies) {
        if (amount <= 0 || copies <= 0) {
            return 0;
        }
        if (amount > Long.MAX_VALUE / copies) {
            return Long.MAX_VALUE;
        }
        return amount * copies;
    }

    public static long refundableBalance(List<GenericStack> inputs, long remaining) {
        if (inputs == null || inputs.isEmpty() || remaining <= 0) {
            return 0;
        }
        long balance = 0;
        for (GenericStack input : inputs) {
            if (input == null || input.what() == null) {
                continue;
            }
            long amount = scaleAmountClamped(input.amount(), remaining);
            if (amount <= 0) {
                continue;
            }
            balance = balance > Long.MAX_VALUE - amount ? Long.MAX_VALUE : balance + amount;
        }
        return balance;
    }

    public static void logDroppedPending(int index, String reason) {
        MekEnergistics.LOGGER.warn("Dropping corrupted smart-multiplication pending entry #{}: {}", index, reason);
    }

}
