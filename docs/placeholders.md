# Placeholders

CatchTheBeacon has placeholders for [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) and
[MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders). Install one of them – the placeholders are registered
automatically, no eCloud download needed. Use them in scoreboards, tab lists, holograms, NPC names, menus, …

## Player stats

Stats of the player who sees the text (see [Stats](stats.md)).

| PlaceholderAPI | MiniPlaceholders | Value |
|---|---|---|
| `%ctb_kills%` | `<ctb_kills>` | kills |
| `%ctb_deaths%` | `<ctb_deaths>` | deaths |
| `%ctb_kd%` | `<ctb_kd>` | kills per death |
| `%ctb_wins%` | `<ctb_wins>` | won games |
| `%ctb_games%` | `<ctb_games>` | played games |
| `%ctb_beacons%` | `<ctb_beacons>` | destroyed beacons |
| `%ctb_achievements%` | `<ctb_achievements>` | number of unlocked [achievements](stats.md#achievements) |
| `%ctb_team%` | `<ctb_team>` | the team of the player (empty if not playing) |
| `%ctb_arena%` | `<ctb_arena>` | the arena the player plays or watches (empty if none) |

## Leaderboard

The best 10 players of a stat – for leaderboard holograms. `<stat>` is `kills`, `deaths`, `wins`, `games` or
`beacons`, `<place>` is 1 to 10. Empty if nobody is at that place. Refreshed every minute.

| PlaceholderAPI | MiniPlaceholders | Value |
|---|---|---|
| `%ctb_top_<stat>_<place>_name%` | `<ctb_top:<stat>:<place>:name>` | name of the player |
| `%ctb_top_<stat>_<place>_value%` | `<ctb_top:<stat>:<place>:value>` | their value |

Example – a hologram with the top 3 by wins:

```
Best players
1. %ctb_top_wins_1_name% - %ctb_top_wins_1_value% wins
2. %ctb_top_wins_2_name% - %ctb_top_wins_2_value% wins
3. %ctb_top_wins_3_name% - %ctb_top_wins_3_value% wins
```

## Games

The game of an arena – for NPC names, holograms or menu items next to your join points. On the lobby server of a
[network](network.md), use the name of the game server instead of the arena.

| PlaceholderAPI | MiniPlaceholders | Value |
|---|---|---|
| `%ctb_players_<arena>%` | `<ctb_players:<arena>>` | players in the game |
| `%ctb_maxplayers_<arena>%` | `<ctb_maxplayers:<arena>>` | max. players of the arena |
| `%ctb_state_<arena>%` | `<ctb_state:<arena>>` | Lobby / Running / Ending / No game (texts from `messages.yml`) |

Example: `Sakura %ctb_players_sakura%/%ctb_maxplayers_sakura% %ctb_state_sakura%`.

!!! tip
    [Join NPCs](signs-and-npcs.md#join-npcs) of Citizens and FancyNpcs show the state on their own – no placeholders
    needed.
