# Changelog (Claude-Sessions)

## 2026-10-03 – Release-Check, To-dos aufgeräumt
- Build grün: 667 Tests, 0 übersprungen.
- Tablist-Punkt aus todo.md entfernt: wird in EasyPrefix gelöst (`display.excluded-worlds`, z. B. `ctb_*`).
- „Could not save data to chunk_tickets.dat“ beim Import der laufenden Welt: ist schon behoben, weil
  `FileManager.SKIPPED_FILES` `chunk_tickets.dat`/`raids.dat` überspringt (Test `importSkipsTheFilesOfTheRunningWorld`).
  Auf einem Wegwerf-Server (Paper 26.2-129) nachgeprüft: 9 Importe von `world`, davon 6 bei ~770 erzwungenen Chunks
  und laufendem `forceload`, keine Meldung. Punkt aus todo.md entfernt.

## 2026-09-29 – Game-Manager, Runde startete doppelt
- **Game-Manager** (`commands/CommandGame`, `ctb.admin`): `/ctb game list` (ID, Arena, Zustand, Spieler/Max,
  Zuschauer, Rundenzeit + Buttons [Start]/[Watch]/[Stop]), `/ctb game <id> start` (Force-Start aus der Lobby, auch
  von der Konsole), `/ctb game <id> stop` (ohne Sieger/Stats, alle zurück – Spielserver: zur Lobby –, Arena bekommt
  sofort eine neue Runde), `stop --remove` (keine neue Runde bis zum Neustart; `auto-game` bleibt),
  `/ctb game <id> spectate`. `GameArgument` (Parser + ID-Vorschläge), `GameManager#stopGame`/`getGame(String)`.
- **Bug: Lobby-Countdown lief weiter:** `LobbyState#stop` hat den Countdown nie beendet. Nach Lobby → Pending (alle
  weg) → Lobby liefen zwei Countdowns, der alte startete die laufende Runde später **ein zweites Mal** (erneut
  teleportiert + Kit, zweiter Ingame-Task). Neu: `State#cancel` (Tasks beenden ohne Zustandswechsel), in
  `LobbyState#stop` und `Game#stop` (vorher tickten z. B. Ingame-Tasks gestoppter Spiele weiter).
- Tests: `CommandGameTest` (8), `GameFlowTest#oldLobbyCountdownDoesntStartTheRoundAgain` (ohne Fix rot geprüft).
- **GUI** (`commands/GameMenu`, über `lib/Menu` → Truhe für Java, Formular für Bedrock): `/ctb game` öffnet die
  Übersicht (Symbol je Zustand: grau Pending, grün Lobby, Beacon Ingame, Rakete Ending; Beschreibung mit Zustand,
  Spielern/Max, Zuschauern, Rundenzeit, bis zu 8 Spielernamen), Klick → Spielmenü (Starten nur in der Lobby,
  Zuschauen nur laufend, Stoppen, Zurück), Stoppen → Nachfrage „neue Runde“ / „entfernen“, danach wieder die
  Übersicht. Jede Aktion prüft, ob das Spiel noch existiert (altes Menü). Konsole: `/ctb game` = Liste.
  Aktionen als `CommandGame#start`/`stop`/`spectate` von Befehlen und Menü gemeinsam genutzt. +4 Tests (667 grün).
- **Untersucht, nicht gelöst:** Bots schwebten mitten in der Runde mit Kit in der Overworld/Void (Server des Users,
  20:46–20:54). Wegwerf-Server mit gleicher Map + allen Plugins des Users: nicht reproduzierbar → [todo.md](todo.md).

## 2026-09-29 – Join-NPCs (Citizens, FancyNpcs), Bedrock-Formulare (Floodgate), MiniPlaceholders
- **Join-NPCs** (`integrations/NpcSupport`, `CitizensNpcs`, `FancyNpcsNpcs`): vorhandenes NPC verknüpfen mit
  `/ctb npc link <arena|server>` + Rechtsklick aufs NPC (30 s), `/ctb npc unlink` + Klick, `/ctb npc list`.
  Klick eines Spielers = wie Join-Schild (beitreten/zuschauen, Lobby-Server: Spielserver). Gespeichert in `npcs.yml`
  (`citizens:<uuid>`, `fancynpcs:<id>`), Arenen verknüpfter NPCs bekommen beim Start ein Spiel. Bei FancyNpcs werden
  die eigenen Aktionen verknüpfter NPCs abgebrochen. `SignManager#resolveTarget`/`join`/`getStatus`/`stateText`
  dafür aus der Schild-Logik herausgezogen.
- **Bedrock-Formulare** (`lib/Menu`, `integrations/BedrockForms`, `FloodgateForms`): neues `Menu` (Buttons mit Icon,
  Name, Bedrock-Zusatzzeile, Lore, Aktion) → Java: InventoryGui wie bisher, Floodgate-Spieler: `SimpleForm`
  (Antwort kommt vom Netzwerk-Thread → Aktion per `runTask`). Team-, Abstimmungs- und Zuschauer-Menü umgestellt.
  Floodgate-API **nicht transitiv**: `geyser-common` enthält ein altes Gson, das Papers Gson verdeckte
  (`JsonParser.parseString` fehlte → Compile-Fehler in `ServerPinger`).
- **MiniPlaceholders** (`integrations/CtbMiniPlaceholders`, API 3.2.0): `<ctb_kills>` … `<ctb_achievements>`,
  `<ctb_team>`, `<ctb_arena>`, `<ctb_top:wins:1:name>`, `<ctb_players:castle>`, `<ctb_maxplayers:castle>`,
  `<ctb_state:castle>`. **PAPI** ergänzt: `%ctb_team%`, `%ctb_arena%`, `%ctb_players_<arena>%`,
  `%ctb_maxplayers_<arena>%`, `%ctb_state_<arena>%` (gemeinsame Logik `integrations/Placeholders`) – z. B. für
  NPC-Namen/Hologramme.
- `softdepend` um MiniPlaceholders, Citizens, FancyNpcs, floodgate ergänzt.
- Tests: `IntegrationsTest` (11, Fake-Plugins), `MenuTest` (Layout; InventoryGui selbst läuft in MockBukkit nicht –
  `Sound` ist ein Interface). 623 Tests grün. **Nicht mit den echten Plugins getestet.**

## 2026-09-29 – Bugfixes Kompass-Teleport, Beacon-Effekte
- **Kompass teleportierte im Spiel:** Ursache war die Navigations-Wand von **WorldEdit** (`navigation-wand.item:
  minecraft:compass`, Linksklick = `/jumpto`, Rechtsklick = `/thru`, für OPs). `RoundListener#onCompass` setzt für
  Spieler in einem Spiel bei Kompassen `useItemInHand = DENY` (Priorität LOW) – WorldEdit ignoriert solche Klicks
  (per `javap` im WorldEdit-7.4.5-Jar geprüft). Zuschauer waren schon geschützt (Event wird abgebrochen).
- **Beacon abgebaut ohne Effekte:** `PlayerEventListener#playDestroyEffects`: Block-Partikel + Explosionspartikel,
  Feuerwerk in Teamfarbe (Power 1, explodiert hoch genug, um niemanden zu verletzen), Titel für alle im Spiel
  (`beacon_destroyed_title`/`_subtitle`), Enderdrachen-Brüllen fürs betroffene Team, Level-up-Sound für die anderen.
  `World#strikeLightningEffect` wäre schöner, ist in MockBukkit aber nicht implementiert (→ übersprungene Tests).
- Tests: `GameFeaturesTest#destroyedBeaconHasEffects`, `#compassIsNoToolOfOtherPlugins`.
- **Advancements anderer Spieler im Spiel sichtbar:** Vanilla-Meldungen („X has made the advancement …“) gingen an
  alle Online-Spieler. `PlayerEventListener#onAdvancementDone` nimmt die Meldung aus dem Event und schickt sie nur an
  Spieler ohne Spiel (+ Konsole). Test `GameFlowTest#playersInTheGameDontSeeAdvancementsOfOthers`.

## 2026-09-29 – Party-Plugins (Parties, Party and Friends)
- Neues Paket `integrations/`: `PartyProvider` (Schnittstelle), `PartiesProvider` (AlessioDP Parties, `parties-api`
  3.2.18), `PartyAndFriendsProvider` (Simonsator, über „Spigot Party API for Party and Friends“ = Plugin
  `Spigot-Party-API-PAF` 1.0.7 + `FriendsAPIForPartyAndFriends` 1.6.2), `PartySupport` (erkennt installierte Plugins,
  fängt Fehler/`LinkageError` inkompatibler Versionen ab). Alles `compileOnly` + `softdepend`, nicht geshadet.
- **Gemeinsam beitreten** (`parties.join-together`): `/ctb join` oder Join-Schild des Party-Leiters nimmt die
  Mitglieder auf diesem Server mit (Spiel mit Platz für alle bevorzugt; Zuschauen ebenso). Mitglieder allein joinen
  normal. Nachricht `joined_with_party`.
- **Gleiches Team** (`parties.same-team`): `LobbyState#assignTeams` verteilt Parties zuerst (größte zuerst) ins Team,
  das ein Mitglied gewählt hat, sonst ins kleinere – aber nie über die Hälfte der Spieler (große Parties werden
  geteilt). Einzelspieler wie bisher (inkl. `game.balance-teams`).
- **Netzwerk:** Lobby-Server schickt Party-Leiter auf einen Spielserver mit Platz für die ganze Party; das Nachziehen
  der Mitglieder übernehmen die Proxy-Plugins.
- Tests: `PartyTest` (Fake-Provider, 10 Tests). Nicht mit den echten Plugins getestet (nicht auf dem Testserver).

## 2026-09-29 (23) – Spielerzustand absturzsicher, Spiel ohne Auswirkung aufs echte Spiel
- **`UserStorage` neu:** Snapshot des gesamten Spielerzustands (Inventar inkl. Rüstung/Offhand, Endertruhe, Position,
  Spawnpunkt, letzter Todesort, Kompassziel, Spielmodus, Flug, Lauf-/Fluggeschwindigkeit, **alle** Attribut-Basiswerte,
  Leben, Absorption, Hunger/Sättigung/Erschöpfung, Feuer/Frost/Luft/Fallhöhe, Level/XP/Gesamt-XP, Effekte, Anzeige-/
  Tablistname). Wird zusätzlich **synchron** nach `backups/<uuid>.yml` geschrieben (Items als Paper-Bytes, Base64) und
  beim Zurückgeben gelöscht. **Nach einem Absturz** stellt `QuitListener#onJoinAfterCrash` den Stand beim nächsten Join
  wieder her (Nachricht `backup_restored`). Nicht im Netzwerk-Modus `game` (Server mit nur einer Arena).
  Schutz: zweites `store()` behält den ersten (echten) Stand, ein nicht wiederhergestelltes Backup wird nie
  überschrieben (umbenannt in `<uuid>-<zeit>.yml`), fehlgeschlagenes Wiederherstellen lässt die Datei liegen,
  Attribute ohne Snapshot-Wert → Standardwert, Locations in entladenen Welten werden übersprungen (vorher
  „World unloaded“-Exception möglich).
- **Keine Auswirkung aufs echte Spiel:** Vanilla-Statistiken (`PlayerStatisticIncrementEvent`) und Rezepte
  (`PlayerRecipeDiscoverEvent`) werden in `ctb_*`-Welten abgebrochen (Erfolge waren es schon), **Endertruhe** lässt sich
  dort nicht öffnen (sonst echte Items rein/raus).
- **Bug behoben (Inventarverlust):** `TeleportListener` stellte 1 s nach *jedem* Teleport in eine normale Welt wieder
  her – wer z. B. `/spawn` machte und sofort per Schild joinete, bekam sein Inventar in der Lobby zurück, beim Start
  wurde es durchs Kit ersetzt, das Backup war verbraucht. Jetzt zählt die aktuelle Welt; wer aus dem Spiel
  wegteleportiert wird, verlässt es ordentlich (`game.quit`).
- `GameTestBase` prüft, dass die Joins im Setup klappen (vorher still ignoriert → irreführende Folgefehler).
- Tests: `UserStorageTest` (alles zurück nach dem Spiel, Backup-Datei nur im Spiel, Absturz-Wiederherstellung, alte
  Backups bleiben, zweites store, Spielserver ohne Datei, keine Statistiken, keine Endertruhe). 561 Tests grün.
- Echter Absturztest (Wegwerf-Server + Bot): 12 Diamanten + 30 Level → `ctb join` → Prozess gekillt → Neustart →
  Bot joint → Diamanten + Level zurück, Datei weg, Nachricht angezeigt.

## 2026-09-29 (22) – Runden-Features getestet, Spiele nach Neustart, Beacons mit Welt, Ende-Dauer
- **`RoundFeaturesTest`** (neu, 46 Test-Methoden): Abbau-Bossbar (alle sehen sie, eigener Beacon/abgeschaltet → keine,
  verschwindet nach Timeout, beim Zerstören und Tod des Abbauers), Beacon-Kompass (Kit, nächster gegnerischer bzw.
  angegriffener eigener Beacon, Actionbar mit Entfernung, Ziel wird wiederhergestellt, nicht in den Drops, abschaltbar),
  Rundenfazit (MVP, meiste Kills, Beacons, Ressourcen, Beacons zählen mehr als Kills, abschaltbar), alle 8 Erfolge
  inkl. Gegenfälle (nur einmal, kein Flawless/Quick bei Aufgabe, kein Solo mit Hilfe, Veteran mit 10. Sieg),
  `/ctb achievements`, Tipps (neue Spieler, Beacon-Tipp, erfahrene/abgeschaltet → keine), Teams nach Stärke,
  Varianten (Mehrheit, Gleichstand, keine Stimmen, Vote-Item, deaktivierte Varianten, Rush inkl. Attribut und Events,
  Nur Bogen, Ein Treffer nur gegen Gegner, Kein Eisen). Gegenprobe: Tests schlagen fehl, wenn `balance-teams` aus
  ist bzw. der Kompass gedroppt wird. MockBukkit kennt `BLOCK_BREAK_SPEED` nicht (kein Default) → per Reflection gesetzt.
- **Beacon-Positionen haben jetzt die Spielwelt** (`Game#prepareGameWorld`). `WorldUtils.compareLocation` →
  `isSameBlock` (vergleicht Welt + Blockkoordinaten), `distance` ist bei verschiedenen Welten unendlich. Der Kompass
  braucht keinen `setWorld`-Umweg mehr. Test: gleiche Koordinaten in einer anderen Welt sind kein Beacon.
- **Spiele ohne Join-Schild nach Neustart:** `creategame` setzt `auto-game: true` in `arenas.yml` (nicht mit `--force`),
  `GameManager#createAutoGames` legt beim Start die Spiele an (nicht auf dem Lobby-Server). Neuer Command
  `/ctb arena <arena> removegame`: stoppt die Spiele der Arena (Spieler zurück, keine neue Runde), löscht das Flag →
  Arena wieder bearbeitbar (`map_in_use` nennt den Command). **`games.yml` entfernt**: wird beim Start einmalig
  übernommen (Arenen bekommen `auto-game`) und gelöscht. Auf dem Wegwerf-Server geprüft (creategame → Neustart →
  Spiel da → removegame → Games: 0, Flag weg).
- **`game.ending-seconds`** (Standard 15, mindestens 3): Dauer nach Rundenende bis zur Rückkehr
  (`EndingState.getDuration()` statt `DURATION`).
- 552 Tests grün (0 übersprungen), Jar deployt (`update/`).

## 2026-09-29 (21) – Adventure/MiniMessage, AsyncChatEvent, alle Texte in messages.yml
- **Text-API** `lib/lang/I`: `i18n(LangText, Object...)` liefert jetzt `Component` (MiniMessage). Alte `&`/`§`-Codes
  (bestehende `messages.yml`/`config.yml` der Server) werden automatisch umgewandelt (`legacyToMiniMessage`, Farbe setzt
  Formatierung zurück wie früher). Argumente: `ComponentLike` behält den Stil, alles andere wird **reiner Text**
  (Spielernamen/Chat können nichts formatieren). Neu: `prefixed`, `button`, `text` (Config-Texte), `lines`, `item`
  (nicht kursiv), `legacy`/`plain` (für InventoryGui bzw. Logs). `GamePlayer#sendMessage(Component | LangText, args…)`.
- Standard-`messages.yml`, `prefix` und `tablist` in `config.yml` auf MiniMessage umgestellt (Kommentar oben erklärt
  das Format); `teams.yml` `chat_color` akzeptiert jetzt auch Namen (`red`) und Hex (`#ff0000`).
- Entfernt/ersetzt: `ChatUtils`, `ChatColor` (bukkit + bungee), `setDisplayName`/`getDisplayName`, `setPlayerListName`,
  `setPlayerListHeaderFooter(String…)` → `sendPlayerListHeaderAndFooter`, `setDeathMessage` → `deathMessage`, Legacy-
  Bossbar-/Titel-/Actionbar-Umwege. `CatchTheBeacon.PREFIX` ist ein `Component`. **0 Deprecation-Warnungen** im Hauptcode
  (vorher 26).
- `Team`: `getDisplayName()` → `Component`, `getChatColor()` → `getColor()` (`TextColor`). Ebenfalls `Component`:
  Namen/Beschreibungen von `Variant`, `Achievement`, `GameEvent`, `Beacon.Position`, `SetupStep`, `Arena#getDisplayName`
  (display-name darf formatiert sein), `Arena#getMissingSetup`, `RoundSummary#getLines`.
- **Chat:** `AsyncPlayerChatEvent` → Paper `AsyncChatEvent` (Nachricht bleibt Component, Shout-Präfix über Plain-Text).
- **Alle Command-, Setup-, Schild-, Scoreboard-Texte** in `messages.yml` (+134 Keys, `LangText` entsprechend). Nur die
  Debug-Ausgaben (`/ctb debug …`) bleiben im Code. Buttons (`button_*`) einzeln anpassbar. Schildzeilen `sign_title`/
  `sign_name`/`sign_players`, Sidebar `scoreboard_*` (erledigt damit den Todo-Punkt „Scoreboard-Texte“).
- **Hilfe:** Texte des Hilfe-Menüs (`help_*`) per `messageProvider`, Command-Beschreibungen sind Keys
  (`@CommandDescription("command_join")` → `descriptionDecorator`). `/ctb help <query>` ist jetzt greedy (vorher fand
  `/ctb help ctb arena` nichts, weil nur ein Wort erlaubt war).
- Fehlermeldungen von Argumenten: `commands/arguments/MessageException` bzw. `MapInUseException` implementieren
  `ComponentMessageThrowable`, der Exception-Handler zeigt deren Text. Keine-Rechte-Meldung hat jetzt den Prefix.
- Tests: Chat-Tests auf `AsyncChatEvent`, neue Tests (alte Codes, Argumente nicht geparst, Component-Argumente, fehlende
  Argumente, jede Command-Beschreibung existiert, Hilfe zeigt Beschreibung). 490 Tests grün. Wegwerf-Server (Paper 26.2)
  startet fehlerfrei, `/ctb`, `/ctb help`, `/ctb arena list`, Fehlermeldungen geprüft. Jar deployt (`update/`).

## 2026-09-29 (20) – Maven-Reste entfernt
- IntelliJ als Gradle-Projekt verlinkt (User, `settings.gradle.kts`); gelöscht: `EasyPrefix.iml`, altes Jar-Artifact
  (`.idea/artifacts`, Ziel 1.16-Server), Maven-Einträge in `.idea/misc.xml`/`compiler.xml`.
- `.github/dependabot.yml` auf `gradle` umgestellt (`.github/` ist im Repo, nicht gitignored).
- User hat erledigt (2026-09-29): Sentry-Einstellungen (IP-Speicherung aus, Data Scrubber, Tokens widerrufen, doppeltes
  Projekt gelöscht), 12 bStats-Charts angelegt, `PRIVACY.md` in die Plugin-Beschreibung, Ingame-Tests (Runden-Features,
  Spielablauf, Netzwerk mit Velocity), Projekt-SDK JDK 25.

## 2026-09-29 (19) – Weitere bStats-Charts
- Simple Pie `team_balancing`, `achievements`; Single Line `rounds_played` (beendete Runden seit dem letzten Senden,
  bStats sendet alle 30 min); Advanced Pie `variants_played`, `players_per_round` (2, 3-4, 5-8, 9-16, 17+), `variant_votes`
  (alle Stimmen am Ende der Lobby aus `LobbyState#stop` – zeigt die beliebteste Variante, nicht nur die Gewinner).
  Gezählt in `Telemetry#roundPlayed` (aus `EndingState#start`), Zähler werden beim Senden zurückgesetzt (`drain`).
- `TelemetryTest` +1, Build grün, Jar deployt.

## 2026-09-29 (18) – bStats + Sentry (`Telemetry`)
- **bStats** 3.2.1 (ID 34381 vom User), relocated; Charts: `network_mode`, `database`, `combat`, `variant_voting`,
  `arenas`, `placeholderapi` (müssen auf bstats.org noch als Custom Charts angelegt werden).
- **Sentry** 8.58.0, relocated, **opt-out** (`sentry.enabled: true`), DSN noch Platzhalter (`Telemetry.SENTRY_DSN`
  leer → nichts wird gesendet). Minimiert: nur Exceptions mit CTB-Code im Stacktrace (nicht nur `libs.`), keine
  Breadcrumbs/User/Servername, Spielernamen + UUIDs in Meldungen → `<player>`/`<uuid>`, jeder Fehler 1× pro Start,
  max. 25; kein globaler Uncaught-Handler (keine Fehler anderer Plugins). Erfasst über JUL-Handler an Server- und
  Plugin-Logger. Hinweis in der Konsole beim Start.
- `-Dctb.offline=true` (in `test`/`mysqlTest` gesetzt): weder bStats noch Sentry.
- `PRIVACY.md` (Offenlegung für die Plugin-Seite). `TelemetryTest` (5). 343 Tests grün, Wegwerf-Server startet fehlerfrei.
- DSN eingetragen (Projekt `catchthebeacon`, Org `itsthechris07`, **US-Region** – die Org ist älter, Region nur bei neuen
  Orgs wählbar; User wählte US). `PRIVACY.md`: Speicherung in den USA, Sentry ist unter dem EU-U.S. Data Privacy
  Framework zertifiziert (laut sentry.io/privacy). Neu: `/ctb debug sentry` (Admin) schickt einen Testbericht –
  vom Wegwerf-Server gesendet. `-Dctb.sentry.debug=true` zeigt Payload + Antwort: „Envelope sent successfully“,
  Payload ohne server_name/user/IP (nur Stacktrace, Tags server/java/network_mode, Release, Modul-Version).

## 2026-09-29 (17) – Runden-Features (Ideenliste des Users, alle konfigurierbar)
- **Bossbar beim Beacon-Abbau** (`base.mining-bossbar`): `BlockBreakProgressUpdateEvent` (Paper) → `Beacon#updateMining`,
  alle im Spiel sehen „The left beacon of Red is being mined: 60%“, Sound beim Start; weg nach 3 s ohne Fortschritt,
  beim Tod des Abbauers, beim Zerstören, am Rundenende (`RoundListener`, `IngameState#checkMining`).
- **Beacon-Kompass** im Kit (`game.beacon-compass`): `setCompassTarget` jede Sekunde → eigener Beacon, der abgebaut wird,
  sonst nächster gegnerischer; in der Hand Entfernung in der Actionbar. `UserStorage` sichert das Kompassziel.
- **Rundenfazit** (`game.round-summary`, `RoundSummary`): Dauer, Variante, MVP (Kills + 3 pro Beacon), meiste Kills,
  zerstörte Beacons mit Zeit, abgebaute Ressourcen pro Team.
- **Erfolge** (`achievements.*`, `stats/Achievement`, `AchievementManager`, Tabelle `ctb_achievements`): 8 Stück,
  nur einmal gemeldet (auch netzwerkweit: `INSERT IGNORE`), `/ctb achievements [player]`, `%ctb_achievements%`.
  Flawless/Quick Win nur, wenn alle gegnerischen Beacons zerstört sind (nicht bei Aufgabe).
- **Tipps** (`tips.rounds: 3`): Actionbar-Tipps in den ersten Runden (Rundenzahl aus dem Stats-Cache).
- **Teams nach Stärke** (`game.balance-teams`): Random-Spieler nach Siegquote (Wins+1)/(Games+2) verteilt.
- **Rundenvarianten** (`variants.voting`/`list`, `game/Variant`): Abstimm-Item (Papier, Slot 7) in der Lobby,
  Rush (`BLOCK_BREAK_SPEED` 2, Events ×0,5), Nur Bogen, Kein Eisen, Ein Treffer; Anzeige im Scoreboard.
- Jar gebaut + deployt, 338 bestehende Tests grün (Fix: MockBukkit-Drops enthalten `null`). **Keine eigenen Tests für die
  neuen Features** (auf Wunsch des Users als To-do in todo.md), ingame ungetestet.

## 2026-09-28 (16) – MySQL wie in EasyPrefix, gegen echten Server getestet
- Config `database.*` (aus (15), nie released) → **`sql.*`** mit den Keys von EasyPrefix (`enabled`, `host`, `port`,
  `database`, `username`, `password`, `table-prefix`; `_` wird angehängt → `ctb_stats`). Die alte CTB-Config hatte
  schon eine `sql:`-Sektion mit denselben Keys (wird jetzt wieder benutzt). Feste JDBC-Parameter: `useSSL=false`,
  `allowPublicKeyRetrieval=true` (MySQL 8), `connectTimeout=5000`. Ungültiges Präfix → Stats aus.
- Log beim Start: „Player data is saved in MySQL (table ctb_stats)“ bzw. SQLite.
- `MySqlTest` (Tag `mysql`, Task `mysqlTest`, Zugang aus `gradle.properties`) gegen den MySQL 8.4 des Users (`pi5`, DB
  `Test`): 5/5 grün, Tabellen danach gelöscht. `DatabaseTest` (Präfix). 288 Tests im normalen Build.
- Echtes Paper 26.2 (Wegwerf-Server) mit `pi5`: verbunden (≈ 3 s beim Start), `ctb_stats` angelegt, `/ctb stats` fragt ab.
- Testserver des Users: `plugins/CatchTheBeacon/config.yml` → `sql.enabled: true`, pi5/Test/test (Backup im Scratchpad);
  greift mit dem neuen Jar beim nächsten Neustart.

## 2026-09-28 (15) – Netzwerk-Modus (ein Spiel pro Server) + Spielerdaten in der Datenbank
- **`network.mode`**: `standalone` (wie bisher) | `game` | `lobby` (`network/NetworkManager`), für Velocity (BungeeCord-Kanal).
  - `game`: Server hostet das Spiel von `network.arena` (leer: erste spielbare Arena), legt es beim Start an. Wer den
    Server betritt, joint automatisch (läuft das Spiel: zuschauen; ohne Zuschauer: zurück zur Lobby). Rejoin hat Vorrang.
    `/ctb quit`, Zuschauer-„Verlassen“ und Rundenende schicken zu `network.lobby-server` (kein [Play again]), die nächste
    Runde wartet ohne Server-Neustart. Status als MOTD `CTB;<LOBBY|INGAME|ENDING|OFFLINE>;<spieler>;<max>;<arena>`.
    Permission `ctb.network.bypass` (auch für OPs standardmäßig aus): wird nicht ins Spiel gesteckt (Setup).
  - `lobby`: pingt `network.servers` (Name + host:port) alle 2 s per Server List Ping (`ServerPinger`, async).
    Join-Schilder mit Servername statt Arena, `/ctb join` → wartender Server mit den meisten Spielern (sonst laufender
    zum Zuschauen), `/ctb spectate` → laufender Server. Lokal werden keine Spiele für Schilder angelegt.
- **Spielerdaten in SQLite/MySQL** (`database.*`, `database/Database`): SQLite (`database.db`) Standard, MySQL/MariaDB
  fürs Netzwerk. Treiber bringt Paper mit (sqlite-jdbc 3.49.1.0, mysql-connector-j 9.2.0). Alle Queries nacheinander
  auf einem eigenen Thread. `StatsManager`: Cache für Online-Spieler (PlaceholderAPI), `/ctb stats` lädt async aus der
  DB (auch Offline-Spieler per Name), alte `stats.yml` wird einmalig importiert (→ `stats.yml.imported`).
  DB nicht erreichbar → Stats aus (Warnung), Plugin läuft weiter.
- Tests: `NetworkTest` (31, u. a. Fake-Server für den Ping-Protokoll-Test), `StatsTest` erweitert; `ProxiedPlayerMock`
  merkt sich „Connect“-Nachrichten (alle Test-Spieler). 287 Tests, 0 übersprungen. Gegenproben (Rejoin-Vorrang,
  Lobby nach Rundenende) schlagen ohne Fix fehl. Echtes Paper 26.2: SQLite-Datei angelegt, Config ergänzt,
  Ping eines Spielservers liefert `CTB;OFFLINE;0;0;` (keine Arena eingerichtet).

## 2026-09-28 (14) – Features für mehr Spannung (alle konfigurierbar)
- **Abbaulähmung** um gegnerische Beacons (`base.mining-fatigue-radius`/`-level`, Stufe 1 ≈ 15 s pro Beacon).
- **Last Hit** (`game.last-hit-seconds`): wer zuletzt getroffen hat, bekommt den Kill (Void, Lava …).
- **Ressourcen** (`resources`): Eisen-/Diamantblöcke geben zufällig Rüstung/Items, bessere Rüstung wird angezogen.
- **Nur platzierte Blöcke abbaubar** optional (`game.only-placed-blocks-breakable`, Standard aus: ganze Map abbaubar).
  Explosionen (TNT) zerstören nie Beacons, Schutzzonen, Ressourcen und unzerstörbare Blöcke.
- **Unzerstörbare Blöcke** (`game.unbreakable-blocks`, Standard: Zaubertisch).
- **Zeitereignisse** (`events`): Beacon-Schutz fällt weg / Effekte für alle (auch nach Respawn) / Spielende (mehr
  Beacons gewinnt, sonst Unentschieden). „Next: … in mm:ss“ im Scoreboard.
- **Base**: Regeneration für Verteidiger, einmalige Falle pro Beacon (Blindheit + Langsamkeit), Anti-Camping (Gift).
- **Zuschauer** (`spectators.enabled`, `/ctb spectate`, `/ctb join` wenn kein Spiel wartet): unsichtbar, fliegend,
  Kompass zum Teleportieren, rote Farbe zum Verlassen; sehen alle Nachrichten, eigener Zuschauer-Chat.
- **Rejoin** (`game.rejoin-seconds`): nach Verbindungsabbruch zurück ins Team (mit Kills).
- **Join-Schilder** mit Status (`[ctb]` + Arena), `signs.yml`; Arenen mit Schild bekommen beim Start ein Spiel.
- **Statistiken** (`stats.enabled`, `stats.yml`, `/ctb stats [player]`, PlaceholderAPI `%ctb_kills|deaths|kd|wins|games|beacons%`).
- **Teamchat + Shout** (`chat.team-chat`, `chat.shout-prefix: "!"`); Farbcodes von Spielern werden nicht mehr übersetzt.
- **Kampfsystem** (`game.combat: modern|legacy`), **Extra-Herzen** (`game.extra-hearts: 3`), Kit: 16 statt 32 Pfeile.
- Bugfixes: Alarm-Nachricht zeigte `LEFT`/`RIGHT` statt `beacon_left`/`beacon_right`; Spiel lief ohne Gegner weiter,
  wenn das letzte Mitglied eines Teams ging, aber noch ≥ 2 Spieler übrig waren; `setMaxHealth` (deprecated) → Attribut.
- `JoinSign`-Stub, `GameManager#setupNewGame` entfernt → `createGame(arena)`; Tests: `GameTestBase` (aus `GameFlowTest`
  herausgezogen), `GameFeaturesTest`, `JoinSignTest`, `StatsTest` → 250 Tests, 0 übersprungen. Gegenproben (Alarm-Name,
  Last Hit, Rejoin) schlagen ohne Fix fehl. Auf echtem Paper 26.2 (Wegwerf-Server) fehlerfrei gestartet.

## 2026-09-28 (13) – Tablist-Name in Teamfarbe (wieder verworfen)
- Versuch: `GameScoreboard` setzt jeden Tick den Tablist-Namen der Spieler im Spiel auf den Namen in Teamfarbe
  (EasyPrefix überschreibt ihn alle `update-interval` s). Auf Wunsch des Users zurückgenommen (Task pro Tick),
  Tablist zeigt weiter das EasyPrefix-Präfix. Idee für später in todo.md.

## 2026-09-28 (12) – Teamauswahl „Random“ verteilte alle ins gleiche Team
- `LobbyState#assignTeams` (beim Spielstart): Teamgrößen wurden nur einmal vor der Schleife gezählt → alle
  Random-/teamlosen Spieler landeten in Rot. Jetzt pro Spieler neu gezählt, kleineres Team (bei Gleichstand zufällig).
- Ist danach ein Team leer (alle haben dasselbe Team gewählt), wird die Hälfte ins leere Team verschoben
  (vorher erst ab Differenz > 3, d. h. 2 Spieler in Rot blieben ein Spiel ohne Gegner).
- `GamePlayer#setTeam`: Limit off-by-one (`>` → `>=`, ein Team konnte max/2 + 1 Spieler haben); eigenes Team erneut
  wählen und „Random“ gehen immer. `assignTeam` setzt ohne Prüfung (Verteilung beim Start).
- Tests in `GameFlowTest` (Random-Verteilung, Auffüllen, Aufteilen, volles Team); Gegenprobe ohne Fix schlägt fehl.

## 2026-09-28 (11) – Restliche Config-Optionen, Fehler beim Server-Stopp
- `prefix`: Präfix aller Nachrichten (`CatchTheBeacon.PREFIX`, jetzt beim Enable aus der Config gesetzt, leer = keins).
- `lobby.countdown`: Lobby-Countdown (min. 10 s). Logik neu: läuft nur mit genug Spielern (sonst alle 30 s
  "Waiting for players"), volle Lobby verkürzt auf 10 s. Vorher fest 180 s mit Sprung auf 120 s.
- `game.show-death-messages`: Todesnachrichten im Spiel an/aus (Kills werden trotzdem gezählt).
- `game.friendly-fire`: Teamkameraden können sich (auch mit Pfeilen) nicht verletzen, wenn `false`.
- `sql.*` aus der config.yml entfernt: das Plugin speichert nichts in einer Datenbank (alles YAML).
- Ingame gemeldet: `stop` während eines Spiels → `IllegalPluginAccessException`. `Game#stop` warf die Spieler raus,
  beim vorletzten griff "letzter Spieler gewinnt" → ENDING startete einen Countdown im Disable. Fix: `Game#stopping`,
  beim Stoppen keine Zustandswechsel. Test `serverStopDuringTheGame` schlägt ohne Fix fehl.
- 137 Tests grün, 0 übersprungen.

## 2026-09-28 (10) – Respawn-Schutz
- `game.respawn-protection` (config.yml, Standard 3 s, stand schon drin, war ohne Wirkung) ist umgesetzt: nach einem
  Respawn im Spiel kein Schaden, Hinweis in der Actionbar; wer selbst angreift, verliert den Schutz sofort.
  (`GamePlayer#protectFor/isProtected`, `PlayerEventListener#onDamageWhileProtected`)
- Tests: geschützt direkt nach Respawn, nach 3 s nicht mehr, Angriff beendet den Schutz. Gegenprobe ohne Fix schlägt fehl.
  MockBukkit: `PlayerMock#damage(amount, attacker)` wendet Schaden auch bei abgebrochenem Event an → Tests lösen das
  `EntityDamageByEntityEvent` selbst aus. 127 Tests grün.

## 2026-09-28 (9) – Warnradius für Beacons
- Warnung "Gegner beim Beacon" ab 10 statt 5 Blöcken, einstellbar über `game.beacon-warning-radius` in config.yml
  (ConfigUpdater ergänzt den Key in bestehenden Configs). Die Prüfung läuft jetzt auf dem Main-Thread statt asynchron.
- Tests: 8 Blöcke warnt, 12 nicht, Radius per Config änderbar. 124 Tests grün.

## 2026-09-28 (8) – Keine Advancements in CTB-Welten
- `PlayerAdvancementCriterionGrantEvent` (Paper, Paket `com.destroystokyo…`) wird in allen `ctb_`-Welten abgebrochen
  (Lobby, Spiel, Setup-Editor); Gamerule `SHOW_ADVANCEMENT_MESSAGES` aus. Außerhalb bleibt alles wie gehabt.
- Test: MockBukkit kann `getAdvancementProgress` nicht (der Event-Konstruktor ruft es auf) → `PlayerMock`-Unterklasse im Test.
  121 Tests grün, 0 übersprungen.

## 2026-09-28 (7) – Feedback nach erster gespielter Runde
- **Spawnschutz** (`game.spawn-protection` aus config.yml war nie umgesetzt): Blöcke im Radius um Team-Spawns
  (und wie bisher 3 um Beacons) können ingame nicht gesetzt/abgebaut werden (`PlayerEventListener#isProtected`).
- **Standard-Kit**, wenn items.yml leer ist: Stein-Schwert/Spitzhacke/Axt, Bogen + Pfeile, Essen, 2×64 Wolle und
  Lederrüstung in Teamfarbe. `GameItems#setItems` klont die Items jetzt (vorher wurden die Vorlagen verändert).
- **Schöneres Spielende** (`EndingState`): Sieger wird gemerkt (`Game#getWinner`, letzter Beacon bzw. letzter Spieler),
  Titel + Sound, 10 s Feuerwerk in Teamfarbe bei den Gewinnern, alle in Adventure und unverwundbar, Countdown, nach 15 s
  zurück mit Klick-Button **[Play again]** (`/ctb join`). RESTART legt die nächste Runde an.
- `/ctb join` nimmt nur noch Spiele in PENDING/LOBBY mit freiem Platz (vorher auch laufende), bevorzugt das vollste.
- **Keine Mobs** in Spiel-/Lobbywelten: Spawns außer `CUSTOM` werden abgebrochen, Mobs der Map beim Laden der Chunks
  entfernt (`EntitiesLoadEvent`), Gamerule `SPAWN_MONSTERS` aus. Editor-Kopien (`*_temp`) sind ausgenommen.
- **Scoreboard** rechts (`lib/GameScoreboard`): Lobby (Arena, Spieler, Team, Countdown), Ingame (Beacons beider Teams
  ✔/⚠/✘, Kills, Zeit), Ende (Sieger). Eigenes Scoreboard pro Spieler, Namensschilder in Teamfarbe.
- **Bugfix**: 17 Listener haben Events per `setCancelled(false)` wieder freigegeben – auch außerhalb der CTB-Welten, also
  Sperren anderer Plugins aufgehoben. Jetzt wird nur noch abgebrochen.
- `GameFlowTest`: ganze Runde mit zwei Mock-Spielern (11 Tests). 119 Tests grün, 0 übersprungen.
  Nicht ingame geprüft: Feuerwerk/Titel-Optik, Scoreboard-Darstellung (MockBukkit kann `numberFormat` nicht → Fallback).

## 2026-09-28 (6) – Setup-Absturz beim Weltwechsel, Test-Bots
- Ingame gemeldet: Enderperle (Arena → Lobby) entlud beide Setup-Welten, Spieler flog raus. Ursache: Rechtsklick auf
  einen Block feuert zwei Interact-Events, und `createWorld` verarbeitet wartende Pakete → zweiter Weltwechsel mitten im
  Laden. Fix: Entprellung (gleiches Tool im selben Tick) + `busy`-Flag in `SetupSession`, `MapTemplate` lädt nie doppelt,
  Setup endet bei Teleport erst, wenn der Spieler einen Tick später wirklich außerhalb ist. Regressionstests
  (`doubleClickSwitchesOnlyOnce` schlägt ohne Fix fehl), 99 Tests grün.
- Neues Unterprojekt `bot/`: Test-Bots als echte Clients (MCProtocolLib 26.2), damit man ingame einen Gegner hat.

## 2026-09-28 (5) – Geführtes Setup, neue Command-Struktur, Welt-Import
- Wunsch des Users: geführtes Setup + sinnvolle Struktur `ctb arena <arena> …`.
- **Setup-Modus** (`/ctb arena <arena> setup`, Paket `game/setup`): Hotbar-Tools, Actionbar mit nächstem Schritt,
  Welt-Wechsel Lobby/Arena, Checkliste, Speichern & Verlassen (auch bei Quit/Teleport weg). Beacon per Rechtsklick auf den Block.
- **Commands neu**: `ctb arena create <name> [--world]`, `ctb arena list`, alles andere unter `ctb arena <arena> …`
  (`setup`, `check`, `edit`, `save`, `creategame`, `lobby …`, `team …`). Namen `create`/`list` gesperrt.
- **Welt-Import**: `create --world <welt>` und `lobby setworld <serverwelt>` kopieren Welten des Servers nach `maps/`
  (auf echtem Paper 26.2 mit laufender Welt getestet).
- **Test-Infrastruktur repariert**: Seit dem GameWorld-Umbau (3) wurden **alle Tests von MockBukkit übersprungen**
  (`getWorldFolder` nicht implementiert → Skip statt Fehler) – in (3)/(4) fälschlich als „grün“ gemeldet. Jetzt:
  Build schlägt bei übersprungenen Tests fehl, `GameWorld.setDimensionsFolder` für Tests, Welt-Ordner aus dem Key.
- 96 Tests grün, 0 übersprungen (neu: `SetupTest` 12, Import/List/Namen in `CommandArenaTest`).

## 2026-09-28 (4) – „duplicate of another world“ beim Map-Edit
- Ingame gefunden: `/ctb arena lobby Sakura edit` → Paper verweigert `ctb_lobby_temp` („duplicate of another world“),
  weil die Map eine Kopie der geladenen Server-Welt `lobby` war. `data/paper/metadata.dat` enthält die Welt-UUID
  (Nachfolger von `uid.dat`) → `GameWorld` löscht sie nach dem Kopieren, Paper vergibt eine neue.
- Fehlgeschlagenes Laden räumt die Kopie sofort weg und entfernt das hängende MapTemplate.
- Auf dem Wegwerf-Server reproduziert (alter Jar: Fehler, neuer Jar: Welt lädt).

## 2026-09-28 (3) – Base-Setup geprüft: Spiel erstellen funktioniert
- **Weltladen für Paper 26 umgebaut** (`GameWorld`): Maps werden nach `world/dimensions/minecraft/ctb_*` kopiert und per
  Key geladen. Vorher wäre eine frisch generierte Welt statt der Map geladen worden (bzw. alte Maps mit level.dat
  wären verschoben worden → Speichern im Editor kaputt). Unterstützt alte Maps, 26.x-Dimensionen und ganze 26.x-Saves.
- Leftover-Cleanup beim Start löscht `dimensions/minecraft/ctb_*`.
- `/ctb arena check <arena>` (zeigt fehlendes Setup), `creategame` prüft vorher (`--force`: nur Lobby nötig) statt mit
  `Error("Location is null!")` abzustürzen. `create`, `lobby setworld`, `check`, `creategame` gehen auch aus der Konsole.
- Bugfixes: Lobby-Spawn mit einer Koordinate = 0 galt als „nicht gesetzt“; gesetzte Beacons wurden erst nach Neustart
  erkannt; `onDisable` erzeugte über ENDING→RESTART (`hasJoinSign()` ist ein Stub = true) ein **neues Spiel beim Shutdown**
  → jetzt `Game#stop()` (schließt auch die Spielwelt); Event-Debug-Broadcast entfernt; `FileManager.copyFolder` meldet
  Fehler in Unterordnern, `deleteDirectory` ohne NPE; leere `teams.red/blue`-Einträge in arenas.yml entfernt.
- `scripts/test-server.ps1`: UTF-8-BOM auf stdin machte den ersten Konsolen-Command kaputt.
- Tests: 78 grün. **Echter Server** (Wegwerf-Server im Scratchpad, Paper 26.2): create → setworld → check →
  creategame refused → creategame --force → Lobby-Welt geladen, Beacon aus der Map vorhanden, sauberer Shutdown,
  Leftover-Cleanup geprüft. **Nicht** geprüft (braucht Spieler): edit/save der Maps, setspawn/setbeacon ingame, join.

## 2026-09-28 (2) – Gradle, Cloud-Commands, Tests
- **Maven → Gradle 9.8** (Kotlin DSL, Setup wie EasyPrefix): `build.gradle.kts`, Wrapper, `scripts/test-server.ps1`.
  `pom.xml` gelöscht. `gradle.properties` (lokal, gitignored) mit `serverPluginsDir` → `build` kopiert den Jar nach
  `D:\Programme\Minecraft\Server\paper\plugins\update\`.
- **Commands auf Cloud v2** umgestellt (`CommandManager`, `CommandCatchTheBeacon`, `CommandArena`, Parser für Arena/Team).
  Alte Command-Klassen (`commandplugin/*`, `BaseCommand`, `CommandHandler`, …) gelöscht, `commands:` aus `plugin.yml` entfernt.
  Syntax geändert: `/ctb arena <name> create` → `/ctb arena create <name>` usw. (Tabelle in `CLAUDE.md`);
  `ctb config items getitems confirm` → `ctb config items get --confirm`; `ctb tp` braucht jetzt `ctb.admin`.
- **Tests**: JUnit 6 + MockBukkit (`PluginTestBase`, `TestCommandManager`), 72 Tests (Plugin-Start, Dateien,
  ConfigUpdater, Messages, Teams, Commands, TestPlayer).
- **Bugfixes** (von Tests abgedeckt):
  - `PluginFile#set` ignorierte das Daten-Präfix → `/ctb arena create` speicherte unter falschem Pfad (min/max-players = 0,
    Arena nach Neustart weg), Lobby-Welt wurde nie gefunden.
  - Plugin-Ordner war relativer Pfad `plugins/CatchTheBeacon` → jetzt `getDataFolder()`.
  - `FileManager` löschte bei jedem Start config.yml/messages.yml/teams.yml (Dev-Hack) → entfernt.
  - `isInGame()` gab immer `true` zurück.
  - `MapHandler` fügte bei unvollständiger Konfiguration `null` als Arena hinzu (NPE bei `getArena`).
  - `onDisable`: ConcurrentModification beim Schließen der MapTemplates, NPE wenn Enable fehlschlug.
- Debug-Autostart in `onEnable` nur noch mit `-Dctb.debug.autostart=true` (crashte vorher ohne Game/Spieler).
- `api-version` zurück auf `"1.13"` (InventoryGui-Enum-Rewrite, siehe decisions.md).
- **Nicht** auf dem echten Server getestet: `testServer` brach ab, weil der Server lief (Exit 3).

## 2026-09-28 (1) – Dependency-Update auf Paper 26.2 / Java 25
- Spigot 1.16.5 → Paper-API 26.2.build.129-stable; Java 1.8 → 25.
- InventoryGui 1.4.2 → 1.6.5-SNAPSHOT (`Click#getEvent()` → `Click#getWhoClicked()`), jetzt relocated.
- Tablist über Bukkit-API statt NMS.
- `Enchantment.LUCK` (entfernt) → `ItemMeta#setEnchantmentGlintOverride(true)`.
- GameRules: `GameRule.DO_*` (for removal) → `GameRules.ADVANCE_TIME`, `SPAWN_MOBS`, `MOB_DROPS`, `ADVANCE_WEATHER`, …
- XSeries, ConfigUpdater (Lib), PlaceholderAPI, Vault, commons-lang entfernt. Details: `decisions.md`.
