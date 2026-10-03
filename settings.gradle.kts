rootProject.name = "CatchTheBeacon"

// test bots that join the local server (not part of the plugin)
if (file("bot").isDirectory) include("bot")
