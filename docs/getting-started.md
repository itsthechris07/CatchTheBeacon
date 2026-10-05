# Getting started

## Installation

1. Make sure your server runs **Paper 26.2** on **Java 25**.
2. Put `CatchTheBeacon.jar` into `plugins/` and start the server.
3. The plugin creates `plugins/CatchTheBeacon/` with `config.yml`, `messages.yml` and `teams.yml`.

As long as there is no arena, CatchTheBeacon tells you in the console and admins (`ctb.admin`) on every join what to
do next – with clickable commands.

## Your first arena in two minutes (map preset)

CatchTheBeacon doesn't ship maps of other authors, but it knows the setup of some maps you can download for free.
The lobby comes with the plugin.

1. Download [Sakura by BreadBuilds](https://www.planetminecraft.com/project/sakura-cores-map/).
2. Put the zip file (don't unpack it) into `plugins/CatchTheBeacon/maps/`.
3. Restart the server – or run `/ctb arena create sakura`.

The map is unpacked, spawns and beacons are set, the lobby `default_lobby` is installed and the arena gets a game.
Players can join right away with `/ctb join`. The zip is deleted afterwards.

!!! tip
    Place a [join sign](signs-and-npcs.md) so players can join with a click: `[ctb]` in the first line, `sakura` in
    the second.

## Your own map

Every team needs **a spawn and two beacons**, the arena needs a **lobby world**. The guided setup walks you through
it – see [Arenas and maps](arenas.md):

```
/ctb arena create castle --world castle_world
/ctb arena castle lobby setworld default_lobby
/ctb arena castle setup
```

## Playing

| What | How |
|---|---|
| Join a game | `/ctb join`, a join sign or a join NPC |
| Choose a team / vote for a variant | items in the lobby hotbar |
| Watch a running game | `/ctb spectate` (or `/ctb join` while no game is waiting) |
| Leave | `/ctb quit` or the item in the lobby |
| Start right away | `/ctb start` or the start item (`ctb.vip`) |

The game starts when the lobby has enough players (`min-players` of the arena) and the
[countdown](configuration.md#lobby) is over. What happens then is explained in [How a round works](gameplay.md).

## What's next

- Tune the game in [`config.yml`](configuration.md) – changes apply after a restart.
- Translate or restyle every message in [`messages.yml`](customization.md#messages).
- Share stats across servers with [MySQL/MariaDB](stats.md#database).
- Run one game per server behind [Velocity](network.md).
