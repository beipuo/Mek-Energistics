package com.beipuo.mekenergistics.common;

import mekanism.api.text.ILangEntry;
import org.jetbrains.annotations.NotNull;

public record MeLangEntry(String getTranslationKey) implements ILangEntry {
    @NotNull
    public static MeLangEntry of(String translationKey) {
        return new MeLangEntry(translationKey);
    }
}
