package me.prexorjustin.serverlistplus.bridge;

/** Implemented by {@code ServerSelectionList.OnlineServerEntry} via mixin. */
public interface ServerEntryHook {
    /** Updates the ping icon and its tooltips from the current server state. */
    void serverlistplus$refreshStatus();

    /**
     * Uploads the server's icon now. A new row otherwise shows the default icon for its first
     * frame, which flickers every time the list is rebuilt, e.g. when switching tabs.
     */
    void serverlistplus$loadIcon();

    /** Whether the point is over the notes symbol drawn in the row. */
    boolean serverlistplus$isOverNotes(double x, double y);
}
