package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerEntryHook;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import me.prexorjustin.serverlistplus.ui.widget.TabStrip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

import java.util.List;

/**
 * Everything ServerListPlus adds to the multiplayer screen: tabs, the header controls, menus,
 * drag and drop and in-place editing. Created anew on every {@code init()}.
 * <p>
 * Lays the parts out and hands input to them in a fixed order: an open info card or wallpaper
 * toolbar first, then an open menu, then whatever is under the pointer.
 */
public final class ServerListUi implements UiContext {
    /** Space reserved above the server list for the tab row. */
    public static final int TAB_ROW = 24;
    /** Space at the top of the sidebar for the header controls. */
    private static final int SIDEBAR_HEADER = 2 + 20 + 4;
    private static final int MARGIN = 6;
    /** Narrowest list next to the docked info card: rows of 230 instead of vanilla's 305 (see ServerSelectionListMixin). */
    private static final int MIN_DOCKED_LIST_WIDTH = 260;
    private static final int VANILLA_ROW_WIDTH = 305;
    /** Room a row leaves at the sides of the list for the scroll bar. */
    public static final int ROW_SIDE_ROOM = 30;

    private final Screen screen;
    private final MultiplayerScreenHook hook;
    private final Font font = Minecraft.getInstance().font;

    private final HeaderRow header;
    private final TabController tabs;
    private final WallpaperAdjuster wallpaper;
    private final DividerController dividers;
    private final ServerDragging dragging;
    private final ServerMenu serverMenu;
    private final InfoCardController infoCard;

    private ContextMenu menu;
    private AbstractWidget focusAfterClick;
    private final ServerSelection selection = new ServerSelection();
    /** Pressed one of several selected servers: unless it becomes a drag, only that one stays selected. */
    private ServerData selectOnlyOnRelease;

    /** Where vanilla put the list, before making room for the tabs; width 0 until known. */
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    public <S extends Screen & MultiplayerScreenHook> ServerListUi(S screen) {
        this.screen = screen;
        this.hook = screen;
        this.wallpaper = new WallpaperAdjuster(this);
        StyleMenus styles = new StyleMenus(this, this::adjustWallpaper);
        this.tabs = new TabController(this, styles);
        this.header = new HeaderRow(this);
        this.dividers = new DividerController(this, styles);
        this.dragging = new ServerDragging(this);
        this.infoCard = new InfoCardController(this);
        this.serverMenu = new ServerMenu(this, tabs, dividers, infoCard);
        tabs.update();
    }

    @Override
    public Screen screen() {
        return screen;
    }

    @Override
    public MultiplayerScreenHook hook() {
        return hook;
    }

    @Override
    public Font font() {
        return font;
    }

    @Override
    public TabStrip tabs() {
        return tabs.strip();
    }

    @Override
    public void changed() {
        ServerListPlus.save();
        hook.serverlistplus$refresh();
        tabs.update();
        relayout();
    }

    @Override
    public void openMenu(List<ContextMenu.Item> items, int x, int y) {
        closeMenu();
        menu = new ContextMenu(font, items, x, y, screen.width, screen.height);
        hook.serverlistplus$addOverlay(menu);
        header.showTooltips(false);
    }

    @Override
    public boolean menuOpen() {
        return menu != null;
    }

    @Override
    public ServerSelection selection() {
        return selection;
    }

    public boolean isMultiSelected(ServerData server) {
        return selection.contains(server);
    }

    /** Vanilla's selected row changed. */
    public void selectedChanged() {
        selection.selectedChanged(list());
    }

    private void closeMenu() {
        if (menu == null) return;
        hook.serverlistplus$remove(menu);
        menu = null;
        header.showTooltips(true);
    }

    @Override
    public void focusAfterClick(AbstractWidget widget) {
        focusAfterClick = widget;
    }

    /** Call right after vanilla positioned the list: makes room for the tabs above or next to it. */
    public void layoutList() {
        ServerSelectionList list = list();
        listX = list.getX();
        listY = list.getY();
        listWidth = list.getWidth();
        listHeight = list.getHeight();
        relayout();
        tabs().scrollIntoView(ServerView.tab());
    }

    /**
     * The tab row above the list, or the sidebar on its left with the header controls at its top, and an open info card to the right of it when there is room.
     * Can be repeated, e.g. when tab names change.
     */
    @Override
    public void relayout() {
        if (listWidth == 0) return;
        int sidebarWidth = tabs.chooseLayout(listWidth, MARGIN);
        boolean shown = !wallpaper.isActive();
        // The sidebar carries the header controls, so only the tab row and the wallpaper toolbar need room above the list
        int tabRow = sidebarWidth > 0 && shown ? 0 : TAB_ROW;
        int top = listY + tabRow;
        int height = listHeight - tabRow;
        if (sidebarWidth > 0) tabs.placeSidebar(listX + MARGIN, top + SIDEBAR_HEADER, height - SIDEBAR_HEADER, top, MARGIN);
        // The room for the rows: right of the sidebar, left of a docked card
        int roomX = listX + sidebarWidth;
        int roomWidth = listWidth - sidebarWidth;
        InfoCardController.Dock dock = null;
        if (infoCard.isOpen()) {
            int cardWidth = Math.min(ServerInfoPanel.WIDTH, roomWidth - MIN_DOCKED_LIST_WIDTH - 2 * MARGIN);
            if (cardWidth >= ServerInfoPanel.MIN_WIDTH) {
                roomWidth -= cardWidth + 2 * MARGIN;
                // From the tab row, if any, down to the end of the list; the tab row stays left of it
                dock = new InfoCardController.Dock(roomX + roomWidth + MARGIN, listY + 2, cardWidth, listHeight - 2);
            }
        }
        // The list keeps its left edge, so the sidebar sits on its background and the rows stay
        // centered under the title. Only when centered rows would run into the sidebar does the
        // list start next to it.
        int x = listX;
        int width = roomX + roomWidth - listX;
        if (sidebarWidth > 0 && x + (width - rowWidth(width)) / 2 < roomX) {
            x = roomX;
            width = roomWidth;
        }
        ServerSelectionList list = list();
        if (list.getX() != x || list.getY() != top || list.getWidth() != width || list.getHeight() != height) {
            // Since 1.21.9 rows store their position, so x must be known when they are placed
            //? if >=1.21.9 {
            list.updateSizeAndPosition(width, height, x, top);
            //?} else {
            /*list.updateSizeAndPosition(width, height, top);
            list.setX(x);
            *///?}
        }

        int rowY = listY + 2;
        int rowRight = dock != null ? dock.x() - MARGIN : screen.width - MARGIN;
        if (sidebarWidth > 0) {
            int slide = tabs.slide();
            header.layoutInSidebar(listX + MARGIN + slide, listX + sidebarWidth + slide, top + 2, shown && !tabs.collapsed());
        } else {
            int controls = header.layout(MARGIN, rowRight, rowY, shown);
            tabs.placeRow(MARGIN, rowY, controls - MARGIN, x + width / 2);
        }
        tabs.show(shown);
        infoCard.place(dock);
    }

    /** Width of the rows in a list of the given width, see ServerSelectionListMixin. */
    public static int rowWidth(int listWidth) {
        return Math.min(VANILLA_ROW_WIDTH, listWidth - ROW_SIDE_ROOM);
    }

    private void adjustWallpaper(String tab, Styled style) {
        tabs.finishEditing(true);
        tabs.selectTab(tab);
        wallpaper.start(tab, style, MARGIN, listY + 2);
        relayout();
    }

    /** Called every frame before the widgets are drawn. */
    public void frame() {
        tabs.frame();
    }

    /** Called on every pointer move, also while dragging. */
    public void pointerMoved(double x, double y) {
        tabs.pointerMoved(x, y);
    }

    public void tick() {
        header.tick();
        tabs.tick();
        dividers.tick();
        // The docked card shows whichever server is selected
        if (infoCard.isDocked() && list().getSelected() instanceof ServerSelectionList.OnlineServerEntry online
            && online.getServerData() != infoCard.server()) {
            infoCard.show(online.getServerData());
        }
    }

    public void openSearch() {
        tabs.expand();
        header.openSearch();
    }

    public void updateSortTooltip() {
        header.updateTooltips();
    }

    /** The screen is left (joining a server, editing one, going back): keeps what was typed. */
    public void close() {
        infoCard.closeQuietly();
        ServerListPlus.save();
    }

    /** Whether the info card lies over the list, rather than docked next to it. */
    public boolean isCardCovering() {
        return infoCard.isCovering();
    }

    /** @return true if the scroll was used and vanilla must not handle it */
    public boolean scroll(double x, double y, double amount) {
        return wallpaper.scroll(x, y, amount);
    }

    /** @return true if the click was used and vanilla must not handle it */
    public boolean press(Pointer pointer) {
        focusAfterClick = null;
        header.pressAnywhere(pointer);
        if (infoCard.isOpen()) {
            switch (infoCard.press(pointer)) {
                case USED -> {
                    return true;
                }
                case INPUT -> {
                    return false;
                }
                case ELSEWHERE -> {
                }
            }
        }
        if (wallpaper.isActive()) return wallpaper.press(pointer);
        if (menu != null) {
            if (!menu.contains(pointer.x(), pointer.y()) || menu.click(pointer.x(), pointer.y())) closeMenu();
            return true;
        }
        tabs.pressAnywhere(pointer);
        dividers.pressAnywhere(pointer);

        ServerSelectionList.Entry over = ServerListBridge.childAt(list(), pointer.x(), pointer.y());
        if (over instanceof HiddenSectionEntry) {
            if (pointer.isLeft()) {
                hook.serverlistplus$setHiddenExpanded(!hook.serverlistplus$hiddenExpanded());
                hook.serverlistplus$refresh();
            }
            return true;
        }
        if (over instanceof DividerEntry divider) {
            dragging.press(null, pointer);
            return dividers.press(divider, pointer);
        }
        dividers.pressElsewhere();

        ServerSelectionList.OnlineServerEntry entry = over instanceof ServerSelectionList.OnlineServerEntry online ? online : null;
        if (pointer.isLeft() && entry != null && ((ServerEntryHook) entry).serverlistplus$isOverNotes(pointer.x(), pointer.y())) {
            list().setSelected(entry);
            infoCard.open(entry.getServerData());
            return true;
        }
        if (pointer.isRight() && entry != null && selection.contains(entry.getServerData())) {
            serverMenu.openForSelection(selection.shown(list()), (int) pointer.x(), (int) pointer.y());
            return true;
        }
        if (pointer.isLeft() && entry != null && (pointer.shift() || pointer.control())) {
            // Also keeps vanilla from joining on a double click or a click on the icon
            selection.extend(list(), entry.getServerData(), pointer.shift());
            return true;
        }
        if (pointer.isLeft() && entry != null) {
            if (selection.contains(entry.getServerData())) selectOnlyOnRelease = entry.getServerData();
            else selection.single(entry.getServerData());
        }
        if (pointer.isRight() && entry != null) {
            list().setSelected(entry);
            serverMenu.open(entry.getServerData(), (int) pointer.x(), (int) pointer.y());
            return true;
        }
        if (pointer.isLeft()) dragging.press(entry, pointer);
        return false;
    }

    /** Call after vanilla handled a click. */
    public void afterClick() {
        if (focusAfterClick != null) hook.serverlistplus$focus(focusAfterClick);
        focusAfterClick = null;
    }

    public boolean drag(Pointer pointer) {
        if (dividers.isDragging()) return dividers.drag(pointer);
        if (wallpaper.drag(pointer)) return true;
        if (tabs.drag(pointer)) return true;
        return dragging.drag(pointer);
    }

    public boolean release(Pointer pointer) {
        if (wallpaper.release()) return true;
        if (dividers.isDragging()) return dividers.release();
        if (tabs.release()) return true;
        boolean dropped = dragging.release();
        if (!dropped && selectOnlyOnRelease != null) selection.single(selectOnlyOnRelease);
        selectOnlyOnRelease = null;
        return dropped;
    }

    /** @return true if the key was used and vanilla must not handle it */
    public boolean key(Key key) {
        if (infoCard.isOpen()) {
            if (infoCard.key(key)) return true;
            // Typing in the card is for the card; next to a docked card the rest works as usual
            if (infoCard.isCovering() || infoCard.hasFocus()) return false;
        }
        if (wallpaper.key(key) || dividers.key(key) || tabs.key(key)) return true;
        if (menu != null && key == Key.ESCAPE) {
            closeMenu();
            return true;
        }
        if (key == Key.ESCAPE && selection.isMulti() && !hook.serverlistplus$searchBox().isFocused()) {
            selection.clear();
            return true;
        }
        return header.key(key);
    }
}
