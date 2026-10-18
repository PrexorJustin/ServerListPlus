package me.prexorjustin.serverlistplus;

import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.update.ModrinthReleases;
import me.prexorjustin.serverlistplus.platform.Platform;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Asks Modrinth once per game start whether a newer release exists for this loader and
 * Minecraft version. Runs in the background and stays quiet when offline or when Modrinth fails.
 */
public final class UpdateCheck {
    private static final String PROJECT = /*$ modrinth_id*/ "lEnmW6v6";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static boolean started;
    private static volatile String available;

    private UpdateCheck() {
    }

    public static void start() {
        if (started) return;
        started = true;
        String query = "loaders=" + jsonList(loader()) + "&game_versions=" + jsonList(ServerListPlus.MINECRAFT);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + PROJECT + "/version?" + query))
            .timeout(TIMEOUT)
            // Modrinth asks for a User-Agent that identifies the project
            .header("User-Agent", "PrexorJustin/ServerListPlus/" + ServerListPlus.VERSION + " (github.com/PrexorJustin/ServerListPlus)")
            .build();
        HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() != 200) {
                    ServerListPlus.LOGGER.debug("Update check got HTTP {}", response.statusCode());
                    return;
                }
                ModrinthReleases.newerThan(ServerListPlus.VERSION, response.body()).ifPresent(version -> {
                    available = version;
                    ServerListPlus.LOGGER.info("ServerListPlus {} is available: {}", version, downloadPage());
                });
            })
            .exceptionally(e -> {
                ServerListPlus.LOGGER.debug("Update check failed", e);
                return null;
            });
    }

    /** The newer release to point out, or null if there is none or it was hidden. */
    public static String pending() {
        String version = available;
        Store store = ServerListPlus.store();
        if (version == null || !store.checksUpdates() || version.equals(store.dismissedUpdate())) return null;
        return version;
    }

    /** The Modrinth versions page, filtered to the jars that fit this game. */
    public static URI downloadPage() {
        return URI.create("https://modrinth.com/mod/" + PROJECT + "/versions?g=" + ServerListPlus.MINECRAFT + "&l=" + loader());
    }

    private static String loader() {
        return Platform.INSTANCE.loaderName().toLowerCase(Locale.ROOT);
    }

    private static String jsonList(String value) {
        return URLEncoder.encode("[\"" + value + "\"]", StandardCharsets.UTF_8);
    }
}
