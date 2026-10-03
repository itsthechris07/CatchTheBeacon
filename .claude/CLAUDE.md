# CatchTheBeacon – Projektkontext für Claude

Minecraft-Minigame-Plugin (Paper), Team Rot vs. Blau: Beacon des Gegners erobern.
Hobbyprojekt von Christian34, begonnen ca. 2020, 2026 wieder aufgenommen.

## Spielprinzip (vom User, Maßstab für neue Features)
- 2 Teams mit je 2 Cores (Beacons) auf einer SkyMap; beide gegnerischen Cores abbauen = Sieg.
- **Zentrale Mechanik:** Abbaulähmung um den gegnerischen Core (Mining Fatigue I → Beacon ≈ 15 s), Angreifer müssen
  den Abbauer verteidigen (`base.mining-fatigue-*`).
- Startausrüstung nach jedem Tod; Verbesserung durch Abbauen von Eisen-/Diamantblöcken der Map (`resources`) –
  **keine Generatoren**, Ressourcen sind Teil der Map (Taktik: Eisen des Gegners holen, eigenes sichern).
- Ganze Map abbaubar (`game.only-placed-blocks-breakable: false` ist Standard).
- Spawnschutz in der Base, **16 Pfeile** (Community wollte nicht mehr, sonst nur Bogenkämpfe), **3 Extra-Herzen**.
- Später: Shop mit Eisen als Währung (z. B. 4 Steinknöpfe für 4 Eisen) → [todo.md](todo.md). Zaubertisch/Ambosse gab es
  zeitweise, wurden wieder entfernt (Zaubertisch per `game.unbreakable-blocks` schützbar).
Mit dem User **Deutsch** sprechen; Code, Kommentare und Plugin-Nachrichten bleiben Englisch.

Schwesterprojekt mit gleichem Setup: `D:\Daten\Development\IdeaProjects\EasyPrefix` (dessen `CLAUDE.md` ist die
Referenz für Build/Test-Konventionen – bei Unklarheiten dort nachsehen).

## Stack
- **Paper API 26.2** (neue jahresbasierte MC-Versionierung, Nachfolger von 1.21.11), **Java 25**, **Gradle 9.8** (Kotlin DSL),
  Shadow-Plugin `com.gradleup.shadow`.
- Nur die aktuelle Version wird unterstützt, **keine** Abwärtskompatibilität → [decisions.md](decisions.md).
- Commands: **Cloud v2** (`org.incendo`) mit Annotations, `LegacyPaperCommandManager` + `MinecraftExceptionHandler` + `MinecraftHelp`.
  Brigadier-Registrierung absichtlich aus (wie EasyPrefix). `simpleCoordinator` → Commands laufen im Main-Thread.
- GUIs: `de.themoep:inventorygui`.
- Geshadet + relocated nach `com.christian34.catchthebeacon.libs.*`: cloud, geantyref, inventorygui.
- `plugin.yml` behält **`api-version: "1.13"` absichtlich**: nur dann schreibt Paper die alten `org.bukkit.Sound`-Enum-Aufrufe
  der geshadeten InventoryGui um (Sound ist jetzt ein Interface). Nicht erhöhen!

## Build & Test
JDK 25 liegt in `$env:JAVA_HOME`. Systemsprache Deutsch → Compiler-Meldungen als `Fehler`/`Warnung`.

- `.\gradlew.bat build` – Tests + Shadow-Jar; kopiert den Jar (wenn `serverPluginsDir` gesetzt) nach
  `D:\Programme\Minecraft\Server\paper\plugins\update\` → wird beim nächsten Serverstart geladen (Windows sperrt den geladenen Jar).
- `.\gradlew.bat test` – nur Tests. `-x test` für schnellen Deploy.
- `.\gradlew.bat mysqlTest` – Stats gegen echtes MySQL (`MySqlTest`, Tag `mysql`, nicht im normalen Build): Zugang in
  `gradle.properties` (`mysql.host/port/database/username/password`, aktuell MySQL 8.4 des Users auf `pi5`, DB `Test`).
  Eigene Tabellen pro Lauf (`ctbtest…_stats`), werden danach gelöscht.
- `.\gradlew.bat testServer "-Pcommands=ctb debug;plugins"` – startet den lokalen Testserver, führt `;`-getrennte
  Konsolen-Commands aus, stoppt ihn und gibt eine WARN/ERROR-Zusammenfassung aus (`scripts/test-server.ps1`).
  - Exit-Code 3 = Server läuft schon (User spielt oft darauf) → **nicht erzwingen**, User bitten neu zu starten / ingame zu testen.
  - Alternative ohne den User zu stören: Wegwerf-Server im Scratchpad (paper.jar + `libraries/`, `versions/`, `cache/` vom
    Testserver kopieren, eigener Port in `server.properties`, `eula=true`) und `scripts/test-server.ps1 -ServerDir <scratch>`.
  - Windows PowerShell schreibt beim Start ein UTF-8-BOM auf stdin → erster Konsolen-Command wurde zu `?ctb ...`
    (im Skript per Leerzeile abgefangen; `EasyPrefix/scripts/test-server.ps1` hat den Fehler noch).
- IntelliJ: als Gradle-Projekt über `settings.gradle.kts` verlinkt (Plugin + `bot`); keine Maven-/`.iml`-Reste mehr.
  Deploy im IDE: Gradle-Task `build` (bzw. `build -x test`). `deployPlugin` ist UP-TO-DATE, wenn die identische Jar schon
  in `update/` liegt (Build ist reproduzierbar).
- Lokale Settings in `gradle.properties` (gitignored): `serverPluginsDir=D:/Programme/Minecraft/Server/paper/plugins`,
  optional `relocateLibraries=false` für HotSwap (Release-Jars müssen relocaten).
- Debug-Autostart (alle Online-Spieler + ein `TestPlayer` joinen, Force-Start): Server mit `-Dctb.debug.autostart=true` starten.
  Achtung: `TestPlayer` ist nur ein Java-Proxy ohne Entity (teilt sich das Inventar mit dem Admin!) – für echte Tests die Bots nutzen.

### Test-Bots (Unterprojekt `bot/`, nicht Teil des Plugins)
- Echte Spieler-Verbindungen über MCProtocolLib (`org.geysermc.mcprotocollib:protocol:26.2-SNAPSHOT`, Repo opencollab) –
  **Version muss zur Server-Version passen**. Server braucht `online-mode=false` (lokaler Testserver: ja, keine Whitelist).
- Bauen: `.\gradlew.bat :bot:shadowJar` → `bot/build/libs/bot-all.jar` (das Startskript von `installDist` scheitert unter
  Windows am zu langen Classpath).
- Starten: `java -jar bot/build/libs/bot-all.jar [anzahl] [--join] [--host host:port]` (Standard `127.0.0.1:25565`).
  Bots heißen `Bot1`, `Bot2`, …; `--join` führt 2 s nach dem Login `/ctb join` aus. Eingaben in der Konsole werden von allen
  Bots als Command ausgeführt (`ctb join`, `ctb quit`, …), `stop` beendet.
- Bots bewegen sich nicht, bestätigen Teleports, respawnen 1 s nach dem Tod, verbinden sich nach Verbindungsabbruch alle 5 s neu.
- Auf echtem Paper 26.2 getestet (Join, `/kill`, Respawn). Zweiter Klick bei Rechtsklick auf Blöcke → siehe SetupSession-Entprellung.

### Tests (`src/test/java`, JUnit 6 + MockBukkit für Paper 26.2)
- Von `PluginTestBase` erben: jeder Test bekommt einen frischen Mock-Server mit aktiviertem Plugin und eigenem Datenordner.
  - `addPlayer(name, permissions...)`, `addAdmin(name)` (hat `ctb.admin`), `addMapFolder(name)` (Welt-Ordner in `maps/`).
  - `execute(sender, "ctb arena create castle")` führt synchron aus; `messages(sender)` liefert Nachrichten ohne Farbcodes;
    `assertContains(...)`.
- Commands laufen auf `TestCommandManager` (reiner Cloud-Manager), da der Paper-Manager in CraftBukkit reflektiert
  (im echten `onEnable` gefangen → Plugin läuft auch ohne Commands weiter).
- **MockBukkit meldet nicht implementierte Methoden als übersprungene Tests** (nicht als Fehler)! `build.gradle.kts` lässt den
  `test`-Task deshalb fehlschlagen, sobald ein Test übersprungen wird. Nicht implementiert u. a.: `World#getWorldFolder`,
  `Server#getWorldContainer`, `World#save` → `GameWorld.getFolder(world)` (aus dem Key) und `GameWorld.setDimensionsFolder`
  (setzt `PluginTestBase` auf ein `@TempDir`). Welten laden (`WorldCreator`) klappt in MockBukkit.
- Die erste Mock-Welt heißt `world`, hat aber den Key `minecraft:overworld` (wie auf dem echten Server).
- Setup-Tools testen: `PlayerInteractEvent` mit dem Item aus dem Hotbar-Slot per `callEvent` auslösen (siehe `SetupTest`).
- Ganze Runden testen: von `GameTestBase` erben (Arena „castle“ fertig eingerichtet, Red1 + Blue1 in der Lobby,
  `startGame()`, `attack()`, `killAndRespawn()`, …) – so `GameFlowTest`, `GameFeaturesTest`, `RoundFeaturesTest`, `JoinSignTest`, `StatsTest`,
  `NetworkTest`. Stats laufen async auf SQLite: vor Assertions `getStatsManager().flush().join()` + ein Tick.
  Config im Test per `getConfigFile().set(...)` (auch Listen von Maps für `events`/`resources`). MockBukkit-Lücken:
  - Chunks gelten als nicht geladen → `block.getChunk().load()` (Schilder werden nur in geladenen Chunks aktualisiert)
  - Spieler haben kein `ATTACK_SPEED`-Attribut → `player.registerAttribute(...)`
  - `BLOCK_BREAK_SPEED` hat in MockBukkit keinen Default (`registerAttribute` wirft NPE) → per Reflection
    (`RoundFeaturesTest#registerBreakSpeed`); Bossbars: `getBossBars()`, Actionbars: `nextActionBar()`
  - Scoreboard-Fallback: Einträge max. 40 Zeichen (werden gekürzt)
  - `Location#getWorld()` kann „World unloaded“ werfen (Welten alter Mock-Server) → vorher `isWorldLoaded()`
  - Events mit Minuten-Bruchteilen testen (`minute: 0.1`); `startGame()` verbraucht schon ~2 s Spielzeit, Spielzeit läuft in Ticks
  - merkt sich den Angreifer nicht → `setKiller` vor `damage`
  - `PlayerMock#damage` wendet Schaden auch bei abgebrochenem Event an → Schutz-Tests lösen `EntityDamageByEntityEvent`
    selbst aus (`GameFlowTest#attack`)
  - Scoreboard ohne `numberFormat`/`customName` → `GameScoreboard` fällt auf normale Einträge zurück
  - `getAdvancementProgress` fehlt → `PlayerMock`-Unterklasse (`GameFlowTest#advancementPlayer`)

## Arena-Setup (Ablauf für Admins)
1. `/ctb arena create <name> --world <serverwelt>` (importiert eine Welt des Servers nach `maps/<name>`) – oder den
   Welt-Ordner selbst nach `plugins/CatchTheBeacon/maps/<name>` kopieren und `/ctb arena create <name>`. Formate:
   alte Welt (`level.dat` + `region/`), 26.x-Dimension (`region/`, `data/`…) oder ganzer 26.x-Save (`dimensions/minecraft/overworld`).
2. `/ctb arena <name> lobby setworld <map oder serverwelt>` (Serverwelten werden nach `maps/` importiert).
3. **Geführtes Setup** `/ctb arena <name> setup` (`game/setup/`): Spieler wird gesichert (Inventar etc.) und in Kopien
   von Lobby-/Arena-Welt teleportiert, Hotbar-Tools: Lobby-Spawn | Spawn Rot/Blau, Beacons Rot/Blau links/rechts
   (Beacon rechtsklicken) | Checkliste | Welt wechseln | Speichern & verlassen. Nächster Schritt glänzt + ist ausgewählt,
   Actionbar zeigt ihn. Verlassen (Smaragd, `/ctb arena <name> setup exit`, Quit, Teleport weg) speichert beide Welten.
4. `/ctb arena <name> check` / `/ctb arena list` zeigen, was fehlt. `/ctb arena <name> creategame` (`--force`: nur Lobby nötig) – merkt sich die Arena (`auto-game` in
   `arenas.yml`), sie bekommt nach jedem Neustart wieder ein Spiel; `removegame` stoppt die Spiele und löscht das Flag
   (nötig, um eine Arena mit Spiel zu bearbeiten).
5. Join-Schild: Schild mit `[ctb]` in Zeile 1 und dem Arena-Namen in Zeile 2 (Admin). Rechtsklick = beitreten bzw. zuschauen,
   Abbauen nur schleichend (Admin). Arenen mit Schild bekommen beim Serverstart automatisch ein Spiel.
Die Einzel-Commands (`edit`/`save`/`team … setspawn` …) gibt es weiterhin.

## Netzwerk (Velocity, ein Spiel pro Server) – `network.*` in config.yml, `network/NetworkManager`
- Spielserver: `network.mode: game`, Arena einrichten (Admin braucht dafür `ctb.network.bypass` oder richtet ein, bevor
  die Arena spielbar ist), optional `network.arena`, `network.lobby-server` = Name in `velocity.toml`.
  Spieler joinen automatisch, danach geht es zurück zur Lobby. Status als MOTD `CTB;LOBBY;3;8;castle` (`ServerStatus`).
- Lobby-Server: `network.mode: lobby`, `network.servers` (Name aus `velocity.toml` + host:port zum Pingen),
  Schilder `[ctb]` + Servername, `/ctb join` / `/ctb spectate` schicken per `BungeeCord`-Kanal („Connect“) weiter.
- Alle Server: `sql.enabled: true` mit derselben Datenbank → gemeinsame Stats.
- Testen: `ProxiedPlayerMock` (alle Test-Spieler) merkt sich die Ziel-Server, `connects(player)` in `PluginTestBase`;
  Ping-Protokoll gegen einen Fake-Server in `NetworkTest`.

## Welten (Paper 26.x!)
- Paper 26 speichert **jede** Welt als Dimension: `world/dimensions/minecraft/<key>/`. `World#getWorldFolder()` zeigt dorthin.
- Ordner neben `world/` werden von `WorldCreator` **ignoriert** (neue Welt wird generiert) bzw. mit `level.dat` einmalig
  dorthin **verschoben** (empirisch getestet 2026-09-28, siehe decisions.md).
- `GameWorld` kopiert die Map deshalb direkt nach `dimensions/minecraft/ctb_<id>` und lädt per
  `new WorldCreator(NamespacedKey.minecraft(name))`; `close(true)` kopiert zurück in den Map-Ordner, danach wird die Kopie gelöscht.
- `data/paper/metadata.dat` = Welt-UUID (wie früher `uid.dat`) → wird beim Kopieren gelöscht, sonst lehnt Paper Kopien
  geladener Welten als „duplicate of another world“ ab.
- Weltnamen werden lowercase (Keys). Übrig gebliebene `ctb_*`-Dimensionen löscht `GameWorld.deleteLeftovers()` beim Start.
- Prüfen per Konsole: `execute in minecraft:ctb_<id>_lobby run forceload add 0 0`, dann
  `execute in minecraft:ctb_<id>_lobby if block X Y Z minecraft:beacon run say OK` (ohne forceload schlägt der Test still fehl).

## Commands (alle unter `/ctb`, Aliase `catchthebeacon`, `catchbeacon`, `ctbeacon`)
| Command | Permission |
|---|---|
| `ctb`, `ctb help [query]`, `ctb join`, `ctb quit`, `ctb spectate`, `ctb stats [player]`, `ctb achievements [player]` | – |
| `ctb start` | `ctb.vip` |
| `ctb tp`, `ctb debug`, `ctb debug info [player]`, `ctb debug setstate <state>`, `ctb debug sentry` | `ctb.admin` |
| `ctb arena create <name> [--world <world>]`, `ctb arena list` | `ctb.admin` |
| `ctb arena <arena> setup` / `setup exit` / `check` / `edit` / `save` / `creategame [--force]` / `removegame` | `ctb.admin` |
| `ctb arena <arena> team <red\|blue> setspawn` / `setbeacon <left\|right>` | `ctb.admin` |
| `ctb arena <arena> lobby setworld <map\|world>` / `edit` / `save` / `setspawn` | `ctb.admin` |
| `ctb config items set` / `get --confirm` | `ctb.admin` |
| `ctb game` (GUI `GameMenu`), `ctb game list`, `ctb game <id> start` / `stop [--remove]` / `spectate` (`CommandGame`) | `ctb.admin` |
| `ctb npc link <arena\|server>` / `unlink` (danach NPC anklicken) / `list` | `ctb.admin` |
| (kein Command) nicht automatisch ins Spiel auf einem Spielserver | `ctb.network.bypass` (default: false) |

Struktur: alles zu einer Arena unter `ctb arena <arena> …` (Cloud erlaubt Literale `create`/`list` neben dem
`<arena>`-Argument; diese Namen sind als Arena-Namen gesperrt). Neue Commands: Methode mit `@Command(ROOT + " ...")` in `CommandCatchTheBeacon` bzw. `CommandArena`; eigene Argument-Typen
als Parser in `commands/arguments` und in `CommandManager` per `TypeToken` registrieren. Test in `src/test/.../commands` ergänzen.

## Architektur (`src/main/java/com/christian34/catchthebeacon`)
- `CatchTheBeacon` – Main-Klasse, Singleton (`getInstance()`), hält Manager.
- `Telemetry` – bStats (ID 34381) + Sentry (opt-out `sentry.enabled`, minimiert, DSN in `SENTRY_DSN`), siehe `PRIVACY.md`.
  Neue bStats-Charts auch auf bstats.org anlegen und in `PRIVACY.md` nennen; Rundenzähler über `roundPlayed`/`votesCast`.
  `-Dctb.sentry.debug=true` zeigt, was Sentry sendet, `/ctb debug sentry` schickt einen Testbericht.
  `-Dctb.offline=true` schaltet beides ab (in den Test-Tasks gesetzt). Geshadet + relocated wie die anderen Libs.
- `commands/` – `CommandManager` (Cloud-Setup), `CommandCatchTheBeacon`, `CommandArena`, `arguments/` (Arena-, Team-Parser).
- `game/` – `Game` (Spieler, Zuschauer, Rejoin, platzierte Blöcke, Sieger-Prüfung `checkForWinner`), `GameManager`
  (`join`: wartendes Spiel, sonst zuschauen), `Team` (Enum, Werte aus `teams.yml`), `Beacon` (inkl. Falle), `Countdown`,
  `GameItems` (Kit, Extra-Herzen, Kampfsystem), `GameEvent` (Zeitereignisse aus `events`), `ResourceBlocks`
  (Belohnungen aus `resources`), `SignManager` (Join-Schilder, `signs.yml`, legt beim Start Spiele für Arenen mit Schild an;
  im Lobby-Modus zeigen Schilder Spielserver).
- `database/Database` – SQLite (`database.db`) oder MySQL/MariaDB (`sql.*`, gleiche Keys wie EasyPrefix, Präfix + `_`),
  Treiber liefert Paper; alle Queries
  nacheinander auf einem eigenen Thread (`query`/`update` → `CompletableFuture`). Spielerdaten gehören immer dorthin.
- `stats/` – `StatsManager` (Tabelle `ctb_stats`, Cache für Online-Spieler, `load`/`find` async), `StatsExpansion` (PlaceholderAPI `%ctb_kills%` …, nur geladen wenn installiert;
  PAPI ist `compileOnly` + `softdepend`).
- Ingame-Mechaniken im Sekundentakt: `IngameState#tick` (Alarm, Abbaulähmung, Regeneration, Falle, Anti-Camping,
  Bossbar-Timeout, Beacon-Kompass, Tipps, Events). `listeners/RoundListener`: Abbau-Bossbar, Regeln der Varianten.
- Runde: `Variant` (Abstimmung in der Lobby, `Game#chooseVariant`), `RoundSummary` (Fazit), `stats/AchievementManager`.
- `listeners/SpectatorListener` (Kompass-Menü, kein Schaden/Interagieren); Zuschauer werden außerdem in
  `EventListener#cancel` blockiert. `QuitListener`: Disconnect → Rejoin-Fenster, Join → zurück ins Team.
- `integrations/` – optionale Fremd-Plugins (`compileOnly` + `softdepend`, Fehler abgefangen): `PartySupport` mit
  `PartyProvider` für Parties und Party and Friends (`parties.*`: gemeinsam joinen, gleiches Team). Tests registrieren
  einen Fake-Provider (`PartyTest`). `NpcSupport` (Citizens/FancyNpcs, `/ctb npc link` + Klick, `npcs.yml`),
  `BedrockForms` (Floodgate: `lib/Menu` zeigt Formulare statt Truhen-Menüs – neue Menüs immer über `Menu`),
  `CtbMiniPlaceholders` + `Placeholders` (auch für PAPI). Tests mit Fakes: `IntegrationsTest`
  (`npcs().addPlugin(...)`, `plugin.setBedrockForms(...)`). Neue Integration → Plugin-Name in `plugin.yml`
  `softdepend`; Fremd-API-Klassen nur in eigenen Klassen, die erst bei installiertem Plugin geladen werden.
  Transitive Abhängigkeiten prüfen (Floodgate brachte ein altes Gson mit → `isTransitive = false`).
- `network/` – `NetworkManager` (Modus, Weiterleiten, Auto-Join, MOTD, Lobby-Ping), `ServerPinger` (Server List Ping),
  `ServerStatus` (MOTD-Format).
- `game/states/` – State-Machine: Pending → Lobby → Ingame → Ending → Restart (`GameStateManager`). Ending: Sieger-Titel,
  Feuerwerk, nach `game.ending-seconds` (`EndingState.getDuration()`) zurück + [Play again]; Restart legt immer die nächste Runde der Arena an.
- `game/setup/` – geführtes Setup: `SetupManager` (Sessions + Listener), `SetupSession`, `SetupStep`, `SetupItem` (per PDC erkannt).
- `game/map/` – `MapHandler` (inkl. Welt-Import)/`Arena`/`LobbyMap`/`MapTemplate`; `GameWorld` kopiert Welten aus `maps/` nach `ctb_<name>` und lädt sie.
- `files/` – YAML-Dateien im Plugin-Datenordner (`PluginFile`-Basisklasse mit Daten-Präfix, z. B. `arenas.`),
  `ConfigUpdater` ergänzt fehlende Keys aus den Jar-Defaults (Kommentare bleiben erhalten).
- `lib/GameScoreboard` – Sidebar pro Spieler (fixe Einträge + `Score#customName` → flackerfrei), Team-Farben für Namensschilder.
- `lib/lang/` – i18n über `messages.yml` (MiniMessage, alte `&`-Codes werden umgewandelt): `I.i18n(LangText.X, args...)`
  → `Component`, Platzhalter `{0}`, `{1}`, …; Argumente: `Component` behält Stil, alles andere = reiner Text.
  `I.prefixed(...)` (mit Prefix), `I.button(...)`, `I.text(configString)`, `I.item(...)` (nicht kursiv),
  `I.legacy(...)` nur für InventoryGui. `GamePlayer#sendMessage(LangText, args...)` sendet mit Prefix.
- `user/` – `GamePlayer` (Wrapper um Player), `UserStorage` (Snapshot des ganzen Spielerzustands, zusätzlich als
  `backups/<uuid>.yml` → Wiederherstellung nach Absturz beim Join; nicht im Modus `game`), `TestPlayer` (Fake-Player
  per Proxy, nur Debug). Spiel darf das echte Spiel nicht beeinflussen: Statistiken/Rezepte/Erfolge/Endertruhe sind
  in `ctb_*`-Welten gesperrt.

## Konventionen / Fallstricke
- Neue Nachricht: Key in `messages.yml` **und** `LangText` (Test `ResourceFilesTest` prüft das).
- `PluginFile#set(key)` und die Getter arbeiten **relativ zum Daten-Präfix**; `getData().set(...)` ist absolut.
- Welten des Plugins beginnen mit `ctb_` und werden beim Start gelöscht.
- Texte nur über Adventure (`Component`), keine `§`-Strings/`ChatColor`/String-`setDisplayName` mehr (Build hat 0
  Deprecation-Warnungen – so lassen). Spielertexte gehören in `messages.yml`, nur `/ctb debug`-Ausgaben im Code.
- Command-Beschreibungen: `@CommandDescription("command_xyz")` = Key in `messages.yml` (Test prüft jeden Command).
- Fehler eigener Argument-Parser: `new MessageException(LangText.X, args)` (wird im Exception-Handler angezeigt).


## Doku-Pflege
Bei jeder Session: Änderungen in [changelog.md](changelog.md), Entscheidungen in [decisions.md](decisions.md),
offene Punkte in [todo.md](todo.md).
