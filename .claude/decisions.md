# Entscheidungen

## 2026-09-29 – Join-NPCs verknüpfen statt erzeugen, Menüs über `Menu`
- NPCs werden nicht von CTB erzeugt, sondern vorhandene NPCs per Klick verknüpft: Aussehen, Skin, Hologramme bleiben
  beim NPC-Plugin (dort ist das Tooling besser); funktioniert für Citizens (Entities) und FancyNpcs (nur Pakete, kein
  Anvisieren möglich) gleich. Status im Namen über die Platzhalter (`%ctb_state_<arena>%`, `<ctb_players:arena>`).
- Bedrock: eine gemeinsame `Menu`-Beschreibung statt doppelter Menü-Logik; Floodgate-Spieler bekommen `SimpleForm`s.

## 2026-09-29 – Fremd-Plugins über `integrations/` (User: „möglichst viele relevante, etablierte Plugins“)
- Jede Integration optional: `compileOnly` + `softdepend`, Klasse mit Fremd-API-Imports wird nur instanziiert, wenn
  das Plugin aktiv ist; Aufrufe fangen `Exception | LinkageError` (API-Änderung darf Joinen nicht kaputt machen).
- Parties: Teamaufteilung hält Teams ausgeglichen (max. Hälfte) statt Party um jeden Preis zusammen – das
  Spielprinzip (2 gleich große Teams) geht vor. Eine explizite Teamwahl bleibt immer erhalten.
- PAF auf Spigot: offizielle „Spigot Party API“ (liest Parties vom Proxy). Nachziehen der Mitglieder im Netzwerk
  machen Parties/PAF auf dem Proxy selbst → CTB bevorzugt nur einen Server mit genug Platz.

## 2026-09-29 – Spielerzustand: Datei-Backup statt Datenbank, Statistiken per Event sperren
- User: das Spiel darf keine Auswirkung auf das eigentliche Spiel haben (Inventar + alle Stats), außer auf reinen
  Spielservern mit einer Arena.
- Backup als **lokale Datei** (`backups/<uuid>.yml`), obwohl Spielerdaten sonst in die Datenbank gehören: muss
  synchron geschrieben sein, *bevor* das Inventar geleert wird (die DB-Queue ist async), gehört zu genau diesem Server
  (gemeinsame MySQL im Netzwerk) und darf nicht von der DB-Erreichbarkeit abhängen.
- Items als `ItemStack#serializeItemsAsBytes` (Paper, mit Datenversion → übersteht MC-Updates).
- Vanilla-Statistiken/Rezepte/Erfolge werden in `ctb_*`-Welten **verhindert** statt gesichert (Snapshot aller
  Statistiken wären ~10.000 Werte pro Join/Quit).
- Backup-Dateien werden nie überschrieben und bei Fehlern nie gelöscht (lieber eine Datei zu viel als Items weg).

## 2026-09-29 – Spiele nach Neustart per Arena-Flag statt games.yml
- `creategame` merkt sich die Arena (`arenas.<name>.auto-game`) statt einzelner Spiele mit IDs: nach einem Neustart
  zählt nur „diese Arena soll ein Spiel haben“ (wie bei Join-Schildern). Nicht bei `--force` (nur zum Testen).
- Weil ein existierendes Spiel die Arena sperrt (`isInUse`), gibt es `removegame` als Gegenstück (Flag weg + Spiele
  stoppen). Join-Schilder legen weiterhin unabhängig davon beim Start ein Spiel an.
- `games.yml` einmalig übernehmen und löschen statt weiter mitschleppen.
- Beacon-Positionen bekommen die Welt erst beim Laden der Spielwelt (die Arena kennt nur Koordinaten, die Welt ist pro
  Spiel eine Kopie); Vergleiche über `WorldUtils.isSameBlock`/`distance` sind damit weltgenau.

## 2026-09-29 – Texte: MiniMessage mit Legacy-Fallback, Argumente als reiner Text
- `messages.yml` im MiniMessage-Format; bestehende Server-Dateien mit `&`-Codes funktionieren weiter (automatische
  Umwandlung zur Laufzeit statt einmaliger Migration der Datei → nichts wird am Server-File verändert).
- Platzhalter bleiben `{0}`, `{1}` (kompatibel mit alten Dateien); intern `<ctb_arg_N>`-Placeholder. String-Argumente
  werden nie geparst (kein Formatieren über Namen/Chat); wer Farbe will, übergibt ein `Component`.
- InventoryGui kennt nur Legacy-Strings → dort `I.legacy(...)` (Bibliotheksgrenze, nicht deprecated).
- Command-Beschreibungen als messages.yml-Keys in `@CommandDescription` (statt englischem Text + Mapping), geprüft
  von `ResourceFilesTest#everyCommandHasADescription`.
- Debug-Ausgaben (`/ctb debug`) und die Namen der Setup-Items bleiben im Code (nur für Admins/Entwicklung).


## 2026-09-29 – bStats + Sentry: opt-out mit Datenminimierung (User-Wahl, Plugin wird öffentlich verteilt)
- DSGVO verlangt keine ausdrückliche Zustimmung, wenn eine andere Rechtsgrundlage greift: berechtigtes Interesse an
  Fehlerbehebung (Art. 6 Abs. 1 lit. f) – Voraussetzung: Minimierung, Transparenz (`PRIVACY.md`, Config-Kommentar,
  Konsolenhinweis), einfacher Widerspruch (`sentry.enabled: false`). Keine Rechtsberatung – Einschätzung von Claude.
- Sentry-Projekt liegt in der **US-Region** (bestehende Org, User-Wahl; Sentry ist DPF-zertifiziert → offengelegt in
  `PRIVACY.md`), IP-Speicherung muss in den Projekteinstellungen deaktiviert sein,
  Server sendet keinen Hostnamen (`setAttachServerName(false)`).
- Kein globaler `UncaughtExceptionHandler` und kein Log-Versand: nur Exceptions, in deren Stacktrace CTB-Code steht.
- bStats wie in EasyPrefix (globaler Opt-out in `plugins/bStats/config.yml` genügt, Plattform-Standard).

## 2026-09-28 – Netzwerk: ein Spiel pro Server (User-Wahl: Velocity, MOTD-Ping, Lobby nach Runde, MySQL)
- **Velocity** über den `BungeeCord`-Kanal („Connect“) – Velocity versteht ihn (`bungee-plugin-message-channel`,
  Standard an); kein Proxy-Plugin nötig. BungeeCord würde genauso funktionieren.
- **Status per Server List Ping (MOTD)** statt Plugin-Messaging/Redis: klappt ohne Zusatz-Software und auch, wenn
  niemand auf dem Spielserver ist (Plugin-Messaging braucht einen Spieler als Träger). Maschinenlesbares Format
  `CTB;STATE;players;max;arena`; Spielserver sind hinter dem Proxy, ihre MOTD sieht kein Spieler.
- **Rundenende**: zurück zur Lobby, Server bleibt an (die Map wird wie bisher neu geladen) – kein Restart-Skript nötig.
- **Ein Modus pro Server** in derselben Jar (`network.mode`), wird bei jeder Verwendung aus der Config gelesen
  (in Tests umschaltbar). Spielserver ohne spielbare Arena schicken niemanden weg (Setup muss möglich bleiben),
  zusätzlich `ctb.network.bypass` mit `default: false` (OPs würden sonst nie ins Spiel kommen).
- **Spielerdaten immer in einer Datenbank** (User): SQLite lokal, MySQL/MariaDB im Netzwerk; keine YAML mehr.
  Keine Connection-Pool-Lib (HikariCP müsste geshadet werden): eine Verbindung auf einem Single-Thread-Executor
  (Reihenfolge bleibt erhalten, `isValid` → Reconnect). Upsert dialektneutral: `INSERT OR IGNORE`/`INSERT IGNORE` +
  `UPDATE col = col + 1`. Cache nur für Online-Spieler (PlaceholderAPI fragt synchron).

## 2026-09-28 – Spiel-Features (alle per config.yml abschaltbar)
- **Abbaulähmung = Vanilla-Effekt Mining Fatigue** statt eigener Abbau-Logik: Beacon hat Härte 3 (ohne Werkzeug-Bonus,
  ≈ 4,5 s), Mining Fatigue I multipliziert mit 0,3 → ≈ 15 s. Client zeigt den Fortschritt korrekt an, nichts zu syncen.
  Gilt wie im Original für alle Blöcke im Radius, nur für Gegner des Beacons.
- **Ganze Map abbaubar** ist Standard (Spielprinzip); `only-placed-blocks-breakable` bleibt als Option. Ressourcen-Blöcke
  sind davon immer ausgenommen, Explosionen zerstören sie nicht (nur wer abbaut, bekommt die Belohnung).
- **Ressourcen** geben eine zufällige Belohnung statt des Drops; Rüstung wird angezogen, wenn besser (Tier nach
  Material-Präfix). Nach dem Tod gibt es wieder nur das Kit – Verbesserungen muss man sich neu holen.
- **Anti-Camping** zählt nicht, solange Gegner im Warnradius des Beacons sind (Verteidigen ist kein Campen).
- **1.8-PvP** nur über das `ATTACK_SPEED`-Attribut (1024 → kein Cooldown); keine Schwert-Blocks/Knockback-Änderungen.
  Attribute (auch Max-Health für Extra-Herzen) sichert/stellt `UserStorage` wieder her.
- **Zuschauer** im ADVENTURE-Modus (fliegend, unverwundbar, per `hidePlayer` unsichtbar) statt SPECTATOR-Modus, damit
  Kompass/Items funktionieren. Pfeile fliegen durch (ProjectileHitEvent abgebrochen).
- **Rejoin** nur bei Verbindungsabbruch (nicht bei `/ctb quit`). Das Team verliert nicht, solange ein Mitglied noch
  zurückkommen kann. Sieger-Prüfung jetzt teambasiert: vorher endete das Spiel erst bei < 2 Spielern insgesamt
  (2 Rot vs. 1 Blau, Blau geht → Spiel lief ohne Gegner weiter).
- **Spielzeit in Ticks** (`Bukkit.getCurrentTick`) statt Systemzeit: Events hängen an der Spielzeit, in Tests steuerbar.
- **Join-Schilder** statt Spiele-Persistenz in `games.yml`: `signs.yml` speichert Schilder, beim Start bekommt jede Arena
  mit Schild ein Spiel. Der `hasJoinSign()`-Stub ist weg, Restart legt immer die nächste Runde an.
- ~~**Statistiken** in einer YAML-Datei~~ → seit (15) in SQLite/MySQL (siehe oben). PlaceholderAPI ist
  optional (`softdepend`), die Expansion-Klasse wird nur geladen, wenn PAPI läuft.
- Listen in der Config (`events`, `resources`, `unbreakable-blocks`) statt Maps: der ConfigUpdater ergänzt fehlende
  Keys – bei einer Map würden vom Admin gelöschte Einträge (z. B. `resources.IRON_BLOCK`) wieder auftauchen.

## 2026-09-28 – Geführtes Setup per Hotbar-Tools, Struktur `ctb arena <arena> …`
- User wählte Hotbar-Tools (statt Chat-Assistent oder Inventar-GUI) und Import von Server-Welten.
- Die frühere Begründung „Cloud kann `<name>` und `<arena>` nicht an derselben Stelle“ war zu streng: Literale
  (`create`, `list`) neben **einem** Argument gehen – nur zwei Argumente an derselben Stelle sind mehrdeutig.
- Setup-Items werden per PersistentDataContainer (`catchthebeacon:setup_item`) erkannt, nicht über Namen/Material.
- Lobby-Spawn-Schritt wird übersprungen, solange keine Lobby-Welt gesetzt ist (Hinweis + Klick-Button auf `setworld`).

## 2026-09-28 – Welten direkt als Dimension kopieren (Paper 26 World Storage)
Empirisch mit einem Probe-Plugin auf Paper 26.2 ermittelt:
| Fall | Ergebnis |
|---|---|
| `new WorldCreator(NamespacedKey.minecraft("x"))` | Ordner `world/dimensions/minecraft/x`, `getWorldFolder()` zeigt dorthin |
| Ordner nach `dimensions/minecraft/y` kopiert, per Key geladen | Blöcke der Kopie sind da ✅ |
| Ordner `<container>/z` ohne level.dat, `WorldCreator("z")` | **ignoriert**, neue Welt generiert ❌ |
| Ordner `<container>/w` mit level.dat, `WorldCreator("w")` | `LegacyCraftBukkitWorldMigration` **verschiebt** ihn nach dimensions/ (Quelle danach weg) |
| Name mit Großbuchstaben | Key/Ordner lowercase |
→ `GameWorld` kopiert nach `dimensions/minecraft/<name>` und lädt per Key; keine Abhängigkeit von der Legacy-Migration.

## 2026-09-28 – Gradle + Cloud + MockBukkit wie EasyPrefix
- Beide Plugins sollen gleich gebaut/getestet werden (gleiche Befehle, gleiches Deploy-/Testserver-Skript).
- Cloud v2 statt eigenem Command-Framework: Permissions, Tab-Completion, Syntaxfehler, Help-Menü und Tests gibt es geschenkt.
  Die alte Syntax `/ctb arena <name> <aktion>` ging mit Cloud nicht (an derselben Position einmal freier Name, einmal
  existierende Arena) → Aktion zuerst: `/ctb arena <aktion> <arena>`.
- `simpleCoordinator` statt async (EasyPrefix nutzt async + `TaskManager`): fast alle CTB-Commands ändern Welten/Spieler.
- Deploy nach `plugins/update/` statt direkt nach `plugins/`: Windows sperrt den Jar eines laufenden Servers; Paper
  tauscht den Jar aus `update/` beim nächsten Start ein.

## 2026-09-28 – api-version bleibt "1.13"
- Zwischenzeitlich auf "1.21" erhöht – falsch: `org.bukkit.Sound` ist jetzt ein Interface, InventoryGui ist gegen das alte
  Enum kompiliert. Paper schreibt die Aufrufe nur für Plugins mit alter api-version um (gleiche Lösung wie EasyPrefix).

## 2026-09-28 – Nur aktuelle Paper-Version, keine Abwärtskompatibilität
- Der Code war nie 1.8-kompatibel (nutzte schon `GREEN_WOOL`, `RED_BED`, `api-version: 1.13`).
- Multi-Version-Support hätte NMS/XSeries-Schichten erzwungen; für ein Minigame, das auf einem eigenen Server läuft, lohnt das nicht.
- → Ziel: **Paper 26.2 / Java 25**. Beim nächsten MC-Update einfach `paper-api`-Version anheben.

## 2026-09-28 – Paper-API statt Spigot-API
- Testserver läuft mit Paper; Paper-API ist Obermenge der Spigot-API.
- Folge: Viele String-basierte Methoden (`setDisplayName`, `broadcastMessage`, `ChatColor`, …) sind in Paper deprecated
  zugunsten von Adventure-Components. Funktionieren weiter, Migration steht in `todo.md`.

## 2026-09-28 – Abhängigkeiten reduziert
- **NMS-Reflection entfernt** (`NMS`, `ClassUtils`, `ClassMethod`, `InstanceMethod`): Paper hat seit 1.20.5 keine versionierten
  `net.minecraft.server.vX_Y`-Pakete mehr → Tablist lief nicht. Ersatz: `Player#setPlayerListHeaderFooter`.
- **XSeries entfernt**: nur für `XMaterial.matchXMaterial` genutzt → `Material.matchMaterial`.
- **ConfigUpdater (tchristofferson) entfernt**: eigene Klasse `files/ConfigUpdater` (Bukkit-YAML erhält Kommentare seit 1.18).
  (EasyPrefix nutzt die Lib `ConfigUpdater 2.2` – bei Problemen mit der eigenen Klasse ggf. angleichen.)
- **PlaceholderAPI, Vault** entfernt: im Code nicht genutzt.
- **commons-lang** (war früher transitiv in der Spigot-API) → `org.bukkit.util.StringUtil` bzw. `String#startsWith`.
- **VersionController** entfernt: ungenutzt und inkompatibel mit neuer Versionierung (26.x).
- **WorldInitListener** entfernt: `setKeepSpawnInMemory` ist no-op, Spawn-Chunks existieren seit 1.21.9 nicht mehr.
- **TestPlayer** von ~1500-Zeilen-`implements Player` auf `java.lang.reflect.Proxy` umgestellt → bricht nicht mehr bei API-Änderungen.
