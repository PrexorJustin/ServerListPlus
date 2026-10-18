package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.UpdateCheck;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Links;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import me.prexorjustin.serverlistplus.ui.widget.Glyph;
import me.prexorjustin.serverlistplus.ui.widget.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Search and settings (which also holds the sort order) at the right end of the tab row. The search is an icon that turns
 * into a field when clicked and back into the icon once it is left empty. At the top of the tab
 * sidebar they fill its width instead, with the search field always open. An update icon joins
 * them while a newer release is available.
 */
final class HeaderRow {
    private static final Component HINT = Component.translatable("serverlistplus.search.hint")
        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    private static final Component SHORT_HINT = Component.translatable("serverlistplus.search.hint_short")
        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    /** Room the field's border and padding take from its text. */
    private static final int SEARCH_TEXT_INSET = 8;
    private static final int GAP = 2;
    private static final int SEARCH_MIN_WIDTH = 80;
    private static final int SEARCH_MAX_WIDTH = 150;
    private static final int SIDEBAR_SEARCH_MIN_WIDTH = 64;
    private static final int SIDEBAR_MIN_WIDTH = SIDEBAR_SEARCH_MIN_WIDTH + GAP + IconButton.SIZE;

    private final UiContext ui;
    private final IconButton searchButton;
    private final IconButton settingsButton;
    private final IconButton updateButton;
    /** The update icon has a place in the row; the check may finish after the screen opened. */
    private boolean updateShown;
    /** The search field is open instead of its icon: while it is in use or holds a search. */
    private boolean searchOpen;
    /** At the top of the sidebar: the search field stays open, even when empty. */
    private boolean inSidebar;
    private boolean tooltipsShown = true;

    HeaderRow(UiContext ui) {
        this.ui = ui;
        this.searchButton = new IconButton(Glyph.SEARCH, Component.translatable("serverlistplus.search.open"), button -> {
            openSearch();
            ui.focusAfterClick(search());
        }, () -> !ui.hook().serverlistplus$query().isEmpty());
        // Highlighted while a sort order is active, since that is set in its menu
        this.settingsButton = new IconButton(Glyph.SETTINGS, Component.translatable("serverlistplus.settings"), this::openSettingsMenu,
            () -> ServerView.sortMode() != SortMode.MANUAL);
        this.updateButton = new IconButton(Glyph.UPDATE, Component.empty(), this::openUpdateMenu, () -> true);
        updateTooltips();
        // At the top of the sidebar they lie on the list, so they must get the clicks there first
        for (IconButton button : List.of(searchButton, settingsButton, updateButton)) ui.hook().serverlistplus$addInFront(button);
        searchOpen = !ui.hook().serverlistplus$search().isEmpty();
    }

    /** The sidebar is at least this wide, so the search field above it stays usable next to the icons. */
    static int sidebarMinWidth() {
        return UpdateCheck.pending() != null ? SIDEBAR_MIN_WIDTH + IconButton.SIZE + GAP : SIDEBAR_MIN_WIDTH;
    }

    private EditBox search() {
        return ui.hook().serverlistplus$searchBox();
    }

    /**
     * Places the controls from {@code right} leftwards; the search field takes a third of what
     * is left up to {@code left}.
     *
     * @param shown false while something else (the wallpaper toolbar) takes the row
     * @return where the controls start, so the tabs end before it
     */
    int layout(int left, int right, int y, boolean shown) {
        // Coming from the sidebar, an empty search goes back to being the icon
        if (inSidebar) searchOpen = !search().getValue().isEmpty();
        inSidebar = false;
        int x = placeIcons(right, y, shown);
        EditBox search = search();
        search.setHint(HINT);
        search.setVisible(shown && searchOpen);
        searchButton.visible = searchButton.active = shown && !searchOpen;
        if (searchOpen) {
            int width = Math.clamp((x - left) / 3, SEARCH_MIN_WIDTH, SEARCH_MAX_WIDTH);
            x -= width;
            search.setRectangle(width, 20, x, y);
        } else {
            x -= IconButton.SIZE;
            searchButton.setPosition(x, y);
        }
        return x - GAP;
    }

    /**
     * Places settings at {@code right} and the open search field left of it, from
     * {@code left}: the top row of the tab sidebar. A narrow field gets the shorter hint.
     */
    void layoutInSidebar(int left, int right, int y, boolean shown) {
        inSidebar = true;
        searchOpen = true;
        int x = placeIcons(right, y, shown);
        searchButton.visible = searchButton.active = false;
        EditBox search = search();
        int width = Math.max(0, x - left);
        search.setHint(ui.font().width(HINT) <= width - SEARCH_TEXT_INSET ? HINT : SHORT_HINT);
        search.setVisible(shown);
        search.setRectangle(width, 20, left, y);
    }

    /** Places settings at {@code right} and the update icon left of it; returns where the next control ends. */
    private int placeIcons(int right, int y, boolean shown) {
        updateShown = UpdateCheck.pending() != null;
        updateButton.visible = updateButton.active = false;
        int x = right;
        for (IconButton button : updateShown ? List.of(settingsButton, updateButton) : List.of(settingsButton)) {
            x -= IconButton.SIZE;
            button.setPosition(x, y);
            button.visible = button.active = shown;
            x -= GAP;
        }
        return x;
    }

    /** Turns the search icon into the field and focuses it. */
    void openSearch() {
        searchOpen = true;
        ui.relayout();
        ui.hook().serverlistplus$focus(search());
    }

    private void closeSearch() {
        searchOpen = false;
        ui.relayout();
    }

    void tick() {
        if ((UpdateCheck.pending() != null) != updateShown) {
            updateTooltips();
            ui.relayout();
        }
        EditBox search = search();
        if (searchOpen && !inSidebar && !search.isFocused() && search.getValue().isEmpty()) closeSearch();
    }

    /** Clicking empty space doesn't take the focus away from a text field, so an empty search is closed here. */
    void pressAnywhere(Pointer pointer) {
        EditBox search = search();
        if (!searchOpen || inSidebar || search.isMouseOver(pointer.x(), pointer.y()) || !search.getValue().isEmpty()) return;
        if (search.isFocused()) ui.hook().serverlistplus$focus(null);
        closeSearch();
    }

    /** Escape in the empty search field closes it instead of leaving the screen. */
    boolean key(Key key) {
        EditBox search = search();
        if (key != Key.ESCAPE || !searchOpen || !search.isFocused() || !search.getValue().isEmpty()) return false;
        ui.hook().serverlistplus$focus(null);
        if (!inSidebar) closeSearch();
        return true;
    }

    private static Component sortLabel(SortMode mode) {
        return Component.translatable("serverlistplus.sort." + mode.name().toLowerCase(Locale.ROOT));
    }

    /** Tooltips would cover an open menu, so they are hidden meanwhile. */
    void showTooltips(boolean shown) {
        tooltipsShown = shown;
        updateTooltips();
    }

    /** The settings tooltip names an active sort order, so it changes with it. */
    void updateTooltips() {
        searchButton.setTooltip(tooltip(Component.translatable("serverlistplus.search.open")));
        SortMode sort = ServerView.sortMode();
        settingsButton.setTooltip(tooltip(sort == SortMode.MANUAL ? Component.translatable("serverlistplus.settings")
            : Component.translatable("serverlistplus.settings.sorted", sortLabel(sort))));
        String update = UpdateCheck.pending();
        updateButton.setTooltip(update == null ? null : tooltip(Component.translatable("serverlistplus.update.available", update)));
    }

    private Tooltip tooltip(Component text) {
        return tooltipsShown ? Tooltip.create(text) : null;
    }

    private List<ContextMenu.Item> sortItems() {
        List<ContextMenu.Item> items = new ArrayList<>();
        for (SortMode mode : SortMode.values()) {
            items.add(ContextMenu.Item.action(sortLabel(mode), () -> ui.hook().serverlistplus$setSort(mode))
                .checked(mode == ServerView.sortMode()));
        }
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.title(Component.translatable("serverlistplus.sort.pinned_first")));
        return items;
    }

    private void openSettingsMenu(IconButton button) {
        ViewSettings view = ui.store().view();
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.title(Component.translatable("serverlistplus.name")));
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.sort", sortLabel(ServerView.sortMode())), sortItems()));

        List<ContextMenu.Item> details = new ArrayList<>();
        for (DetailsMode mode : DetailsMode.values()) {
            details.add(ContextMenu.Item.action(Component.translatable("serverlistplus.details." + mode.name().toLowerCase(Locale.ROOT)), () -> {
                view.setDetails(mode);
                ServerListPlus.save();
            }).checked(mode == view.details()));
        }
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.details"), details));

        List<ContextMenu.Item> refresh = new ArrayList<>();
        for (int seconds : ViewSettings.AUTO_REFRESH_SECONDS) {
            Component label = seconds == 0 ? Component.translatable("serverlistplus.auto_refresh.off")
                : Component.translatable("serverlistplus.auto_refresh.every", seconds);
            refresh.add(ContextMenu.Item.action(label, () -> {
                view.setAutoRefreshSeconds(seconds);
                ServerListPlus.save();
            }).checked(seconds == view.autoRefreshSeconds()));
        }
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.auto_refresh"), refresh));

        List<ContextMenu.Item> layouts = new ArrayList<>();
        for (TabLayout layout : TabLayout.values()) {
            layouts.add(ContextMenu.Item.action(Component.translatable("serverlistplus.tab_layout." + layout.name().toLowerCase(Locale.ROOT)), () -> {
                view.setTabLayout(layout);
                ServerListPlus.save();
                ui.relayout();
                ui.tabs().scrollIntoView(ServerView.tab());
            }).checked(layout == view.tabLayout()));
        }
        layouts.add(ContextMenu.Item.separator());
        layouts.add(ContextMenu.Item.action(Component.translatable("serverlistplus.tab_layout.auto_hide"), () -> {
            view.setAutoHideSidebar(!view.autoHideSidebar());
            ServerListPlus.save();
            ui.relayout();
        }).checked(view.autoHideSidebar()));
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.tab_layout"), layouts));

        Store store = ui.store();
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.update.check"), () -> {
            store.setCheckUpdates(!store.checksUpdates());
            ServerListPlus.save();
            if (store.checksUpdates()) UpdateCheck.start();
        }).checked(store.checksUpdates()));
        ui.openMenu(items, button.getX(), button.getBottom() + 2);
    }

    private void openUpdateMenu(IconButton button) {
        String version = UpdateCheck.pending();
        if (version == null) return;
        List<ContextMenu.Item> items = List.of(
            ContextMenu.Item.title(Component.translatable("serverlistplus.update.available", version)),
            ContextMenu.Item.separator(),
            ContextMenu.Item.action(Component.translatable("serverlistplus.update.open"), () -> Links.open(ui.screen(), UpdateCheck.downloadPage())),
            ContextMenu.Item.action(Component.translatable("serverlistplus.update.dismiss"), () -> {
                ui.store().setDismissedUpdate(version);
                ServerListPlus.save();
            }));
        ui.openMenu(items, button.getX(), button.getBottom() + 2);
    }
}
