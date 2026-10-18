package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Servers selected together with Shift + click (a range) or Ctrl/Cmd + click (one more or one
 * less), to be dragged or put into folders at once. Vanilla's selected row is always one of them.
 * Like the rows, servers are told apart by identity.
 */
final class ServerSelection {
    private final Set<ServerData> servers = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Where a Shift + click range starts: the server clicked last without Shift. */
    private ServerData anchor;

    /** More than one server is selected; a single one is just vanilla's selection. */
    boolean isMulti() {
        return servers.size() > 1;
    }

    boolean contains(ServerData server) {
        return isMulti() && servers.contains(server);
    }

    void clear() {
        servers.clear();
    }

    /** A plain click: only this server is selected, and a later range starts here. */
    void single(ServerData server) {
        servers.clear();
        anchor = server;
    }

    /** The selected servers that are shown, in the order they are shown. */
    List<ServerData> shown(ServerSelectionList list) {
        List<ServerData> shown = new ArrayList<>();
        for (ServerSelectionList.OnlineServerEntry entry : ServerListBridge.shownServers(list)) {
            if (servers.contains(entry.getServerData())) shown.add(entry.getServerData());
        }
        return shown;
    }

    /**
     * Shift: selects everything from the anchor to {@code clicked}. Otherwise adds {@code clicked},
     * or removes it if it was selected. Then selects a selected row in the list, preferably the clicked one.
     */
    void extend(ServerSelectionList list, ServerData clicked, boolean range) {
        ServerData current = list.getSelected() instanceof ServerSelectionList.OnlineServerEntry online ? online.getServerData() : null;
        if (servers.isEmpty() && current != null) servers.add(current);
        if (anchor == null || !servers.contains(anchor)) anchor = current != null ? current : clicked;
        if (range) {
            List<ServerData> shown = ServerListBridge.shownServers(list).stream().map(ServerSelectionList.OnlineServerEntry::getServerData).toList();
            int from = shown.indexOf(anchor);
            int to = shown.indexOf(clicked);
            if (from < 0) from = to;
            servers.clear();
            for (int i = Math.min(from, to); i <= Math.max(from, to); i++) servers.add(shown.get(i));
        } else {
            if (!servers.add(clicked)) servers.remove(clicked);
            anchor = clicked;
        }
        ServerData select = servers.contains(clicked) ? clicked : servers.stream().findFirst().orElse(null);
        if (select != null) ServerView.select(list, select);
        else list.setSelected(null);
    }

    /**
     * Vanilla's selection moved, e.g. with the arrow keys: outside the selected servers, it starts
     * over. No selection at all only happens for a moment while the list is rebuilt, so it is ignored.
     */
    void selectedChanged(ServerSelectionList list) {
        ServerSelectionList.Entry selected = list.getSelected();
        if (selected == null) return;
        if (!(selected instanceof ServerSelectionList.OnlineServerEntry online) || !servers.contains(online.getServerData())) {
            servers.clear();
        }
    }
}
