# Offene Punkte

## Priorität hoch
- [ ] **Bots verlassen mitten in der Runde die Spielwelt** (2026-09-29, Server des Users): Spiel `2a110414` startete
      mit Bot1 + Bot2 (User hatte kurz vor dem Start den Server verlassen, danach mehrfach zugeschaut/verlassen).
      Später schwebte Bot1 mit Kit „in einer Void-Map“, Bot2 wurde in der Overworld getötet; um 20:54 ließen sich die
      Spielwelten entladen → kein Spieler mehr darin, obwohl `INGAME`. Bot1s Backup-Standort ist `world` bei exakt
      dem Blau-Spawn (-72.4/81/256.7) → schon in einer früheren Runde passiert. Nicht reproduzierbar (Wegwerf-Server,
      gleiche Map, alle Plugins, 2 Bots + Force-Start). Nächster Schritt: beim nächsten Auftreten `/ctb debug info Bot1`
      + `/data get entity Bot1 Dimension`, und herausfinden, welche Aktion (Zuschauer rein/raus?) es auslöst.
- [ ] **Shop** (Spielprinzip: Eisen als Währung, z. B. 4 Steinknöpfe für 4 Eisen): GUI (InventoryGui) mit Angeboten
      aus einer Config-Datei, Shop-Zugang (NPC? Item im Inventar? Block in der Base?) mit dem User klären. Dazu passend:
      `resources` für Eisenblöcke evtl. auf `IRON_INGOT:x` umstellen.

## Plugin-Integrationen (User: möglichst viele etablierte Plugins unterstützen)
- [x] Parties, Party and Friends (`integrations/`)
- [x] Citizens/FancyNpcs (Join-NPCs), Floodgate (Bedrock-Formulare), MiniPlaceholders, PAPI-Status-Platzhalter
- [ ] Mit echten Jars testen (Wegwerf-Server + Bots): Parties; Citizens + FancyNpcs (Klick, `npcs.yml`); Floodgate
      braucht Geyser + Bedrock-Client; MiniPlaceholders (`/miniplaceholders parse`). Offen v. a.: dürfen Bukkit-Plugins
      (CTB hat plugin.yml) auf Klassen von Paper-Plugins (MiniPlaceholders, evtl. FancyNpcs) zugreifen? Falls nicht:
      Hook scheitert sauber mit Warnung.
- [ ] Vorschläge, noch nicht umgesetzt (mit dem User priorisieren):
  - **Vault** (Economy): Geld für Sieg/Kill/Beacon (`rewards.*`)
  - **TAB** (NEZNAMY): Tablist/Namensschilder im Spiel nicht überschreiben
  - **EssentialsX**: God-Mode/Fly/AFK/Vanish beim Beitreten aus, `/back` in `ctb_*`-Welten verhindern
  - **SuperVanish/PremiumVanish**: Vanish-Spieler nicht mitzählen/automatisch joinen
  - **Multiverse-Core/-Inventories**: `ctb_*`-Welten ignorieren (keine Inventar-Gruppen dort)
  - Join-NPCs: Status direkt im NPC-Namen (aktuell über Platzhalter in Citizens/FancyNpcs-Namen/Hologrammen)

## Ideen (nicht beauftragt)
- 1.8-PvP vollständiger: Schwert-Blocken, alter Knockback, Sweep-Attacken aus (aktuell nur ohne Angriffs-Cooldown).
- Stats-Ranking (`/ctb top`), jetzt einfach per SQL (`ORDER BY wins DESC`).
- Netzwerk: Server-Auswahl-Menü (GUI) in der Lobby (Parties: siehe Plugin-Integrationen).
- Zuschauer beim eigenen Tod (Final Kill) statt Respawn – bräuchte Betten/Respawn-Limit, passt nicht zum Spielprinzip.

## Modernisierung (optional, funktioniert aktuell)
- [ ] Namen/Lore der Setup-Items (`SetupItem`) stehen noch englisch im Code (nur Admins) → bei Bedarf in messages.yml.
- [ ] InventoryGui kennt nur Legacy-Strings (`I.legacy`) – falls eine Version mit Components erscheint, umstellen.

