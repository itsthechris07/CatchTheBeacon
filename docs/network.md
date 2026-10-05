# Velocity network

For bigger servers, CatchTheBeacon can run **one game per server** behind a [Velocity](https://papermc.io/software/velocity)
(or BungeeCord) proxy: a lobby server sends players to game servers, which send them back after the round.

```
              Velocity
          /      |       \
     lobby     ctb-1     ctb-2
  (signs, NPCs) (game)   (game)
```

Velocity understands the BungeeCord plugin channel CatchTheBeacon uses (`bungee-plugin-message-channel = true` in
`velocity.toml`, the default). No plugin is needed on the proxy.

## Game servers

Install CatchTheBeacon, set up **one arena** (see [Arenas and maps](arenas.md)) and set in `config.yml`:

```yaml
network:
  mode: game
  lobby-server: lobby   # the name of the lobby server in velocity.toml
  arena: ""             # the arena of this server (empty: the first playable one)
```

- Every player joining the server joins the game – or watches it while it is running.
- After the round, or with `/ctb quit`, players are sent to the lobby server.
- The state of the game is sent as the server's MOTD, so the lobby server can read it.

!!! warning "Setting up a game server"
    Players – also admins – are put into the game as soon as they join. To set up the arena, give yourself
    `ctb.network.bypass` (not even ops have it), or set the arena up before it can be played. Vanished players aren't
    put into the game either.

## Lobby server

Install CatchTheBeacon on the lobby server too and list the game servers:

```yaml
network:
  mode: lobby
  servers:
    - name: ctb-1               # the name in velocity.toml
      address: 127.0.0.1:25566  # host:port to read the state from
    - name: ctb-2
      address: 127.0.0.1:25567
```

The lobby server reads the state of every game server every 2 seconds.

- `/ctb join` sends the player to the game server with the most players waiting – or to a running game to watch it.
- `/ctb spectate` sends the player to a running game.
- [Join signs](signs-and-npcs.md#join-signs): `[ctb]` in the first line, the **server name** (e.g. `ctb-1`) in the
  second.
- [Join NPCs](signs-and-npcs.md#join-npcs): `/ctb npc link ctb-1`.
- [Placeholders](placeholders.md#games) take the server name: `%ctb_players_ctb-1%`.

## Shared stats

Use the same MySQL/MariaDB database on **all** servers, so stats, leaderboards and achievements are the same
everywhere:

```yaml
sql:
  enabled: true
  host: 'db.example.com'
  # ...
```

See [Database](stats.md#database).

## Parties

With Parties or Party and Friends, the party leader takes the party along: the lobby server picks a game server with
room for the whole party, and the proxy plugin of the party plugin sends the members after the leader. Party and
Friends needs *Spigot Party API for Party and Friends* on the game servers. See [Parties](integrations.md#parties).
