package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Pointer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A row of tabs above the server list: every server, one tab per folder and "+".
 * Scrolls horizontally when the tabs don't fit, and then also offers a list of all tabs.
 */
public final class TabBar extends TabStrip {
    private static final String SCROLL_LEFT = "<";
    private static final String SCROLL_RIGHT = ">";
    private static final String ALL_TABS = "v";
    private static final int ARROW_WIDTH = 12;

    private static final int PAD = 7;
    private static final int GAP = 2;
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
    private static final int ICON_GAP = 3;

    private final Font font;
    private final Listener listener;
    private List<Tab> tabs = List.of();
    private String selected;
    /** Index of the first tab shown while not all tabs fit. */
    private int first;
    private String dropTarget;
    private boolean dropValid;
    private String editing;
    private int editingTextWidth;
    private int center;

    public TabBar(Font font, Listener listener) {
        super(0, 20);
        this.font = font;
        this.listener = listener;
    }

    @Override
    public boolean vertical() {
        return false;
    }

    /** Where the tabs are centered while they all fit; they move aside to stay inside the bar. */
    public void setCenter(int center) {
        this.center = center;
    }

    @Override
    public void setTabs(List<Tab> tabs, String selected) {
        this.tabs = List.copyOf(tabs);
        this.selected = selected;
        clampScroll();
    }

    @Override
    public void setDropTarget(String id, boolean valid) {
        this.dropTarget = id;
        this.dropValid = valid;
    }

    /** The tab is sized for {@code textWidth}, so the field on top fits and the following tabs move aside. */
    @Override
    public void setEditing(String id, int textWidth) {
        this.editing = id;
        this.editingTextWidth = textWidth;
        clampScroll();
    }

    @Override
    public int textInset(String id) {
        return PAD + tabs.stream().filter(tab -> tab.id().equals(id)).findFirst().map(this::iconWidth).orElse(0);
    }

    @Override
    public int editorWidth(String id, int textWidth) {
        return textWidth;
    }

    private int iconWidth(Tab tab) {
        return tab.icon() == null ? 0 : font.width(tab.icon()) + ICON_GAP;
    }

    private int tabWidth(Tab tab) {
        if (tab.id().equals(editing)) return iconWidth(tab) + editingTextWidth + 2 * PAD;
        int width = iconWidth(tab) + font.width(tab.label()) + 2 * PAD;
        if (tab.count() >= 0) width += font.width(" " + tab.count());
        return width;
    }

    private int newTabWidth() {
        return font.width(NEW_TAB) + 2 * PAD;
    }

    /** Width needed to show every tab without scrolling. */
    public int contentWidth() {
        int total = 0;
        for (Tab tab : tabs) total += tabWidth(tab) + GAP;
        return total + newTabWidth();
    }

    private boolean overflows() {
        return contentWidth() > this.getWidth();
    }

    private void clampScroll() {
        first = Math.max(0, Math.min(first, tabs.size() - 1));
        // Don't leave empty room at the end when scrolled further than needed
        while (first > 0 && fitsFrom(first - 1)) first--;
    }

    /** Whether all tabs from {@code index} to the end fit next to the arrows, the tab list and "+". */
    private boolean fitsFrom(int index) {
        int room = this.getWidth() - 3 * ARROW_WIDTH - GAP - newTabWidth() - GAP;
        int used = 0;
        for (int i = index; i < tabs.size(); i++) used += tabWidth(tabs.get(i)) + GAP;
        return used <= room;
    }

    /**
     * Visible tabs with their positions. When not all tabs fit, whole tabs are shown starting at
     * {@link #first}, framed by scroll arrows, and the tab list and "+" stay at the right edge.
     */
    @Override
    public List<Slot> slots() {
        List<Slot> slots = new ArrayList<>();
        if (!overflows()) {
            // Everything fits: centered on the given point, as far as the bar allows
            int x = Math.clamp(center - contentWidth() / 2, this.getX(), this.getRight() - contentWidth());
            for (Tab tab : tabs) {
                int width = tabWidth(tab);
                slots.add(new Slot(tab.id(), x, getY(), width, getHeight()));
                x += width + GAP;
            }
            slots.add(new Slot(NEW_TAB, x, getY(), newTabWidth(), getHeight()));
            return slots;
        }

        int plusX = this.getRight() - newTabWidth();
        int listX = plusX - GAP - ARROW_WIDTH;
        int arrowRightX = listX - GAP - ARROW_WIDTH;
        slots.add(new Slot(SCROLL_LEFT, this.getX(), getY(), ARROW_WIDTH, getHeight()));
        int x = this.getX() + ARROW_WIDTH + GAP;
        for (int i = first; i < tabs.size(); i++) {
            int width = tabWidth(tabs.get(i));
            if (x + width > arrowRightX - GAP) break;
            slots.add(new Slot(tabs.get(i).id(), x, getY(), width, getHeight()));
            x += width + GAP;
        }
        slots.add(new Slot(SCROLL_RIGHT, arrowRightX, getY(), ARROW_WIDTH, getHeight()));
        slots.add(new Slot(ALL_TABS, listX, getY(), ARROW_WIDTH, getHeight()));
        slots.add(new Slot(NEW_TAB, plusX, getY(), newTabWidth(), getHeight()));
        return slots;
    }

    @Override
    public List<Tab> tabs() {
        return tabs;
    }

    @Override
    public String tabAt(double x, double y) {
        String id = slotAt(x, y);
        return SCROLL_LEFT.equals(id) || SCROLL_RIGHT.equals(id) || ALL_TABS.equals(id) ? null : id;
    }

    private String slotAt(double x, double y) {
        if (!this.isMouseOver(x, y)) return null;
        for (Slot slot : slots()) {
            if (x >= slot.x() && x < slot.right()) return slot.id();
        }
        return null;
    }

    private boolean canScrollLeft() {
        return overflows() && first > 0;
    }

    private boolean canScrollRight() {
        if (!overflows()) return false;
        List<Slot> slots = slots();
        // The last real tab is shown when the slot before the right arrow is it
        String lastShown = slots.get(slots.size() - 4).id();
        return !tabs.isEmpty() && !tabs.getLast().id().equals(lastShown);
    }

    private void scrollBy(int tabsToMove) {
        if (tabsToMove > 0 && !canScrollRight()) return;
        first += tabsToMove;
        clampScroll();
    }

    @Override
    public void scrollIntoView(String id) {
        int index = -1;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(id)) index = i;
        }
        if (index < 0 || !overflows()) return;
        if (index < first) {
            first = index;
        } else {
            while (first < index && slot(id).isEmpty()) first++;
        }
        clampScroll();
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        int top = this.getY();
        int bottom = top + this.getHeight();
        String hovered = slotAt(mouseX, mouseY);

        for (Slot slot : slots()) {
            if (slot.id().equals(SCROLL_LEFT) || slot.id().equals(SCROLL_RIGHT)) {
                boolean enabled = slot.id().equals(SCROLL_LEFT) ? canScrollLeft() : canScrollRight();
                if (enabled && isHoveredSlot(slot, hovered)) gfx.roundedRect(slot.x(), top, slot.right(), bottom, HOVER_BG);
                String arrow = slot.id().equals(SCROLL_LEFT) ? "‹" : "›";
                gfx.text(font, Component.literal(arrow), slot.x() + (ARROW_WIDTH - font.width(arrow)) / 2, top + (this.getHeight() - 8) / 2,
                    enabled ? TEXT_COLOR : ARROW_DISABLED);
                continue;
            }
            if (slot.id().equals(ALL_TABS)) {
                if (isHoveredSlot(slot, hovered)) gfx.roundedRect(slot.x(), top, slot.right(), bottom, HOVER_BG);
                gfx.text(font, Component.literal("▾"), slot.x() + (ARROW_WIDTH - font.width("▾")) / 2, top + (this.getHeight() - 8) / 2,
                    isHoveredSlot(slot, hovered) ? TEXT_COLOR : TEXT_DIM);
                continue;
            }
            boolean isNew = slot.id().equals(NEW_TAB);
            boolean isSelected = slot.id().equals(selected);
            boolean isHovered = slot.id().equals(hovered);
            int textY = top + (this.getHeight() - 8) / 2;
            if (isNew) {
                if (isHovered) gfx.roundedRect(slot.x(), top, slot.right(), bottom, HOVER_BG);
                gfx.text(font, Component.literal(NEW_TAB), slot.x() + PAD, textY, isHovered ? TEXT_COLOR : TEXT_DIM);
                continue;
            }

            Tab tab = tabs.stream().filter(t -> t.id().equals(slot.id())).findFirst().orElseThrow();
            int accent = tab.color() != 0 ? tab.color() : ACCENT;
            // Selected and hovered tabs are tinted with their own color
            if (isSelected) gfx.roundedRect(slot.x(), top, slot.right(), bottom, tint(accent, SELECTED_ALPHA));
            else if (isHovered) gfx.roundedRect(slot.x(), top, slot.right(), bottom, tint(accent, HOVER_ALPHA));
            if (isSelected) gfx.fill(slot.x() + 2, bottom - 2, slot.right() - 2, bottom, accent | 0xFF000000);
            if (slot.id().equals(dropTarget)) gfx.outline(slot.x(), top, slot.right(), bottom, dropValid ? DROP_OK : DROP_BAD);
            if (slot.id().equals(editing)) gfx.outline(slot.x(), top, slot.right(), bottom, EDITING_OUTLINE);

            int x = slot.x() + PAD;
            if (tab.icon() != null) {
                gfx.text(font, Component.literal(tab.icon()), x, textY, tab.color() != 0 ? accent | 0xFF000000 : TEXT_DIM);
                x += iconWidth(tab);
            }
            if (slot.id().equals(editing)) continue;
            gfx.text(font, tab.label(), x, textY, isSelected || isHovered ? TEXT_COLOR : TEXT_DIM);
            if (tab.count() >= 0) gfx.text(font, Component.literal(" " + tab.count()), x + font.width(tab.label()), textY, COUNT_COLOR);
        }

    }

    private static boolean isHoveredSlot(Slot slot, String hovered) {
        return slot.id().equals(hovered);
    }

    private static int tint(int color, int alpha) {
        return alpha << 24 | color & 0xFFFFFF;
    }

    @Override
    protected boolean press(Pointer pointer, boolean doubleClick) {
        String slot = slotAt(pointer.x(), pointer.y());
        if (SCROLL_LEFT.equals(slot) || SCROLL_RIGHT.equals(slot)) {
            if (pointer.isLeft()) scrollBy(SCROLL_LEFT.equals(slot) ? -1 : 1);
            return true;
        }
        if (ALL_TABS.equals(slot)) {
            if (pointer.isLeft()) slot(ALL_TABS).ifPresent(list -> listener.openTabList(list.x(), this.getBottom() + 2));
            return true;
        }
        String id = tabAt(pointer.x(), pointer.y());
        if (id == null) return false;
        if (pointer.isRight()) {
            if (!id.equals(NEW_TAB)) listener.openTabMenu(id, (int) pointer.x(), (int) pointer.y());
            return true;
        }
        if (!pointer.isLeft()) return false;
        if (id.equals(NEW_TAB)) listener.createFolder();
        else if (doubleClick) listener.renameTab(id);
        else listener.selectTab(id);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY) || !overflows()) return false;
        double amount = scrollY != 0 ? scrollY : scrollX;
        scrollBy(amount > 0 ? -1 : 1);
        return true;
    }
}
