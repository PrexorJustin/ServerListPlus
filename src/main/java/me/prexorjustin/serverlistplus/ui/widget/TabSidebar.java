package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Pointer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The tabs as a column left of the server list, with "+" below them.
 * Scrolls vertically when the tabs don't fit.
 */
public final class TabSidebar extends TabStrip {
    public static final int MIN_WIDTH = 80;
    public static final int MAX_WIDTH = 140;
    private static final String SCROLL_UP = "^";
    private static final String SCROLL_DOWN = "v";
    private static final int ROW = 16;
    private static final int ARROW_HEIGHT = 10;
    private static final int GAP = 1;
    private static final int PAD = 6;
    private static final int ACCENT_WIDTH = 2;
    private static final int ICON_GAP = 3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA8A8A8;
    private static final int COUNT_COLOR = 0xFF7A7A7A;
    private static final int HOVER_BG = 0x18FFFFFF;
    private static final int SELECTED_ALPHA = 0x40;
    private static final int HOVER_ALPHA = 0x20;
    private static final int ACCENT = 0xFFFFFFFF;
    private static final int DROP_OK = 0xFF55FF55;
    private static final int DROP_BAD = 0xFFFF5555;
    private static final int ARROW_DISABLED = 0xFF505050;
    private static final int EDITING_OUTLINE = 0x80FFFFFF;
    private static final Component NEW_FOLDER = Component.translatable("serverlistplus.folder.new");

    private final Font font;
    private final Listener listener;
    private List<Tab> tabs = List.of();
    private String selected;
    /** Index of the first tab shown while not all tabs fit. */
    private int first;
    private String dropTarget;
    private boolean dropValid;
    private String editing;

    public TabSidebar(Font font, Listener listener) {
        super(MIN_WIDTH, 0);
        this.font = font;
        this.listener = listener;
    }

    @Override
    public boolean vertical() {
        return true;
    }

    @Override
    public void setTabs(List<Tab> tabs, String selected) {
        this.tabs = List.copyOf(tabs);
        this.selected = selected;
        clampScroll();
    }

    @Override
    public List<Tab> tabs() {
        return tabs;
    }

    @Override
    public void setDropTarget(String id, boolean valid) {
        this.dropTarget = id;
        this.dropValid = valid;
    }

    @Override
    public void setEditing(String id, int textWidth) {
        this.editing = id;
    }

    @Override
    public int textInset(String id) {
        return ACCENT_WIDTH + PAD + tabs.stream().filter(tab -> tab.id().equals(id)).findFirst().map(this::iconWidth).orElse(0);
    }

    @Override
    public int editorWidth(String id, int textWidth) {
        return Math.max(10, this.getWidth() - textInset(id) - PAD);
    }

    private int iconWidth(Tab tab) {
        return tab.icon() == null ? 0 : font.width(tab.icon()) + ICON_GAP;
    }

    private String countText(Tab tab) {
        return tab.count() >= 0 ? String.valueOf(tab.count()) : "";
    }

    /** Wide enough for the longest tab and "+ New folder", within {@link #MIN_WIDTH} and {@link #MAX_WIDTH}. */
    public int preferredWidth() {
        int widest = ACCENT_WIDTH + PAD + font.width(NEW_TAB) + ICON_GAP + font.width(NEW_FOLDER) + PAD;
        for (Tab tab : tabs) {
            widest = Math.max(widest, ACCENT_WIDTH + PAD + iconWidth(tab) + font.width(tab.label()) + 6 + font.width(countText(tab)) + PAD);
        }
        return Math.clamp(widest, MIN_WIDTH, MAX_WIDTH);
    }

    private int contentHeight() {
        return tabs.size() * (ROW + GAP) + ROW;
    }

    private boolean overflows() {
        return contentHeight() > this.getHeight();
    }

    /** How many tabs fit between the scroll arrows, above "+". */
    private int rowsWhenScrolling() {
        int room = this.getHeight() - 2 * (ARROW_HEIGHT + GAP) - ROW - GAP;
        return Math.max(1, room / (ROW + GAP));
    }

    private void clampScroll() {
        if (!overflows()) {
            first = 0;
            return;
        }
        first = Math.max(0, Math.min(first, tabs.size() - rowsWhenScrolling()));
    }

    /**
     * Tabs top to bottom with "+" right below them. When not all tabs fit, the tabs from
     * {@link #first} are shown between scroll arrows and "+" stays at the bottom.
     */
    @Override
    public List<Slot> slots() {
        List<Slot> slots = new ArrayList<>();
        int x = this.getX();
        int width = this.getWidth();
        int y = this.getY();
        if (!overflows()) {
            for (Tab tab : tabs) {
                slots.add(new Slot(tab.id(), x, y, width, ROW));
                y += ROW + GAP;
            }
            slots.add(new Slot(NEW_TAB, x, y, width, ROW));
            return slots;
        }

        slots.add(new Slot(SCROLL_UP, x, y, width, ARROW_HEIGHT));
        y += ARROW_HEIGHT + GAP;
        int last = Math.min(tabs.size(), first + rowsWhenScrolling());
        for (int i = first; i < last; i++) {
            slots.add(new Slot(tabs.get(i).id(), x, y, width, ROW));
            y += ROW + GAP;
        }
        int plusY = this.getBottom() - ROW;
        slots.add(new Slot(SCROLL_DOWN, x, plusY - GAP - ARROW_HEIGHT, width, ARROW_HEIGHT));
        slots.add(new Slot(NEW_TAB, x, plusY, width, ROW));
        return slots;
    }

    private String slotAt(double x, double y) {
        if (!this.isMouseOver(x, y)) return null;
        for (Slot slot : slots()) {
            if (y >= slot.y() && y < slot.bottom()) return slot.id();
        }
        return null;
    }

    @Override
    public String tabAt(double x, double y) {
        String id = slotAt(x, y);
        return SCROLL_UP.equals(id) || SCROLL_DOWN.equals(id) ? null : id;
    }

    private boolean canScrollUp() {
        return overflows() && first > 0;
    }

    private boolean canScrollDown() {
        return overflows() && first + rowsWhenScrolling() < tabs.size();
    }

    private void scrollBy(int rows) {
        first += rows;
        clampScroll();
    }

    @Override
    public void scrollIntoView(String id) {
        if (!overflows()) return;
        for (int i = 0; i < tabs.size(); i++) {
            if (!tabs.get(i).id().equals(id)) continue;
            if (i < first) first = i;
            else if (i >= first + rowsWhenScrolling()) first = i - rowsWhenScrolling() + 1;
        }
        clampScroll();
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        String hovered = slotAt(mouseX, mouseY);
        for (Slot slot : slots()) {
            boolean isHovered = slot.id().equals(hovered);
            int textY = slot.y() + (slot.height() - 8) / 2;
            if (slot.id().equals(SCROLL_UP) || slot.id().equals(SCROLL_DOWN)) {
                boolean enabled = slot.id().equals(SCROLL_UP) ? canScrollUp() : canScrollDown();
                if (enabled && isHovered) gfx.roundedRect(slot.x(), slot.y(), slot.right(), slot.bottom(), HOVER_BG);
                String arrow = slot.id().equals(SCROLL_UP) ? "▴" : "▾";
                gfx.text(font, Component.literal(arrow), slot.x() + (slot.width() - font.width(arrow)) / 2, slot.y() + (slot.height() - 8) / 2 + 1,
                    enabled ? TEXT_COLOR : ARROW_DISABLED);
                continue;
            }
            if (slot.id().equals(NEW_TAB)) {
                // "+" in the icon column, the label lined up with the tab names; dimmer than them, as it isn't a tab
                if (isHovered) gfx.roundedRect(slot.x(), slot.y(), slot.right(), slot.bottom(), HOVER_BG);
                int color = isHovered ? TEXT_COLOR : COUNT_COLOR;
                int x = slot.x() + ACCENT_WIDTH + PAD;
                gfx.text(font, Component.literal(NEW_TAB), x, textY, color);
                x += font.width(NEW_TAB) + ICON_GAP;
                gfx.text(font, fit(NEW_FOLDER, slot.right() - PAD - x), x, textY, color);
                continue;
            }

            Tab tab = tabs.stream().filter(t -> t.id().equals(slot.id())).findFirst().orElseThrow();
            boolean isSelected = slot.id().equals(selected);
            int accent = tab.color() != 0 ? tab.color() : ACCENT;
            if (isSelected) gfx.roundedRect(slot.x(), slot.y(), slot.right(), slot.bottom(), tint(accent, SELECTED_ALPHA));
            else if (isHovered) gfx.roundedRect(slot.x(), slot.y(), slot.right(), slot.bottom(), tint(accent, HOVER_ALPHA));
            if (isSelected) gfx.fill(slot.x(), slot.y() + 2, slot.x() + ACCENT_WIDTH, slot.bottom() - 2, accent | 0xFF000000);
            if (slot.id().equals(dropTarget)) gfx.outline(slot.x(), slot.y(), slot.right(), slot.bottom(), dropValid ? DROP_OK : DROP_BAD);
            if (slot.id().equals(editing)) gfx.outline(slot.x(), slot.y(), slot.right(), slot.bottom(), EDITING_OUTLINE);

            int x = slot.x() + ACCENT_WIDTH + PAD;
            if (tab.icon() != null) {
                gfx.text(font, Component.literal(tab.icon()), x, textY, tab.color() != 0 ? accent | 0xFF000000 : TEXT_DIM);
                x += iconWidth(tab);
            }
            if (slot.id().equals(editing)) continue;
            String count = countText(tab);
            int countX = slot.right() - PAD - font.width(count);
            gfx.text(font, Component.literal(count), countX, textY, COUNT_COLOR);
            gfx.text(font, fit(tab.label(), countX - 4 - x), x, textY, isSelected || isHovered ? TEXT_COLOR : TEXT_DIM);
        }
    }

    private Component fit(Component label, int width) {
        if (font.width(label) <= width) return label;
        String text = font.plainSubstrByWidth(label.getString(), Math.max(0, width - font.width("…")));
        return Component.literal(text + "…").withStyle(label.getStyle());
    }

    private static int tint(int color, int alpha) {
        return alpha << 24 | color & 0xFFFFFF;
    }

    @Override
    protected boolean press(Pointer pointer, boolean doubleClick) {
        String slot = slotAt(pointer.x(), pointer.y());
        if (SCROLL_UP.equals(slot) || SCROLL_DOWN.equals(slot)) {
            if (pointer.isLeft()) scrollBy(SCROLL_UP.equals(slot) ? -1 : 1);
            return true;
        }
        if (slot == null) return false;
        if (pointer.isRight()) {
            if (!slot.equals(NEW_TAB)) listener.openTabMenu(slot, (int) pointer.x(), (int) pointer.y());
            return true;
        }
        if (!pointer.isLeft()) return false;
        if (slot.equals(NEW_TAB)) listener.createFolder();
        else if (doubleClick) listener.renameTab(slot);
        else listener.selectTab(slot);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;
        // Used even when there's nothing to scroll, so the list below doesn't scroll instead
        if (overflows()) scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }
}
