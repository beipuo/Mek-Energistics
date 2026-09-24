package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.stacks.AEKey;
import java.util.*;

final class SmartPatternQueue {
    private final List<SmartPatternRequest> entries = new ArrayList<>();
    private final Set<SmartPatternRequest> live = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<SmartPatternRequestKey, SmartPatternRequest> byKey = new HashMap<>();
    private final Map<AEKey, Set<SmartPatternRequest>> byInput = new HashMap<>();
    private final List<SmartPatternRequest> hot = new ArrayList<>();
    private int cursor;
    List<SmartPatternRequest> entries() { return entries; }
    List<SmartPatternRequest> hotEntries() { return hot; }
    boolean hasPending() { return !entries.isEmpty(); }
    int size() { return entries.size(); }
    SmartPatternRequest find(SmartPatternRequestKey key) { return byKey.get(key); }
    boolean contains(SmartPatternRequest request) { return live.contains(request); }
    void add(SmartPatternRequest request) { entries.add(request); live.add(request); byKey.put(request.key(), request); for (AEKey key:request.inputKeys()) byInput.computeIfAbsent(key,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(request); }
    Set<SmartPatternRequest> matching(AEKey key) { return byInput.get(key); }
    int cursor() { return cursor; }
    void clamp() { if (cursor < 0 || cursor >= entries.size()) cursor=0; }
    void advance() { if (entries.isEmpty()) cursor=0; else cursor=(cursor+1)%entries.size(); }
    SmartPatternRequest current() { return entries.get(cursor); }
    void rememberHot(SmartPatternRequest r) { forgetHot(r); hot.add(0,r); if (hot.size()>64) hot.remove(hot.size()-1); }
    void forgetHot(SmartPatternRequest r) { hot.removeIf(candidate -> candidate == r); }
    void removeCurrent() { remove(current()); }
    void remove(SmartPatternRequest removed) { int index=entries.indexOf(removed); if(index<0){forgetHot(removed);return;} entries.remove(index); live.remove(removed); for(AEKey key:removed.inputKeys()){Set<SmartPatternRequest> set=byInput.get(key); if(set!=null){set.remove(removed);if(set.isEmpty())byInput.remove(key);}} if(index<cursor)cursor--; if(byKey.remove(removed.key(),removed)){for(SmartPatternRequest r:entries)if(r.key().equals(removed.key())){byKey.put(r.key(),r);break;}} forgetHot(removed); clamp(); }
    void clear() { entries.clear(); live.clear(); byKey.clear(); byInput.clear(); hot.clear(); cursor=0; }
}
