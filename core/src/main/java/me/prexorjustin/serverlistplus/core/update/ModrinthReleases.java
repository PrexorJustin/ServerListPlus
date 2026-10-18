package me.prexorjustin.serverlistplus.core.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Optional;

/**
 * Reads the version list the Modrinth API returns for one loader and Minecraft version.
 * Versions are published as {@code 1.2.0+1.21.11-fabric}, so only the part before the
 * {@code +} is the mod version.
 */
public final class ModrinthReleases {
    private ModrinthReleases() {
    }

    /**
     * @param json the response of {@code /v2/project/{id}/version}
     * @return the newest release that is newer than {@code current}; betas are skipped
     */
    public static Optional<String> newerThan(String current, String json) {
        String newest = null;
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject version = element.getAsJsonObject();
            if (!"release".equals(string(version, "version_type"))) continue;
            String number = string(version, "version_number");
            if (number == null) continue;
            String modVersion = modVersion(number);
            if (compare(modVersion, current) > 0 && (newest == null || compare(modVersion, newest) > 0)) newest = modVersion;
        }
        return Optional.ofNullable(newest);
    }

    static String modVersion(String number) {
        int plus = number.indexOf('+');
        return plus < 0 ? number : number.substring(0, plus);
    }

    /** Compares dotted version numbers part by part; a missing part counts as 0. */
    static int compare(String a, String b) {
        String[] left = a.split("\\.");
        String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int result = Integer.compare(part(left, i), part(right, i));
            if (result != 0) return result;
        }
        return 0;
    }

    /** The leading digits of a part, so {@code 0-rc1} counts as 0. */
    private static int part(String[] parts, int index) {
        if (index >= parts.length) return 0;
        String part = parts[index];
        int end = 0;
        while (end < part.length() && end < 9 && Character.isDigit(part.charAt(end))) end++;
        return end == 0 ? 0 : Integer.parseInt(part, 0, end, 10);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
