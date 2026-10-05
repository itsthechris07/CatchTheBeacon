# Supported plugins

CatchTheBeacon works on its own. If one of these plugins is installed, it is used automatically – none of them is
needed.

| Plugin | What CatchTheBeacon does with it |
|---|---|
| [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) | [placeholders](placeholders.md) `%ctb_…%` |
| [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) | [placeholders](placeholders.md) `<ctb_…>` |
| [Parties](https://github.com/AlessioDP/Parties) | [parties](#parties) join together and play in the same team |
| [Party and Friends](https://github.com/simonsator/BungeecordPartyAndFriends) | the same, with *Spigot Party API for Party and Friends* on the server |
| [Citizens](https://www.spigotmc.org/resources/citizens.13811/) | [join NPCs](signs-and-npcs.md#join-npcs) with the state of the game as a hologram line |
| [FancyNpcs](https://modrinth.com/plugin/fancynpcs) | [join NPCs](signs-and-npcs.md#join-npcs) with the state behind the name |
| [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) or Vault | [money rewards](stats.md#rewards) with an economy plugin |
| [TAB](https://github.com/NEZNAMY/TAB) | stays out of the way while players are in a game |
| [EssentialsX](https://essentialsx.net) | god mode, vanish, AFK and teleports don't get into games |
| SuperVanish, PremiumVanish, CMI, … | vanished players aren't put into games automatically |
| [Multiverse-Core / -Inventories](https://modrinth.com/plugin/multiverse-core) | the game worlds are left alone |
| [Floodgate](https://geysermc.org) | Bedrock players get forms instead of chest menus |

## Parties

With **Parties** (by AlessioDP) or **Party and Friends** (by Simonsator), parties play together:

| Option | Default | Meaning |
|---|---|---|
| `parties.enabled` | `true` | use the party plugin |
| `parties.join-together` | `true` | the leader takes the party members on this server into the game (`/ctb join`, signs, NPCs) |
| `parties.same-team` | `true` | parties play in the same team – as long as the teams stay even; players who chose a team keep it |

Party and Friends keeps its parties on the proxy: install *Spigot Party API for Party and Friends* on the servers
with CatchTheBeacon. In a [network](network.md), the proxy plugins take the members along when the leader is sent to
a game server.

## Economy

Rewards use the API of [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked). Economy plugins of both the new
VaultUnlocked API and the old Vault API work (e.g. EssentialsX). See [Rewards](stats.md#rewards).

## TAB

While a player is in a game, TAB doesn't overwrite the tab list header and footer (`tablist.*`), the name tags in the
team colors and the sidebar of the game. Everything of TAB comes back when the player leaves the game.

## EssentialsX

- God mode, vanish and AFK are turned off when a player joins a game (god mode and vanish come back afterwards).
- `/god`, `/fly` and `/vanish` don't work while playing.
- Teleports of Essentials (`/back`, `/tpa`, `/home`, …) don't lead into the worlds of games.

## Vanish plugins

Vanished players (SuperVanish, PremiumVanish, EssentialsX, CMI, …) aren't put into games automatically – not on a
game server of a network, not as a party member – and can only play once they are visible again. Handy for admins
checking a server.

## Multiverse

- Multiverse-Core would import the game worlds (`ctb_…`) and load them again after a restart – they are removed from
  it.
- Multiverse-Inventories would swap inventories when players enter game worlds – CatchTheBeacon saves and restores
  the inventory itself, so Multiverse-Inventories leaves players in games alone.

## Floodgate (Bedrock players)

Chest menus are awkward on Bedrock. Players joining through Geyser/Floodgate get **forms** instead: team selection,
voting, the spectator compass, the game manager, …

## Other plugins

CatchTheBeacon saves the whole state of a player (inventory, health, game mode, effects, …) when they join a game
and restores it afterwards – also after a crash, from `backups/`. Plugins that change players in other worlds should
leave the worlds starting with `ctb_` alone.
