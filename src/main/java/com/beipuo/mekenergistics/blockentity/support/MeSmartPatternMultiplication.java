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
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;

public final class MeSmartPatternMultiplication {
    private static final String TAG_ENABLED = "SmartPatternMultiplication";
    private final SmartPatternQueue queue = new SmartPatternQueue();
    private final SmartPatternScheduler scheduler = new SmartPatternScheduler();
    private final MePendingPatternStore pendingStore = new MePendingPatternStore();
    private boolean enabled = MekEnergisticsConfig.smartPatternMultiplicationDefault();
    private final java.util.List<RefundBalance> refunds = new java.util.ArrayList<>();
    private record RefundBalance(AEKey key, java.math.BigInteger amount) {}

    public boolean requestRefund() {
        for (SmartPatternRequest request : List.copyOf(queue.entries())) {
            for (GenericStack input : request.inputs()) {
                refunds.add(new RefundBalance(input.what(), java.math.BigInteger.valueOf(input.amount())
                        .multiply(java.math.BigInteger.valueOf(request.remaining()))));
            }
        }
        boolean changed = queue.hasPending();
        queue.clear();
        return changed;
    }

    public boolean refundPending(java.util.function.ToLongFunction<GenericStack> insert) {
        boolean changed = false;
        for (int i = refunds.size() - 1; i >= 0; i--) {
            RefundBalance balance = refunds.get(i);
            long offered = balance.amount().min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
            long inserted = Math.clamp(insert.applyAsLong(new GenericStack(balance.key(), offered)), 0, offered);
            if (inserted == 0) continue;
            java.math.BigInteger left = balance.amount().subtract(java.math.BigInteger.valueOf(inserted));
            if (left.signum() == 0) refunds.remove(i);
            else refunds.set(i, new RefundBalance(balance.key(), left));
            changed = true;
        }
        return changed;
    }

    public boolean canRepresent(IPatternDetails details, KeyCounter[] holder) {
        return SmartPatternRequest.create(details, holder) != null;
    }

    public KeyCounter[] oneCraftInputs(IPatternDetails details, KeyCounter[] holder) {
        SmartPatternRequest request = SmartPatternRequest.create(details, holder);
        return request == null ? null : request.toKeyCounters(1);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean hasPendingWork() { return queue.hasPending() || !refunds.isEmpty(); }
    public boolean isRefunding() { return !refunds.isEmpty(); }
    public void wake() { scheduler.wake(); }

    public boolean enqueue(IPatternDetails details, KeyCounter[] holder) {
        if (!enabled || isRefunding()) return false;
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
        if (isRefunding() || scheduler.delayed()) return false;
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
        net.minecraft.nbt.ListTag balances = new net.minecraft.nbt.ListTag();
        for (RefundBalance balance : refunds) {
            CompoundTag saved = new CompoundTag();
            saved.put("Key", GenericStack.writeTag(registries, new GenericStack(balance.key(), 1)));
            saved.putString("Amount", balance.amount().toString());
            balances.add(saved);
        }
        tag.put("SmartPatternRefunds", balances);
    }

    public void loadPending(CompoundTag tag, HolderLookup.Provider registries) {
        queue.clear();
        refunds.clear();
        pendingStore.loadQuarantined(tag);
        net.minecraft.nbt.ListTag balances = tag.getList("SmartPatternRefunds", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < balances.size(); i++) {
            CompoundTag saved = balances.getCompound(i);
            try {
                GenericStack key = GenericStack.readTag(registries, saved.getCompound("Key"));
                java.math.BigInteger amount = new java.math.BigInteger(saved.getString("Amount"));
                if (key == null || amount.signum() <= 0) throw new IllegalArgumentException("Invalid refund balance");
                refunds.add(new RefundBalance(key.what(), amount));
            } catch (RuntimeException ex) {
                pendingStore.quarantine(i, "Invalid refund balance", saved);
            }
        }
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
            pendingStore.quarantine(i, entry.reason(), entry.raw());
        }
    }
    private void enqueueLoaded(SmartPatternRequest request) { SmartPatternRequest existing=queue.find(request.key()); if(existing!=null && existing.merge(request)) return; queue.add(request); }
    public int quarantinedPendingCount() { return pendingStore.quarantinedCount(); }

    public interface Feeder { boolean feed(KeyCounter[] oneCraftInputs); default Iterable<AEKey> activeInputKeys(){return List.of();} }
    public interface CapacityAwareFeeder extends Feeder { long maxAcceptedCopies(KeyCounter[] oneCraftInputs); }
}
