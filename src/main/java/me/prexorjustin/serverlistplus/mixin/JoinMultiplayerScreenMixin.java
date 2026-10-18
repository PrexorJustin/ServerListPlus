package me.prexorjustin.serverlistplus.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.AutoRefresh;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.naming.FolderPath;
import me.prexorjustin.serverlistplus.core.query.Query;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.ui.serverlist.DividerEntry;
import me.prexorjustin.serverlistplus.ui.serverlist.HiddenSectionEntry;
import me.prexorjustin.serverlistplus.ui.serverlist.ServerListUi;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen implements MultiplayerScreenHook {
    @Unique
    private static final Component SEARCH_HINT = Component.translatable("serverlistplus.search.hint")
        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    @Unique
    private static final int ROW_HEIGHT = 20;
    /** Re-sorting by ping or players happens at most this often, so rows don't jump around constantly. */
    @Unique
    private static final long LIVE_SORT_INTERVAL_NANOS = 1_000_000_000L;

    @Shadow
    protected ServerSelectionList serverSelectionList;
    @Shadow
    private ServerList servers;
    @Shadow
    private ServerData editingServer;
    //? if >=26.3 {
    @Shadow
    private Button joinButton;
    //?} else {
    /*@Shadow
    private Button selectButton;
    *///?}

    //? if >=1.21.9 {
    @Shadow
    public abstract void join(ServerData data);
    //?} else {
    /*@Shadow
    private void join(ServerData data) {
        throw new AssertionError();
    }
    *///?}

    // Survive init() being called again (resize on old versions, returning from the edit screen)
    @Unique
    private String serverlistplus$search = "";
    @Unique
    private Query serverlistplus$query = Query.EMPTY;
    @Unique
    private EditBox serverlistplus$searchBox;
    @Unique
    private ServerListUi serverlistplus$ui;
    @Unique
    private long serverlistplus$lastLiveSort;
    @Unique
    private AutoRefresh serverlistplus$autoRefresh;
    @Unique
    private boolean serverlistplus$hiddenExpanded;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Override
    public Query serverlistplus$query() {
        return serverlistplus$query;
    }

    @Override
    public String serverlistplus$search() {
        return serverlistplus$search;
    }

    @Override
    public void serverlistplus$setSearch(String value) {
        if (serverlistplus$searchBox != null) serverlistplus$searchBox.setValue(value);
        else serverlistplus$onSearchChanged(value);
    }

    @Override
    public boolean serverlistplus$hiddenExpanded() {
        return serverlistplus$hiddenExpanded;
    }

    @Override
    public void serverlistplus$setHiddenExpanded(boolean expanded) {
        serverlistplus$hiddenExpanded = expanded;
    }

    @Override
    public EditBox serverlistplus$searchBox() {
        return serverlistplus$searchBox;
    }

    @Override
    public ServerSelectionList serverlistplus$list() {
        return serverSelectionList;
    }

    @Override
    public ServerList serverlistplus$servers() {
        return servers;
    }

    @Override
    public boolean serverlistplus$isMultiSelected(ServerData server) {
        return serverlistplus$ui != null && serverlistplus$ui.isMultiSelected(server);
    }

    @Override
    public void serverlistplus$frame() {
        if (serverlistplus$ui != null) serverlistplus$ui.frame();
    }

    @Override
    public void serverlistplus$add(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Override
    public boolean serverlistplus$isCardCovering() {
        return serverlistplus$ui != null && serverlistplus$ui.isCardCovering();
    }

    @Override
    public void serverlistplus$addOverlay(AbstractWidget widget) {
        this.addRenderableOnly(widget);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serverlistplus$addInFront(AbstractWidget widget) {
        // Drawn after everything added so far, but first in line for input
        this.addRenderableOnly(widget);
        ((List<GuiEventListener>) this.children()).addFirst(widget);
    }

    @Override
    public void serverlistplus$remove(AbstractWidget widget) {
        this.removeWidget(widget);
    }

    @Override
    public void serverlistplus$focus(AbstractWidget widget) {
        this.setFocused(widget);
    }

    @Override
    public void serverlistplus$join(ServerData server) {
        this.join(server);
    }

    @Override
    public void serverlistplus$refresh() {
        ServerView.refresh(serverSelectionList, servers);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void serverlistplus$addControls(CallbackInfo ci) {
        EditBox box = new EditBox(this.font, 0, 0, 100, ROW_HEIGHT, SEARCH_HINT);
        box.setMaxLength(256);
        box.setValue(serverlistplus$search);
        box.setHint(SEARCH_HINT);
        box.setResponder(this::serverlistplus$onSearchChanged);
        serverlistplus$searchBox = box;
        // At the top of the tab sidebar it lies on the list, so it must get the clicks there first
        serverlistplus$addInFront(box);

        serverlistplus$ui = new ServerListUi(this);
        serverlistplus$ui.layoutList();
        if (serverlistplus$autoRefresh == null) serverlistplus$autoRefresh = new AutoRefresh((JoinMultiplayerScreen) (Object) this);
    }

    //? if >=1.21.9 {
    /** Since 1.21.9 resizing re-lays out the screen instead of calling init() again. */
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void serverlistplus$reposition(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.layoutList();
    }
    //?}

    @Unique
    private void serverlistplus$onSearchChanged(String value) {
        if (value.equals(serverlistplus$search)) return;
        boolean wasEmpty = serverlistplus$query.isEmpty();
        serverlistplus$search = value;
        serverlistplus$query = Query.parse(value);
        if (wasEmpty && serverlistplus$query.isEmpty()) return;
        ServerView.showFromTop(serverSelectionList, servers);
    }

    @Override
    public void serverlistplus$setSort(SortMode mode) {
        ServerListPlus.store().view().setSort(mode);
        ServerListPlus.save();
        if (serverlistplus$ui != null) serverlistplus$ui.updateSortTooltip();
        ServerView.refresh(serverSelectionList, servers);
    }

    /** Joining a server or opening another screen while the info card is open must not lose its edits. */
    @Inject(method = "removed", at = @At("HEAD"))
    private void serverlistplus$removed(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.close();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void serverlistplus$tick(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.tick();
        if (serverlistplus$autoRefresh != null) {
            serverlistplus$autoRefresh.tick(serverSelectionList, ServerListPlus.store().view().autoRefreshSeconds());
        }
        serverlistplus$liveSort();
    }

    /** Ping results arrive after the list is shown; re-sort when they change the order. */
    @Unique
    private void serverlistplus$liveSort() {
        SortMode mode = ServerView.sortMode();
        if (!mode.isLive()) return;
        long now = System.nanoTime();
        if (now - serverlistplus$lastLiveSort < LIVE_SORT_INTERVAL_NANOS) return;

        // The tab and the hidden section at its end are sorted separately
        List<ServerSelectionList.OnlineServerEntry> shown = new ArrayList<>();
        List<ServerSelectionList.OnlineServerEntry> hidden = new ArrayList<>();
        List<ServerSelectionList.OnlineServerEntry> part = shown;
        for (ServerSelectionList.Entry entry : serverSelectionList.children()) {
            if (entry instanceof HiddenSectionEntry) part = hidden;
            else if (entry instanceof ServerSelectionList.OnlineServerEntry online) part.add(online);
        }
        if (ServerView.SORTER.isSorted(shown, mode) && ServerView.SORTER.isSorted(hidden, mode)) return;
        serverlistplus$lastLiveSort = now;
        ServerView.refresh(serverSelectionList, servers);
    }

    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    private void serverlistplus$selectionChanged(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.selectedChanged();
    }

    /** A divider can be reached with the arrow keys; there is nothing to join. */
    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    private void serverlistplus$noJoinOnDivider(CallbackInfo ci) {
        if (serverSelectionList.getSelected() instanceof DividerEntry || serverSelectionList.getSelected() instanceof HiddenSectionEntry) {
            //? if >=26.3 {
            joinButton.active = false;
            //?} else
            /*selectButton.active = false;*/
        }
    }

    //? if <1.21.9 {
    /*/^* Old versions join LAN entries directly instead of asking the entry. ^/
    @Inject(method = "joinSelectedServer", at = @At("HEAD"), cancellable = true)
    private void serverlistplus$noJoinOnDividerKey(CallbackInfo ci) {
        if (serverSelectionList.getSelected() instanceof DividerEntry || serverSelectionList.getSelected() instanceof HiddenSectionEntry) ci.cancel();
    }
    *///?}

    @Inject(method = "addServerCallback", at = @At("HEAD"))
    private void serverlistplus$beforeAdd(boolean confirmed, CallbackInfo ci) {
        if (!confirmed) return;
        // The server is not in the list yet; it gets the key it will have once vanilla adds it
        Folder folder = serverlistplus$takeFolderPrefix(editingServer);
        String tab = ServerListPlus.store().view().tab();
        if (folder == null) folder = ServerListPlus.store().folder(tab).filter(open -> !open.isSmart()).orElse(null);
        if (folder != null) ServerView.metaOrCreate(editingServer).addToFolder(folder.id());
        ServerListPlus.save();
    }

    /** Data moves along with a renamed server by itself; only a folder prefix needs handling here. */
    @Inject(method = "editServerCallback", at = @At("HEAD"))
    private void serverlistplus$beforeEdit(boolean confirmed, CallbackInfo ci) {
        if (!confirmed) return;
        Folder folder = serverlistplus$takeFolderPrefix(editingServer);
        if (folder != null && serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry entry) {
            ServerView.metaOrCreate(entry.getServerData()).addToFolder(folder.id());
            ServerListPlus.save();
        }
    }

    /**
     * Handles names like {@code "Friends/Survival SMP"}: the server is renamed to "Survival SMP"
     * and folder "Friends" is created if needed.
     *
     * @return the folder named in the prefix, null if there was none
     */
    @Unique
    private Folder serverlistplus$takeFolderPrefix(ServerData server) {
        FolderPath.Split split = FolderPath.parse(server.name).orElse(null);
        if (split == null) return null;
        Store store = ServerListPlus.store();
        Folder folder = store.folderNamed(split.folder()).filter(existing -> !existing.isSmart())
            .orElseGet(() -> store.addFolder(new Folder(split.folder())));
        server.name = split.name();
        return folder;
    }

    //? if >=1.21.9 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void serverlistplus$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        //? if >=26.3 {
        // SDL key codes: shortcutKey() is layout-aware, the quirk maps Ctrl to Cmd on macOS
        boolean find = event.shortcutKey() == 'f' && event.hasControlDownWithQuirk();
        //?} elif >=26.1 {
        /*boolean find = event.key() == InputConstants.KEY_F && event.hasControlDownWithQuirk();
        *///?} else
        /*boolean find = event.key() == InputConstants.KEY_F && event.hasControlDown();*/
        Key key = find ? Key.FIND : event.isConfirmation() ? Key.CONFIRM : event.isEscape() ? Key.ESCAPE : Key.OTHER;
        serverlistplus$handleKey(key, cir);
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void serverlistplus$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Key key = keyCode == InputConstants.KEY_F && Screen.hasControlDown() ? Key.FIND
            : keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER ? Key.CONFIRM
            : keyCode == InputConstants.KEY_ESCAPE ? Key.ESCAPE
            : Key.OTHER;
        serverlistplus$handleKey(key, cir);
    }
    *///?}

    @Unique
    private void serverlistplus$handleKey(Key key, CallbackInfoReturnable<Boolean> cir) {
        if (serverlistplus$ui != null && serverlistplus$ui.key(key)) {
            cir.setReturnValue(true);
        } else if (key == Key.FIND && serverlistplus$ui != null) {
            serverlistplus$ui.openSearch();
            cir.setReturnValue(true);
        } else if (key == Key.ESCAPE && serverlistplus$searchBox != null && serverlistplus$searchBox.isFocused()
            && !serverlistplus$searchBox.getValue().isEmpty()) {
            // First Escape clears the search, the next one leaves the screen as usual
            serverlistplus$searchBox.setValue("");
            cir.setReturnValue(true);
        }
    }

    // ServerListPlus sees every mouse event before vanilla
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (serverlistplus$ui != null) serverlistplus$ui.pointerMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (serverlistplus$ui != null && serverlistplus$ui.scroll(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        //? if >=26.1 {
        boolean control = event.hasControlDownWithQuirk();
        //?} else
        /*boolean control = event.hasControlDown();*/
        if (serverlistplus$ui != null && serverlistplus$ui.press(new Pointer(event.x(), event.y(), event.button(), event.hasShiftDown(), control))) return true;
        boolean used = super.mouseClicked(event, doubleClick);
        if (serverlistplus$ui != null) serverlistplus$ui.afterClick();
        return used;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (serverlistplus$ui != null && serverlistplus$ui.drag(new Pointer(event.x(), event.y(), event.button()))) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean used = serverlistplus$ui != null && serverlistplus$ui.release(new Pointer(event.x(), event.y(), event.button()));
        return super.mouseReleased(event) || used;
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (serverlistplus$ui != null && serverlistplus$ui.press(new Pointer(mouseX, mouseY, button, hasShiftDown(), hasControlDown()))) return true;
        boolean used = super.mouseClicked(mouseX, mouseY, button);
        if (serverlistplus$ui != null) serverlistplus$ui.afterClick();
        return used;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (serverlistplus$ui != null && serverlistplus$ui.drag(new Pointer(mouseX, mouseY, button))) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean used = serverlistplus$ui != null && serverlistplus$ui.release(new Pointer(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button) || used;
    }
    *///?}
}
