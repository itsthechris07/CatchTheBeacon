![CatchTheBeacon](https://raw.githubusercontent.com/itsthechris07/CatchTheBeacon/main/branding/banner.png)

# CatchTheBeacon

**Team PvP minigame for Paper – Red vs Blue on a sky map, every team guards two beacons (cores).** Break both enemy
beacons to win. Known as **Cores** or **Destroy the Core**, and a good fit for everybody who likes BedWars – but
without generators and without waiting for a shop.

- ⚔️ **Attacks need teamwork** – near an enemy beacon you get Mining Fatigue: one player mines for about 15 seconds,
  the rest of the team keeps them alive.
- ⛏️ **No generators** – iron and diamond blocks are part of the map. Steal the enemy's iron, guard your own.
- 🚀 **Playable in minutes** – drop a supported map into a folder and the arena sets itself up, or use the guided
  setup for your own map.
- 🌐 **One server or a network** – runs on a single server or with one game per server behind Velocity.
- 🎮 **Java and Bedrock** – Bedrock players (Geyser/Floodgate) get forms instead of chest menus.
- 🌍 **English and German** included, every message can be changed (MiniMessage).

📖 **[Documentation](https://itsthechris07.github.io/CatchTheBeacon/)** ·
🐛 **[Report a bug or suggest a feature](https://github.com/itsthechris07/CatchTheBeacon/issues)**

![How a round works](https://raw.githubusercontent.com/itsthechris07/CatchTheBeacon/main/branding/how-it-works.png)

## Quick start

1. Put `CatchTheBeacon.jar` into `plugins/` and start the server.
2. Download the map [Sakura by BreadBuilds](https://www.planetminecraft.com/project/sakura-cores-map/), put the zip
   into `plugins/CatchTheBeacon/maps/` and restart. The arena and the lobby are set up automatically.
3. Players join with `/ctb join` – the round starts as soon as enough players are in the lobby.

Your own map: `/ctb arena create <name> --world <world>`, then `/ctb arena <name> setup` and follow the checklist
(every team needs a spawn and two beacons). More in the
[documentation](https://itsthechris07.github.io/CatchTheBeacon/getting-started/).

## Features

![Features](https://raw.githubusercontent.com/itsthechris07/CatchTheBeacon/main/branding/features.png)

**Gameplay**
- **Two beacons per team** – both enemy beacons destroyed = victory. A boss bar shows everybody when a beacon is
  being mined.
- **Mining Fatigue around beacons** – breaking a beacon takes about 15 seconds, attacks need teamwork.
- **Base defense** – the team is warned when enemies come close, every beacon has a trap (blindness + slowness),
  defenders regenerate near their beacons and campers get poison.
- **No generators** – iron and diamond blocks of the map give armor and swords when mined.
- **Fair start** – a fresh kit on every respawn (16 arrows, wool, stone tools, leather armor in team colors),
  3 extra hearts, spawn and respawn protection, a compass pointing to the next enemy beacon.
- **Modern or 1.8 PvP** – with or without attack cooldown.
- **Variants** – players vote in the lobby: *Rush* (beacons break twice as fast, events come twice as often),
  *Bow only*, *No iron*, *One hit*.
- **Timed events** – beacon protection ends after 10 minutes, effects after 15, the round ends after 25 (all configurable).
- **The whole map can be broken** – or only placed blocks, with protected blocks of your choice.
- **Stats, leaderboard and achievements** – kills, deaths, K/D, wins, beacons, `/ctb top`, 8 achievements, a round
  summary with MVP. SQLite or MySQL/MariaDB.
- **Spectators and rejoin** – watch running games, players who lose their connection get back into their team.
- **Balanced teams** by win rate, team chat with `!` to shout, game chats don't mix with the server chat.
- **Tips** for new players during their first rounds.

**For server owners**
- **Guided arena setup** – `/ctb arena <name> setup` gives you hotbar tools for spawns and beacons and a checklist.
- **Map presets** – a supported map is set up automatically, the lobby comes with the plugin.
- **Join signs and NPCs** – `[ctb]` signs with live status, Citizens or FancyNpcs as join NPCs.
- **Game manager GUI** – `/ctb game` lists, starts, stops and creates games.
- **Velocity network** – one game per server, a lobby server with join signs that show the state of every game
  server, shared stats in MySQL.
- **VIP perks** (`ctb.vip`) – start the game, join full lobbies, double vote, join message, kill particles and a
  victory firework. Cosmetic only, no advantage in the fight.
- **Your world stays untouched** – inventories, stats, advancements and recipes of the players are saved and given back.
- **Clickable commands** in chat messages, placeholders for PlaceholderAPI and MiniPlaceholders.

## Languages

| Language | `language` in `config.yml` |
|---|---|
| English | `en` (default) |
| German (Deutsch) | `de` |

Every text is in `messages.yml` and can be changed. For another language copy the file to `messages_<language>.yml`
and translate it – missing texts fall back to English. Translations are welcome as a pull request.

## Supported plugins

All optional:

| Plugin | What CatchTheBeacon does with it |
|---|---|
| PlaceholderAPI, MiniPlaceholders | Placeholders for stats, leaderboard and game state |
| Parties, Party and Friends | Parties join together and play in the same team |
| Citizens, FancyNpcs | Join NPCs that show the state of the game |
| Vault / VaultUnlocked | Money for kills, beacons, wins and played rounds |
| TAB | Tab list, name tags in team colors and sidebar during the game |
| EssentialsX, SuperVanish, PremiumVanish | God mode, vanish and AFK are turned off in the game, no `/back` into games |
| Multiverse-Core, Multiverse-Inventories | Game worlds are kept out of Multiverse |
| Floodgate (Geyser) | Bedrock players get forms instead of chest menus |

## Requirements

- **Paper 26.2 or 26.3** (forks of Paper like Purpur work too, Spigot doesn't)
- **Java 25**

## Commands

| Command | Permission | |
|---|---|---|
| `/ctb join`, `/ctb quit`, `/ctb spectate` | – | Join, leave or watch a game |
| `/ctb stats [player]`, `/ctb achievements [player]`, `/ctb top [stat]` | – | Stats and leaderboard |
| `/ctb start` | `ctb.vip` | Starts the game in the lobby immediately |
| `/ctb arena …`, `/ctb game`, `/ctb npc …`, `/ctb config …` | `ctb.admin` | Arenas, games, join NPCs and the kit |

All commands, permissions and placeholders:
[commands](https://itsthechris07.github.io/CatchTheBeacon/commands/) ·
[placeholders](https://itsthechris07.github.io/CatchTheBeacon/placeholders/) ·
[network](https://itsthechris07.github.io/CatchTheBeacon/network/)

## Building

```bash
./gradlew build
```

The jar is in `build/libs/`. Tests run with MockBukkit.

## Privacy

CatchTheBeacon sends anonymous statistics (bStats), anonymous error reports (Sentry) and checks GitHub for updates.
Everything can be disabled – see [PRIVACY.md](PRIVACY.md).

## License

[Apache License 2.0](LICENSE)
