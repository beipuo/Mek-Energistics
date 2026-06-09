package com.beipuo.mekenergistics.blockentity.api;

public enum PatternMirrorRole {
    OFF,
    MASTER,
    SLAVE;

    public static PatternMirrorRole byId(int id) {
        PatternMirrorRole[] values = values();
        return id >= 0 && id < values.length ? values[id] : OFF;
    }

    public PatternMirrorRole next() {
        return byId((ordinal() + 1) % values().length);
    }
}
