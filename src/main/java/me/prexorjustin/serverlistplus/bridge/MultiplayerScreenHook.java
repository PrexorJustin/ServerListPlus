package me.prexorjustin.serverlistplus.bridge;

import me.prexorjustin.serverlistplus.core.query.Query;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

/** Implemented by {@code JoinMultiplayerScreen} via mixin, exposes its state and protected helpers. */
public interface MultiplayerScreenHook {
    Query serverlistplus$query();

    /** Whether the info card lies over the list, so rows must not react to the pointer. */
    boolean serverlistplus$isCardCovering();

    String serverlistplus$search();

    void serverlistplus$setSearch(String value);

    EditBox serverlistplus$searchBox();

    /** Whether the servers hidden from "All" are folded out at its end; kept while the screen is open. */
    boolean serverlistplus$hiddenExpanded();

    void serverlistplus$setHiddenExpanded(boolean expanded);

    ServerSelectionList serverlistplus$list();

    ServerList serverlistplus$servers();

    void serverlistplus$add(AbstractWidget widget);

    /** Adds a widget that is only drawn, on top of everything added before. */
    void serverlistplus$addOverlay(AbstractWidget widget);

    /** One of several servers selected together; vanilla only draws its own selected row. */
    boolean serverlistplus$isMultiSelected(ServerData server);

    /** Called every frame while the list is drawn, before the widgets on top of it. */
    void serverlistplus$frame();

    /** Adds a widget that gets clicks before everything else, also where it lies over the list. */
    void serverlistplus$addInFront(AbstractWidget widget);

    void serverlistplus$remove(AbstractWidget widget);

    void serverlistplus$focus(AbstractWidget widget);

    void serverlistplus$join(ServerData server);

    void serverlistplus$setSort(SortMode mode);

    /** Rebuilds the server list from the current search, tab and sort order. */
    void serverlistplus$refresh();
}
