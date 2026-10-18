package me.prexorjustin.serverlistplus.core.model;

import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.core.style.TabStyle;

/**
 * How the server list is presented; remembered between sessions.
 */
public final class ViewSettings {
    /** Tab showing every server. */
    public static final String TAB_ALL = "all";
    /** Choices for refreshing the server status automatically; 0 is off. */
    public static final int[] AUTO_REFRESH_SECONDS = {0, 15, 30, 60, 120};
    private static final int MIN_AUTO_REFRESH_SECONDS = 15;

    private SortMode sort = SortMode.MANUAL;
    /** {@link #TAB_ALL} or a folder id. */
    private String tab = TAB_ALL;
    /** Tab opened when the game starts, null to keep the last one. */
    private String defaultTab;
    private DetailsMode details = DetailsMode.INLINE;
    private int autoRefreshSeconds;
    private TabLayout tabLayout = TabLayout.TOP;
    /** The sidebar folds away until the pointer comes near it. */
    private boolean autoHideSidebar;
    private TabStyle allStyle = TabStyle.all();

    public SortMode sort() {
        return sort;
    }

    public void setSort(SortMode sort) {
        this.sort = sort == null ? SortMode.MANUAL : sort;
    }

    public String tab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab == null ? TAB_ALL : tab;
    }

    public String defaultTab() {
        return defaultTab;
    }

    public void setDefaultTab(String defaultTab) {
        this.defaultTab = defaultTab;
    }

    /** Switches to the default tab, if one is set. Called once when the game starts. */
    public void openDefaultTab() {
        if (defaultTab != null) tab = defaultTab;
    }

    public DetailsMode details() {
        return details;
    }

    public void setDetails(DetailsMode details) {
        this.details = details == null ? DetailsMode.INLINE : details;
    }

    public int autoRefreshSeconds() {
        return autoRefreshSeconds;
    }

    /** Values below the minimum are raised to it, so servers aren't pinged constantly; 0 or less turns it off. */
    public void setAutoRefreshSeconds(int seconds) {
        this.autoRefreshSeconds = seconds <= 0 ? 0 : Math.max(seconds, MIN_AUTO_REFRESH_SECONDS);
    }

    public TabLayout tabLayout() {
        return tabLayout;
    }

    public void setTabLayout(TabLayout tabLayout) {
        this.tabLayout = tabLayout == null ? TabLayout.TOP : tabLayout;
    }

    public boolean autoHideSidebar() {
        return autoHideSidebar;
    }

    public void setAutoHideSidebar(boolean autoHideSidebar) {
        this.autoHideSidebar = autoHideSidebar;
    }

    public TabStyle allStyle() {
        return allStyle;
    }

    void sanitize(Store store) {
        if (allStyle == null) allStyle = TabStyle.all();
        allStyle.sanitize();
        if (sort == null) sort = SortMode.MANUAL;
        if (details == null) details = DetailsMode.INLINE;
        setAutoRefreshSeconds(autoRefreshSeconds);
        if (tabLayout == null) tabLayout = TabLayout.TOP;
        if (!isTab(store, tab)) tab = TAB_ALL;
        if (defaultTab != null && !isTab(store, defaultTab)) defaultTab = null;
    }

    private static boolean isTab(Store store, String id) {
        return id != null && (id.equals(TAB_ALL) || store.folder(id).isPresent());
    }
}
