package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ui.text.NameText;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.core.view.Tabs;
import me.prexorjustin.serverlistplus.ui.widget.DragGhost;
import me.prexorjustin.serverlistplus.ui.widget.TabStrip;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dragging servers: onto another server or a divider to reorder, or onto a tab to put them into
 * that folder. Grabbing one of several selected servers drags all of them, in their shown order.
 */
final class ServerDragging {
    /** How far the pointer must move before a press becomes a drag. */
    static final int THRESHOLD = 5;

    private final UiContext ui;

    private ServerSelectionList.OnlineServerEntry pressed;
    private double pressX;
    private double pressY;
    private DragGhost ghost;
    /** The dragged servers in their shown order, the grabbed one among them. */
    private List<ServerData> moving = List.of();
    private String dropTab;
    private ServerSelectionList.OnlineServerEntry dropEntry;
    private Divider dropDivider;
    private boolean dropAfter;

    ServerDragging(UiContext ui) {
        this.ui = ui;
    }

    /** Remembers where a press on a server (or empty space, for null) started. */
    void press(ServerSelectionList.OnlineServerEntry entry, Pointer pointer) {
        pressed = entry;
        pressX = pointer.x();
        pressY = pointer.y();
    }

    boolean isDragging() {
        return ghost != null;
    }

    boolean drag(Pointer pointer) {
        if (pressed == null || !pointer.isLeft()) return false;
        if (ghost == null) {
            if (Math.hypot(pointer.x() - pressX, pointer.y() - pressY) < THRESHOLD) return false;
            ServerData grabbed = pressed.getServerData();
            moving = ui.selection().contains(grabbed) ? ui.selection().shown(ui.list()) : List.of(grabbed);
            Component label = moving.size() > 1
                ? Component.translatable("serverlistplus.selection.count", moving.size())
                : NameText.of(grabbed.name);
            ghost = new DragGhost(ui.font(), label, row(ui.list(), pressed, moving), pressX, pressY);
            ghost.setCount(moving.size());
            ui.hook().serverlistplus$addOverlay(ghost);
        }
        updateTarget(pointer.x(), pointer.y());
        return true;
    }

    boolean release() {
        ServerSelectionList.OnlineServerEntry dragged = pressed;
        pressed = null;
        if (ghost == null || dragged == null) return false;

        ServerData grabbed = dragged.getServerData();
        if (dropTab != null && Tabs.acceptsDrop(ui.store(), dropTab)) {
            dropOnTab(moving, dropTab);
        } else if (dropDivider != null) {
            dropOnDivider(moving, grabbed, dropDivider, dropAfter);
        } else if (dropEntry != null) {
            // Dragging is a clear wish for a custom order, so leave any automatic sort order
            if (ServerView.sortMode() != SortMode.MANUAL) ui.hook().serverlistplus$setSort(SortMode.MANUAL);
            for (ServerData server : moving) ServerListBridge.keepDividersInPlace(ui.list(), server);
            ServerListBridge.moveAllNextTo(ui.servers(), moving, dropEntry.getServerData(), dropAfter);
            ServerListPlus.save();
            ui.hook().serverlistplus$refresh();
            ServerView.select(ui.list(), grabbed);
        }
        ui.hook().serverlistplus$remove(ghost);
        ghost = null;
        moving = List.of();
        dropTab = null;
        dropEntry = null;
        dropDivider = null;
        ui.tabs().setDropTarget(null, false);
        return true;
    }

    private void updateTarget(double x, double y) {
        ServerSelectionList list = ui.list();
        int left = list.getX() + (list.getWidth() - list.getRowWidth()) / 2;
        TabStrip tabs = ui.tabs();
        String tab = tabs.tabAt(x, y);
        dropTab = null;
        dropEntry = null;
        dropDivider = null;
        if (tab != null && !tab.equals(TabStrip.NEW_TAB)) {
            boolean valid = Tabs.acceptsDrop(ui.store(), tab);
            tabs.setDropTarget(tab, valid);
            ghost.setValid(valid);
            ghost.setCompact(true);
            ghost.setLine(-1, 0, 0);
            dropTab = tab;
            return;
        }
        tabs.setDropTarget(null, false);
        ghost.setCompact(false);

        // Pinned servers stay at the top, so they can't go next to a divider
        if (ServerListBridge.childAt(list, x, y) instanceof DividerEntry divider && moving.stream().noneMatch(ServerView::isPinned)) {
            int[] row = ServerListBridge.rowBounds(list, divider);
            dropAfter = y > (row[0] + row[1]) / 2.0;
            ghost.setLine(dropAfter ? row[1] + 2 : row[0] - 2, left, left + list.getRowWidth());
            ghost.setValid(true);
            dropDivider = divider.divider();
            return;
        }
        ServerSelectionList.OnlineServerEntry target = ServerListBridge.entryAt(list, x, y);
        boolean ontoMoving = target != null && moving.contains(target.getServerData());
        if (target != null && !ontoMoving && canReorder(moving, target.getServerData())) {
            int[] row = ServerListBridge.rowBounds(list, target);
            dropAfter = y > (row[0] + row[1]) / 2.0;
            ghost.setLine(dropAfter ? row[1] + 2 : row[0] - 2, left, left + list.getRowWidth());
            ghost.setValid(true);
            dropEntry = target;
        } else {
            ghost.setLine(-1, 0, 0);
            ghost.setValid(target == null || ontoMoving);
        }
    }

    private static DragGhost.Row row(ServerSelectionList list, ServerSelectionList.Entry grabbed, List<ServerData> moving) {
        return new DragGhost.Row() {
            @Override
            public int[] box() {
                return ServerListBridge.rowBox(list, grabbed);
            }

            @Override
            public List<int[]> spots() {
                List<int[]> spots = new ArrayList<>();
                for (ServerSelectionList.OnlineServerEntry entry : ServerListBridge.shownServers(list)) {
                    if (moving.contains(entry.getServerData())) spots.add(ServerListBridge.rowBox(list, entry));
                }
                return spots;
            }

            @Override
            public int[] clip() {
                return new int[]{list.getY(), list.getBottom()};
            }

            @Override
            public void draw(Gfx gfx, int dx, int dy) {
                ServerListBridge.drawRowMoved(gfx, list, grabbed, dx, dy);
            }
        };
    }

    /** Pinned servers stay among pinned ones; a drop in a sorted view switches to manual order. */
    private static boolean canReorder(List<ServerData> moved, ServerData target) {
        boolean pinned = ServerView.isPinned(target);
        return moved.stream().allMatch(server -> ServerView.isPinned(server) == pinned);
    }

    /**
     * Dragged out of a folder, the servers move: they leave the open folder. Dragged out of "All",
     * they are added to the target folder and stay in the folders they were in.
     */
    private void dropOnTab(List<ServerData> servers, String tab) {
        String open = ServerView.tab();
        boolean leaveOpen = !tab.equals(open) && ui.store().folder(open).filter(folder -> !folder.isSmart()).isPresent();
        for (ServerData server : servers) {
            ServerMeta meta = ServerView.metaOrCreate(server);
            if (leaveOpen) meta.removeFromFolder(open);
            if (!ViewSettings.TAB_ALL.equals(tab)) meta.addToFolder(tab);
        }
        ui.changed();
    }

    /** Upper half: the servers go above the divider; lower half: directly below it. */
    private void dropOnDivider(List<ServerData> servers, ServerData grabbed, Divider divider, boolean below) {
        if (ServerView.sortMode() != SortMode.MANUAL) ui.hook().serverlistplus$setSort(SortMode.MANUAL);
        for (ServerData server : servers) ServerListBridge.keepDividersInPlace(ui.list(), server);
        List<ServerSelectionList.OnlineServerEntry> shown = ServerListBridge.shownServers(ui.list());
        ServerData anchor = null;
        for (ServerSelectionList.OnlineServerEntry entry : shown) {
            if (ServerView.key(entry.getServerData()).equals(divider.beforeKey())) anchor = entry.getServerData();
        }
        if (anchor != null && !servers.contains(anchor)) {
            ServerListBridge.moveAllNextTo(ui.servers(), servers, anchor, false);
        } else if (anchor == null && !shown.isEmpty() && !servers.contains(shown.getLast().getServerData())) {
            // Divider at the end: move the servers behind the last shown server
            ServerListBridge.moveAllNextTo(ui.servers(), servers, shown.getLast().getServerData(), true);
        }
        if (below) divider.setBeforeKey(ServerView.key(servers.getFirst()));
        ui.changed();
        ServerView.select(ui.list(), grabbed);
    }
}
