package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import me.prexorjustin.serverlistplus.ui.widget.TabStrip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;

import java.util.List;

/**
 * What the parts of the server list UI share: the screen, the data and a few actions that
 * affect the whole screen. Implemented by {@link ServerListUi}.
 */
interface UiContext {
    Screen screen();

    MultiplayerScreenHook hook();

    Font font();

    default Store store() {
        return ServerListPlus.store();
    }

    default ServerSelectionList list() {
        return hook().serverlistplus$list();
    }

    default ServerList servers() {
        return hook().serverlistplus$servers();
    }

    /** The tabs currently shown, as a row or as the sidebar. */
    TabStrip tabs();

    /** Saves, then rebuilds the list and the tabs after data changed. */
    void changed();

    /** Places tabs, list and header again, e.g. after the tab layout setting changed. */
    void relayout();

    /** Opens a right-click menu, closing any other one. */
    void openMenu(List<ContextMenu.Item> items, int x, int y);

    boolean menuOpen();

    /** Servers selected together with Shift or Ctrl/Cmd + click. */
    ServerSelection selection();

    /**
     * Vanilla focuses the clicked widget after a click, so a text field opened by that click
     * would lose the focus again. This one gets it once vanilla is done with the click.
     */
    void focusAfterClick(AbstractWidget widget);
}
