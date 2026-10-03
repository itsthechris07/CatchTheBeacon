# Data CatchTheBeacon sends

CatchTheBeacon sends anonymous data to help develop the plugin and checks for updates. All of it can be disabled.

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

## Update checker (GitHub)
At the start and every 12 hours, CatchTheBeacon requests the list of its releases from the GitHub API
(`api.github.com`, GitHub, Inc., USA) to tell admins about new versions. Nothing about the server or its players is
sent; like every web request, GitHub sees the IP address of the server and the installed version of CatchTheBeacon
(user agent), see [GitHub's privacy statement](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).

**Disable:** set `update-checker.enabled: false` in `plugins/CatchTheBeacon/config.yml`.

Legal basis (GDPR): legitimate interest in finding and fixing errors and in keeping the plugin up to date
(Art. 6 (1) (f)); you can object at any time by disabling the reports and the update checker as described above.
