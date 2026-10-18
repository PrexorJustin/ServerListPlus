package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.compat.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Follows the mouse while servers, a divider or a tab are dragged, and marks where they would be
 * inserted. Dragged servers are shown as the grabbed row, held where it was grabbed, while their
 * spots in the list are dimmed; over a tab the row shrinks to a label, so the tab stays visible.
 */
public final class DragGhost extends SlpWidget {
    private static final int BACKGROUND = 0xE0101010;
    private static final int ROW_BACKGROUND = 0xFF000000;
    private static final int BORDER_OK = 0xFFFFFFFF;
    private static final int BORDER_BAD = 0xFFFF5555;
    private static final int MARKER = 0xFFFFFFFF;
    private static final int LEFT_SPOT = 0xA0000000;
    /** How far each row of a dragged stack sticks out below and right of the one in front. */
    private static final int STACK_OFFSET = 3;
    private static final int MAX_STACKED = 2;

    /** The grabbed row: where the dragged rows are in the list and how to draw the grabbed one elsewhere. */
    public interface Row {
        /** The box around the grabbed row in the list: x1, y1, x2, y2. */
        int[] box();

        /** The boxes of every dragged row that is shown, to dim their spots. */
        List<int[]> spots();

        /** The visible part of the list: top and bottom. */
        int[] clip();

        /** Draws the grabbed row's content moved by {@code dx}, {@code dy}. */
        void draw(Gfx gfx, int dx, int dy);
    }

    private final Font font;
    private final Component label;
    private final Row row;
    /** Where the row was grabbed, from its top left corner. */
    private final int grabX;
    private final int grabY;
    private int count = 1;
    private boolean valid = true;
    private boolean compact;
    /** Insertion marker x1, y1, x2, y2; null for none. */
    private int[] marker;

    /** Only shows {@code label} next to the pointer. */
    public DragGhost(Font font, Component label) {
        this(font, label, null, 0, 0);
    }

    /** Shows the whole row, held at the point where it was pressed. */
    public DragGhost(Font font, Component label, Row row, double pressX, double pressY) {
        super(0, 0, 0, 0, label);
        this.font = font;
        this.label = label;
        this.row = row;
        int[] box = row != null ? row.box() : new int[4];
        this.grabX = (int) pressX - box[0];
        this.grabY = (int) pressY - box[1];
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /** Only the label instead of the row, e.g. over a tab. */
    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    /** How many servers are dragged; more than one shows the row as a stack with the number. */
    public void setCount(int count) {
        this.count = count;
    }

    /** Shows the insertion line at {@code y}; a negative {@code y} hides it. */
    public void setLine(int y, int left, int right) {
        setMarker(y < 0 ? null : new int[]{left, y - 1, right, y + 1});
    }

    /** Shows the insertion marker as the given rectangle, or none for null. */
    public void setMarker(int[] marker) {
        this.marker = marker;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        gfx.overlay();
        if (row != null) {
            int[] clip = row.clip();
            for (int[] spot : row.spots()) {
                int top = Math.max(spot[1], clip[0]);
                int bottom = Math.min(spot[3], clip[1]);
                if (top < bottom) gfx.fill(spot[0], top, spot[2], bottom, LEFT_SPOT);
            }
        }
        if (marker != null) gfx.fill(marker[0], marker[1], marker[2], marker[3], MARKER);
        int border = valid ? BORDER_OK : BORDER_BAD;
        if (row == null || compact) {
            int x = mouseX + 10;
            int y = mouseY + 6;
            int width = font.width(label) + 8;
            gfx.fill(x, y, x + width, y + 14, BACKGROUND);
            gfx.outline(x, y, x + width, y + 14, border);
            gfx.text(font, label, x + 4, y + 3, 0xFFFFFFFF);
            return;
        }
        int[] box = row.box();
        int width = box[2] - box[0];
        int height = box[3] - box[1];
        int x = mouseX - grabX;
        int y = mouseY - grabY;
        // Further rows peek out behind the grabbed one, backmost first
        for (int i = Math.min(count - 1, MAX_STACKED); i >= 0; i--) {
            int offset = i * STACK_OFFSET;
            // Like the selected row: a border around black
            gfx.fill(x + offset, y + offset, x + offset + width, y + offset + height, border);
            gfx.fill(x + offset + 1, y + offset + 1, x + offset + width - 1, y + offset + height - 1, ROW_BACKGROUND);
        }
        row.draw(gfx, x - box[0], y - box[1]);
        if (count > 1) {
            String number = String.valueOf(count);
            int badgeWidth = Math.max(11, font.width(number) + 6);
            int badgeX = x + width - badgeWidth / 2;
            int badgeY = y - 5;
            gfx.roundedRect(badgeX, badgeY, badgeX + badgeWidth, badgeY + 11, 3, border);
            gfx.text(font, Component.literal(number), badgeX + (badgeWidth - font.width(number) + 1) / 2, badgeY + 2, 0xFF000000);
        }
    }
}
