# Stats and rewards

## Stats

Every player's kills, deaths, wins, played games and destroyed beacons are saved (`stats.enabled`).

| Command | What it shows |
|---|---|
| `/ctb stats [player]` | the stats of a player, including K/D |
| `/ctb top [stat]` | the best 10 by `wins` (default), `kills`, `games`, `beacons` or `deaths`, and your own rank |

Use them in scoreboards and holograms with [placeholders](placeholders.md), e.g. `%ctb_wins%` or a leaderboard with
`%ctb_top_wins_1_name%`.

Stats only count in real games. Nothing a player does in a game changes their normal Minecraft statistics,
advancements or recipes, and the ender chest is locked in game worlds.

## Database

By default stats are saved in `plugins/CatchTheBeacon/database.db` (SQLite), so there is nothing to set up.

For a network, or if you prefer it, use **MySQL or MariaDB** so all servers share the stats:

```yaml
sql:
  enabled: true
  host: 'localhost'
  port: 3306
  database: 'CatchTheBeacon'
  username: 'username'
  password: 'password'
  table-prefix: 'ctb'   # tables: ctb_stats, ctb_achievements
```

The database must exist; the tables are created automatically. The keys are the same as in EasyPrefix, so both
plugins can use the same database. Paper ships the drivers.

### Keep the password out of config.yml

`config.yml` is often shared: in a support chat, with an AI assistant or in git. Instead of the password, write
`${NAME}` and set the environment variable `NAME` on the server:

```yaml
sql:
  password: "${CTB_MYSQL_PASSWORD}"
```

This works for every `sql.*` value (e.g. `host: "${CTB_MYSQL_HOST}"`, `port: "${CTB_MYSQL_PORT}"`). The variable is
read when the plugin loads the config and never written into the file. If it isn't set, the console warns (with the
name of the variable, not its value) and the value stays empty. Values without `${...}` work as before.

=== "Start script"

    ```bash
    # Linux (or Environment=CTB_MYSQL_PASSWORD=... in a systemd service)
    export CTB_MYSQL_PASSWORD='secret'
    java -jar paper.jar --nogui
    ```

    ```bat
    rem Windows (start.bat)
    set CTB_MYSQL_PASSWORD=secret
    java -jar paper.jar --nogui
    ```

=== "Docker"

    ```yaml
    # docker-compose.yml
    services:
      minecraft:
        environment:
          CTB_MYSQL_PASSWORD: "secret"
    ```

    or `docker run -e CTB_MYSQL_PASSWORD=secret ...`. With an `env_file:` the password stays out of the compose file too.

=== "Pterodactyl"

    Environment variables come from the egg: the panel admin adds a variable `CTB_MYSQL_PASSWORD` under
    *Admin → Nests → (egg) → Variables* (user viewable and editable, not shown in the startup command), then the value
    is set in the *Startup* tab of the server.

!!! tip "A MySQL user of its own"
    Don't use `root`. Create a user that only has rights on the plugin's database. If the password leaks anyway,
    nothing else is at risk:

    ```sql
    CREATE DATABASE CatchTheBeacon;
    CREATE USER 'ctb'@'%' IDENTIFIED BY 'a-long-random-password';
    GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON CatchTheBeacon.* TO 'ctb'@'%';
    ```

    Replace `%` with the address of the Minecraft server if you can.

!!! note "Older versions"
    A `stats.yml` of an older version of CatchTheBeacon is imported into the database once and renamed to
    `stats.yml.imported`.

## Achievements

Players unlock achievements while playing (`achievements.enabled`). They see them with `/ctb achievements [player]`,
the number is `%ctb_achievements%`.

| Achievement | How to get it |
|---|---|
| First victory | win a game |
| First blood | make the first kill of a round |
| Last-second rescue | kill an enemy while they are mining your beacon |
| No problems | win without losing a beacon |
| That was quick | win within `achievements.quick-win-minutes` (5) minutes |
| One-man army | destroy both enemy beacons yourself in one round |
| Unstoppable | kill 5 enemies without dying |
| Veteran | win 10 games |

Names and descriptions are in [`messages.yml`](customization.md#messages) (`achievement_*`).

## Round summary

At the end of a round everybody sees a summary in the chat (`game.round-summary`):

- duration and variant
- the **MVP**: the player with the most points (a kill = 1, a destroyed beacon = 3)
- the player with the most kills
- who destroyed which beacon, and when
- what every player got from resource blocks

## Tips for new players

During their first rounds (`tips.rounds`, default 3) new players get short tips in the action bar, e.g. that a beacon takes
about 15 seconds to mine. This needs the stats to know who is new.

## Rewards

With an economy plugin, players can earn money (`rewards.enabled`, off by default). This needs
[VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) or Vault and an economy plugin such as EssentialsX.

```yaml
rewards:
  enabled: true
  kill: 5
  beacon: 20
  win: 50     # every player of the winning team
  game: 10    # every player who is still in the game at the end, also without a winner
```

`0` gives nothing for that. Players see what they got in the chat (`+20 (beacon destroyed)`).
