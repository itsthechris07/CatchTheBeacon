# How a round works

Two teams – **Red** and **Blue** – play on a sky map. Every team has **two beacons** (cores). The team that destroys
both enemy beacons wins.

![How a round works](assets/how-it-works.png)

## 1. Lobby

Players join with `/ctb join`, a [join sign or NPC](signs-and-npcs.md) and wait in the lobby. Their hotbar:

| Item | What it does |
|---|---|
| Red bed – *Select team* | choose Red, Blue or Random |
| Paper – *Vote for a variant* | vote for the [variant](#variants) of this round |
| Lime dye – *Start the game* | starts in 5 seconds (only with `ctb.vip`) |
| Red dye – *Leave* | leave the game |

As soon as the arena's `min-players` are in the lobby, the countdown starts (`lobby.countdown`, default 180 seconds).
It is shortened to 10 seconds when the lobby is full. `/ctb start` starts the game in 5 seconds.

Players who chose *Random* are put into the teams by their win rate (`game.balance-teams`), so both teams are even.
[Parties](integrations.md#parties) play in the same team.

## 2. Gear up

Everybody spawns in their base with the **kit**: a stone sword, a bow with 16 arrows, a stone pickaxe and axe,
two stacks of wool (in the team color) to build bridges, 16 steaks, leather armor in the team color and a compass
pointing to the nearest enemy beacon. The kit is given again on every respawn and
[can be changed](customization.md#kit). Players have
**3 extra hearts** (`game.extra-hearts`).

There are **no generators**. Better gear comes from the map itself: breaking an **iron block** gives a random piece of
iron armor or an iron sword, a **diamond block** the diamond version (`resources`). Armor is put on directly if it is
better than the one you wear. Steal the enemy's iron, guard your own.

Players are protected after spawning (`game.respawn-protection`), and nobody can build or break blocks close to the
team spawns (`game.spawn-protection`).

## 3. Attack and defend

Beacons are protected by the base around them:

- **Mining Fatigue** – enemies near a beacon get Mining Fatigue I, breaking the beacon takes about 15 seconds
  (`base.mining-fatigue-*`). One player mines, the rest of the team has to keep them alive. This is the central idea
  of the game.
- **Warning** – the team is warned when an enemy comes close to one of its beacons (`game.beacon-warning-radius`).
- **Trap** – the first enemy who comes close to a beacon gets blindness and slowness for a few seconds
  (`base.trap-*`).
- **Regeneration** – defenders regenerate near their own beacons (`base.regeneration-radius`).
- **No camping** – players who stand at their own beacon for too long get poison (`base.camping-*`). The time doesn't
  count while enemies are near: defending is not camping.
- **Boss bar** – everybody sees when a beacon is being mined and how far it is (`base.mining-bossbar`).

While one of your beacons is being mined, your compass points to it.

The whole map can be broken (`game.only-placed-blocks-breakable: false`). Blocks in `game.unbreakable-blocks` can't be
broken, not even by explosions.

## 4. Timed events

Events happen after some minutes and are shown as *Next event* in the scoreboard (`events`). By default:

| Minute | Event |
|---:|---|
| 10 | Beacon protection ends: the blocks around the beacons can be changed |
| 15 | Everybody gets Speed I and Strength I |
| 25 | The round ends: the team with more beacons left wins, otherwise nobody |

## 5. The end

A team wins when both enemy beacons are destroyed. Everybody sees the winner with fireworks and a
[round summary](stats.md#round-summary) (MVP, most kills, destroyed beacons, mined resources). After
`game.ending-seconds` everybody is sent back – with a **[Play again]** button – and the arena gets a new round.

## Dying, leaving and coming back

- Kills count even when the victim falls into the void or lava within `game.last-hit-seconds` after the last hit.
- Below `game.instant-death-height` (default: 0) players die instantly instead of falling for a while.
- Who loses the connection or leaves a running game can come back into their team within `game.rejoin-seconds`.
- Friendly fire is off by default (`game.friendly-fire`).

## Variants

Players vote for a variant in the lobby, the one with the most votes is played (no votes: a normal round). Votes of
VIPs (`ctb.vip`) count twice.

| Variant | Rules |
|---|---|
| Rush | beacons break twice as fast, events come twice as often |
| Bow only | only arrows hurt, bows have Infinity |
| No iron | resource blocks give nothing |
| One hit | every hit of an enemy kills |

Turn voting off or remove variants under [`variants`](configuration.md#variants).

## Spectators

Players can watch running games with `/ctb spectate` (or `/ctb join` while no game is waiting). Spectators can't
interact with the game; their compass teleports them to players.

## Chat

Only the players of a game read its chat. While playing, messages only go to the own team; messages starting with
`!` go to everybody in the game. Spectators chat among themselves. Players in games don't read the chat of the rest
of the server (`chat.*`).
