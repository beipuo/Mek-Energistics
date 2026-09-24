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
import org.jetbrains.annotations.Nullable;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;

public final class MeSmartPatternMultiplication {
    private static final String TAG_ENABLED = "SmartPatternMultiplication";
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
        SmartPatternPersistence.save(tag, registries, queue.entries());
        pendingStore.saveQuarantined(tag);
    }

    public void loadPending(CompoundTag tag, HolderLookup.Provider registries) { loadPending(tag, registries, null); }
    public void loadPending(CompoundTag tag, HolderLookup.Provider registries,
            @Nullable MePendingPatternStore.PendingBalanceRefund refund) {
        queue.clear();
        pendingStore.loadQuarantined(tag);
        List<SmartPatternPersistence.SavedEntry> entries = SmartPatternPersistence.load(tag, registries);
        for (int i = 0; i < entries.size(); i++) {
            SmartPatternPersistence.SavedEntry entry = entries.get(i);
            if (entry.valid()) {
                enqueueLoaded(new SmartPatternRequest(entry.definition(), entry.inputs(), entry.remaining()));
                continue;
            }
            MePendingPatternStore.logDroppedPending(i, entry.reason());
            if (entry.remaining() <= 0) {
                continue;
            }
            long balance = MePendingPatternStore.refundableBalance(entry.inputs(), entry.remaining());
            if (balance > 0 && refund != null && refund.refund(entry.inputs(), entry.remaining()) >= balance) {
                continue;
            }
            pendingStore.quarantine(i, entry.reason(), entry.raw());
        }
    }
    private void enqueueLoaded(SmartPatternRequest request) { SmartPatternRequest existing=queue.find(request.key()); if(existing!=null && existing.merge(request)) return; queue.add(request); }
    public int quarantinedPendingCount() { return pendingStore.quarantinedCount(); }

    public interface Feeder { boolean feed(KeyCounter[] oneCraftInputs); default Iterable<AEKey> activeInputKeys(){return List.of();} }
    public interface CapacityAwareFeeder extends Feeder { long maxAcceptedCopies(KeyCounter[] oneCraftInputs); }
}
