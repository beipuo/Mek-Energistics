package com.beipuo.mekenergistics.blockentity.support.io;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.api.Action;
import me.ramidzkh.mekae2.ae2.MekanismKey;

public final class MePatternInputRouter {
    /** A malformed port must never be able to monopolize the server thread. */
    private static final int MAX_ASSIGNMENT_STEPS = 256;
    /**
     * Bounds the total backtracking work of one assignment. {@link #MAX_ASSIGNMENT_STEPS} only
     * limits search depth; without a call budget a multi-key request over many ports could still
     * explore a combinatorial number of partial plans in one tick.
     */
    private static final int MAX_ASSIGNMENT_CALLS = 4096;

    private MePatternInputRouter() {
    }

    public static boolean route(KeyCounter[] inputHolders, List<? extends MeInputPort> ports) {
        Map<AEKey, Long> requests = normalize(inputHolders);
        if (requests == null || requests.isEmpty() || ports.isEmpty()) {
            return false;
        }

        List<Map.Entry<AEKey, Long>> requestList = new ArrayList<>(requests.entrySet());
        List<Insertion> plan = new ArrayList<>();
        boolean assigned = requestList.size() == 1
                ? assignSingle(requestList.get(0), ports, plan,
                        new java.util.IdentityHashMap<>(), new java.util.IdentityHashMap<>())
                : assign(requestList, 0, requestList.get(0).getValue(),
                        ports, plan, 0, new java.util.IdentityHashMap<>(), new java.util.IdentityHashMap<>());
        if (!assigned) {
            return false;
        }
        return executePlan(plan);
    }

    /**
     * Routes one AE counter per physical lane as one transaction. A port can
     * appear in more than one lane, but its simulated reservations are shared
     * across the complete assignment and all lanes roll back together.
     */
    public static boolean routeLanes(KeyCounter[] laneInputs,
            List<? extends List<? extends MeInputPort>> lanePorts) {
        if (laneInputs == null || lanePorts == null || laneInputs.length == 0
                || laneInputs.length != lanePorts.size()) {
            return false;
        }

        List<Insertion> plan = new ArrayList<>();
        for (List<? extends MeInputPort> ports : lanePorts) {
            if (ports == null || ports.isEmpty()) {
                return false;
            }
            for (MeInputPort port : ports) {
                if (port == null) {
                    return false;
                }
            }
        }

        boolean assigned = assignLanes(laneInputs, lanePorts, 0, plan,
                new java.util.IdentityHashMap<>(), new java.util.IdentityHashMap<>());
        return assigned && executePlan(plan);
    }

    /** Returns the largest whole-copy batch that can be simulated by the ports. */
    public static long maxAcceptedCopies(KeyCounter[] oneCraftInputs,
            List<? extends MeInputPort> ports) {
        if (oneCraftInputs == null || ports == null || ports.isEmpty()) {
            return 0;
        }
        Map<AEKey, Long> requests = normalize(oneCraftInputs);
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        if (requests.size() == 1) {
            Map.Entry<AEKey, Long> request = requests.entrySet().iterator().next();
            long capacity = 0;
            long probeAmount = request.getKey() instanceof MekanismKey ? Long.MAX_VALUE : Integer.MAX_VALUE;
            for (MeInputPort port : ports) {
                if (port.supports(request.getKey())) {
                    long accepted = Math.max(0, port.insert(request.getKey(), probeAmount, Action.SIMULATE));
                    capacity = accepted > Long.MAX_VALUE - capacity ? Long.MAX_VALUE : capacity + accepted;
                }
            }
            return capacity / request.getValue();
        }
        if (!canRoute(oneCraftInputs, ports)) {
            return 0;
        }
        long low = 1;
        long high = 2;
        while (high > low && high < Long.MAX_VALUE
                && canRoute(scale(oneCraftInputs, high), ports)) {
            low = high;
            high = high > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : high * 2;
        }
        if (high == Long.MAX_VALUE && canRoute(scale(oneCraftInputs, high), ports)) {
            return high;
        }
        while (low + 1 < high) {
            long middle = low + (high - low) / 2;
            if (canRoute(scale(oneCraftInputs, middle), ports)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /** Returns the largest batch accepted by position-sensitive lanes. */
    public static long maxAcceptedLaneCopies(KeyCounter[] oneCraftInputs,
            List<? extends List<? extends MeInputPort>> lanePorts) {
        if (oneCraftInputs == null || lanePorts == null || lanePorts.isEmpty()
                || !canRouteLanes(oneCraftInputs, lanePorts)) {
            return 0;
        }
        long low = 1;
        long high = 2;
        while (high > low && high < Long.MAX_VALUE
                && canRouteLanes(scale(oneCraftInputs, high), lanePorts)) {
            low = high;
            high = high > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : high * 2;
        }
        if (high == Long.MAX_VALUE && canRouteLanes(scale(oneCraftInputs, high), lanePorts)) {
            return high;
        }
        while (low + 1 < high) {
            long middle = low + (high - low) / 2;
            if (canRouteLanes(scale(oneCraftInputs, middle), lanePorts)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static boolean assign(List<Map.Entry<AEKey, Long>> requests, int index, long remaining,
            List<? extends MeInputPort> ports, List<Insertion> plan,
            int requestPlanStart,
            Map<MeInputPort, AEKey> reservedKeys, Map<MeInputPort, Long> reservedAmounts) {
        return assign(requests, index, remaining, ports, plan, requestPlanStart, reservedKeys, reservedAmounts,
                new int[] {MAX_ASSIGNMENT_CALLS});
    }

    private static boolean assign(List<Map.Entry<AEKey, Long>> requests, int index, long remaining,
            List<? extends MeInputPort> ports, List<Insertion> plan,
            int requestPlanStart,
            Map<MeInputPort, AEKey> reservedKeys, Map<MeInputPort, Long> reservedAmounts, int[] budget) {
        if (plan.size() >= MAX_ASSIGNMENT_STEPS || --budget[0] < 0) {
            return false;
        }
        if (index >= requests.size()) {
            return true;
        }
        Map.Entry<AEKey, Long> request = requests.get(index);
        if (remaining <= 0) {
            return assign(requests, index + 1,
                    index + 1 < requests.size() ? requests.get(index + 1).getValue() : 0,
                    ports, plan, plan.size(), reservedKeys, reservedAmounts, budget);
        }
        for (MeInputPort port : ports) {
            boolean usedInRequest = false;
            for (int i = requestPlanStart; i < plan.size(); i++) {
                if (plan.get(i).port() == port) {
                    usedInRequest = true;
                    break;
                }
            }
            if (usedInRequest) {
                continue;
            }
            AEKey reservedKey = reservedKeys.get(port);
            if (!port.supports(request.getKey()) || reservedKey != null && !reservedKey.equals(request.getKey())) {
                continue;
            }
            long alreadyReserved = reservedAmounts.getOrDefault(port, 0L);
            long available = simulateAdditionalCapacity(
                    port, request.getKey(), remaining, alreadyReserved);
            if (available <= 0) {
                continue;
            }
            long accepted = Math.min(remaining, available);
            if (accepted <= 0 || alreadyReserved > Long.MAX_VALUE - accepted) {
                continue;
            }
            int planSize = plan.size();
            plan.add(new Insertion(port, request.getKey(), accepted));
            reservedKeys.put(port, request.getKey());
            reservedAmounts.put(port, alreadyReserved + accepted);
            if (assign(requests, index, remaining - accepted, ports, plan, requestPlanStart,
                    reservedKeys, reservedAmounts, budget)) {
                return true;
            }
            while (plan.size() > planSize) {
                plan.remove(plan.size() - 1);
            }
            if (alreadyReserved == 0) {
                reservedKeys.remove(port);
                reservedAmounts.remove(port);
            } else {
                reservedAmounts.put(port, alreadyReserved);
            }
        }
        return false;
    }

    /** Fast path for the overwhelmingly common single-key request, including large factories. */
    private static boolean assignSingle(Map.Entry<AEKey, Long> request,
            List<? extends MeInputPort> ports, List<Insertion> plan,
            Map<MeInputPort, AEKey> reservedKeys, Map<MeInputPort, Long> reservedAmounts) {
        long remaining = request.getValue();
        for (MeInputPort port : ports) {
            AEKey reservedKey = reservedKeys.get(port);
            if (!port.supports(request.getKey()) || reservedKey != null && !reservedKey.equals(request.getKey())) {
                continue;
            }
            long alreadyReserved = reservedAmounts.getOrDefault(port, 0L);
            long available = simulateAdditionalCapacity(
                    port, request.getKey(), remaining, alreadyReserved);
            long accepted = Math.min(remaining, available);
            if (accepted <= 0 || alreadyReserved > Long.MAX_VALUE - accepted) {
                continue;
            }
            plan.add(new Insertion(port, request.getKey(), accepted));
            reservedKeys.put(port, request.getKey());
            reservedAmounts.put(port, alreadyReserved + accepted);
            remaining -= accepted;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean assignLanes(KeyCounter[] laneInputs,
            List<? extends List<? extends MeInputPort>> lanePorts, int lane,
            List<Insertion> plan, Map<MeInputPort, AEKey> reservedKeys,
            Map<MeInputPort, Long> reservedAmounts) {
        if (plan.size() >= MAX_ASSIGNMENT_STEPS) {
            return false;
        }
        if (lane >= laneInputs.length) {
            return true;
        }
        Map<AEKey, Long> requests = normalize(new KeyCounter[] {laneInputs[lane]});
        if (requests == null || requests.size() != 1) {
            return false;
        }
        Map.Entry<AEKey, Long> request = requests.entrySet().iterator().next();
        return assignLanePorts(laneInputs, lanePorts, lane, request.getKey(), request.getValue(),
                lanePorts.get(lane), 0, plan, reservedKeys, reservedAmounts);
    }

    private static boolean assignLanePorts(KeyCounter[] laneInputs,
            List<? extends List<? extends MeInputPort>> lanePorts, int lane, AEKey key,
            long remaining, List<? extends MeInputPort> ports, int portIndex,
            List<Insertion> plan, Map<MeInputPort, AEKey> reservedKeys,
            Map<MeInputPort, Long> reservedAmounts) {
        if (remaining <= 0) {
            return assignLanes(laneInputs, lanePorts, lane + 1, plan, reservedKeys, reservedAmounts);
        }
        if (portIndex >= ports.size() || plan.size() >= MAX_ASSIGNMENT_STEPS) {
            return false;
        }

        MeInputPort port = ports.get(portIndex);
        AEKey reservedKey = reservedKeys.get(port);
        if (port.supports(key) && (reservedKey == null || reservedKey.equals(key))) {
            long alreadyReserved = reservedAmounts.getOrDefault(port, 0L);
            long available = simulateAdditionalCapacity(port, key, remaining, alreadyReserved);
            long maximum = Math.min(remaining, available);
            if (maximum > 0) {
                long capacityAfter = remainingLaneCapacity(
                        key, remaining, ports, portIndex + 1, reservedKeys, reservedAmounts);
                long minimum = Math.max(0L, remaining - capacityAfter);
                if (tryLaneAllocation(laneInputs, lanePorts, lane, key, remaining, ports,
                        portIndex, plan, reservedKeys, reservedAmounts, port, alreadyReserved, maximum)) {
                    return true;
                }
                if (minimum > 0 && minimum < maximum
                        && tryLaneAllocation(laneInputs, lanePorts, lane, key, remaining, ports,
                                portIndex, plan, reservedKeys, reservedAmounts, port, alreadyReserved, minimum)) {
                    return true;
                }
            }
        }

        return assignLanePorts(laneInputs, lanePorts, lane, key, remaining, ports,
                portIndex + 1, plan, reservedKeys, reservedAmounts);
    }

    private static boolean tryLaneAllocation(KeyCounter[] laneInputs,
            List<? extends List<? extends MeInputPort>> lanePorts, int lane, AEKey key,
            long remaining, List<? extends MeInputPort> ports, int portIndex,
            List<Insertion> plan, Map<MeInputPort, AEKey> reservedKeys,
            Map<MeInputPort, Long> reservedAmounts, MeInputPort port,
            long alreadyReserved, long amount) {
        int planSize = plan.size();
        plan.add(new Insertion(port, key, amount));
        reservedKeys.put(port, key);
        reservedAmounts.put(port, alreadyReserved + amount);
        if (assignLanePorts(laneInputs, lanePorts, lane, key, remaining - amount, ports,
                portIndex + 1, plan, reservedKeys, reservedAmounts)) {
            return true;
        }
        while (plan.size() > planSize) {
            plan.remove(plan.size() - 1);
        }
        if (alreadyReserved == 0) {
            reservedKeys.remove(port);
            reservedAmounts.remove(port);
        } else {
            reservedAmounts.put(port, alreadyReserved);
        }
        return false;
    }

    private static long remainingLaneCapacity(AEKey key, long requested,
            List<? extends MeInputPort> ports, int start,
            Map<MeInputPort, AEKey> reservedKeys, Map<MeInputPort, Long> reservedAmounts) {
        long capacity = 0;
        for (int i = start; i < ports.size() && capacity < requested; i++) {
            MeInputPort port = ports.get(i);
            AEKey reservedKey = reservedKeys.get(port);
            if (!port.supports(key) || reservedKey != null && !reservedKey.equals(key)) {
                continue;
            }
            long available = simulateAdditionalCapacity(
                    port, key, requested - capacity, reservedAmounts.getOrDefault(port, 0L));
            capacity = available > Long.MAX_VALUE - capacity ? Long.MAX_VALUE : capacity + available;
        }
        return capacity;
    }

    private static long simulateAdditionalCapacity(MeInputPort port, AEKey key,
            long requested, long alreadyReserved) {
        long numericLimit = key instanceof AEItemKey || key instanceof AEFluidKey
                ? Integer.MAX_VALUE : Long.MAX_VALUE;
        if (requested <= 0 || alreadyReserved < 0 || alreadyReserved >= numericLimit) {
            return 0;
        }
        long offered = requested > numericLimit - alreadyReserved
                ? numericLimit : requested + alreadyReserved;
        long acceptedIncludingReservations = port.insert(key, offered, Action.SIMULATE);
        return Math.max(0L, acceptedIncludingReservations - alreadyReserved);
    }

    private static Map<AEKey, Long> normalize(KeyCounter[] holders) {
        if (holders == null || holders.length == 0) {
            return null;
        }
        Map<AEKey, Long> result = new LinkedHashMap<>();
        try {
            for (KeyCounter holder : holders) {
                // One physical lane must resolve to one AE key. Alternatives belong in
                // separate lanes/ports and must not be merged into one transaction request.
                if (holder == null || holder.isEmpty() || holder.size() != 1) {
                    return null;
                }
                for (var entry : holder) {
                    if (entry.getKey() == null || entry.getLongValue() <= 0) {
                        return null;
                    }
                    result.merge(entry.getKey(), entry.getLongValue(), Math::addExact);
                }
            }
        } catch (ArithmeticException ignored) {
            return null;
        }
        return result;
    }

    private static boolean canRoute(KeyCounter[] inputs, List<? extends MeInputPort> ports) {
        Map<AEKey, Long> requests = normalize(inputs);
        if (requests == null || requests.isEmpty()) {
            return false;
        }
        List<Insertion> plan = new ArrayList<>();
        List<Map.Entry<AEKey, Long>> requestList = new ArrayList<>(requests.entrySet());
        boolean accepted = requestList.size() == 1
                ? assignSingle(requestList.get(0), ports, plan,
                        new java.util.IdentityHashMap<>(), new java.util.IdentityHashMap<>())
                : assign(requestList, 0, requestList.get(0).getValue(), ports, plan, 0,
                        new java.util.IdentityHashMap<>(), new java.util.IdentityHashMap<>());
        return accepted;
    }

    private static boolean canRouteLanes(KeyCounter[] inputs,
            List<? extends List<? extends MeInputPort>> lanePorts) {
        if (inputs == null || lanePorts == null || inputs.length == 0 || inputs.length != lanePorts.size()
                || lanePorts.stream().anyMatch(ports -> ports == null || ports.isEmpty()
                        || ports.stream().anyMatch(java.util.Objects::isNull))) {
            return false;
        }
        List<Insertion> plan = new ArrayList<>();
        return assignLanes(inputs, lanePorts, 0, plan,
                new java.util.IdentityHashMap<>(), new java.util.IdentityHashMap<>());
    }

    private static KeyCounter[] scale(KeyCounter[] inputs, long copies) {
        KeyCounter[] scaled = new KeyCounter[inputs.length];
        try {
            for (int i = 0; i < inputs.length; i++) {
                KeyCounter counter = new KeyCounter();
                if (inputs[i] == null) {
                    return new KeyCounter[inputs.length];
                }
                for (var entry : inputs[i]) {
                    counter.add(entry.getKey(), Math.multiplyExact(entry.getLongValue(), copies));
                }
                scaled[i] = counter;
            }
            return scaled;
        } catch (ArithmeticException ex) {
            return new KeyCounter[inputs.length];
        }
    }

    /**
     * Executes a fully simulated plan, rolling every touched port back if any real insert falls
     * short. Snapshots are taken lazily, right before the first real insert into each port:
     * planning only simulates, so that is the same state an up-front snapshot would capture, but
     * ports the plan never touches are no longer copied (the smart scheduler routes up to hundreds
     * of times per tick). Ports may wrap overlapping slots (a grouped port and one of its members),
     * so rollback restores in reverse order and the earliest, untouched snapshot is applied last.
     */
    private static boolean executePlan(List<Insertion> plan) {
        Set<MeInputPort> snapshotted = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        List<MeInputPort> touchedPorts = new ArrayList<>();
        List<Object> touchedSnapshots = new ArrayList<>();
        for (Insertion insertion : plan) {
            MeInputPort port = insertion.port();
            if (snapshotted.add(port)) {
                touchedPorts.add(port);
                touchedSnapshots.add(port.snapshot());
            }
            if (port.insert(insertion.key(), insertion.amount(), Action.EXECUTE) != insertion.amount()) {
                for (int i = touchedPorts.size() - 1; i >= 0; i--) {
                    touchedPorts.get(i).restore(touchedSnapshots.get(i));
                }
                return false;
            }
        }
        return true;
    }

    private record Insertion(MeInputPort port, AEKey key, long amount) {
    }
}
