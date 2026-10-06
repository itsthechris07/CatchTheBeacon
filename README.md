![CatchTheBeacon](https://raw.githubusercontent.com/itsthechris07/CatchTheBeacon/main/branding/banner.png)

# CatchTheBeacon

A **Cores** minigame for Paper: **Red vs Blue on a sky map, every team guards two beacons (cores).** Break both enemy beacons
to win – but near an enemy beacon you get Mining Fatigue, so one player mines while the rest of the team keeps them
alive.

📖 **Documentation: [itsthechris07.github.io/CatchTheBeacon](https://itsthechris07.github.io/CatchTheBeacon/)** –
setup, configuration, commands, placeholders, network and more.

![How a round works](https://raw.githubusercontent.com/itsthechris07/CatchTheBeacon/main/branding/how-it-works.png)

## Features

![Features](https://raw.githubusercontent.com/itsthechris07/CatchTheBeacon/main/branding/features.png)

**Gameplay**
- **Two beacons per team** – both enemy beacons destroyed = victory. A boss bar shows everybody when a beacon is
  being mined.
- **Mining Fatigue around beacons** – breaking a beacon takes about 15 seconds, attacks need teamwork.
- **Base defense** – the team is warned when enemies come close, every beacon has a trap (blindness + slowness),
  defenders regenerate near their beacons and campers get poison.
- **No generators** – iron and diamond blocks of the map give armor and swords when mined. Steal the enemy's iron,
  guard your own.
- **Fair start** – a fresh kit on every respawn (16 arrows, wool, stone tools, leather armor in team colors),
  3 extra hearts, spawn and respawn protection, a compass pointing to the next enemy beacon.
- **Modern or 1.8 PvP** – with or without attack cooldown.
- **Variants** – players vote in the lobby: *Rush* (beacons break twice as fast, events come twice as often), *Bow only*, *No iron*, *One hit*.
- **Timed events** – beacon protection ends after 10 minutes, effects after 15, the round ends after 25 (all configurable).
- **The whole map can be broken** – or only placed blocks, with protected blocks of your choice.
- **Stats, leaderboard and achievements** – kills, deaths, K/D, wins, beacons, `/ctb top`, 8 achievements, a round
  summary with MVP. SQLite or MySQL/MariaDB.
- **Spectators and rejoin** – watch running games, players who lose their connection get back into their team.
- **Balanced teams** by win rate, team chat with `!` to shout, game chats don't mix with the server chat.
- **Tips** for new players during their first rounds.

**For server owners**
- **Guided arena setup** – `/ctb arena <name> setup` gives you hotbar tools for spawns and beacons and a checklist.
- **Map presets** – download a supported map, drop the zip into `plugins/CatchTheBeacon/maps` and it is set up
  automatically (currently: [Sakura by BreadBuilds](https://www.planetminecraft.com/project/sakura-cores-map/)).
  The lobby comes with the plugin.
- **Join signs and NPCs** – `[ctb]` signs with live status, Citizens or FancyNpcs as join NPCs.
- **Game manager GUI** – `/ctb game` lists, starts, stops and creates games.
- **Velocity network** – one game per server, lobby server with join signs that show the state of every game server.
- **VIP perks** (`ctb.vip`) – start the game, join full lobbies, double vote, join message, kill particles and a
  victory firework. Cosmetic only, no advantage in the fight.
- **Your world stays untouched** – inventories, stats, advancements and recipes of the players are saved and given back.
- **Every message can be changed** in `messages.yml` (MiniMessage).

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

- **Paper 26.2** (only the current version is supported)
- **Java 25**

## Getting started

1. Put `CatchTheBeacon.jar` into `plugins/` and start the server.
2. Get a map:
   - **Preset:** download [Sakura](https://www.planetminecraft.com/project/sakura-cores-map/), put the zip into
     `plugins/CatchTheBeacon/maps/` and restart (or run `/ctb arena create sakura`). Done – the game starts as soon
     as enough players join.
   - **Your own map:** copy the world folder to `plugins/CatchTheBeacon/maps/<name>` (or use
     `/ctb arena create <name> --world <world>`), set a lobby world with `/ctb arena <name> lobby setworld <map or world>`,
     then run `/ctb arena <name> setup` and follow the steps. Every team needs a spawn and two beacons.
3. `/ctb arena <name> creategame` – or place a sign with `[ctb]` in the first line and the arena name in the second.
4. Players join with `/ctb join` or the sign.

## Commands

| Command | Permission | |
|---|---|---|
| `/ctb join`, `/ctb quit`, `/ctb spectate` | – | Join, leave or watch a game |
| `/ctb stats [player]`, `/ctb achievements [player]`, `/ctb top [stat]` | – | Stats and leaderboard |
| `/ctb start` | `ctb.vip` | Starts the game in the lobby immediately |
| `/ctb arena create <name> [--world <world>]`, `/ctb arena list` | `ctb.admin` | Create and list arenas |
| `/ctb arena <arena> setup` / `check` / `creategame` / `removegame` | `ctb.admin` | Set up and run an arena |
| `/ctb game` | `ctb.admin` | Game manager (GUI) |
| `/ctb npc link <arena>` / `unlink` / `list` | `ctb.admin` | Join NPCs |
| `/ctb config items set` | `ctb.admin` | Saves your inventory as the kit |

`/ctb help` shows all commands. `ctb.network.bypass`: not put into the game automatically on a game server.

## Placeholders

PlaceholderAPI (`%ctb_…%`) and MiniPlaceholders:
`kills`, `deaths`, `kd`, `wins`, `games`, `beacons`, `achievements`, `team`, `arena`,
`players_<arena>`, `maxplayers_<arena>`, `state_<arena>`, `top_<stat>_<place>_<name|value>`.

## Network (Velocity)

- **Game servers:** `network.mode: game` – one arena per server, players joining the server join the game and are sent
  back to the lobby afterwards.
- **Lobby server:** `network.mode: lobby` with the game servers in `network.servers` – join signs (`[ctb]` + server
  name) and `/ctb join` send players to a free game.
- Use the same MySQL database on all servers (`sql.enabled: true`) for shared stats.

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
