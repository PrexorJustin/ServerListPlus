package me.prexorjustin.serverlistplus.mixin;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerEntryHook;
import me.prexorjustin.serverlistplus.bridge.ServerKeys;
import me.prexorjustin.serverlistplus.bridge.ServerSelectionListHook;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.core.view.DividerLayout;
import me.prexorjustin.serverlistplus.ui.serverlist.DividerEntry;
import me.prexorjustin.serverlistplus.ui.serverlist.HiddenSectionEntry;
import me.prexorjustin.serverlistplus.ui.serverlist.ServerListUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerSelectionList.class)
public abstract class ServerSelectionListMixin extends ObjectSelectionList<ServerSelectionList.Entry> implements ServerSelectionListHook {
    @Unique
    private static final int DIVIDER_COLOR = 0xFFA0A0A0;

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;
    @Shadow
    @Final
    private List<ServerSelectionList.OnlineServerEntry> onlineServers;

    /** Layout of the last {@code updateOnlineServers}, reused when LAN servers refresh the list. */
    @Unique
    private List<DividerLayout.Row<ServerSelectionList.OnlineServerEntry>> serverlistplus$rows = List.of();
    @Unique
    private ServerView.Arrangement serverlistplus$arrangement = ServerView.Arrangement.EMPTY;

    private ServerSelectionListMixin(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Override
    public JoinMultiplayerScreen serverlistplus$screen() {
        return screen;
    }

    @Override
    public void serverlistplus$scrollTo(ServerSelectionList.Entry entry) {
        //? if >=1.21.9 {
        scrollToEntry(entry);
        //?} else
        /*ensureVisible(entry);*/
    }

    /**
     * Vanilla's rows are 305 wide. Next to the docked info card the list can be narrower, so the
     * rows shrink with it; vanilla wraps the MOTD to the row width on every frame.
     */
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void serverlistplus$fitRows(CallbackInfoReturnable<Integer> cir) {
        int room = this.getWidth() - ServerListUi.ROW_SIDE_ROOM;
        if (room < cir.getReturnValueI()) cir.setReturnValue(room);
    }

    /** Filters and sorts the saved servers right before vanilla puts them into the list. */
    @Inject(
        method = "updateOnlineServers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;refreshEntries()V")
    )
    private void serverlistplus$arrange(ServerList servers, CallbackInfo ci) {
        MultiplayerScreenHook hook = (MultiplayerScreenHook) screen;
        var query = hook.serverlistplus$query();
        ServerKeys.index(servers);
        serverlistplus$arrangement = ServerView.arrange(onlineServers, query, hook.serverlistplus$hiddenExpanded());
        // Only the shown rows: a filtered out one releases its icon texture, which a shown row of the same address shares
        for (ServerSelectionList.OnlineServerEntry entry : onlineServers) ((ServerEntryHook) entry).serverlistplus$loadIcon();
        serverlistplus$rows = ServerView.withDividers(serverlistplus$arrangement.visible(), query);
    }

    /** Vanilla only knows server entries; put the dividers between them and the hidden section after them. */
    @Inject(method = "refreshEntries", at = @At("TAIL"))
    private void serverlistplus$addDividers(CallbackInfo ci) {
        ServerView.Arrangement arrangement = serverlistplus$arrangement;
        if (serverlistplus$rows.stream().noneMatch(row -> row instanceof DividerLayout.Line) && !arrangement.hasHiddenSection()) return;
        ServerSelectionList self = (ServerSelectionList) (Object) this;
        ServerSelectionList.Entry selected = this.getSelected();
        Styled style = ServerListPlus.store().style(ServerView.tab());
        int color = style != null && style.color() != null ? style.color().argb(0xFF) : DIVIDER_COLOR;

        List<ServerSelectionList.Entry> entries = new ArrayList<>();
        for (DividerLayout.Row<ServerSelectionList.OnlineServerEntry> row : serverlistplus$rows) {
            switch (row) {
                case DividerLayout.Server<ServerSelectionList.OnlineServerEntry> server -> entries.add(server.value());
                case DividerLayout.Line<ServerSelectionList.OnlineServerEntry> line -> entries.add(new DividerEntry(screen, line.divider(),
                    line.divider().color() != null ? line.divider().color().argb(0xFF) : color));
            }
        }
        if (arrangement.hasHiddenSection()) {
            entries.add(new HiddenSectionEntry(screen, arrangement.hiddenCount(), arrangement.hiddenExpanded()));
            entries.addAll(arrangement.hiddenShown());
        }
        // LAN header and LAN servers stay below
        for (ServerSelectionList.Entry entry : this.children()) {
            if (!(entry instanceof ServerSelectionList.OnlineServerEntry)) entries.add(entry);
        }
        this.replaceEntries(entries);
        if (selected != null && entries.contains(selected)) self.setSelected(selected);
    }
}
