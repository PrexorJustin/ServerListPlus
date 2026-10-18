package me.prexorjustin.serverlistplus.core.storage;

import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.core.style.Palette;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStoreTest {
    @TempDir
    Path dir;

    private JsonStore jsonStore() {
        return new JsonStore(dir.resolve("serverlistplus/servers.json"));
    }

    @Test
    void missingFileGivesEmptyStore() throws IOException {
        JsonStore.LoadResult result = jsonStore().load();
        assertEquals(JsonStore.Status.MISSING, result.status());
        assertTrue(result.store().servers().isEmpty());
    }

    @Test
    void roundTrip() throws IOException {
        Store store = new Store();
        Folder folder = new Folder("Friends");
        store.folders().add(folder);
        ServerMeta meta = store.getOrCreate("play.example.com");
        meta.setNotes("Ünïcode & <html>");
        meta.setTags(List.of("pvp"));
        meta.addToFolder(folder.id());
        meta.setPinnedIn(folder.id(), true);
        store.activityOrCreate("play.example.com").setLastPlayed(123);
        store.activityOrCreate("play.example.com").addPlaytime(456);
        store.view().setSort(SortMode.PING);
        store.view().setDetails(DetailsMode.HOVER);
        store.view().setAutoRefreshSeconds(30);
        store.view().setTabLayout(TabLayout.SIDEBAR);
        store.view().setAutoHideSidebar(true);
        Divider divider = new Divider(folder.id(), "play.example.com");
        divider.setColor(Palette.byKey("teal").orElseThrow());
        store.dividers().add(divider);

        jsonStore().save(store);
        JsonStore.LoadResult result = jsonStore().load();

        assertEquals(JsonStore.Status.LOADED, result.status());
        Store loaded = result.store();
        assertEquals(folder.id(), loaded.folders().getFirst().id());
        assertEquals("Friends", loaded.folders().getFirst().name());
        ServerMeta copy = loaded.get("play.example.com").orElseThrow();
        assertEquals("Ünïcode & <html>", copy.notes());
        assertEquals(List.of("pvp"), copy.tags());
        assertEquals(List.of(folder.id()), copy.folderIds());
        assertEquals(List.of(folder.id()), copy.pinnedTabs());
        assertEquals(123, loaded.activity("play.example.com").orElseThrow().lastPlayed());
        assertEquals(456, loaded.activity("play.example.com").orElseThrow().playtimeMs());
        assertNull(copy.orphanedSince());
        assertEquals(SortMode.PING, loaded.view().sort());
        assertEquals(DetailsMode.HOVER, loaded.view().details());
        assertEquals(30, loaded.view().autoRefreshSeconds());
        assertEquals(TabLayout.SIDEBAR, loaded.view().tabLayout());
        assertTrue(loaded.view().autoHideSidebar());
        assertEquals(Palette.byKey("teal"), java.util.Optional.ofNullable(loaded.dividers().getFirst().color()));
    }

    @Test
    void unknownSortModeFallsBackToManual() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{\"version\": 1, \"view\": {\"sort\": \"FUTURE_MODE\"}}");

        JsonStore.LoadResult result = json.load();

        assertEquals(JsonStore.Status.LOADED, result.status());
        assertEquals(SortMode.MANUAL, result.store().view().sort());
    }

    @Test
    void unknownDetailsModeFallsBackToInline() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{\"version\": 1, \"view\": {\"details\": \"FUTURE_MODE\"}}");

        JsonStore.LoadResult result = json.load();

        assertEquals(JsonStore.Status.LOADED, result.status());
        assertEquals(DetailsMode.INLINE, result.store().view().details());
    }

    @Test
    void globalPinsMoveToAllAndTheirFolder() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), """
            {"version": 1,
             "folders": [{"id": "f1", "name": "Friends"}],
             "servers": {"a": {"pinned": true, "folderId": "f1"}, "b": {"pinned": true}, "c": {"pinned": false, "notes": "x"}},
             "view": {"tab": "favorites", "favoritesHidden": false}}
            """);

        Store store = json.load().store();

        assertEquals(Store.CURRENT_VERSION, store.version());
        assertEquals(List.of("all", "f1"), store.get("a").orElseThrow().pinnedTabs());
        assertEquals(List.of("all"), store.get("b").orElseThrow().pinnedTabs());
        assertFalse(store.get("c").orElseThrow().isPinnedAnywhere());
        assertEquals("all", store.view().tab());
    }

    @Test
    void missingTabLayoutMeansTop() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{\"version\": 1, \"view\": {}}");

        assertEquals(TabLayout.TOP, json.load().store().view().tabLayout());
        assertFalse(json.load().store().view().autoHideSidebar());
    }

    @Test
    void saveLeavesNoTempFile() throws IOException {
        JsonStore json = jsonStore();
        json.save(new Store());
        json.save(new Store());
        try (var files = Files.list(json.file().getParent())) {
            assertEquals(List.of("servers.json"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void corruptFileIsBackedUp() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{ not json");

        JsonStore.LoadResult result = json.load();

        assertEquals(JsonStore.Status.CORRUPT, result.status());
        assertTrue(result.store().servers().isEmpty());
        assertEquals("{ not json", Files.readString(json.backupFile()));
    }

    @Test
    void wrongTypesCountAsCorrupt() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{\"version\": \"one\", \"servers\": []}");

        assertEquals(JsonStore.Status.CORRUPT, json.load().status());
    }

    @Test
    void newerVersionIsReadOnly() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{\"version\": 99, \"servers\": {\"a\": {\"notes\": \"keep\"}}, \"future\": true}");

        JsonStore.LoadResult result = json.load();

        assertEquals(JsonStore.Status.NEWER_VERSION, result.status());
        assertFalse(result.writable());
        assertEquals("keep", result.store().get("a").orElseThrow().notes());
    }

    @Test
    void unversionedPartialFileIsRepaired() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), "{\"folders\": [{\"name\": \"no id\"}], \"servers\": {\"a\": {\"folderId\": \"gone\", \"tags\": null}}}");

        JsonStore.LoadResult result = json.load();

        assertEquals(JsonStore.Status.LOADED, result.status());
        Store store = result.store();
        assertEquals(Store.CURRENT_VERSION, store.version());
        assertTrue(store.folders().isEmpty());
        ServerMeta meta = store.get("a").orElseThrow();
        assertTrue(meta.folderIds().isEmpty());
        assertEquals(List.of(), meta.tags());
        assertEquals("", meta.notes());
    }

    @Test
    void singleFolderBecomesAList() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), """
            {"version": 2, "folders": [{"id": "f1", "name": "Friends"}], "servers": {"a": {"folderId": "f1"}}}
            """);

        assertEquals(List.of("f1"), json.load().store().get("a").orElseThrow().folderIds());
    }

    @Test
    void playtimeMovesToOneRecordPerAddress() throws IOException {
        JsonStore json = jsonStore();
        Files.createDirectories(json.file().getParent());
        Files.writeString(json.file(), """
            {"version": 3, "servers": {
              "play.example.com#Space Chunks": {"lastPlayed": 100, "playtimeMs": 9000, "notes": "a"},
              "play.example.com#BlockBound": {"lastPlayed": 300, "playtimeMs": 9000},
              "other.net": {"playtimeMs": 50}}}
            """);

        Store store = json.load().store();

        assertEquals(300, store.activity("play.example.com").orElseThrow().lastPlayed());
        assertEquals(9000, store.activity("play.example.com").orElseThrow().playtimeMs());
        assertEquals(50, store.activity("other.net").orElseThrow().playtimeMs());
        assertEquals("a", store.get("play.example.com#Space Chunks").orElseThrow().notes());
    }
}
