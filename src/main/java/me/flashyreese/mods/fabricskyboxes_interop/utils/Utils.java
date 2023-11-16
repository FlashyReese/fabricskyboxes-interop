package me.flashyreese.mods.fabricskyboxes_interop.utils;

import io.github.amerebagatelle.fabricskyboxes.util.object.MinMaxEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Utils {
    private static final Pattern OPTIFINE_RANGE_SEPARATOR = Pattern.compile("(\\d|\\))-(\\d|\\()");

    public static Number toTickTime(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2)
            return null;
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        return h * 1000 + (m / 0.06F) - 6000;
    }

    public static int normalizeTickTime(int tickTime) {
        int result = tickTime % 24000;
        if (result < 0) {
            result += 24000;
        }
        return result;
    }

    public static List<MinMaxEntry> parseMinMaxEntries(String str) {
        List<MinMaxEntry> minMaxEntries = new ArrayList<>();
        String[] strings = str.split(" ,");

        for (String s : strings) {
            MinMaxEntry minMaxEntry = parseMinMaxEntry(s);

            if (minMaxEntry != null) {
                minMaxEntries.add(minMaxEntry);
            }
        }

        return minMaxEntries;
    }

    private static MinMaxEntry parseMinMaxEntry(String str) {
        if (str != null) {
            if (str.contains("-")) {
                String[] strings = str.split("-");
                if (strings.length == 2) {
                    int min = parseInt(strings[0], -1);
                    int max = parseInt(strings[1], -1);
                    if (min >= 0 && max >= 0) {
                        return new MinMaxEntry(min, max);
                    }
                }
            } else {
                int value = parseInt(str, -1);

                if (value >= 0) {
                    return new MinMaxEntry(value, value);
                }
            }
        }

        return null;
    }

    public static List<MinMaxEntry> parseMinMaxEntriesNegative(String str) {
        List<MinMaxEntry> minMaxEntries = new ArrayList<>();
        String[] strings = str.split(" ,");

        for (String s : strings) {
            MinMaxEntry minMaxEntry = parseMinMaxEntryNegative(s);

            if (minMaxEntry != null) {
                minMaxEntries.add(minMaxEntry);
            }
        }

        return minMaxEntries;
    }

    private static MinMaxEntry parseMinMaxEntryNegative(String str) {
        if (str != null) {
            String s = OPTIFINE_RANGE_SEPARATOR.matcher(str).replaceAll("$1=$2");

            if (s.contains("=")) {
                String[] strings = s.split("=");

                if (strings.length == 2) {
                    int j = parseInt(stripBrackets(strings[0]), Integer.MIN_VALUE);
                    int k = parseInt(stripBrackets(strings[1]), Integer.MIN_VALUE);

                    if (j != Integer.MIN_VALUE && k != Integer.MIN_VALUE) {
                        int min = Math.min(j, k);
                        int max = Math.max(j, k);
                        return new MinMaxEntry(min, max);
                    }
                }
            } else {
                int i = parseInt(stripBrackets(str), Integer.MIN_VALUE);

                if (i != Integer.MIN_VALUE) {
                    return new MinMaxEntry(i, i);
                }
            }
        }
        return null;
    }

    private static String stripBrackets(String str) {
        return str.startsWith("(") && str.endsWith(")") ? str.substring(1, str.length() - 1) : str;
    }

    public static int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
