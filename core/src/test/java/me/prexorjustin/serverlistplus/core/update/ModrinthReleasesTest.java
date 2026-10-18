package me.prexorjustin.serverlistplus.core.update;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthReleasesTest {
    private static String version(String number, String type) {
        return "{\"version_number\":\"" + number + "\",\"version_type\":\"" + type + "\"}";
    }

    @Test
    void picksNewestRelease() {
        String json = "[" + version("1.3.0-beta.1+1.21.11-fabric", "beta") + ","
            + version("1.1.1+1.21.11-fabric", "release") + ","
            + version("1.2.0+1.21.11-fabric", "release") + ","
            + version("1.1.2+1.21.11-fabric", "release") + "]";
        assertEquals(Optional.of("1.2.0"), ModrinthReleases.newerThan("1.1.1", json));
    }

    @Test
    void nothingNewer() {
        String json = "[" + version("1.1.1+1.21.11-fabric", "release") + "," + version("1.0.0+1.21.11-fabric", "release") + "]";
        assertTrue(ModrinthReleases.newerThan("1.1.1", json).isEmpty());
        assertTrue(ModrinthReleases.newerThan("1.1.1", "[]").isEmpty());
    }

    @Test
    void skipsIncompleteEntries() {
        String json = "[{\"version_type\":\"release\"}," + version("2.0.0", "release") + "]";
        assertEquals(Optional.of("2.0.0"), ModrinthReleases.newerThan("1.1.1", json));
    }

    @Test
    void compare() {
        assertTrue(ModrinthReleases.compare("1.10.0", "1.9.9") > 0);
        assertTrue(ModrinthReleases.compare("1.1", "1.1.1") < 0);
        assertEquals(0, ModrinthReleases.compare("1.1", "1.1.0"));
        assertEquals(0, ModrinthReleases.compare("1.2.0-rc1", "1.2.0"));
    }

    @Test
    void modVersion() {
        assertEquals("1.2.0", ModrinthReleases.modVersion("1.2.0+26.3-neoforge"));
        assertEquals("1.2.0", ModrinthReleases.modVersion("1.2.0"));
    }
}
