package com.beipuo.mekenergistics.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Every shipped locale must translate exactly the keys en_us defines, with the same format
 * arguments. Minecraft silently falls back to English for a missing key, so a locale drifting
 * behind en_us (ru_ru once lacked 152 keys) shows up only as half-translated screens in game.
 * The config screen keys are checked too: a key whose config option was renamed renders raw.
 */
class LocaleParityTest {
    private static final Path LANG = Path.of("src/main/resources/assets/mekenergistics/lang");
    private static final List<String> LOCALES = List.of("zh_cn", "ru_ru");
    private static final Pattern FORMAT_ARG = Pattern.compile("%(?:\\d+\\$)?[sd]");
    private static final Path CONFIG = Path.of(
            "src/main/java/com/beipuo/mekenergistics/config/MekEnergisticsConfig.java");

    @Test
    void everyLocaleHasExactlyTheEnglishKeys() throws IOException {
        JsonObject english = read("en_us");
        for (String locale : LOCALES) {
            JsonObject translated = read(locale);
            Set<String> missing = new TreeSet<>(english.keySet());
            missing.removeAll(translated.keySet());
            Set<String> extra = new TreeSet<>(translated.keySet());
            extra.removeAll(english.keySet());
            assertTrue(missing.isEmpty(), () -> locale + " is missing keys: " + missing);
            assertTrue(extra.isEmpty(), () -> locale + " has keys en_us does not define: " + extra);
        }
    }

    @Test
    void translationsKeepTheEnglishFormatArguments() throws IOException {
        JsonObject english = read("en_us");
        for (String locale : LOCALES) {
            JsonObject translated = read(locale);
            List<String> mismatched = new ArrayList<>();
            for (String key : english.keySet()) {
                if (translated.has(key) && formatArgs(english.get(key).getAsString())
                        != formatArgs(translated.get(key).getAsString())) {
                    mismatched.add(key);
                }
            }
            assertTrue(mismatched.isEmpty(), () -> locale + " changes format arguments of " + mismatched);
        }
    }

    @Test
    void everyConfigOptionHasAConfigScreenTranslation() throws IOException {
        String source = Files.readString(CONFIG);
        Matcher option = Pattern.compile("\\.(?:define|defineInRange)\\(\"([A-Za-z]+)\"").matcher(source);
        List<String> options = new ArrayList<>();
        while (option.find()) {
            options.add(option.group(1));
        }
        assertTrue(options.size() >= 5, "Found too few config options -- this check has gone vacuous");
        for (String locale : List.of("en_us", "zh_cn", "ru_ru")) {
            JsonObject lang = read(locale);
            for (String name : options) {
                String key = "mekenergistics.configuration." + name;
                assertTrue(lang.has(key), () -> locale + " has no config screen label " + key);
                assertTrue(lang.has(key + ".tooltip"), () -> locale + " has no config screen tooltip " + key);
            }
        }
    }

    private static int formatArgs(String value) {
        Matcher matcher = FORMAT_ARG.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static JsonObject read(String locale) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(LANG.resolve(locale + ".json"))).getAsJsonObject();
        assertEquals(false, json.isEmpty(), locale + " is empty");
        return json;
    }
}
