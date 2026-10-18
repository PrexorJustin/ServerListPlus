package me.prexorjustin.serverlistplus.compat;

import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;

import java.net.URI;

/** Opens web links; the confirm screen takes a {@link URI} from 1.21 on. */
public final class Links {
    private Links() {
    }

    /** Asks before opening the link in the browser, like links in chat. */
    public static void open(Screen parent, URI uri) {
        //? if >=1.21 {
        ConfirmLinkScreen.confirmLinkNow(parent, uri);
        //?} else
        /*ConfirmLinkScreen.confirmLinkNow(parent, uri.toString());*/
    }
}
