# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project uses [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-10-18

Thanks to [@FllipEis](https://github.com/FllipEis) for multi-select, tab reordering and the
sidebar rework ([#1](https://github.com/PrexorJustin/ServerListPlus/pull/1)).

### Added
- When a newer release is on Modrinth for your Minecraft version and loader, the game log says so
  and an update icon appears next to the settings in the server list. It can be hidden until the
  next version, and the check can be turned off in the settings.
- Several servers can be selected with Shift + click (a range) and Ctrl/Cmd + click (one more or
  one less), then dragged together, dropped onto a tab, or put into folders, pinned and hidden
  from their right-click menu.
- Tabs can be reordered by dragging them, in the row and in the sidebar.
- Option to auto-hide the sidebar: it slides in while the pointer is over its place.
- A dragged server is shown as its whole row instead of only its name.

### Changed
- With the sidebar, the server list keeps its full width, so the rows stay centered under the
  title, and the search field and settings sit at the top of the sidebar instead of above the list.
- The tabs in the row are centered over the server list instead of the space left of the icons.
- Sorting moved into the settings menu; the settings icon is highlighted while a sort order is active.
- The sidebar's "+" became a "New folder" row.

### Fixed
- Server icons flickered to the default icon when switching tabs.

## [1.1.1] - 2026-10-17

### Fixed
- The mod showed a "Template Mod" placeholder as its icon in launchers and mod lists; it now has
  the ServerListPlus logo.

## [1.1.0] - 2026-10-16

### Added
- Servers with notes show a note symbol next to their name: hover to read the notes, click to edit them.
- Option per server to show its notes when joining it, in a toast with the server's name and a bar
  that runs out; longer notes stay longer.
- Search filters `note:`, `has:notes`, `has:tags`, `has:folder`, `is:online`, `is:offline`,
  `played:` (total playtime) and `last:` (time since the last join).

### Changed
- Plain search words also look into the notes.
- The info card sits next to the server list when there is room, with narrower server rows, and
  shows whichever server is selected. In small windows it stays over the list and on screen.
- The info card puts the notes first, with a larger text box.

### Fixed
- After clicking an empty spot of the info card, clicks on its text fields went to the servers under it.
- Edits in the info card were lost when joining a server (or leaving the screen) without closing the card first.
- With the tabs as a sidebar, the server rows could be drawn shifted to the left after leaving a
  server, until the list was scrolled (Minecraft 1.21.9 and newer).

## [1.0.0] - 2026-10-13

### Added
- Folders as tabs above the list or as a sidebar; a server can be in several folders.
- Smart folders that fill themselves from a saved search.
- Search with `tag:`, `folder:`, `ping:`, `players:`, `domain:`, `is:pinned` and `is:hidden`.
- Sorting by name, ping, players or last played, and manual order by drag and drop.
- Pinning per tab, titled and colored dividers, hiding servers from "All".
- Notes and tags per server; last join and playtime per address.
- Direct Connection preview of the typed server and a history of recent addresses.
- Automatic status refresh at a chosen interval.
- Tab colors, symbols and backgrounds (gradient, server icon or own pictures).
- English and German translations.
- MiniMessage tags in server names: colors, decorations, gradients and rainbows.
- Option to hide last played and playtime in the server rows.
- Shift+arrow keys move a server across dividers into the next section.
