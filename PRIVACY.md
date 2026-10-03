# Data CatchTheBeacon sends

CatchTheBeacon sends two kinds of anonymous data to help develop the plugin. Both can be disabled.

## Statistics (bStats)
[bStats](https://bstats.org/plugin/bukkit/CatchTheBeacon/34381) collects anonymous statistics like the number of
servers and players, the server and Java version and a few settings of the plugin (network mode, database type,
combat system, variant voting, team balancing, achievements, number of arenas, PlaceholderAPI installed, number of
rounds played with their variant and player count, variant votes). IP addresses are not stored.

**Disable:** set `enabled: false` in `plugins/bStats/config.yml` (disables bStats for all plugins).

## Error reports (Sentry)
When an error occurs in the code of CatchTheBeacon, a report is sent to [Sentry](https://sentry.io) (Functional
Software, Inc., data stored in the USA; Sentry is certified under the EU-U.S. Data Privacy Framework, see
[sentry.io/privacy](https://sentry.io/privacy/)). It contains the error with its stack trace, the version of CatchTheBeacon, the server software and version,
the Java version and the network mode.

It does **not** contain: player names or UUIDs (they are removed from the error messages), the name or address of
the server, logs, or errors of other plugins. The Sentry project does not store IP addresses. Every error is reported
once per server start, at most 25 per start.

**Disable:** set `sentry.enabled: false` in `plugins/CatchTheBeacon/config.yml`.

Legal basis (GDPR): legitimate interest in finding and fixing errors (Art. 6 (1) (f)); you can object at any time by
disabling the reports as described above.
