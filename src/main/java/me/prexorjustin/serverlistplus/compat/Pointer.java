package me.prexorjustin.serverlistplus.compat;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A mouse button event, independent of how the current version delivers it.
 *
 * @param shift   Shift was held, for presses
 * @param control Ctrl was held (Cmd on macOS), for presses
 */
public record Pointer(double x, double y, int button, boolean shift, boolean control) {
    public Pointer(double x, double y, int button) {
        this(x, y, button, false, false);
    }

    public boolean isLeft() {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    public boolean isRight() {
        return button == InputConstants.MOUSE_BUTTON_RIGHT;
    }
}
