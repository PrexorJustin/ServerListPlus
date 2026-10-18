<div align="center">

<img src="docs/images/brand/readme-banner.png" alt="ServerListPlus" width="100%">

**The multiplayer server list, finally manageable.**<br>
Folders as tabs, search with filters, sorting, pinning, notes, tags and playtime, all client-side.

[![Available on Modrinth][badge-modrinth]][modrinth]
[![Available on CurseForge][badge-curseforge]][curseforge]
[![Available on GitHub][badge-github]][releases]

[![Modrinth downloads](https://img.shields.io/modrinth/dt/lEnmW6v6?logo=modrinth&label=Modrinth&color=1bd96a&style=flat-square)][modrinth]
[![CurseForge downloads](https://img.shields.io/curseforge/dt/1744778?logo=curseforge&label=CurseForge&color=f16436&style=flat-square)][curseforge]
[![Latest release](https://img.shields.io/github/v/release/PrexorJustin/ServerListPlus?label=release&color=5865f2&style=flat-square)][releases]
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.5_–_26.3-62b47a?style=flat-square)
[![License](https://img.shields.io/github/license/PrexorJustin/ServerListPlus?color=lightgrey&style=flat-square)](LICENSE)

[Features](#-features) · [Installation](#-installation) · [Usage](#-usage) · [Search](#-search) · [Your data](#-your-data) · [Contributing](#-contributing)

</div>

<br>

<p align="center">
  <img src="docs/images/server-list.png" alt="The server list with folders as tabs" width="90%">
</p>

> [!TIP]
> Everything is stored next to `servers.dat`, not in it. Remove the mod and your server list is exactly as before.

## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

### 🗂️ Organize
- **Tabs for folders**: "All" plus one tab per folder, as a row above the list or as a sidebar.
  A server can be in any number of folders; drag it onto a tab to put it there.
- **Smart folders**: save a search as a folder that fills itself, e.g. `tag:pvp ping:<80`.
- **Pinning**: keep servers at the top of a tab, also while sorting automatically.
- **Dividers**: titled, colored separators to group servers within a tab.
- **Hiding**: keep servers out of "All" without deleting them; they wait in a folded section.

</td>
<td width="50%" valign="top">

### 🔎 Find
- **Search and filters**: by name, address, MOTD and notes, plus `tag:`, `folder:`, `note:`,
  `has:`, `ping:`, `players:`, `played:`, `last:`, `domain:` and `is:`. See [Search](#-search).
- **Sorting**: manually by drag and drop, or by name, ping, players or last played.
- **Direct Connection**: a live preview of the typed server and the addresses you joined last.
- **Auto refresh**: re-ping the shown servers every few seconds, without the list flickering.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📝 Remember
- **Notes**: per server notes, marked by a symbol in the list that shows them on hover and opens
  them on click, in a card next to the list. Optionally shown in the corner when you join the
  server, e.g. for coordinates.
- **Tags and history**: per server tags; last join and total playtime per address,
  shown next to the name, as a clock symbol, on hover or not at all.

</td>
<td width="50%" valign="top">

### 🎨 Style
- **Colored names**: MiniMessage tags in server names, e.g.
  `<gradient:#ff5555:#ffaa00>Hypixel</gradient> <gray>Lobby`.
- **Looks**: tab colors and symbols, background gradients or your own pictures per tab.

</td>
</tr>
</table>

### 📸 Screenshots

| Right-click menu | Search | Settings |
|:---:|:---:|:---:|
| ![Context menu of a server](docs/images/server-menu.png) | ![Searching a folder](docs/images/search.png) | ![Settings menu](docs/images/settings.png) |

| Tabs as a sidebar | Direct Connection |
|:---:|:---:|
| ![Tabs as a sidebar](docs/images/sidebar.png) | ![Direct connection preview](docs/images/direct-connection.png) |

## 📦 Installation

1. Install [Fabric](https://fabricmc.net/use/) with the [Fabric API](https://modrinth.com/mod/fabric-api),
   or [NeoForge](https://neoforged.net/).
2. Download the jar for your Minecraft version and loader from [Modrinth][modrinth],
   [CurseForge][curseforge] or the [GitHub releases][releases].
3. Put it into the `mods` folder of your instance.

<div align="center">

| Minecraft | <img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/fabric_vector.svg" alt="Fabric" height="28"> | <img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/neoforge_vector.svg" alt="NeoForge" height="28"> |
|:---|:---:|:---:|
| 1.20.5 – 1.20.6 | ✅ | ✅ |
| 1.21 – 1.21.11 | ✅ | ✅ |
| 26.1 – 26.3 | ✅ | ✅ |

</div>

## 🕹️ Usage

| To | Do |
|---|---|
| Create a folder | Click **+** next to the tabs |
| Rename a tab or divider | Double click it |
| Change a tab's color, symbol or background | Right click the tab |
| Put a server into folders | Right click it → **Folders**, or drag it onto a tab |
| Pin, hide or annotate a server | Right click it |
| Read or edit a server's notes | Hover or click the note symbol next to its name |
| Add a divider | Right click a server → **Insert divider above** |
| Search | Click the magnifier or press <kbd>Ctrl</kbd> + <kbd>F</kbd> |
| Change sorting | Click the gear → **Sort** |
| Move the selected server | Drag it, or <kbd>Shift</kbd> + <kbd>↑</kbd>/<kbd>↓</kbd> (also across dividers) |
| Select several servers | <kbd>Shift</kbd> + click for a range, <kbd>Ctrl</kbd>/<kbd>Cmd</kbd> + click to add or remove one |
| Move several servers | Drag one of the selected servers, or onto a tab to put them all into that folder |
| Put several servers into folders, pin or hide them | Right click one of the selected servers |
| Reorder tabs | Drag a tab between the others |
| Settings | Click the gear: sorting, details display, auto refresh, tab layout |

> [!NOTE]
> A server named `Friends/Survival SMP` is put into the folder "Friends" (created if needed)
> and named "Survival SMP".

<details>
<summary><b>Colored server names (MiniMessage)</b></summary>
<br>

Server names understand MiniMessage colors (`<red>`, `<#ff8800>`, `<color:gold>`), decorations
(`<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`, negated as `<!bold>`),
`<gradient:…>`, `<rainbow>` and `<reset>`. Unknown tags stay as typed, `\<` writes a plain `<`.
Search and sorting use the name without tags.

</details>

## 🔍 Search

All terms must match. Case and Minecraft color codes are ignored.

```text
tag:pvp ping:<100 -is:hidden "mini games"
```

<details>
<summary><b>All search terms</b></summary>
<br>

| Term | Matches |
|---|---|
| `survival` | name, address, MOTD or notes contain the word |
| `"mini games"` | the exact phrase |
| `tag:pvp` | a tag starting with "pvp" (`tag:"mini g"` works too) |
| `folder:friends` | one of its folders contains "friends" |
| `note:base` | the notes contain "base" |
| `has:notes`, `has:tags`, `has:folder` | has notes, tags or is in a folder |
| `is:pinned` | pinned in any tab |
| `is:hidden` | hidden from "All"; searching for it shows them there |
| `is:online`, `is:offline` | the last ping reached the server or failed |
| `ping:<100`, `players:>10` | compares ping or online players with `<`, `>` or `=` |
| `played:>2h`, `played:0` | compares the total playtime; `0` finds servers never played |
| `last:<7d` | last joined less than 7 days ago (`min`, `h`, `d`, `w`, `mo`, `y`) |
| `domain:example.com` | that domain and its subdomains (`host:` works too) |
| `domain:*.example.com` | `*` matches anything, here only subdomains |
| `-term` | negates any term, e.g. `-tag:modded` |

Servers that were not pinged yet or are offline never match `ping:` or `players:`, and servers
never joined never match `last:`.

</details>

## 💾 Your data

Everything ServerListPlus adds is stored in `config/serverlistplus/servers.json`; your own
pictures for backgrounds go into `config/serverlistplus/wallpapers/`. The vanilla
`servers.dat` only changes the way it would without the mod: when you reorder or add servers,
and when a server sends a new icon.

Servers are recognised by address and name. Two entries with the same address are separate
servers (e.g. a network listed twice under different names), while playtime and last join
are shared by address. Data of a deleted server is kept for 30 days in case it comes back.

## 🤝 Contributing

Bug reports, ideas and pull requests are welcome. [CONTRIBUTING.md](CONTRIBUTING.md) explains
how to build, test and change the mod; [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) explains
how it is put together.

## 📄 License

Released under the [MIT License](LICENSE).

[modrinth]: https://modrinth.com/mod/lEnmW6v6
[curseforge]: https://www.curseforge.com/minecraft/mc-mods/serverlistplus
[releases]: https://github.com/PrexorJustin/ServerListPlus/releases
[badge-modrinth]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg
[badge-curseforge]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg
[badge-github]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg
