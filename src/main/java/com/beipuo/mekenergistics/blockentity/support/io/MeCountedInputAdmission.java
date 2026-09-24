package com.beipuo.mekenergistics.blockentity.support.io;

import appeng.api.stacks.KeyCounter;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import org.jetbrains.annotations.Nullable;

/** One synchronous, one-copy counted input transaction. */
public final class MeCountedInputAdmission {
    private final KeyCounter[] prototype;
    private final long count;
    private final Function<KeyCounter[], Boolean> route;
    private boolean attempted;
    private boolean transferred;

    private MeCountedInputAdmission(KeyCounter[] prototype, long count,
            Function<KeyCounter[], Boolean> route) {
        this.prototype = prototype;
        this.count = count;
        this.route = route;
    }

    @Nullable
    public static MeCountedInputAdmission prepare(KeyCounter[] prototype, long requested,
            ToLongFunction<KeyCounter[]> capacity, Function<KeyCounter[], Boolean> route) {
        if (!validPrototype(prototype) || requested <= 0) {
            return null;
        }
        long count = Math.min(requested, Math.max(0, capacity.applyAsLong(prototype)));
        return count <= 0 || scale(prototype, count) == null
                ? null : new MeCountedInputAdmission(prototype, count, route);
    }

    public long count() {
        return count;
    }

    public boolean hasTransferredInputOwnership() {
        return transferred;
    }

    public boolean commit(KeyCounter[] deliveredPrototype) {
        if (deliveredPrototype != prototype) {
            throw new IllegalArgumentException("Admission must use its prepared prototype");
        }
        if (attempted) {
            throw new IllegalStateException("Admission has already been committed");
        }
        attempted = true;
        KeyCounter[] scaled = scale(prototype, count);
        if (scaled == null || !route.apply(scaled)) {
            return false;
        }
        transferred = true;
        return true;
    }

    public static boolean validPrototype(@Nullable KeyCounter[] prototype) {
        if (prototype == null || prototype.length == 0) {
            return false;
        }
        for (KeyCounter counter : prototype) {
            if (counter == null || counter.isEmpty()) {
                return false;
            }
            for (var entry : counter) {
                if (entry.getKey() == null || entry.getLongValue() <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    public static KeyCounter[] scale(KeyCounter[] prototype, long copies) {
        if (!validPrototype(prototype) || copies <= 0) {
            return null;
        }
        KeyCounter[] scaled = new KeyCounter[prototype.length];
        try {
            for (int i = 0; i < prototype.length; i++) {
                KeyCounter counter = new KeyCounter();
                for (var entry : prototype[i]) {
                    counter.add(entry.getKey(), Math.multiplyExact(entry.getLongValue(), copies));
                }
                scaled[i] = counter;
            }
            return scaled;
        } catch (ArithmeticException exception) {
            return null;
        }
    }
}
