package me.prexorjustin.serverlistplus.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerEntryHook;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Toasts;
import me.prexorjustin.serverlistplus.core.key.ServerKey;
import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.ui.serverlist.DividerEntry;
import me.prexorjustin.serverlistplus.ui.serverlist.HiddenSectionEntry;
import me.prexorjustin.serverlistplus.ui.widget.IconButton;
import me.prexorjustin.serverlistplus.ui.widget.NotesToast;
import me.prexorjustin.serverlistplus.ui.widget.TabSidebar;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.Comparator;
import java.util.List;

/**
 * Starts the real game and goes through the screens ServerListPlus changes. Each step checks
 * the result and saves a screenshot to {@code build/run/clientGameTest/screenshots}.
 */
public final class ServerListScreensTest implements FabricClientGameTest {
    /** Nothing listens there, so pings fail fast and the test needs no network. */
    private static final String SHARED_ADDRESS = "127.0.0.1:1";

    @Override
    public void runTest(ClientGameTestContext context) {
        if (ReadmeScreenshots.ENABLED) return;
        context.runOnClient(ServerListScreensTest::writeServerList);
        openServerList(context);
        context.takeScreenshot("01-server-list");

        sameAddressKeepsSeparateFolders(context);
        hiddenServersFoldAway(context);
        menusOpenOnClick(context);
        sidebarLayout(context);
        detailsModes(context);
        keyboardMoveCrossesDividers(context);
        miniMessageNames(context);
        notes(context);
        directConnection(context);

        // The test framework expects to end where it started
        context.setScreen(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
    }

    private static void writeServerList(Minecraft minecraft) {
        ServerList servers = new ServerList(minecraft);
        servers.add(new ServerData("Space Chunks", SHARED_ADDRESS, ServerData.Type.OTHER), false);
        servers.add(new ServerData("BlockBound", SHARED_ADDRESS, ServerData.Type.OTHER), false);
        servers.add(new ServerData("Unreachable", "127.0.0.1:2", ServerData.Type.OTHER), false);
        servers.save();
    }

    private static void openServerList(ClientGameTestContext context) {
        context.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
        context.waitForScreen(JoinMultiplayerScreen.class);
        context.waitTicks(10);
    }

    private static void sameAddressKeepsSeparateFolders(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            Store store = ServerListPlus.store();
            Folder folder = store.addFolder(new Folder("Space Chunks only"));
            ServerView.metaOrCreate(server(minecraft, "Space Chunks")).addToFolder(folder.id());
            store.view().setTab(folder.id());
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(5);
        List<String> shown = context.computeOnClient(ServerListScreensTest::shownNames);
        check(shown.equals(List.of("Space Chunks")), "folder should only show Space Chunks, but shows " + shown);
        context.takeScreenshot("02-folder-with-one-of-two-same-address-servers");
    }

    private static void hiddenServersFoldAway(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            ServerView.metaOrCreate(server(minecraft, "BlockBound")).setHiddenInAll(true);
            ServerListPlus.store().view().setTab(ViewSettings.TAB_ALL);
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(5);
        List<String> shown = context.computeOnClient(ServerListScreensTest::shownNames);
        check(!shown.contains("BlockBound"), "BlockBound is hidden but still shown: " + shown);
        boolean section = context.computeOnClient(minecraft -> list(minecraft).children().stream()
            .anyMatch(entry -> entry instanceof HiddenSectionEntry));
        check(section, "the hidden section header is missing");
        context.takeScreenshot("03-hidden-section");
    }

    /** Real clicks, so the input routing of the screen is tested too, not just the data. */
    private static void menusOpenOnClick(ClientGameTestContext context) {
        double[] server = context.computeOnClient(minecraft -> {
            ServerSelectionList list = list(minecraft);
            ServerSelectionList.Entry first = list.children().getFirst();
            int[] row = ServerListBridge.rowBounds(list, first);
            return new double[]{list.getX() + list.getWidth() / 2.0, (row[0] + row[1]) / 2.0};
        });
        click(context, server[0], server[1], InputConstants.MOUSE_BUTTON_RIGHT);
        context.takeScreenshot("04-server-menu");
        click(context, 4, 4, InputConstants.MOUSE_BUTTON_LEFT); // Closes the menu

        double[] settings = context.computeOnClient(minecraft -> {
            IconButton button = minecraft.gui.screen().children().stream()
                .filter(child -> child instanceof IconButton)
                .map(child -> (IconButton) child)
                .max(Comparator.comparingInt(IconButton::getX)) // Settings is the rightmost
                .orElseThrow(() -> new AssertionError("no header icons"));
            return new double[]{button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0};
        });
        click(context, settings[0], settings[1], InputConstants.MOUSE_BUTTON_LEFT);
        context.takeScreenshot("05-settings-menu");
        click(context, 4, 4, InputConstants.MOUSE_BUTTON_LEFT);
    }

    /** Rows store their position since 1.21.9; it has to follow the list when the list moves. */
    private static void checkRowsAligned(ClientGameTestContext context, String when) {
        String wrong = context.computeOnClient(minecraft -> {
            ServerSelectionList list = list(minecraft);
            return list.children().stream()
                .filter(entry -> entry.getX() != list.getRowLeft())
                .map(entry -> entry.getX() + " instead of " + list.getRowLeft())
                .findFirst().orElse(null);
        });
        check(wrong == null, "rows are not where the list puts them " + when + ": " + wrong);
    }

    /** Clicks at a position in GUI coordinates. */
    private static void click(ClientGameTestContext context, double x, double y, int button) {
        double scale = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
        context.getInput().setCursorPos(x * scale, y * scale);
        context.getInput().pressMouse(button);
        context.waitTicks(2);
    }

    /** The sidebar needs room next to the list, so this step uses a wider window. */
    private static void sidebarLayout(ClientGameTestContext context) {
        context.getInput().resizeWindow(1600, 900);
        context.runOnClient(minecraft -> ServerListPlus.store().view().setTabLayout(TabLayout.SIDEBAR));
        // The layout is applied when the screen is laid out again
        openServerList(context);
        boolean sidebar = context.computeOnClient(minecraft -> minecraft.gui.screen().children().stream()
            .anyMatch(child -> child instanceof TabSidebar sidebarWidget && sidebarWidget.visible));
        check(sidebar, "the sidebar is not shown in a wide window");
        checkRowsAligned(context, "after opening with the sidebar");
        context.takeScreenshot("06-sidebar");

        // A 4:3 window, as reported: coming back to the list and resizing while it is open
        context.getInput().resizeWindow(1711, 1290);
        context.waitTicks(2);
        checkRowsAligned(context, "after resizing to 4:3 with the list open");
        openServerList(context);
        checkRowsAligned(context, "after opening the list in a 4:3 window");
        context.takeScreenshot("06-sidebar-4-3");
        context.getInput().resizeWindow(1600, 900);
        context.waitTicks(2);
        checkRowsAligned(context, "after resizing back to 16:9");

        // Leaving a server shows the same screen object again; the window may have changed meanwhile
        Screen[] multiplayer = new Screen[1];
        context.runOnClient(minecraft -> multiplayer[0] = minecraft.gui.screen());
        context.setScreen(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
        context.getInput().resizeWindow(1711, 1290);
        context.waitTicks(2);
        context.setScreen(() -> multiplayer[0]);
        context.waitForScreen(JoinMultiplayerScreen.class);
        context.waitTicks(5);
        checkRowsAligned(context, "after coming back to the same screen in a resized window");
        context.takeScreenshot("06-sidebar-back-resized");

        // Rows left where an earlier layout put them, with the list itself in place (as seen after
        // leaving a server): they follow the list before the next frame is drawn
        context.runOnClient(minecraft -> list(minecraft).children().forEach(entry -> entry.setX(entry.getX() - 40)));
        context.waitTicks(2);
        checkRowsAligned(context, "after the rows were left behind");
        context.getInput().resizeWindow(1600, 900);
        context.runOnClient(minecraft -> ServerListPlus.store().view().setTabLayout(TabLayout.TOP));
        context.getInput().resizeWindow(854, 480);
        openServerList(context);
    }

    private static void detailsModes(ClientGameTestContext context) {
        for (DetailsMode mode : DetailsMode.values()) {
            context.runOnClient(minecraft -> {
                ServerListPlus.store().activityOrCreate(SHARED_ADDRESS).setLastPlayed(System.currentTimeMillis() - 3_600_000);
                ServerListPlus.store().view().setDetails(mode);
            });
            context.waitTicks(2);
            context.takeScreenshot("07-details-" + mode.name().toLowerCase());
        }
    }

    /**
     * Shift + arrow key moves a server across a divider instead of stopping in front of it. Real key
     * presses in a row, so the focus has to follow the server for the next press to arrive.
     */
    private static void keyboardMoveCrossesDividers(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            ServerListPlus.store().dividers().add(new Divider(ViewSettings.TAB_ALL, ServerView.key(server(minecraft, "Unreachable"))));
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(2);
        checkRows(context, "Space Chunks", "---", "Unreachable");

        clickServer(context, "Space Chunks");
        pressShifted(context, InputConstants.KEY_DOWN);
        checkRows(context, "---", "Space Chunks", "Unreachable");
        pressShifted(context, InputConstants.KEY_DOWN);
        checkRows(context, "---", "Unreachable", "Space Chunks");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "---", "Space Chunks", "Unreachable");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "Space Chunks", "---", "Unreachable");

        clickServer(context, "Unreachable");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "Space Chunks", "Unreachable", "---");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "Unreachable", "Space Chunks", "---");
        context.takeScreenshot("08-moved-by-keyboard");
    }

    private static void clickServer(ClientGameTestContext context, String name) {
        double[] center = context.computeOnClient(minecraft -> {
            ServerData data = server(minecraft, name);
            ServerSelectionList list = list(minecraft);
            ServerSelectionList.Entry entry = list.children().stream()
                .filter(child -> child instanceof ServerSelectionList.OnlineServerEntry online && online.getServerData() == data)
                .findFirst().orElseThrow();
            int[] row = ServerListBridge.rowBounds(list, entry);
            return new double[]{list.getX() + list.getWidth() / 2.0, (row[0] + row[1]) / 2.0};
        });
        click(context, center[0], center[1], InputConstants.MOUSE_BUTTON_LEFT);
    }

    /**
     * Shift + key through the screen, so it travels the same focus path as a real key press. The
     * test input cannot be used here: it always sends key events without modifiers.
     */
    private static void pressShifted(ClientGameTestContext context, int key) {
        context.runOnClient(minecraft -> minecraft.gui.screen().keyPressed(new KeyEvent(key, 0, InputConstants.MOD_SHIFT)));
        context.waitTicks(2);
    }

    /** Servers and dividers ("---") in the order shown, without the hidden section. */
    private static void checkRows(ClientGameTestContext context, String... expected) {
        List<String> rows = context.computeOnClient(minecraft -> list(minecraft).children().stream()
            .takeWhile(entry -> !(entry instanceof HiddenSectionEntry))
            .map(entry -> entry instanceof DividerEntry ? "---"
                : entry instanceof ServerSelectionList.OnlineServerEntry online ? online.getServerData().name : null)
            .filter(row -> row != null)
            .toList());
        check(rows.equals(List.of(expected)), "expected rows " + List.of(expected) + ", but got " + rows);
    }

    private static void miniMessageNames(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            server(minecraft, "Space Chunks").name = "<gradient:#ff5555:#ffaa00>Space Chunks</gradient> <gray>SMP";
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(2);
        context.takeScreenshot("09-minimessage-name");
    }

    /**
     * A server with notes shows a symbol in its row; clicking it opens the card, and the notes
     * can be searched for. In a small window the card lies over the list, in a wide one next to it.
     */
    private static void notes(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            ServerView.metaOrCreate(server(minecraft, "Unreachable")).setNotes("Base at -120 64 300");
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(2);
        clickNotesSymbol(context, "Unreachable");
        check(context.computeOnClient(minecraft -> minecraft.gui.screen().getFocused() instanceof MultiLineEditBox),
            "clicking the notes symbol should open the info card with the cursor in the notes");
        context.takeScreenshot("10-notes-card");

        // A click on an empty spot of the card must not reach the servers under it
        double[] box = notesBoxCenter(context);
        click(context, box[0], box[1] - 30, InputConstants.MOUSE_BUTTON_LEFT);
        click(context, box[0], box[1], InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(minecraft -> minecraft.gui.screen().getFocused() instanceof MultiLineEditBox),
            "after clicking the card, clicking the notes should still put the cursor there");

        // Leaving the screen with the card still open (as joining a server does) keeps its edits
        context.getInput().pressKey(InputConstants.KEY_END);
        context.getInput().typeChars(" (north)");
        double[] checkbox = context.computeOnClient(minecraft -> minecraft.gui.screen().children().stream()
            .filter(child -> child instanceof Checkbox)
            .map(child -> (Checkbox) child)
            .map(checkboxWidget -> new double[]{checkboxWidget.getX() + 8, checkboxWidget.getY() + checkboxWidget.getHeight() / 2.0})
            .findFirst().orElseThrow(() -> new AssertionError("no \"show when joining\" checkbox")));
        click(context, checkbox[0], checkbox[1], InputConstants.MOUSE_BUTTON_LEFT);
        context.setScreen(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
        boolean kept = context.computeOnClient(minecraft -> {
            // The server list is gone with its screen, so the key is built from address and name
            ServerMeta meta = ServerListPlus.store().get(ServerKey.of("127.0.0.1:2", "Unreachable")).orElseThrow();
            return meta.notesOnJoin() && meta.notes().equals("Base at -120 64 300 (north)");
        });
        check(kept, "notes and \"show when joining\" should be kept when the screen is left with the card open");
        openServerList(context);

        context.runOnClient(minecraft -> hook(minecraft).serverlistplus$setSearch("has:notes"));
        context.waitTicks(2);
        List<String> shown = context.computeOnClient(ServerListScreensTest::shownNames);
        check(shown.equals(List.of("Unreachable")), "has:notes should only find Unreachable, but shows " + shown);
        context.runOnClient(minecraft -> hook(minecraft).serverlistplus$setSearch("base"));
        context.waitTicks(2);
        shown = context.computeOnClient(ServerListScreensTest::shownNames);
        check(shown.equals(List.of("Unreachable")), "plain search should look into the notes, but shows " + shown);
        context.runOnClient(minecraft -> hook(minecraft).serverlistplus$setSearch(""));
        context.waitTicks(2);

        dockedNotes(context);
    }

    /**
     * With room next to the list the card docks there, and follows the selected server. 1080p with
     * the automatic GUI scale is only 480 wide, so the rows have to shrink for it.
     */
    private static void dockedNotes(ClientGameTestContext context) {
        context.getInput().resizeWindow(1920, 1080);
        openServerList(context);
        int fullWidth = context.computeOnClient(minecraft -> list(minecraft).getWidth());
        clickNotesSymbol(context, "Unreachable");
        boolean docked = context.computeOnClient(minecraft -> {
            ServerSelectionList list = list(minecraft);
            return list.getWidth() < fullWidth && notesBox(minecraft).getX() >= list.getRight();
        });
        check(docked, "in a wide window the card should sit right of the list");
        context.takeScreenshot("11-notes-docked");

        // Opening the screen again reloaded servers.dat, which never had the styled name
        clickServer(context, "Space Chunks");
        context.waitTicks(2);
        String value = context.computeOnClient(minecraft -> notesBox(minecraft).getValue());
        check(value.isEmpty(), "the docked card should show the newly selected server, but shows notes \"" + value + "\"");
        double[] box = notesBoxCenter(context);
        click(context, box[0], box[1], InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(minecraft -> minecraft.gui.screen().getFocused() instanceof MultiLineEditBox),
            "clicking into the docked notes should put the cursor there");

        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(2);
        int width = context.computeOnClient(minecraft -> list(minecraft).getWidth());
        check(width == fullWidth, "closing the docked card should give the list its width back");
        context.getInput().resizeWindow(854, 480);
        openServerList(context);
    }

    /** Clicks the notes symbol in a server's row; found by asking the row, as it moves with the name. */
    private static void clickNotesSymbol(ClientGameTestContext context, String name) {
        double[] symbol = context.computeOnClient(minecraft -> {
            ServerData data = server(minecraft, name);
            ServerSelectionList list = list(minecraft);
            ServerSelectionList.OnlineServerEntry entry = list.children().stream()
                .filter(child -> child instanceof ServerSelectionList.OnlineServerEntry online && online.getServerData() == data)
                .map(child -> (ServerSelectionList.OnlineServerEntry) child)
                .findFirst().orElseThrow();
            int[] row = ServerListBridge.rowBounds(list, entry);
            for (int y = row[0]; y < row[1]; y++) {
                for (int x = list.getX(); x < list.getRight(); x++) {
                    if (((ServerEntryHook) entry).serverlistplus$isOverNotes(x, y)) return new double[]{x + 2, y + 2};
                }
            }
            throw new AssertionError("no notes symbol drawn for " + name);
        });
        click(context, symbol[0], symbol[1], InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static MultiLineEditBox notesBox(Minecraft minecraft) {
        return minecraft.gui.screen().children().stream()
            .filter(child -> child instanceof MultiLineEditBox)
            .map(child -> (MultiLineEditBox) child)
            .findFirst().orElseThrow(() -> new AssertionError("the info card is not open"));
    }

    private static double[] notesBoxCenter(ClientGameTestContext context) {
        return context.computeOnClient(minecraft -> {
            MultiLineEditBox box = notesBox(minecraft);
            return new double[]{box.getX() + box.getWidth() / 2.0, box.getY() + box.getHeight() / 2.0};
        });
    }

    /** The preview pings while the address is typed; drawing it before the ping started once crashed. */
    private static void directConnection(ClientGameTestContext context) {
        context.runOnClient(minecraft -> minecraft.gui.setScreen(new DirectJoinServerScreen(minecraft.gui.screen(),
            joined -> { }, new ServerData("", "", ServerData.Type.OTHER))));
        context.waitForScreen(DirectJoinServerScreen.class);
        context.getInput().typeChars("127.0.0.1:3");
        context.waitTicks(30);
        context.takeScreenshot("12-direct-connection");

        context.runOnClient(minecraft -> Toasts.show(new NotesToast("<gradient:#46a758:#12a594>Friends SMP</gradient>",
            "Base at -120 64 300\nNether hub: 12 / 40 (north tunnel)\nWhitelist: ask Alex in Discord")));
        context.waitTicks(20);
        context.takeScreenshot("13-notes-toast");
    }

    private static ServerData server(Minecraft minecraft, String name) {
        ServerList servers = hook(minecraft).serverlistplus$servers();
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).name.equals(name)) return servers.get(i);
        }
        throw new AssertionError("no server named " + name);
    }

    private static List<String> shownNames(Minecraft minecraft) {
        return list(minecraft).children().stream()
            .filter(entry -> entry instanceof ServerSelectionList.OnlineServerEntry)
            .map(entry -> ((ServerSelectionList.OnlineServerEntry) entry).getServerData().name)
            .toList();
    }

    private static ServerSelectionList list(Minecraft minecraft) {
        return hook(minecraft).serverlistplus$list();
    }

    private static MultiplayerScreenHook hook(Minecraft minecraft) {
        return (MultiplayerScreenHook) minecraft.gui.screen();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
