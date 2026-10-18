package me.prexorjustin.serverlistplus.bridge;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.view.DividerLayout;
import me.prexorjustin.serverlistplus.mixin.AbstractSelectionListInvoker;
import me.prexorjustin.serverlistplus.mixin.ServerListAccessor;
import me.prexorjustin.serverlistplus.ui.serverlist.DividerEntry;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between positions in the on-screen list and the vanilla server list.
 * Vanilla assumes both are identical, which no longer holds once entries are hidden or pinned.
 */
public final class ServerListBridge {
    private ServerListBridge() {
    }

    public static int indexOf(ServerList servers, ServerData data) {
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i) == data) return i;
        }
        return -1;
    }

    /**
     * Swaps the servers shown at two list positions in the vanilla list. Does nothing if either
     * position is not a saved server (e.g. the LAN header) or only one of them is pinned.
     */
    public static void swapVisible(ServerSelectionList list, ServerList servers, int from, int to) {
        List<ServerSelectionList.Entry> children = list.children();
        if (from < 0 || to < 0 || from >= children.size() || to >= children.size()) return;
        if (!(children.get(from) instanceof ServerSelectionList.OnlineServerEntry moved)) return;
        if (!(children.get(to) instanceof ServerSelectionList.OnlineServerEntry other)) return;
        if (ServerView.isPinned(moved.getServerData()) != ServerView.isPinned(other.getServerData())) return;

        int movedIndex = indexOf(servers, moved.getServerData());
        int otherIndex = indexOf(servers, other.getServerData());
        if (movedIndex < 0 || otherIndex < 0) return;

        // A divider above the upper of the two stays above whichever server is on top afterwards
        ServerData upper = to > from ? moved.getServerData() : other.getServerData();
        ServerData lower = to > from ? other.getServerData() : moved.getServerData();
        for (var divider : ServerListPlus.store().dividers(ServerView.tab())) {
            if (ServerView.key(upper).equals(divider.beforeKey())) divider.setBeforeKey(ServerView.key(lower));
        }
        servers.swap(movedIndex, otherIndex);
        list.updateOnlineServers(servers);
        focus(list, moved.getServerData());
    }

    /**
     * Selects, focuses and scrolls to a server after the list was rebuilt. The rebuilt rows are new
     * objects, so without this the keyboard focus stays on the old row and further key presses get lost.
     */
    private static void focus(ServerSelectionList list, ServerData data) {
        ServerView.select(list, data);
        ServerSelectionList.Entry entry = list.getSelected();
        if (entry == null) return;
        list.setFocused(entry);
        ((ServerSelectionListHook) list).serverlistplus$scrollTo(entry);
    }

    /**
     * Shift + arrow key: moves a server one row up or down. Next to a divider the server crosses it
     * into the neighbouring section instead of swapping with a server, so dividers are never skipped.
     */
    public static void moveByKey(ServerSelectionList list, ServerList servers, ServerSelectionList.OnlineServerEntry moved, boolean up) {
        List<ServerSelectionList.Entry> children = list.children();
        int from = children.indexOf(moved);
        if (from < 0) return;
        int to = up ? from - 1 : from + 1;
        if (to < 0 || to >= children.size()) return;
        if (!(children.get(to) instanceof DividerEntry crossed)) {
            swapVisible(list, servers, from, to);
            return;
        }
        // Dividers stay out of the pinned servers, as when dragging
        if (ServerView.isPinned(moved.getServerData())) return;
        Divider divider = crossed.divider();
        List<Divider> dividers = ServerListPlus.store().dividers();
        dividers.remove(divider);
        String key = ServerView.key(moved.getServerData());
        if (up) {
            // From directly above the server to directly below it: first divider of the next server
            divider.setBeforeKey(DividerLayout.nextAnchor(shownServers(list), entry -> ServerView.key(entry.getServerData()), key));
            dividers.add(0, divider);
        } else {
            // From directly below the server to directly above it: last divider of this server
            divider.setBeforeKey(key);
            dividers.add(divider);
        }
        ServerListPlus.save();
        ServerView.refresh(list, servers);
        focus(list, moved.getServerData());
    }

    /** Moves a server directly before or after another one in the vanilla list and saves it. */
    public static void moveNextTo(ServerList servers, ServerData moved, ServerData target, boolean after) {
        moveAllNextTo(servers, List.of(moved), target, after);
    }

    /**
     * Moves servers directly before or after another one in the vanilla list, in the given order,
     * and saves it. Does nothing if the target is one of them.
     */
    public static void moveAllNextTo(ServerList servers, List<ServerData> moved, ServerData target, boolean after) {
        if (moved.contains(target)) return;
        List<ServerData> list = ((ServerListAccessor) servers).serverlistplus$serverList();
        if (!list.removeIf(moved::contains)) return;
        int index = list.indexOf(target);
        if (index < 0) {
            list.addAll(moved);
        } else {
            list.addAll(after ? index + 1 : index, moved);
        }
        servers.save();
    }

    /** The list row under the pointer (server, divider, LAN entry), or null. */
    public static ServerSelectionList.Entry childAt(ServerSelectionList list, double x, double y) {
        if (!list.isMouseOver(x, y)) return null;
        return list.getChildAt(x, y).orElse(null) instanceof ServerSelectionList.Entry entry ? entry : null;
    }

    /** Saved servers in the order they are shown. */
    public static List<ServerSelectionList.OnlineServerEntry> shownServers(ServerSelectionList list) {
        List<ServerSelectionList.OnlineServerEntry> shown = new ArrayList<>();
        for (ServerSelectionList.Entry entry : list.children()) {
            if (entry instanceof ServerSelectionList.OnlineServerEntry online) shown.add(online);
        }
        return shown;
    }

    /**
     * Before a server leaves its place, dividers sitting above it move to the server that followed it,
     * so they stay where the user put them instead of travelling along.
     */
    public static void keepDividersInPlace(ServerSelectionList list, ServerData leaving) {
        String key = ServerView.key(leaving);
        List<ServerSelectionList.OnlineServerEntry> shown = shownServers(list);
        String next = DividerLayout.nextAnchor(shown, entry -> ServerView.key(entry.getServerData()), key);
        for (var divider : ServerListPlus.store().dividers(ServerView.tab())) {
            if (key.equals(divider.beforeKey())) divider.setBeforeKey(next);
        }
    }

    /** The saved server under the pointer, or null. */
    public static ServerSelectionList.OnlineServerEntry entryAt(ServerSelectionList list, double x, double y) {
        if (!list.isMouseOver(x, y)) return null;
        return list.getChildAt(x, y).orElse(null) instanceof ServerSelectionList.OnlineServerEntry entry ? entry : null;
    }

    /** Top and bottom screen coordinate of an entry's row. */
    private static boolean drawingMoved;

    /** The box vanilla draws around a selected row: x1, y1, x2, y2. */
    public static int[] rowBox(ServerSelectionList list, ServerSelectionList.Entry entry) {
        //? if >=1.21.9 {
        return new int[]{entry.getX(), entry.getY(), entry.getX() + entry.getWidth(), entry.getY() + entry.getHeight()};
        //?} else {
        /*int[] row = rowBounds(list, entry);
        int left = list.getX() + (list.getWidth() - list.getRowWidth()) / 2;
        return new int[]{left, row[0] - 2, left + list.getRowWidth(), row[1] - 2};
        *///?}
    }

    /** Draws a row's content moved by {@code dx}, {@code dy} and not hovered, e.g. for a dragged server. */
    public static void drawRowMoved(Gfx gfx, ServerSelectionList list, ServerSelectionList.Entry entry, int dx, int dy) {
        // Far away, so nothing in the row reacts to the pointer
        int mouse = -10000;
        drawingMoved = true;
        gfx.push(dx, dy);
        //? if >=26.1 {
        entry.extractContent(gfx.raw(), mouse, mouse, false, 0);
        //?} elif >=1.21.9 {
        /*entry.renderContent(gfx.raw(), mouse, mouse, false, 0);*/
        //?} else {
        /*int index = list.children().indexOf(entry);
        int[] row = rowBounds(list, entry);
        if (index >= 0) entry.render(gfx.raw(), index, row[0], list.getRowLeft(), list.getRowWidth(), row[1] - row[0] - 4, mouse, mouse, false, 0);
        *///?}
        gfx.pop();
        drawingMoved = false;
    }

    /** While {@link #drawRowMoved} draws, the row brings its own frame. */
    public static boolean isDrawingMoved() {
        return drawingMoved;
    }

    public static int[] rowBounds(ServerSelectionList list, ServerSelectionList.Entry entry) {
        int index = list.children().indexOf(entry);
        AbstractSelectionListInvoker invoker = (AbstractSelectionListInvoker) list;
        return new int[]{invoker.serverlistplus$rowTop(index), invoker.serverlistplus$rowBottom(index)};
    }
}
