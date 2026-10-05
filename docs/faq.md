# FAQ and troubleshooting

## Which versions are supported?

Only the current version: **Paper 26.2** with **Java 25** (forks of Paper like Purpur work too). Spigot isn't
supported. When a new Minecraft version comes out, update CatchTheBeacon as well.

## Players can't join – "no game"

An arena only gets a game when it is set up completely and
- `/ctb arena <arena> creategame` was run (it is remembered for restarts), or
- the arena has a [join sign](signs-and-npcs.md).

`/ctb arena list` and `/ctb arena <arena> check` show what is missing, `/ctb game list` shows the running games.

## I can't change my arena

Arenas with games can't be changed. Run `/ctb arena <arena> removegame` first, and `creategame` when you are done.

## The console warns that there is no beacon at a position

The beacon positions in `arenas.yml` don't match the map – e.g. after the map was changed or replaced. Set the beacons
again with the guided setup (`/ctb arena <arena> setup`).

## My map loads as an empty or new world

Paper 26.x saves worlds differently than older versions. Put the map into `plugins/CatchTheBeacon/maps/<name>` – old
worlds (`level.dat` + `region/`) and 26.x worlds both work there – and create the arena with
`/ctb arena create <name>`. To use a world of the server, import it with `--world <world>`; don't copy world folders
next to `world/` yourself.

## Are my maps changed by the games?

No. Every game plays on a copy (`ctb_…`); the copies are deleted after the game and at every start. Only the
`edit`/`save` commands and the guided setup change the maps in `maps/`.

## Does joining a game change a player's inventory?

No. The whole state of the player (inventory, armor, health, food, XP, game mode, effects, location, …) is saved when
they join and restored when they leave. It is also saved to `plugins/CatchTheBeacon/backups/` – after a crash, the
player gets everything back on the next join.

In game worlds, normal Minecraft statistics, advancements, recipes and the ender chest are locked, so a game doesn't
affect the rest of the server.

## Can I use `/reload` or PlugMan?

No – restart the server. Running games and their worlds would be lost.

## Can players break the whole map?

Yes, by default (`game.only-placed-blocks-breakable: false`) – bridging into the enemy base from below is part of the
game. Set it to `true` to only allow blocks placed by players, or protect single blocks with `game.unbreakable-blocks`.
Nobody can build or break close to the team spawns (`game.spawn-protection`).

## Can I run several games at the same time?

Yes. Every [join sign](signs-and-npcs.md) has its own game, so two signs of an arena run two games side by side.
Admins can also create games in the game menu (`/ctb game` → *New game*). For one game per server, see
[Velocity network](network.md).

## How do I update?

Replace the jar and restart the server. New options of `config.yml`, `teams.yml` and `messages.yml` are added to your
files automatically, your values stay. Admins are told about new versions on join (`update-checker.enabled`).

## Where do I report bugs?

Open an [issue on GitHub](https://github.com/itsthechris07/CatchTheBeacon/issues). Please add the output of
`/ctb debug`, your Paper version and the errors of the console.
