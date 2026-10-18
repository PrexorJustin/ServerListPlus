package me.prexorjustin.serverlistplus.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.style.Palette;
import me.prexorjustin.serverlistplus.ui.widget.IconButton;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.Comparator;
import java.util.List;

/**
 * Takes the screenshots for the README with real public servers. Runs only with
 * {@code ./gradlew :26.3-fabric:runClientGameTest -PreadmeScreenshots}, because it needs the
 * network; the regular tests are skipped then. Copy the results to {@code docs/images/}.
 */
public final class ReadmeScreenshots implements FabricClientGameTest {
    static final boolean ENABLED = Boolean.getBoolean("serverlistplus.readmeScreenshots");

    private static final long HOUR = 3_600_000;
    /** Long enough for every server to answer its ping. */
    private static final int PING_TICKS = 100;

    private record Server(String name, String address) {
    }

    private static final Server SPACE_CHUNKS = new Server("SpaceChunks", "chunks.space");
    private static final Server HYPIXEL = new Server("Hypixel", "mc.hypixel.net");
    private static final List<Server> SERVERS = List.of(
        SPACE_CHUNKS,
        HYPIXEL,
        new Server("CubeCraft", "play.cubecraft.net"),
        new Server("MCC Island", "play.mccisland.net"),
        new Server("GommeHD", "gommehd.net"),
        new Server("Wynncraft", "play.wynncraft.com"),
        new Server("2b2t", "2b2t.org")
    );

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!ENABLED) return;
        context.runOnClient(minecraft -> {
            minecraft.options.guiScale().set(2);
            writeServerList(minecraft);
        });
        context.getInput().resizeWindow(1280, 720);
        openServerList(context);
        context.runOnClient(ReadmeScreenshots::organize);
        openServerList(context);
        context.waitTicks(PING_TICKS);
        context.takeScreenshot("server-list");

        rightClickServer(context, SPACE_CHUNKS.name());
        context.takeScreenshot("server-menu");
        clickAt(context, 4, 4, InputConstants.MOUSE_BUTTON_LEFT);

        clickHeaderIcon(context, 1);
        context.takeScreenshot("settings");
        clickAt(context, 4, 4, InputConstants.MOUSE_BUTTON_LEFT);

        clickHeaderIcon(context, 0);
        context.getInput().typeChars("folder:minigames");
        context.waitTicks(5);
        context.takeScreenshot("search");
        context.runOnClient(minecraft -> hook(minecraft).serverlistplus$setSearch(""));

        context.runOnClient(minecraft -> ServerListPlus.store().view().setTabLayout(TabLayout.SIDEBAR));
        openServerList(context);
        context.waitTicks(PING_TICKS);
        context.takeScreenshot("sidebar");
        context.runOnClient(minecraft -> ServerListPlus.store().view().setTabLayout(TabLayout.TOP));

        directConnection(context);

        context.setScreen(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
    }

    private static void writeServerList(Minecraft minecraft) {
        ServerList servers = new ServerList(minecraft);
        for (Server server : SERVERS) servers.add(new ServerData(server.name(), server.address(), ServerData.Type.OTHER), false);
        servers.save();
    }

    /** Folders, pins, tags, a divider, a hidden server and some history, like after a while of use. */
    private static void organize(Minecraft minecraft) {
        Store store = ServerListPlus.store();
        Folder spaceChunks = folder(store, "SpaceChunks", "purple", "✦");
        Folder minigames = folder(store, "Minigames", "amber", "⚔");
        Folder adventure = folder(store, "Adventure", "green", "⛏");

        ServerMeta chunks = meta(minecraft, SPACE_CHUNKS.name());
        chunks.addToFolder(spaceChunks.id());
        chunks.setPinnedIn(ViewSettings.TAB_ALL, true);
        chunks.setTags(List.of("community"));
        chunks.setNotes("Our home server.");
        for (String name : List.of("Hypixel", "CubeCraft", "MCC Island", "GommeHD")) meta(minecraft, name).addToFolder(minigames.id());
        meta(minecraft, HYPIXEL.name()).setTags(List.of("bedwars", "skyblock"));
        meta(minecraft, "Wynncraft").addToFolder(adventure.id());
        meta(minecraft, "2b2t").setHiddenInAll(true);

        Divider divider = new Divider(ViewSettings.TAB_ALL, ServerView.key(server(minecraft, HYPIXEL.name())));
        divider.setLabel("Networks");
        divider.setColor(Palette.byKey("amber").orElseThrow());
        store.dividers().add(divider);

        long now = System.currentTimeMillis();
        store.activityOrCreate(SPACE_CHUNKS.address()).setLastPlayed(now - 2 * HOUR);
        store.activityOrCreate(SPACE_CHUNKS.address()).addPlaytime(14 * HOUR + 3 * 60_000);
        store.activityOrCreate(HYPIXEL.address()).setLastPlayed(now - 3 * 24 * HOUR);
        store.activityOrCreate(HYPIXEL.address()).addPlaytime(52 * HOUR);
        store.view().setDetails(DetailsMode.INLINE);
        for (String address : List.of("play.cubecraft.net", "mc.hypixel.net", "chunks.space")) store.rememberDirect(address);
        ServerListPlus.save();
    }

    private static Folder folder(Store store, String name, String color, String icon) {
        Folder folder = store.addFolder(new Folder(name));
        folder.setColor(Palette.byKey(color).orElseThrow());
        folder.setIcon(icon);
        return folder;
    }

    private static void directConnection(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            // The field starts with the last address, so empty it for the typed one
            minecraft.options.lastMpIp = "";
            minecraft.gui.setScreen(new DirectJoinServerScreen(minecraft.gui.screen(),
                joined -> { }, new ServerData("", "", ServerData.Type.OTHER)));
        });
        context.waitForScreen(DirectJoinServerScreen.class);
        context.getInput().typeChars(SPACE_CHUNKS.address());
        context.waitTicks(PING_TICKS);
        context.takeScreenshot("direct-connection");
    }

    private static void openServerList(ClientGameTestContext context) {
        context.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
        context.waitForScreen(JoinMultiplayerScreen.class);
        context.waitTicks(5);
    }

    private static void rightClickServer(ClientGameTestContext context, String name) {
        double[] position = context.computeOnClient(minecraft -> {
            ServerSelectionList list = hook(minecraft).serverlistplus$list();
            for (ServerSelectionList.Entry entry : list.children()) {
                if (entry instanceof ServerSelectionList.OnlineServerEntry online && online.getServerData().name.equals(name)) {
                    int[] row = ServerListBridge.rowBounds(list, entry);
                    return new double[]{list.getX() + list.getWidth() / 2.0, (row[0] + row[1]) / 2.0};
                }
            }
            throw new AssertionError("no server named " + name);
        });
        clickAt(context, position[0], position[1], InputConstants.MOUSE_BUTTON_RIGHT);
    }

    /** Clicks one of the icons right of the tabs, from the left: 0 search, 1 settings. */
    private static void clickHeaderIcon(ClientGameTestContext context, int index) {
        double[] position = context.computeOnClient(minecraft -> {
            List<IconButton> icons = minecraft.gui.screen().children().stream()
                .filter(child -> child instanceof IconButton)
                .map(child -> (IconButton) child)
                .sorted(Comparator.comparingInt(IconButton::getX))
                .toList();
            IconButton icon = icons.get(index);
            return new double[]{icon.getX() + icon.getWidth() / 2.0, icon.getY() + icon.getHeight() / 2.0};
        });
        clickAt(context, position[0], position[1], InputConstants.MOUSE_BUTTON_LEFT);
    }

    /** Clicks at a position in GUI coordinates. */
    private static void clickAt(ClientGameTestContext context, double x, double y, int button) {
        double scale = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
        context.getInput().setCursorPos(x * scale, y * scale);
        context.getInput().pressMouse(button);
        context.waitTicks(3);
    }

    private static ServerMeta meta(Minecraft minecraft, String name) {
        return ServerView.metaOrCreate(server(minecraft, name));
    }

    private static ServerData server(Minecraft minecraft, String name) {
        ServerList servers = hook(minecraft).serverlistplus$servers();
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).name.equals(name)) return servers.get(i);
        }
        throw new AssertionError("no server named " + name);
    }

    private static MultiplayerScreenHook hook(Minecraft minecraft) {
        return (MultiplayerScreenHook) minecraft.gui.screen();
    }
}
