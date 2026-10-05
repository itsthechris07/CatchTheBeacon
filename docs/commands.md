# Commands and permissions

All commands start with `/ctb` (aliases: `/catchthebeacon`, `/catchbeacon`, `/ctbeacon`). `/ctb help [query]` lists
the commands a player can use – click a command to put it into the chat.

## Permissions

| Permission | Default | Meaning |
|---|---|---|
| *(none)* | everybody | play: join, spectate, stats, … |
| `ctb.vip` | op | start the game early and the [VIP perks](#vip-perks) |
| `ctb.admin` | op | set up arenas, manage games, join signs and NPCs, update notifications |
| `ctb.network.bypass` | **nobody** (not even ops) | not put into the game when joining a [game server](network.md) – e.g. to set up its arena |

## Players

| Command | What it does |
|---|---|
| `/ctb` | shows the plugin version |
| `/ctb help [query]` | shows all commands |
| `/ctb join` | joins the game that waits for players – or watches a running one |
| `/ctb spectate` | watches a running game |
| `/ctb quit` | leaves the current game |
| `/ctb stats [player]` | shows the [statistics](stats.md) of a player |
| `/ctb top [stat]` | shows the best players: `wins` (default), `kills`, `games`, `beacons` or `deaths` |
| `/ctb achievements [player]` | shows the [achievements](stats.md#achievements) of a player |

## VIPs (`ctb.vip`)

| Command | What it does |
|---|---|
| `/ctb start` | starts the game in the lobby in 5 seconds (also: the start item in the lobby) |

### VIP perks

Nothing that helps in the fight – VIP ranks are often sold, so they only give comfort and cosmetics:

- start the game early (`/ctb start` or the lime dye in the lobby)
- join a full lobby (the last player without VIP makes room)
- votes for the [variant](gameplay.md#variants) count twice
- a highlighted join message
- particles on kills
- a golden firework when your team wins

## Arenas (`ctb.admin`)

See [Arenas and maps](arenas.md) for the whole setup.

| Command | What it does |
|---|---|
| `/ctb arena create <name> [--world <world>]` | creates an arena from a map in `maps/` (`--world`: imports a world of the server) |
| `/ctb arena list` | lists all arenas and what they are missing |
| `/ctb arena <arena> setup` | starts the guided setup with tools in your hotbar |
| `/ctb arena <arena> setup exit` | saves the worlds and leaves the guided setup |
| `/ctb arena <arena> check` | shows what is missing to play the arena |
| `/ctb arena <arena> edit` / `save` | teleports you into an editable copy of the arena world / saves it |
| `/ctb arena <arena> team <red\|blue> setspawn` | sets the spawn of a team to your location |
| `/ctb arena <arena> team <red\|blue> setbeacon <left\|right>` | sets a beacon of a team (searches a beacon within 3 blocks) |
| `/ctb arena <arena> lobby setworld <map\|world>` | sets the lobby world: a map in `maps/` or a world of the server (gets imported) |
| `/ctb arena <arena> lobby edit` / `save` | the same as `edit` / `save` for the lobby world |
| `/ctb arena <arena> lobby setspawn` | sets the lobby spawn to your location |
| `/ctb arena <arena> creategame [--force]` | starts a game with the arena, also after every restart (`--force`: only the lobby has to be set up) |
| `/ctb arena <arena> removegame` | stops the games of the arena and creates none after a restart, so the arena can be changed |

## Games (`ctb.admin`)

| Command | What it does |
|---|---|
| `/ctb game` | opens the game manager: all games, start, stop, watch, *New game* (console: lists the games) |
| `/ctb game list` | lists the games with their state and players |
| `/ctb game <id> start` | starts a game in the lobby (enough players needed) |
| `/ctb game <id> stop [--remove]` | stops a game without a winner, the arena gets a new round (`--remove`: none until the restart) |
| `/ctb game <id> spectate` | watches the running game |

## Kit, NPCs and debugging (`ctb.admin`)

| Command | What it does |
|---|---|
| `/ctb config items set` | saves your inventory as the [kit](customization.md#kit) |
| `/ctb config items get [--confirm]` | replaces your inventory with the kit |
| `/ctb npc link <arena\|server>` | links the [NPC](signs-and-npcs.md#join-npcs) you click next |
| `/ctb npc unlink` | removes the link of the NPC you click next |
| `/ctb npc list` | lists the join NPCs |
| `/ctb debug` | shows debug information |
| `/ctb debug info [player]` | shows the game of a player |
| `/ctb debug setstate <state>` | sets the state of your current game |
| `/ctb debug sentry` | sends a test error report to Sentry (see [Privacy](privacy.md)) |

## Startup flags

For testing, these Java system properties can be added to the start command of the server (`java -D… -jar paper.jar`):

| Flag | Effect |
|---|---|
| `-Dctb.offline=true` | no bStats, Sentry or update check |
| `-Dctb.sentry.debug=true` | logs what would be sent to Sentry |
| `-Dctb.debug.autostart=true` | all online players join a game which starts right away (only for testing) |
