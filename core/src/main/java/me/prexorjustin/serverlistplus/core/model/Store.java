package me.prexorjustin.serverlistplus.core.model;

import me.prexorjustin.serverlistplus.core.key.AddressKey;
import me.prexorjustin.serverlistplus.core.key.ServerKey;
import me.prexorjustin.serverlistplus.core.style.Palette;
import me.prexorjustin.serverlistplus.core.style.Styled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * All persisted ServerListPlus data. Server entries are keyed by
 * {@link ServerKey address and name}.
 */
public final class Store {
    public static final int CURRENT_VERSION = 4;
    /** How many addresses joined through Direct Connection are remembered. */
    public static final int MAX_RECENT_DIRECT = 10;
    /** How long the data of a deleted server is kept, in case it is added again. */
    public static final long ORPHAN_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private int version = CURRENT_VERSION;
    private List<Folder> folders = new ArrayList<>();
    private Map<String, ServerMeta> servers = new LinkedHashMap<>();
    /** Playtime and last join by normalized address, shared by all entries of that address. */
    private Map<String, Activity> activity = new LinkedHashMap<>();
    private ViewSettings view = new ViewSettings();
    private List<Divider> dividers = new ArrayList<>();
    /** Addresses joined through Direct Connection, newest first, as they were typed. */
    private List<String> recentDirect = new ArrayList<>();
    /** Whether Modrinth is asked for a newer release when the game starts. */
    private boolean checkUpdates = true;
    /** A release the user chose to hide; newer ones are pointed out again. */
    private String dismissedUpdate;

    public int version() {
        return version;
    }

    public ViewSettings view() {
        return view;
    }

    public List<Folder> folders() {
        return folders;
    }

    public Optional<Folder> folder(String id) {
        return folders.stream().filter(folder -> folder.id().equals(id)).findFirst();
    }

    public List<Divider> dividers() {
        return dividers;
    }

    /** Dividers of one tab, in the order they were created. */
    public List<Divider> dividers(String tab) {
        return dividers.stream().filter(divider -> divider.tab().equals(tab)).toList();
    }

    public Optional<Divider> divider(String id) {
        return dividers.stream().filter(divider -> divider.id().equals(id)).findFirst();
    }

    public List<String> recentDirect() {
        return Collections.unmodifiableList(recentDirect);
    }

    /** Puts an address joined directly at the top; the same server typed differently is kept once. */
    public void rememberDirect(String address) {
        String value = address == null ? "" : address.trim();
        if (value.isEmpty()) return;
        forgetDirect(value);
        recentDirect.addFirst(value);
        while (recentDirect.size() > MAX_RECENT_DIRECT) recentDirect.removeLast();
    }

    public void forgetDirect(String address) {
        String key = AddressKey.normalize(address);
        recentDirect.removeIf(entry -> AddressKey.normalize(entry).equals(key));
    }

    public boolean checksUpdates() {
        return checkUpdates;
    }

    public void setCheckUpdates(boolean checkUpdates) {
        this.checkUpdates = checkUpdates;
    }

    public String dismissedUpdate() {
        return dismissedUpdate;
    }

    public void setDismissedUpdate(String version) {
        this.dismissedUpdate = version;
    }

    /** The look of a tab: a folder, or one of the built-in tabs. Null for an unknown tab. */
    public Styled style(String tab) {
        if (ViewSettings.TAB_ALL.equals(tab)) return view.allStyle();
        return folder(tab).orElse(null);
    }

    /** Adds a folder at the end and gives it the next color if it has none. */
    public Folder addFolder(Folder folder) {
        if (folder.color() == null) folder.setColor(Palette.forIndex(folders.size()));
        folders.add(folder);
        return folder;
    }

    /** Moves a folder one position towards the start ({@code delta = -1}) or end ({@code delta = 1}). */
    public void moveFolder(String id, int delta) {
        for (int i = 0; i < folders.size(); i++) {
            if (!folders.get(i).id().equals(id)) continue;
            int target = i + delta;
            if (target >= 0 && target < folders.size()) folders.add(target, folders.remove(i));
            return;
        }
    }

    /**
     * Moves a folder to just before the folder at {@code index} of the current order, or to the
     * end for {@code index = folders().size()}: where it was dropped between the tabs.
     */
    public void moveFolderTo(String id, int index) {
        for (int i = 0; i < folders.size(); i++) {
            if (!folders.get(i).id().equals(id)) continue;
            Folder folder = folders.remove(i);
            // Everything after the old place moved up by one
            int target = index > i ? index - 1 : index;
            folders.add(Math.clamp(target, 0, folders.size()), folder);
            return;
        }
    }

    /** Finds a folder by name, ignoring case. */
    public Optional<Folder> folderNamed(String name) {
        return folders.stream().filter(folder -> folder.name().equalsIgnoreCase(name)).findFirst();
    }

    /** Removes the folder; its servers fall back to having no folder. */
    public void removeFolder(String id) {
        folders.removeIf(folder -> folder.id().equals(id));
        if (id.equals(view.tab())) view.setTab(ViewSettings.TAB_ALL);
        if (id.equals(view.defaultTab())) view.setDefaultTab(null);
        dividers.removeIf(divider -> divider.tab().equals(id));
        for (ServerMeta meta : servers.values()) {
            meta.removeFromFolder(id);
            meta.setPinnedIn(id, false);
        }
    }

    /** When and how long the server at this address was played, if ever. */
    public Optional<Activity> activity(String address) {
        return Optional.ofNullable(activity.get(AddressKey.normalize(address)));
    }

    public Activity activityOrCreate(String address) {
        return activity.computeIfAbsent(AddressKey.normalize(address), key -> new Activity());
    }

    public Optional<ServerMeta> get(String key) {
        return Optional.ofNullable(servers.get(key));
    }

    public ServerMeta getOrCreate(String key) {
        return servers.computeIfAbsent(key, k -> new ServerMeta());
    }

    public Map<String, ServerMeta> servers() {
        return servers;
    }

    /**
     * Lets data follow servers whose key changed (renamed, new address, or a duplicate that moved
     * up when an identical entry before it was removed). All entries are taken out before any is
     * put back, so keys may be swapped or shifted along a chain. Dividers follow as well.
     *
     * @param moves old key to new key
     */
    public void moveKeys(Map<String, String> moves) {
        Map<String, ServerMeta> taken = new LinkedHashMap<>();
        for (var move : moves.entrySet()) {
            ServerMeta meta = servers.remove(move.getKey());
            if (meta != null) taken.put(move.getValue(), meta);
        }
        servers.putAll(taken);
        for (Divider divider : dividers) {
            String target = divider.beforeKey() == null ? null : moves.get(divider.beforeKey());
            if (target != null) divider.setBeforeKey(target);
        }
    }

    /**
     * Moves data stored under the address alone (before names were part of keys) to the keys of
     * the saved servers with that address. If several share it, each gets a copy, which can then
     * be changed independently. Dividers follow to the first of them.
     *
     * @param keys keys of the saved servers, in list order
     * @return true if anything changed
     */
    public boolean adoptLegacyKeys(List<String> keys) {
        boolean changed = false;
        for (String legacy : List.copyOf(servers.keySet())) {
            if (!ServerKey.isLegacy(legacy)) continue;
            List<String> owners = keys.stream().filter(key -> ServerKey.address(key).equals(legacy)).distinct().toList();
            if (owners.isEmpty()) continue;
            ServerMeta meta = servers.remove(legacy);
            for (String owner : owners) {
                ServerMeta existing = servers.get(owner);
                if (existing == null) servers.put(owner, meta.copy());
                else existing.absorb(meta);
            }
            changed = true;
        }
        for (Divider divider : dividers) {
            String anchor = divider.beforeKey();
            if (anchor == null || !ServerKey.isLegacy(anchor)) continue;
            keys.stream().filter(key -> ServerKey.address(key).equals(anchor)).findFirst().ifPresent(divider::setBeforeKey);
            changed = true;
        }
        return changed;
    }

    /**
     * Flags entries whose server is no longer in the list instead of deleting them,
     * so data survives if the server is added again. Entries in use get their flag cleared.
     */
    public boolean markOrphans(Set<String> presentKeys, long now) {
        boolean changed = false;
        for (Map.Entry<String, ServerMeta> entry : servers.entrySet()) {
            ServerMeta meta = entry.getValue();
            if (presentKeys.contains(entry.getKey())) {
                changed |= meta.orphanedSince() != null;
                meta.setOrphanedSince(null);
            } else if (meta.orphanedSince() == null) {
                meta.setOrphanedSince(now);
                changed = true;
            }
        }
        return changed;
    }

    /** Drops entries without any data, e.g. a server that was pinned and unpinned again. */
    public void pruneEmpty() {
        servers.values().removeIf(ServerMeta::isEmpty);
    }

    /**
     * Deletes the data of servers that have been gone for longer than {@link #ORPHAN_RETENTION_MILLIS}.
     *
     * @return how many entries were removed
     */
    public int purgeOrphans(long now) {
        int before = servers.size();
        servers.values().removeIf(meta -> meta.orphanedSince() != null && now - meta.orphanedSince() > ORPHAN_RETENTION_MILLIS);
        return before - servers.size();
    }

    /** Repairs structures Gson may have left null or inconsistent after loading. */
    public void sanitize() {
        if (view == null) view = new ViewSettings();
        if (dividers == null) dividers = new ArrayList<>();
        if (recentDirect == null) recentDirect = new ArrayList<>();
        recentDirect.removeIf(address -> address == null || address.isBlank());
        while (recentDirect.size() > MAX_RECENT_DIRECT) recentDirect.removeLast();
        dividers.removeIf(divider -> divider == null || !divider.isValid());
        dividers.forEach(Divider::sanitize);

        if (folders == null) folders = new ArrayList<>();
        folders.removeIf(folder -> folder == null || !folder.isValid());
        folders.forEach(Folder::sanitize);
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).color() == null) folders.get(i).setColor(Palette.forIndex(i));
        }

        if (activity == null) activity = new LinkedHashMap<>();
        activity.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        if (servers == null) servers = new LinkedHashMap<>();
        servers.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        for (ServerMeta meta : servers.values()) {
            meta.sanitize();
            for (String folder : meta.folderIds()) {
                if (folder(folder).isEmpty()) meta.removeFromFolder(folder);
            }
            for (String tab : meta.pinnedTabs()) {
                if (!ViewSettings.TAB_ALL.equals(tab) && folder(tab).isEmpty()) meta.setPinnedIn(tab, false);
            }
        }
        view.sanitize(this);
    }
}
