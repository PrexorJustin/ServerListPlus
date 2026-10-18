package me.prexorjustin.serverlistplus.core.model;

import me.prexorjustin.serverlistplus.core.key.ServerKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreTest {
    @Test
    void orphansAreFlaggedRestoredAndPurged() {
        Store store = new Store();
        store.getOrCreate("a");
        store.getOrCreate("b");

        assertTrue(store.markOrphans(Set.of("a"), 100));
        assertNull(store.get("a").orElseThrow().orphanedSince());
        assertEquals(100L, store.get("b").orElseThrow().orphanedSince());

        assertFalse(store.markOrphans(Set.of("a"), 200));
        assertEquals(100L, store.get("b").orElseThrow().orphanedSince(), "first orphan time is kept");

        assertTrue(store.markOrphans(Set.of("a", "b"), 300));
        assertNull(store.get("b").orElseThrow().orphanedSince());

        store.markOrphans(Set.of("a"), 400);
        assertEquals(0, store.purgeOrphans(400 + Store.ORPHAN_RETENTION_MILLIS), "kept until the retention time is over");
        assertEquals(1, store.purgeOrphans(401 + Store.ORPHAN_RETENTION_MILLIS));
        assertEquals(List.of("a"), List.copyOf(store.servers().keySet()));
    }

    @Test
    void removingFolderUnassignsServers() {
        Store store = new Store();
        Folder folder = new Folder("Friends");
        store.folders().add(folder);
        store.getOrCreate("a").addToFolder(folder.id());

        store.removeFolder(folder.id());

        assertTrue(store.folders().isEmpty());
        assertTrue(store.get("a").orElseThrow().folderIds().isEmpty());
    }

    @Test
    void moveFolder() {
        Store store = new Store();
        Folder a = new Folder("A");
        Folder b = new Folder("B");
        Folder c = new Folder("C");
        store.folders().addAll(List.of(a, b, c));

        store.moveFolder(c.id(), -1);
        assertEquals(List.of(a, c, b), store.folders());
        store.moveFolder(a.id(), -1);
        assertEquals(List.of(a, c, b), store.folders(), "first folder stays first");
        store.moveFolder(b.id(), 1);
        assertEquals(List.of(a, c, b), store.folders(), "last folder stays last");
    }

    @Test
    void moveFolderTo() {
        Store store = new Store();
        Folder a = new Folder("A");
        Folder b = new Folder("B");
        Folder c = new Folder("C");
        store.folders().addAll(List.of(a, b, c));

        store.moveFolderTo(a.id(), 3);
        assertEquals(List.of(b, c, a), store.folders(), "to the end");
        store.moveFolderTo(a.id(), 0);
        assertEquals(List.of(a, b, c), store.folders(), "to the start");
        store.moveFolderTo(a.id(), 2);
        assertEquals(List.of(b, a, c), store.folders(), "before C");
        store.moveFolderTo(c.id(), 1);
        assertEquals(List.of(b, c, a), store.folders(), "before A");
        store.moveFolderTo(c.id(), 1);
        assertEquals(List.of(b, c, a), store.folders(), "onto its own place");
        store.moveFolderTo(c.id(), 2);
        assertEquals(List.of(b, c, a), store.folders(), "right after itself");
    }

    @Test
    void folderNamedIgnoresCase() {
        Store store = new Store();
        Folder folder = new Folder("Friends");
        store.folders().add(folder);
        assertEquals(folder, store.folderNamed("friends").orElseThrow());
        assertTrue(store.folderNamed("work").isEmpty());
    }

    @Test
    void removingSelectedFolderSwitchesToAllTab() {
        Store store = new Store();
        Folder folder = new Folder("Friends");
        store.folders().add(folder);
        store.view().setTab(folder.id());

        store.removeFolder(folder.id());

        assertEquals(ViewSettings.TAB_ALL, store.view().tab());
    }

    @Test
    void defaultTabOpensOnStart() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        store.view().setDefaultTab(folder.id());
        store.view().setTab(ViewSettings.TAB_ALL);

        store.view().openDefaultTab();

        assertEquals(folder.id(), store.view().tab());
    }

    @Test
    void noDefaultTabKeepsLastTab() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        store.view().setTab(folder.id());
        store.view().openDefaultTab();
        assertEquals(folder.id(), store.view().tab());
    }

    @Test
    void deletingDefaultFolderClearsDefault() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        store.view().setDefaultTab(folder.id());
        store.removeFolder(folder.id());
        assertNull(store.view().defaultTab());
    }

    @Test
    void unknownDefaultTabIsDroppedOnSanitize() {
        Store store = new Store();
        store.view().setDefaultTab("deleted");
        store.sanitize();
        assertNull(store.view().defaultTab());
    }

    @Test
    void unknownTabFallsBackToAllOnSanitize() {
        Store store = new Store();
        store.view().setTab("deleted");
        store.sanitize();
        assertEquals(ViewSettings.TAB_ALL, store.view().tab());
    }

    @Test
    void blankQueryMakesNormalFolder() {
        assertFalse(new Folder("A", "  ").isSmart());
        assertTrue(new Folder("A", " tag:pvp ").isSmart());
        assertEquals("tag:pvp", new Folder("A", " tag:pvp ").query());
    }

    @Test
    void pruneEmptyKeepsEntriesWithData() {
        Store store = new Store();
        store.getOrCreate("empty");
        store.getOrCreate("pinned").setPinnedIn(ViewSettings.TAB_ALL, true);

        store.pruneEmpty();

        assertEquals(List.of("pinned"), List.copyOf(store.servers().keySet()));
    }

    @Test
    void tagsAreTrimmedAndDeduplicated() {
        ServerMeta meta = new ServerMeta();
        meta.setTags(List.of(" PvP ", "pvp", "", "  ", "Survival"));
        assertEquals(List.of("PvP", "Survival"), meta.tags());
    }

    @Test
    void newMetaIsEmpty() {
        ServerMeta meta = new ServerMeta();
        assertTrue(meta.isEmpty());
        meta.setNotes("x");
        assertFalse(meta.isEmpty());
    }

    @Test
    void rememberDirectPutsNewestFirst() {
        Store store = new Store();
        store.rememberDirect("a.example.com");
        store.rememberDirect("b.example.com");
        assertEquals(List.of("b.example.com", "a.example.com"), store.recentDirect());
    }

    @Test
    void rememberDirectKeepsTheSameServerOnce() {
        Store store = new Store();
        store.rememberDirect("Play.Example.com:25565");
        store.rememberDirect("other.example.com");
        store.rememberDirect(" play.example.com ");
        assertEquals(List.of("play.example.com", "other.example.com"), store.recentDirect());
    }

    @Test
    void rememberDirectIgnoresBlankAddresses() {
        Store store = new Store();
        store.rememberDirect("  ");
        store.rememberDirect(null);
        assertTrue(store.recentDirect().isEmpty());
    }

    @Test
    void rememberDirectDropsTheOldest() {
        Store store = new Store();
        for (int i = 0; i <= Store.MAX_RECENT_DIRECT; i++) store.rememberDirect("server" + i + ".example.com");
        assertEquals(Store.MAX_RECENT_DIRECT, store.recentDirect().size());
        assertEquals("server" + Store.MAX_RECENT_DIRECT + ".example.com", store.recentDirect().getFirst());
        assertFalse(store.recentDirect().contains("server0.example.com"));
    }

    @Test
    void forgetDirectMatchesNormalizedAddress() {
        Store store = new Store();
        store.rememberDirect("play.example.com");
        store.forgetDirect("PLAY.example.com:25565");
        assertTrue(store.recentDirect().isEmpty());
    }

    @Test
    void pinsArePerTab() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        ServerMeta meta = store.getOrCreate("a");
        meta.setPinnedIn(folder.id(), true);
        assertTrue(meta.isPinnedIn(folder.id()));
        assertFalse(meta.isPinnedIn(ViewSettings.TAB_ALL));
        assertTrue(meta.isPinnedAnywhere());
        meta.setPinnedIn(folder.id(), false);
        assertFalse(meta.isPinnedAnywhere());
    }

    @Test
    void deletingAFolderRemovesItsPins() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        ServerMeta meta = store.getOrCreate("a");
        meta.setPinnedIn(folder.id(), true);
        meta.setPinnedIn(ViewSettings.TAB_ALL, true);
        store.removeFolder(folder.id());
        assertEquals(List.of(ViewSettings.TAB_ALL), meta.pinnedTabs());
    }

    @Test
    void pinsOfUnknownTabsAreDroppedOnSanitize() {
        Store store = new Store();
        ServerMeta meta = store.getOrCreate("a");
        meta.setPinnedIn("deleted", true);
        meta.setPinnedIn(ViewSettings.TAB_ALL, true);
        store.sanitize();
        assertEquals(List.of(ViewSettings.TAB_ALL), meta.pinnedTabs());
    }

    @Test
    void hiddenServersCountAsData() {
        Store store = new Store();
        store.getOrCreate("hidden").setHiddenInAll(true);
        store.pruneEmpty();
        assertTrue(store.get("hidden").isPresent());
    }

    @Test
    void serverCanBeInSeveralFolders() {
        Store store = new Store();
        Folder a = store.addFolder(new Folder("A"));
        Folder b = store.addFolder(new Folder("B"));
        ServerMeta meta = store.getOrCreate("x");
        meta.addToFolder(a.id());
        meta.addToFolder(b.id());
        meta.addToFolder(a.id());
        assertEquals(List.of(a.id(), b.id()), meta.folderIds());
        store.removeFolder(a.id());
        assertEquals(List.of(b.id()), meta.folderIds());
    }

    @Test
    void legacyDataIsCopiedToEveryServerWithThatAddress() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Space Chunks"));
        store.getOrCreate("play.example.com").addToFolder(folder.id());
        store.getOrCreate("play.example.com").setNotes("shared");
        Divider divider = new Divider(ViewSettings.TAB_ALL, "play.example.com");
        store.dividers().add(divider);
        String first = ServerKey.of("play.example.com", "Space Chunks");
        String second = ServerKey.of("Play.Example.com:25565", "BlockBound");

        assertTrue(store.adoptLegacyKeys(List.of(first, second, ServerKey.of("other.net", "Other"))));

        assertTrue(store.get("play.example.com").isEmpty());
        assertEquals("shared", store.get(first).orElseThrow().notes());
        assertEquals("shared", store.get(second).orElseThrow().notes());
        store.get(second).orElseThrow().clearFolders();
        assertEquals(List.of(folder.id()), store.get(first).orElseThrow().folderIds());
        assertEquals(first, divider.beforeKey());
        assertFalse(store.adoptLegacyKeys(List.of(first, second)));
    }

    @Test
    void legacyDataWithoutServerIsKept() {
        Store store = new Store();
        store.getOrCreate("gone.example.com").setNotes("keep");
        assertFalse(store.adoptLegacyKeys(List.of(ServerKey.of("other.net", "Other"))));
        assertEquals("keep", store.get("gone.example.com").orElseThrow().notes());
    }

    @Test
    void movedKeysTakeTheirDataAlong() {
        Store store = new Store();
        store.getOrCreate("a").setNotes("first");
        store.getOrCreate("b").setNotes("second");
        store.getOrCreate("gone").setNotes("deleted server");
        Divider divider = new Divider(ViewSettings.TAB_ALL, "b");
        store.dividers().add(divider);

        // a and b swap, and b's old key is reused while "gone" is overwritten by a's data
        store.moveKeys(Map.of("a", "gone", "b", "a"));

        assertEquals("first", store.get("gone").orElseThrow().notes());
        assertEquals("second", store.get("a").orElseThrow().notes());
        assertTrue(store.get("b").isEmpty());
        assertEquals("a", divider.beforeKey());
    }

    @Test
    void activityIsSharedByAddress() {
        Store store = new Store();
        store.activityOrCreate("Play.Example.com:25565").addPlaytime(5_000);
        assertEquals(5_000, store.activity("play.example.com").orElseThrow().playtimeMs());
        assertTrue(store.activity("other.example.com").isEmpty());
    }

    @Test
    void legacyDataMergesIntoExistingEntry() {
        Store store = new Store();
        ServerMeta legacy = store.getOrCreate("play.example.com");
        legacy.setTags(List.of("pvp", "Friends"));
        legacy.setNotes("old note");
        legacy.setPinnedIn(ViewSettings.TAB_ALL, true);
        String key = ServerKey.of("play.example.com", "A");
        ServerMeta current = store.getOrCreate(key);
        current.setTags(List.of("friends", "survival"));
        current.setNotes("new note");

        store.adoptLegacyKeys(List.of(key));

        ServerMeta merged = store.get(key).orElseThrow();
        assertEquals(List.of("friends", "survival", "pvp"), merged.tags());
        assertEquals("new note\nold note", merged.notes());
        assertTrue(merged.isPinnedIn(ViewSettings.TAB_ALL));
        assertEquals(1, store.servers().size());
    }
}
