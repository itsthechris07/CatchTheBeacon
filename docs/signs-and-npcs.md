# Join signs and NPCs

Players don't have to type commands: a click on a sign or an NPC lets them join.

## Join signs

Place a sign (any wood, wall or standing) as an admin (`ctb.admin`) and write:

| Line | Text |
|---|---|
| 1 | `[ctb]` |
| 2 | the name of the arena, e.g. `sakura` |

The sign then shows the arena and the state of its game:

```
[CatchTheBeacon]
Sakura
Lobby
3/8
```

| State | Meaning |
|---|---|
| Lobby | players can join |
| Running | the game is on – a click lets you watch |
| Ending | the round is over, a new one starts soon |
| No game | the arena has no game (see below) |

- **Right-click** joins the game of the sign (or spectates it while it is running).
- Arenas with a sign get a game automatically when the server starts.
- **Every sign has its own game.** Two signs of the same arena run two games side by side – useful for busy servers.
- Signs can only be broken by admins while **sneaking**, so nobody breaks them by accident.

Signs are stored in `signs.yml`. The texts (`sign_title`, `sign_status_*`, …) are in
[`messages.yml`](customization.md#messages).

!!! tip "Network"
    On the lobby server of a [Velocity network](network.md) the second line is the name of a game server instead.

## Join NPCs

With [Citizens](https://www.spigotmc.org/resources/citizens.13811/) or
[FancyNpcs](https://modrinth.com/plugin/fancynpcs) installed, any NPC can be a join button:

1. Create the NPC with Citizens or FancyNpcs.
2. Run `/ctb npc link <arena>` (on a network lobby: `/ctb npc link <server>`).
3. Right-click the NPC within 30 seconds.

Now a click on the NPC joins the arena. The NPC shows the state of the game (Citizens: an extra hologram line,
FancyNpcs: behind the name) – turn this off with `npcs.show-status: false`.

| Command | What it does |
|---|---|
| `/ctb npc link <arena\|server>` | link the next NPC you click |
| `/ctb npc unlink` | unlink the next NPC you click |
| `/ctb npc list` | show all linked NPCs |

Links are stored in `npcs.yml`.

## Other ways

- `/ctb join` joins the game that is waiting for players (or spectates a running one).
- Admins open the game menu with `/ctb game` to start, stop or watch games.
- Other plugins can run `ctb join` for the player, e.g. a menu plugin or a portal.
