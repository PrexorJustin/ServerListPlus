package me.prexorjustin.serverlistplus.mixin;

import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerEntryHook;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.core.model.Activity;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.ui.widget.Glyph;
import me.prexorjustin.serverlistplus.ui.text.InfoText;
import me.prexorjustin.serverlistplus.ui.text.NotePreview;
import net.minecraft.network.chat.Component;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.prexorjustin.serverlistplus.ui.text.NameText;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?} else {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
*///?}
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Arrays;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin implements ServerEntryHook {
    @Unique
    private static final int PIN_COLOR = 0xFFE5484D;
    @Unique
    private static final int NOTE_COLOR = 0xFFE8C468;
    /** Vanilla draws the name right of the 32px icon plus 3px gap. */
    @Unique
    private static final int NAME_OFFSET = 32 + 3;

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;
    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    private byte[] lastIconBytes;

    @Shadow
    private boolean uploadServerIcon(byte[] iconBytes) {
        throw new AssertionError();
    }

    @Shadow
    private void refreshStatus() {
        throw new AssertionError();
    }

    /** Where the notes symbol was drawn last frame; x is MIN_VALUE when none was drawn. */
    @Unique
    private int serverlistplus$noteX = Integer.MIN_VALUE;
    @Unique
    private int serverlistplus$noteY;

    @Override
    public void serverlistplus$refreshStatus() {
        refreshStatus();
    }

    /** The same as vanilla does when drawing the row, only before its first frame. */
    @Override
    public void serverlistplus$loadIcon() {
        byte[] iconBytes = serverData.getIconBytes();
        if (!Arrays.equals(iconBytes, lastIconBytes) && uploadServerIcon(iconBytes)) lastIconBytes = iconBytes;
    }

    @Override
    public boolean serverlistplus$isOverNotes(double x, double y) {
        return serverlistplus$overNote(serverlistplus$noteX, serverlistplus$noteY, x, y);
    }

    /** Slightly larger than the symbol, so it is easy to hit. */
    @Unique
    private static boolean serverlistplus$overNote(int noteX, int noteY, double x, double y) {
        return noteX != Integer.MIN_VALUE && x >= noteX - 2 && x <= noteX + Glyph.NOTE.width() + 1
            && y >= noteY - 2 && y <= noteY + Glyph.NOTE.height() + 1;
    }

    /**
     * Vanilla only moves by Shift + arrow key while the neighbouring row is a server and limits the
     * range by the number of saved servers. Both break with dividers, pinned or hidden servers.
     */
    @Unique
    private boolean serverlistplus$moveByKey(boolean up) {
        MultiplayerScreenHook hook = (MultiplayerScreenHook) screen;
        if (ServerView.showsVanillaOrder(screen.getServers(), hook.serverlistplus$query())) return false;
        if (ServerView.sortMode() == SortMode.MANUAL) {
            ServerListBridge.moveByKey(hook.serverlistplus$list(), screen.getServers(), (ServerSelectionList.OnlineServerEntry) (Object) this, up);
        }
        return true;
    }

    //? if >=1.21.9 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void serverlistplus$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.hasShiftDown() && (event.isUp() || event.isDown()) && serverlistplus$moveByKey(event.isUp())) {
            cir.setReturnValue(true);
        }
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void serverlistplus$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        boolean up = keyCode == InputConstants.KEY_UP;
        if (Screen.hasShiftDown() && (up || keyCode == InputConstants.KEY_DOWN) && serverlistplus$moveByKey(up)) {
            cir.setReturnValue(true);
        }
    }
    *///?}

    @Unique
    private static final int NAME_COLOR = 0xFFFFFFFF;
    @Unique
    private static final int DETAILS_COLOR = 0xFF8C8C8C;
    @Unique
    private static final int TAG_TEXT = 0xFFF0F0F0;
    /** Vanilla's ping icon and the gaps around it, right of the player count. */
    @Unique
    private static final int PING_ICON_SPACE = 10 + 5 + 5;
    @Unique
    private static final int STATUS_GAP = 6;

    /** Where vanilla's player count (or version, if incompatible) starts, minus a small gap. */
    @Unique
    private int serverlistplus$statusLeft(Font font, int contentRight) {
        Component status = serverData.state() == ServerData.State.INCOMPATIBLE ? serverData.version : serverData.status;
        int width = status == null ? 0 : font.width(status);
        return contentRight - PING_ICON_SPACE - width - STATUS_GAP;
    }

    /** The details text that fits: everything, only "last played", only the playtime, or null. */
    @Unique
    private static Component serverlistplus$fittingSummary(Font font, Activity activity, int room) {
        for (Component text : new Component[]{InfoText.summary(activity), InfoText.lastPlayed(activity), InfoText.playtimeOrNull(activity)}) {
            if (text != null && font.width(text) <= room) return text;
        }
        return null;
    }

    /** Clock symbol with the details as tooltip; returns where the next element goes. */
    @Unique
    private static int serverlistplus$drawClock(Gfx gfx, Font font, Component summary, int x, int y, int limit, int mouseX, int mouseY) {
        Glyph clock = Glyph.CLOCK;
        if (x + clock.width() > limit) return x;
        boolean over = mouseX >= x - 1 && mouseX <= x + clock.width() && mouseY >= y - 1 && mouseY <= y + clock.height();
        clock.draw(gfx, x, y, over ? TAG_TEXT : DETAILS_COLOR);
        if (over) gfx.tooltip(font, summary, mouseX, mouseY);
        return x + clock.width() + 5;
    }

    /**
     * Everything after the server name on the first line: pin, notes symbol, "2 d ago · 14 h" and tag chips,
     * as far as there is room before the player count. Depending on the settings the time is
     * shown as a clock symbol or only as a tooltip while the row is hovered.
     */
    @Unique
    private void serverlistplus$drawDetails(Gfx gfx, int contentX, int contentY, int contentRight, int mouseX, int mouseY, boolean hovered) {
        if (((MultiplayerScreenHook) screen).serverlistplus$isCardCovering()) {
            // The card covers the row: no tooltips from under it
            mouseX = Integer.MIN_VALUE;
            mouseY = Integer.MIN_VALUE;
            hovered = false;
        }
        Font font = Minecraft.getInstance().font;
        int y = contentY + 1;
        Component name = NameText.of(serverData.name);
        if (NameText.isStyled(serverData.name)) gfx.text(font, name, contentX + NAME_OFFSET, y, NAME_COLOR);
        int x = contentX + NAME_OFFSET + font.width(name) + 4;
        int limit = serverlistplus$statusLeft(font, contentRight);
        if (ServerView.isPinned(serverData)) {
            Glyph.PIN.draw(gfx, x, y, PIN_COLOR);
            x += Glyph.PIN.width() + 4;
        }
        ServerMeta meta = ServerView.meta(serverData);
        serverlistplus$noteX = Integer.MIN_VALUE;
        if (meta != null && !meta.notes().isBlank() && x + Glyph.NOTE.width() <= limit) {
            serverlistplus$noteX = x;
            serverlistplus$noteY = y;
            boolean over = serverlistplus$overNote(x, y, mouseX, mouseY);
            Glyph.NOTE.draw(gfx, x, y, over ? TAG_TEXT : NOTE_COLOR);
            if (over) gfx.tooltip(font, NotePreview.tooltip(font, meta.notes()), mouseX, mouseY);
            x += Glyph.NOTE.width() + 5;
        }
        Activity activity = ServerView.activity(serverData);
        Component summary = InfoText.summary(activity);
        if (summary != null) {
            switch (ServerView.detailsMode()) {
                case INLINE -> {
                    // Shortened as needed; when not even part of it fits, the clock keeps it reachable
                    Component text = serverlistplus$fittingSummary(font, activity, limit - x);
                    if (text != null) {
                        gfx.text(font, text, x, y, DETAILS_COLOR);
                        x += font.width(text) + 5;
                    } else {
                        x = serverlistplus$drawClock(gfx, font, summary, x, y, limit, mouseX, mouseY);
                    }
                }
                case ICON -> x = serverlistplus$drawClock(gfx, font, summary, x, y, limit, mouseX, mouseY);
                case HOVER -> {
                    // Not over ping and player count, which have tooltips of their own
                    if (hovered && mouseX < limit && !serverlistplus$overNote(serverlistplus$noteX, serverlistplus$noteY, mouseX, mouseY)) {
                        gfx.tooltip(font, summary, mouseX, mouseY);
                    }
                }
                case OFF -> {
                }
            }
        }
        if (meta == null) return;
        for (String tag : meta.tags()) {
            int width = font.width(tag) + 6;
            if (x + width > limit) break;
            gfx.roundedRect(x, y - 1, x + width, y + 9, InfoText.tagColor(tag));
            gfx.text(font, Component.literal(tag), x + 3, y, TAG_TEXT);
            x += width + 3;
        }
    }

    /** A name with MiniMessage tags is drawn formatted by {@code drawDetails} instead of as typed. */
    @ModifyExpressionValue(method = RENDER_METHOD, at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ServerData;name:Ljava/lang/String;", opcode = Opcodes.GETFIELD))
    private String serverlistplus$hideStyledName(String name) {
        return NameText.isStyled(name) ? "" : name;
    }

    /** Screen readers read the name without tags. */
    @ModifyExpressionValue(method = "getNarration", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ServerData;name:Ljava/lang/String;", opcode = Opcodes.GETFIELD))
    private String serverlistplus$plainNarration(String name) {
        return NameText.plain(name);
    }

    //? if >=26.1 {
    @Unique
    private static final String RENDER_METHOD = "extractContent";
    //?} elif >=1.21.9 {
    /*@Unique
    private static final String RENDER_METHOD = "renderContent";
    *///?} else {
    /*@Unique
    private static final String RENDER_METHOD = "render";
    *///?}

    //? if >=26.1 {
    @Inject(method = "extractContent", at = @At("HEAD"))
    private void serverlistplus$multiSelection(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        serverlistplus$drawMultiSelection(new Gfx(graphics));
    }
    //?} elif >=1.21.9 {
    /*@Inject(method = "renderContent", at = @At("HEAD"))
    private void serverlistplus$multiSelection(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        serverlistplus$drawMultiSelection(new Gfx(graphics));
    }
    *///?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void serverlistplus$multiSelection(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height,
                                               int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        serverlistplus$drawMultiSelection(new Gfx(graphics));
    }
    *///?}

    /** Further selected rows get the frame vanilla draws around its selected row, before their content. */
    @Unique
    private void serverlistplus$drawMultiSelection(Gfx gfx) {
        MultiplayerScreenHook hook = (MultiplayerScreenHook) screen;
        ServerSelectionList list = hook.serverlistplus$list();
        ServerSelectionList.Entry self = (ServerSelectionList.Entry) (Object) this;
        if (ServerListBridge.isDrawingMoved() || list.getSelected() == self || !hook.serverlistplus$isMultiSelected(serverData)) return;
        int[] box = ServerListBridge.rowBox(list, self);
        gfx.fill(box[0], box[1], box[2], box[3], list.isFocused() ? 0xFFFFFFFF : 0xFF808080);
        gfx.fill(box[0] + 1, box[1] + 1, box[2] - 1, box[3] - 1, 0xFF000000);
    }

    //? if >=26.1 {
    @Inject(method = "extractContent", at = @At("RETURN"))
    private void serverlistplus$details(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        ServerSelectionList.Entry self = (ServerSelectionList.Entry) (Object) this;
        serverlistplus$drawDetails(new Gfx(graphics), self.getContentX(), self.getContentY(), self.getContentX() + self.getContentWidth(), mouseX, mouseY, hovered);
    }
    //?} elif >=1.21.9 {
    /*@Inject(method = "renderContent", at = @At("RETURN"))
    private void serverlistplus$details(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        ServerSelectionList.Entry self = (ServerSelectionList.Entry) (Object) this;
        serverlistplus$drawDetails(new Gfx(graphics), self.getContentX(), self.getContentY(), self.getContentX() + self.getContentWidth(), mouseX, mouseY, hovered);
    }
    *///?} else {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void serverlistplus$details(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height,
                                        int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        serverlistplus$drawDetails(new Gfx(graphics), left, top, left + width - 5, mouseX, mouseY, hovered);
    }
    *///?}
}
