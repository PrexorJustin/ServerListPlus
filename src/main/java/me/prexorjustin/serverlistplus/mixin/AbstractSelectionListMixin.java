package me.prexorjustin.serverlistplus.mixin;

import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerSelectionListHook;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.ui.serverlist.Wallpapers;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Background drawing lives in the shared list class. Only the server list gets a wallpaper, and
 * its rows get moved back into place before they're drawn.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
    //? if >=26.1 {
    @Inject(method = "extractListBackground", at = @At("TAIL"))
    //?} else
    /*@Inject(method = "renderListBackground", at = @At("TAIL"))*/
    private void serverlistplus$beforeRows(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!((Object) this instanceof ServerSelectionList list)) return;
        serverlistplus$keepRowsInPlace(list);
        MultiplayerScreenHook screen = (MultiplayerScreenHook) ((ServerSelectionListHook) list).serverlistplus$screen();
        // The list is drawn before the widgets on top of it, so they can still be moved for this frame
        screen.serverlistplus$frame();
        Wallpapers.draw(new Gfx(graphics), list.getX(), list.getY(), list.getWidth(), list.getHeight(), screen.serverlistplus$servers());
    }

    /**
     * Since 1.21.9 rows store their position and only update it on resize or scroll. Coming back
     * from a server, the list (shifted by the tab sidebar) showed its rows at the old spot until
     * the next scroll. So do what scrolling does whenever they're out of place.
     */
    @Unique
    private static void serverlistplus$keepRowsInPlace(ServerSelectionList list) {
        //? if >=1.21.9 {
        if (!list.children().isEmpty() && list.children().getFirst().getX() != list.getRowLeft()) {
            list.setScrollAmount(list.scrollAmount());
        }
        //?}
    }
}
