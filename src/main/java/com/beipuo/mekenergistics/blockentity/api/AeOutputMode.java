package com.beipuo.mekenergistics.blockentity.api;

import mekanism.common.lib.transmitter.TransmissionType;

public enum AeOutputMode {
    BOTH("AE: All", true, true),
    ITEMS("AE: Item", true, false),
    CHEMICALS("AE: Chem", false, true),
    NONE("AE: Off", false, false);

    private static final AeOutputMode[] VALUES = values();
    private final String label;
    private final boolean items;
    private final boolean chemicals;

    AeOutputMode(String label, boolean items, boolean chemicals) {
        this.label = label;
        this.items = items;
        this.chemicals = chemicals;
    }

    public String label() {
        return this.label;
    }

    public boolean items() {
        return this.items;
    }

    public boolean chemicals() {
        return this.chemicals;
    }

    public AeOutputMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public AeOutputMode toggle(TransmissionType type) {
        return switch (type) {
            case ITEM -> byFlags(!this.items, this.chemicals);
            case CHEMICAL -> byFlags(this.items, !this.chemicals);
            default -> this;
        };
    }

    private static AeOutputMode byFlags(boolean items, boolean chemicals) {
        for (AeOutputMode mode : VALUES) {
            if (mode.items == items && mode.chemicals == chemicals) {
                return mode;
            }
        }
        return BOTH;
    }

    public static AeOutputMode byId(int id) {
        return id < 0 || id >= VALUES.length ? BOTH : VALUES[id];
    }
}
