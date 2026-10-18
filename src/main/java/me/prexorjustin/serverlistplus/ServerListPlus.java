package me.prexorjustin.serverlistplus;

import me.prexorjustin.serverlistplus.platform.Platform;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Toasts;
import me.prexorjustin.serverlistplus.core.key.AddressKey;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.session.PlaytimeTracker;
import me.prexorjustin.serverlistplus.core.storage.JsonStore;
import net.minecraft.client.Minecraft;
import me.prexorjustin.serverlistplus.ui.widget.NotesToast;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Loader-independent entry point. Called by the Fabric and NeoForge bootstrap classes.
 */
public final class ServerListPlus {
    public static final String MOD_ID = "serverlistplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "1.2.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";

    private static JsonStore jsonStore;
    private static Store store = new Store();
    private static boolean writable;
    private static PlaytimeTracker tracker;

    private ServerListPlus() {
    }

    public static void init() {
        jsonStore = new JsonStore(Platform.INSTANCE.configDir().resolve(MOD_ID).resolve("servers.json"));
        loadStore();
        tracker = new PlaytimeTracker(store, () -> System.nanoTime() / 1_000_000, System::currentTimeMillis);
        LOGGER.info("ServerListPlus {} initialized on {} (Minecraft {})",
            VERSION, Platform.INSTANCE.loaderName(), MINECRAFT);
        if (store.checksUpdates()) UpdateCheck.start();
    }

    public static Store store() {
        return store;
    }

    /** Called by the loader when a connection to a server is established. */
    public static void onJoin() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        // Singleplayer has no server, LAN worlds and Realms have no stable address
        if (server == null || server.isLan() || server.isRealm()) return;
        tracker.join(AddressKey.normalize(server.ip));
        save();
        ServerMeta meta = ServerView.meta(server);
        if (meta != null && meta.remindsOnJoin()) {
            Toasts.show(new NotesToast(server.name, meta.notes()));
        }
    }

    /** Called by the loader when the connection ends and when the game closes. */
    public static void onLeave() {
        if (tracker != null && tracker.leave()) save();
    }

    /** Called by the loader every client tick. */
    public static void onTick() {
        if (tracker != null && tracker.tick()) save();
    }

    /** Writes the store to disk right away. Changes are rare user actions, so no batching is needed. */
    public static void save() {
        if (!writable) return;
        store.pruneEmpty();
        try {
            jsonStore.save(store);
        } catch (IOException e) {
            LOGGER.error("Could not save {}", jsonStore.file(), e);
        }
    }

    private static void loadStore() {
        try {
            JsonStore.LoadResult result = jsonStore.load();
            store = result.store();
            writable = result.writable();
            store.view().openDefaultTab();
            switch (result.status()) {
                case CORRUPT -> LOGGER.error("{} is unreadable, a copy was saved as {} and an empty list is used",
                    jsonStore.file(), jsonStore.backupFile().getFileName());
                case NEWER_VERSION -> LOGGER.warn("{} was written by a newer ServerListPlus version, changes will not be saved",
                    jsonStore.file());
                default -> {
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not read {}, changes will not be saved", jsonStore.file(), e);
            writable = false;
        }
    }
}
