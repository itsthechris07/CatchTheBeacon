# Configuration

Everything is set in `plugins/CatchTheBeacon/config.yml`. Change it while the server is stopped, or restart the server
afterwards. New options of an update are added to your file automatically – your values and comments stay.

Texts use [MiniMessage](https://docs.papermc.io/adventure/minimessage/format) (`<red>`, `<bold>`, `<#ff8800>`,
`<gradient:aqua:blue>`, …). Old color codes like `&c` still work.

!!! tip "Other files"
    Teams, the kit and all messages are in their own files – see [Teams, kit and messages](customization.md).
    Settings of single arenas (players, display name) are in [`arenas.yml`](arenas.md#settings-in-arenasyml).

## General

| Option | Default | Meaning |
|---|---|---|
| `prefix` | `[CatchTheBeacon]` | in front of all messages, empty for none |
| `tablist.header` / `footer` | | tab list of players in games (lines, MiniMessage) |

## Lobby

| Option | Default | Meaning |
|---|---|---|
| `lobby.countdown` | `180` | seconds until the game starts once the arena's `min-players` are in the lobby (at least 10). Shortened to 10 seconds when the lobby is full. |

## Game

| Option | Default | Meaning |
|---|---|---|
| `game.spawn-protection` | `5` | radius around the team spawns where no blocks can be placed or broken (0: off) |
| `game.beacon-warning-radius` | `10` | the team is warned when an enemy comes this close to one of its beacons |
| `game.respawn-protection` | `3` | seconds without damage after spawning |
| `game.show-death-messages` | `true` | "x was killed by y" |
| `game.friendly-fire` | `false` | players can hurt their own team |
| `game.last-hit-seconds` | `10` | the last attacker gets the kill if the victim dies within this time (void, lava, …), 0: only direct kills |
| `game.instant-death-height` | `0` | players die instantly below this height (`false`: off) |
| `game.only-placed-blocks-breakable` | `false` | `true`: only blocks placed by players can be broken, the map stays as it is |
| `game.rejoin-seconds` | `120` | players who leave a running game can come back into their team within this time (0: off) |
| `game.extra-hearts` | `3` | hearts in addition to the normal 10 |
| `game.combat` | `modern` | `modern`: current PvP with attack cooldown, `legacy`: 1.8 PvP without cooldown |
| `game.unbreakable-blocks` | `[ENCHANTING_TABLE]` | [materials](https://jd.papermc.io/paper/org/bukkit/Material.html) nobody can break, not even explosions |
| `game.beacon-compass` | `true` | the compass of the kit points to the nearest enemy beacon, or to an own beacon while it is being mined |
| `game.round-summary` | `true` | [summary](stats.md#round-summary) at the end of a round |
| `game.ending-seconds` | `15` | seconds from the end of a round until players are sent back (at least 3) |
| `game.balance-teams` | `true` | players who chose *Random* are put into teams by their win rate |

## Bases

The bases around the beacons – the heart of the game, see [How a round works](gameplay.md#3-attack-and-defend).
All radii are in blocks, 0 turns the feature off.

| Option | Default | Meaning |
|---|---|---|
| `base.mining-fatigue-radius` | `6` | enemies near a beacon get Mining Fatigue |
| `base.mining-fatigue-level` | `1` | 1 = Mining Fatigue I (a beacon takes ~15 s), 2 = Mining Fatigue II (much slower) |
| `base.regeneration-radius` | `8` | defenders regenerate near their own beacons |
| `base.trap-radius` | `5` | the first enemy coming this close gets blindness and slowness |
| `base.trap-seconds` | `4` | how long the trap lasts |
| `base.camping-radius` / `camping-seconds` | `4` / `30` | players near their own beacon for longer get poison (not while enemies are near) |
| `base.mining-bossbar` | `true` | everybody sees a boss bar while a beacon is being mined |

## Resources

Blocks of the map that give a reward when broken (instead of their normal drop). One of the rewards is picked by
chance; armor is put on directly if it is better than the worn one.

```yaml
resources:
  - block: IRON_BLOCK
    rewards:
      - IRON_HELMET
      - IRON_CHESTPLATE
      - IRON_LEGGINGS
      - IRON_BOOTS
      - IRON_SWORD
  - block: GOLD_BLOCK
    rewards:
      - GOLDEN_APPLE:2     # material:amount
      - ARROW:8
```

## Events

Things that happen after some minutes of a round, shown as *Next event* in the scoreboard. Minutes can be fractions
(`minute: 7.5`).

| Action | Effect |
|---|---|
| `beacon-protection-off` | the blocks around the beacons can be changed |
| `effects` | all players get the [effects](https://jd.papermc.io/paper/org/bukkit/potion/PotionEffectType.html) in `effects` (`"SPEED:1"` = Speed II) |
| `end` | the round ends: the team with more beacons left wins, otherwise nobody |

```yaml
events:
  - minute: 10
    action: beacon-protection-off
  - minute: 15
    action: effects
    effects:
      - "SPEED:0"
      - "STRENGTH:0"
  - minute: 25
    action: end
```

Without an `end` event a round lasts until a team has lost both beacons.

## Spectators and chat

| Option | Default | Meaning |
|---|---|---|
| `spectators.enabled` | `true` | players can watch running games |
| `chat.team-chat` | `true` | while playing, messages only go to the own team |
| `chat.shout-prefix` | `!` | messages starting with this go to everybody in the game (empty: off) |
| `chat.isolate-games` | `true` | players in games don't read the chat of players outside of games |

## Stats and database

| Option | Default | Meaning |
|---|---|---|
| `stats.enabled` | `true` | save kills, deaths, wins, … of every player |
| `sql.*` | off | MySQL/MariaDB instead of SQLite – see [Stats](stats.md#database) |
| `achievements.enabled` | `true` | [achievements](stats.md#achievements) |
| `achievements.quick-win-minutes` | `5` | time limit of *That was quick* |
| `tips.rounds` | `3` | new players get tips in the action bar during their first rounds (0: off) |
| `rewards.*` | off | money for kills, beacons, wins – see [Rewards](stats.md#rewards) |

## Variants

| Option | Default | Meaning |
|---|---|---|
| `variants.voting` | `true` | players vote for a variant in the lobby |
| `variants.list` | all four | the variants to vote for: `rush`, `bow-only`, `no-iron`, `one-hit` |

See [Variants](gameplay.md#variants) for their rules.

## Network, parties and NPCs

| Option | Meaning |
|---|---|
| `network.*` | one game per server behind Velocity – see [Velocity network](network.md) |
| `parties.*` | parties join together and play in the same team – see [Supported plugins](integrations.md#parties) |
| `npcs.show-status` | join NPCs show the state of their game – see [Join NPCs](signs-and-npcs.md#join-npcs) |

## Telemetry and updates

| Option | Default | Meaning |
|---|---|---|
| `sentry.enabled` | `true` | anonymous error reports – see [Privacy](privacy.md) |
| `update-checker.enabled` | `true` | checks GitHub for new versions and tells admins on join |

bStats is turned off in `plugins/bStats/config.yml`.

## The default config.yml

??? example "config.yml"
    ```yaml
    --8<-- "src/main/resources/config.yml"
    ```
