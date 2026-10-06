# Teams, kit and messages

Besides `config.yml`, three files in `plugins/CatchTheBeacon/` change how the game looks. Change them while the server
is stopped, or restart it afterwards.

## Teams

`teams.yml` sets the colors and items of the teams. *Random* is the choice in the team menu that lets the plugin pick.
The names come from the [messages](#messages) (`team_red`, `team_blue`, `team_random`), so they follow the language –
add `name: "..."` to a team to give it another name.

```yaml
teams:
  blue:
    chat_color: "9"              # color code (0-9, a-f), color name (dark_blue) or hex (#0000cd)
    item: "BLUE_WOOL"            # icon in the team menu, also the wool of the kit
    leather_color: "0, 0, 205"   # leather armor of the kit (RGB)
  red:
    chat_color: "c"
    item: "RED_WOOL"
    leather_color: "176, 46, 38"
  random:
    chat_color: "a"
    item: "WHITE_WOOL"
    leather_color: "255, 255, 255"
```

There are always exactly two teams – the keys `blue` and `red` stay, only their look changes.

## Kit

The kit is what every player gets at the start and after every death. By default:

| Slot | Item |
|---|---|
| 1 | stone sword |
| 2 | bow |
| 3 | stone pickaxe |
| 4 | stone axe |
| 5, 6 | 64 wool each (in the team color) |
| 8 | 16 steaks |
| 9 | 16 arrows |
| armor | leather armor (in the team color) |

Players also get the beacon compass (`game.beacon-compass`). 16 arrows on purpose: with more, every fight becomes a
bow fight.

### Your own kit

1. Put the items into your inventory exactly as players should get them (hotbar, inventory, armor slots).
2. Run `/ctb config items set`.

The kit is saved in `items.yml` and used from the next spawn on. **Leather armor and wool are recolored** to the team
of the player, so put in any color. Enchantments, names and other item data are kept.

`/ctb config items get --confirm` gives you the current kit to change it – it replaces your inventory and only works
while you are in a team (in a game). To go back to the default kit, delete `items.yml`.

Better gear doesn't belong into the kit: players get it from the [resource blocks](configuration.md#resources) of
the map.

## Messages

Every text players see is in `messages.yml` – messages, item names, menus, scoreboard, signs, titles. Change the
style or the wording:

```yaml
player_joined_game: '<yellow>{0} <gray>joined the game! (<yellow>{1}<gray>/<yellow>{2}<gray>)'
scoreboard_title: '<aqua><bold>Catch<white>The<aqua>Beacon'
sign_status_lobby: '<green>Lobby'
```

- Texts use [MiniMessage](https://docs.papermc.io/adventure/minimessage/format): `<red>`, `<bold>`, `<#ff8800>`,
  `<gradient:aqua:blue>`, … Old color codes like `&c` still work.
- Commands in messages are clickable: `<run:"/ctb quit">/ctb quit</run>` runs the command,
  `<suggest:"/ctb arena create ">/ctb arena create \<name></suggest>` puts it into the chat box. Players see what a
  click does when they hover over it.
- `{0}`, `{1}`, … are replaced with names, numbers, … – keep them in the text.
- Write `\<` for a literal `<`.
- The prefix in front of messages is `prefix` in `config.yml`, the tab list header and footer are `tablist.*`.

New messages of an update are added to your file automatically; texts you changed stay as they are. To get the
default text of a message back, delete its line and restart.

### Language

Set `language` in `config.yml` and restart:

| `language` | File |
|---|---|
| `en` (default) | `messages.yml` |
| `de` | `messages_de.yml` (German) |

For another language, copy `messages.yml` to `messages_<language>.yml` (e.g. `messages_fr.yml`), translate it and
set `language: fr`. Texts missing in your file are shown in English. A translation you'd like to share is welcome as
a [pull request](https://github.com/itsthechris07/CatchTheBeacon/pulls).

## Lobby

The default lobby `default_lobby` is a normal map in `maps/` – change it like any other map with
`/ctb arena <arena> lobby edit` and `lobby save`, or build your own and set it with `lobby setworld`.
