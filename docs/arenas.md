# Arenas and maps

An **arena** is a map plus its setup: a lobby world with a spawn, and for both teams a spawn and two beacons. Arenas
are stored in `plugins/CatchTheBeacon/arenas.yml`, their worlds in `plugins/CatchTheBeacon/maps/`.

Every game runs on **copies** of the worlds (`ctb_…`), so the maps never change. The copies are deleted after the
round and at every start.

## Map presets

For some maps that can be downloaded for free, CatchTheBeacon knows the whole setup. Put the downloaded zip (or the
unpacked world folder) into `plugins/CatchTheBeacon/maps/` and restart the server or run `/ctb arena create <name>`.

| Preset | Map | Players |
|---|---|---|
| `sakura` | [Sakura by BreadBuilds](https://www.planetminecraft.com/project/sakura-cores-map/) | 2–8 |

The lobby `default_lobby` comes with the plugin – a glass-walled platform any arena can use:
`/ctb arena <arena> lobby setworld default_lobby`.

`/ctb arena list` shows the arenas and links the downloads of presets that aren't installed yet.

## Your own map

### 1. Create the arena

Either import a world of the server:

```
/ctb arena create castle --world castle_world
```

or copy the world folder to `plugins/CatchTheBeacon/maps/castle` yourself and run `/ctb arena create castle`.

These folder layouts work:

- an old world (`level.dat` + `region/`)
- a 26.x dimension (`region/`, `data/`, …)
- a whole 26.x save (`dimensions/minecraft/overworld/` inside)

Arena names are lower case. `create` and `list` can't be used as names.

### 2. Set the lobby world

```
/ctb arena castle lobby setworld default_lobby
```

The lobby can be a map in `maps/` or a world of the server (it is imported into `maps/`). Several arenas can share a
lobby map.

### 3. Guided setup

```
/ctb arena castle setup
```

Your inventory, health, game mode etc. are saved, and you are teleported into editable copies of the lobby and the
arena. The hotbar holds the tools:

| Tool | Use |
|---|---|
| Nether star – *Lobby spawn* (in the lobby) | sets the lobby spawn where you stand |
| Red / blue banner – *Spawn* (in the arena) | sets the team spawn where you stand, including your view direction |
| Red / blue glass – *Beacon left / right* | right-click the beacon block |
| Book – *Checklist* | shows what is still missing |
| Ender pearl – *Switch world* | switches between lobby and arena |
| Emerald – *Save and leave* | saves both worlds and gives you your inventory back |

The next step glows and is selected, the action bar tells you what to do. Changes you make to the worlds (building,
breaking) are saved too. Leaving in any other way – `/ctb arena castle setup exit`, quitting, teleporting away – also
saves.

!!! note "Beacons"
    Place the beacon blocks in the map first. *Left* and *right* are just names – each team needs two beacons, the
    order doesn't matter.

### 4. Check and play

```
/ctb arena castle check
/ctb arena castle creategame
```

`check` lists what is missing. `creategame` starts a game with the arena and remembers it (`auto-game`): the arena
gets a game again after every restart, and a new round after every game. Arenas with a [join sign](signs-and-npcs.md)
get a game automatically, too.

`creategame --force` only needs the lobby to be set up – for testing.

## Changing an arena

An arena with running games can't be changed. Stop them first:

```
/ctb arena castle removegame
```

This stops the games of the arena and removes `auto-game`. Then use the guided setup again, or the single commands:

| Command | What it does |
|---|---|
| `/ctb arena <arena> edit` / `save` | teleports you into an editable copy of the arena world / saves it |
| `/ctb arena <arena> team <red\|blue> setspawn` | sets the team spawn to your location |
| `/ctb arena <arena> team <red\|blue> setbeacon <left\|right>` | sets the beacon within 3 blocks of you |
| `/ctb arena <arena> lobby edit` / `save` | the same for the lobby world |
| `/ctb arena <arena> lobby setspawn` | sets the lobby spawn to your location |

Afterwards run `/ctb arena <arena> creategame` again.

## Settings in arenas.yml

Some settings of an arena are only in `arenas.yml` (change them while the server is stopped):

```yaml
arenas:
  castle:
    display-name: "<gold>Castle"   # shown in messages, signs and the scoreboard (MiniMessage)
    min-players: 2                 # the countdown starts with this many players
    max-players: 8                 # the lobby is full (VIPs can still join)
    auto-game: true                # set by creategame / removegame
```

## Map tips

- Both sides should be the same (mirrored or rotated by 180°), so the game is fair.
- Put iron and diamond blocks on the map – they are the only way to better gear (see `resources`).
- Leave some room around the beacons: Mining Fatigue reaches 6 blocks by default, the trap 5 blocks.
- Use a void world. Players die at y 0 (`game.instant-death-height`).
- Protect blocks that must not be broken with `game.unbreakable-blocks` (e.g. an enchanting table).
