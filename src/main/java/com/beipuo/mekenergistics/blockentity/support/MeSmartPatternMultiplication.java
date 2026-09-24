package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;

public final class MeSmartPatternMultiplication {
    private static final String TAG_ENABLED = "SmartPatternMultiplication";
    private static final String TAG_PENDING = "SmartPatternMultiplicationPending";
    private static final String TAG_REMAINING = "Remaining";
    private static final String TAG_DEFINITION = "Definition";
    private static final String TAG_INPUTS = "Inputs";
    private static final String TAG_INPUT = "Input";
    private final SmartPatternQueue queue = new SmartPatternQueue();
    private final SmartPatternScheduler scheduler = new SmartPatternScheduler();
    private final MePendingPatternStore pendingStore = new MePendingPatternStore();
    private boolean enabled = MekEnergisticsConfig.smartPatternMultiplicationDefault();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean hasPendingWork() { return queue.hasPending(); }
    public void wake() { scheduler.wake(); }

    public boolean enqueue(IPatternDetails details, KeyCounter[] holder) {
        if (!enabled) return false;
        SmartPatternRequest request = SmartPatternRequest.create(details, holder);
        return request != null && enqueueRequest(request);
    }

    boolean enqueueForTesting(AEKey definition, List<GenericStack> inputs, long copies) {
        return definition != null && inputs != null && !inputs.isEmpty() && copies > 0
                && enqueueRequest(new SmartPatternRequest(definition, inputs, copies));
    }

    private boolean enqueueRequest(SmartPatternRequest request) {
        SmartPatternRequest existing = queue.find(request.key());
        if (existing != null) {
            boolean merged = existing.merge(request);
            if (merged) wake();
            return merged;
        }
        queue.add(request);
        wake();
        return true;
    }

    public boolean processNext(Feeder feeder) {
        if (scheduler.delayed()) return false;
        boolean changed = false;
        int feeds = 0;
        queue.clamp();
        Set<SmartPatternRequest> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AEKey key : feeder.activeInputKeys()) {
            Set<SmartPatternRequest> matching = queue.matching(key);
            if (matching != null) for (SmartPatternRequest request : List.copyOf(matching)) if (queue.contains(request)) queue.rememberHot(request);
        }
        int hotVisited = 0;
        for (SmartPatternRequest request : List.copyOf(queue.hotEntries())) {
            if (hotVisited >= SmartPatternScheduler.HOT_BUDGET || feeds >= SmartPatternScheduler.FEED_BUDGET) break;
            if (!queue.contains(request)) { queue.forgetHot(request); continue; }
            hotVisited++; visited.add(request);
            if (request.remaining() <= 0) { queue.remove(request); changed = true; continue; }
            SmartPatternScheduler.Result result = SmartPatternScheduler.feed(request, feeder, SmartPatternScheduler.FEED_BUDGET - feeds);
            feeds += result.feedAttempts(); changed |= result.changed();
            if (request.remaining() <= 0) queue.remove(request); else if (result.changed()) queue.rememberHot(request);
        }
        int scanned = 0, steps = 0, maxSteps = queue.size();
        while (queue.hasPending() && scanned < SmartPatternScheduler.SCAN_BUDGET && steps++ < maxSteps && feeds < SmartPatternScheduler.FEED_BUDGET) {
            SmartPatternRequest request = queue.current();
            if (!visited.add(request)) { queue.advance(); continue; }
            scanned++;
            if (request.remaining() <= 0) { queue.removeCurrent(); changed = true; continue; }
            SmartPatternScheduler.Result result = SmartPatternScheduler.feed(request, feeder, SmartPatternScheduler.FEED_BUDGET - feeds);
            feeds += result.feedAttempts(); changed |= result.changed();
            if (request.remaining() <= 0) queue.removeCurrent(); else { if (result.changed()) queue.rememberHot(request); queue.advance(); }
        }
        scheduler.finish(changed);
        return changed;
    }

    public void saveConfig(CompoundTag tag) { tag.putBoolean(TAG_ENABLED, enabled); }
    public void loadConfig(CompoundTag tag) { if (tag.contains(TAG_ENABLED)) enabled = tag.getBoolean(TAG_ENABLED); }

    public void savePending(CompoundTag tag, HolderLookup.Provider registries) {
        if (!queue.hasPending() && pendingStore.quarantinedCount() == 0) { tag.remove(TAG_PENDING); pendingStore.saveQuarantined(tag); return; }
        if (!queue.hasPending()) tag.remove(TAG_PENDING); else {
            ListTag list = new ListTag();
            for (SmartPatternRequest request : queue.entries()) {
                CompoundTag saved = new CompoundTag(); saved.putLong(TAG_REMAINING, request.remaining());
                saved.put(TAG_DEFINITION, GenericStack.writeTag(registries, new GenericStack(request.definition(), 1)));
                ListTag inputs = new ListTag();
                for (GenericStack input : request.inputs()) { CompoundTag item = new CompoundTag(); item.put(TAG_INPUT, GenericStack.writeTag(registries, input)); inputs.add(item); }
                saved.put(TAG_INPUTS, inputs); list.add(saved);
            }
            tag.put(TAG_PENDING, list);
        }
        pendingStore.saveQuarantined(tag);
    }

    public void loadPending(CompoundTag tag, HolderLookup.Provider registries) { loadPending(tag, registries, null); }
    public void loadPending(CompoundTag tag, HolderLookup.Provider registries, @Nullable MePendingPatternStore.PendingBalanceRefund refund) {
        queue.clear(); pendingStore.loadQuarantined(tag);
        ListTag list = tag.getList(TAG_PENDING, CompoundTag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            CompoundTag saved=list.getCompound(i); long remaining=saved.getLong(TAG_REMAINING);
            if (remaining<=0) { MePendingPatternStore.logDroppedPending(i, "non-positive remaining " + remaining); continue; }
            String reason=null; List<GenericStack> inputs=List.of();
            try {
                ListTag inputTags=saved.getList(TAG_INPUTS, CompoundTag.TAG_COMPOUND);
                inputs=inputTags.isEmpty()?List.of():MePendingPatternStore.decodeInputs(registries,inputTags);
                GenericStack definition=GenericStack.readTag(registries,saved.getCompound(TAG_DEFINITION));
                if(definition==null) reason="undecodable definition";
                else if(!(definition.what() instanceof appeng.api.stacks.AEItemKey key)) reason="definition is not an item key: " + definition.what();
                else if(inputTags.isEmpty()) reason="no inputs listed";
                else if(inputs.isEmpty()) reason="no usable inputs";
                else if(inputs.size()<inputTags.size()) reason="only " + inputs.size() + " of " + inputTags.size() + " inputs decoded";
                else { enqueueLoaded(new SmartPatternRequest(key, inputs, remaining)); continue; }
            } catch(RuntimeException ex) { reason="decode failed: " + ex.getMessage(); }
            MePendingPatternStore.logDroppedPending(i, reason); long balance=MePendingPatternStore.refundableBalance(inputs,remaining);
            if(balance>0 && refund!=null && refund.refund(inputs,remaining)>=balance) continue;
            pendingStore.quarantine(i,reason,saved);
        }
    }
    private void enqueueLoaded(SmartPatternRequest request) { SmartPatternRequest existing=queue.find(request.key()); if(existing!=null && existing.merge(request)) return; queue.add(request); }
    public int quarantinedPendingCount() { return pendingStore.quarantinedCount(); }

    public interface Feeder { boolean feed(KeyCounter[] oneCraftInputs); default Iterable<AEKey> activeInputKeys(){return List.of();} }
    public interface CapacityAwareFeeder extends Feeder { long maxAcceptedCopies(KeyCounter[] oneCraftInputs); }
}
