package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.stacks.KeyCounter;

final class SmartPatternScheduler {
    static final int SCAN_BUDGET = 256;
    static final int HOT_BUDGET = 64;
    static final int FEED_BUDGET = 512;
    private int retryDelay;
    private int unchangedPasses;
    void wake() { retryDelay=0; unchangedPasses=0; }
    boolean delayed() { if(retryDelay<=0)return false; retryDelay--; return true; }
    void finish(boolean changed) { if(changed) wake(); else if(++unchangedPasses>1) retryDelay=Math.min(20,Math.max(2,retryDelay==0?2:retryDelay*2)); }
    static Result feed(SmartPatternRequest request, MeSmartPatternMultiplication.Feeder feeder, int budget) { boolean changed=false; int attempts=0; while(request.remaining()>0){long capacity=request.maxAcceptedBy(feeder);if(capacity<=0)return new Result(changed,attempts);long attempt=Math.min(request.nextBatchAttempt(),capacity);boolean fed=false;while(attempt>0&&attempts<budget){attempts++;if(feeder.feed(request.toKeyCounters(attempt))){request.remove(attempt);request.recordSuccessfulBatch(attempt);changed=true;fed=true;break;}request.recordFailedBatch(attempt);attempt/=2;}if(!fed)return new Result(changed,attempts);}return new Result(changed,attempts); }
    record Result(boolean changed,int feedAttempts) {}
}
