package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

final class SmartPatternPersistence {
    static final String TAG_PENDING = "SmartPatternMultiplicationPending";
    static final String TAG_REMAINING = "Remaining";
    static final String TAG_DEFINITION = "Definition";
    static final String TAG_INPUTS = "Inputs";
    static final String TAG_INPUT = "Input";
    private static final String TAG_SCHEMA = "SchemaVersion";
    private static final int CURRENT_SCHEMA = 1;

    private SmartPatternPersistence() {}

    static void save(CompoundTag tag, HolderLookup.Provider registries, Iterable<SmartPatternRequest> requests) {
        ListTag list = new ListTag();
        for (SmartPatternRequest request : requests) {
            CompoundTag saved = new CompoundTag();
            saved.putLong(TAG_REMAINING, request.remaining());
            saved.put(TAG_DEFINITION, GenericStack.writeTag(registries,
                    new GenericStack(request.definition(), 1)));
            ListTag inputs = new ListTag();
            for (GenericStack input : request.inputs()) {
                CompoundTag item = new CompoundTag();
                item.put(TAG_INPUT, GenericStack.writeTag(registries, input));
                inputs.add(item);
            }
            saved.put(TAG_INPUTS, inputs);
            list.add(saved);
        }
        if (list.isEmpty()) {
            tag.remove(TAG_PENDING);
        } else {
            tag.put(TAG_PENDING, list);
        }
    }

    static List<SavedEntry> load(CompoundTag tag, HolderLookup.Provider registries) {
        int version = tag.contains(TAG_SCHEMA) ? tag.getInt(TAG_SCHEMA) : 0;
        ListTag list = tag.getList(TAG_PENDING, CompoundTag.TAG_COMPOUND);
        List<SavedEntry> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag saved = list.getCompound(i);
            if (version > CURRENT_SCHEMA) {
                result.add(SavedEntry.invalid(List.of(), saved.getLong(TAG_REMAINING), saved,
                        "unknown pending schema version " + version));
                continue;
            }
            result.add(decode(saved, registries));
        }
        return result;
    }

    private static SavedEntry decode(CompoundTag saved, HolderLookup.Provider registries) {
        long remaining = saved.getLong(TAG_REMAINING);
        if (remaining <= 0) {
            return SavedEntry.invalid(List.of(), remaining, saved, "non-positive remaining " + remaining);
        }
        ListTag inputTags = saved.getList(TAG_INPUTS, CompoundTag.TAG_COMPOUND);
        List<GenericStack> inputs = inputTags.isEmpty() ? List.of()
                : MePendingPatternStore.decodeInputs(registries, inputTags);
        try {
            GenericStack definition = GenericStack.readTag(registries, saved.getCompound(TAG_DEFINITION));
            if (definition == null) return SavedEntry.invalid(inputs, remaining, saved, "undecodable definition");
            if (!(definition.what() instanceof AEItemKey key)) {
                return SavedEntry.invalid(inputs, remaining, saved, "definition is not an item key: " + definition.what());
            }
            if (inputTags.isEmpty()) return SavedEntry.invalid(inputs, remaining, saved, "no inputs listed");
            if (inputs.isEmpty()) return SavedEntry.invalid(inputs, remaining, saved, "no usable inputs");
            if (inputs.size() < inputTags.size()) {
                return SavedEntry.invalid(inputs, remaining, saved, "only " + inputs.size() + " of " + inputTags.size() + " inputs decoded");
            }
            return new SavedEntry(key, inputs, remaining, saved, null);
        } catch (RuntimeException ex) {
            return SavedEntry.invalid(inputs, remaining, saved, "decode failed: " + ex.getMessage());
        }
    }

    record SavedEntry(AEItemKey definition, List<GenericStack> inputs, long remaining,
            CompoundTag raw, String reason) {
        static SavedEntry invalid(CompoundTag raw, String reason) {
            return invalid(List.of(), 0, raw, reason);
        }
        static SavedEntry invalid(List<GenericStack> inputs, CompoundTag raw, String reason) {
            return invalid(inputs, 0, raw, reason);
        }
        static SavedEntry invalid(List<GenericStack> inputs, long remaining, CompoundTag raw, String reason) {
            return new SavedEntry(null, inputs, remaining, raw, reason);
        }
        boolean valid() { return definition != null && reason == null; }
    }
}
