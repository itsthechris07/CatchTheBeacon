package com.christian34.catchthebeacon.bot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects test players ("bots") to the local server, so games can be tested alone. The bots don't move - they stand
 * where they spawn, can be hit and killed, respawn automatically and can run commands.
 * <p>
 * Usage: {@code ./gradlew :bot:run --args="[count] [--join] [--host host:port]"}, e.g. {@code --args="1 --join"}.
 * The server has to run with {@code online-mode=false}. Console input while running:
 * {@code <command>} runs the command for all bots (e.g. "ctb join", "ctb quit"), "stop" disconnects them.
 *
 * @author Christian34
 */
public final class TestBots {

    private TestBots() {
    }

    public static void main(String[] args) throws Exception {
        int count = 1;
        boolean join = false;
        String host = "127.0.0.1";
        int port = 25565;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--join" -> join = true;
                case "--host" -> {
                    String[] address = args[++i].split(":");
                    host = address[0];
                    if (address.length > 1) port = Integer.parseInt(address[1]);
                }
                default -> count = Integer.parseInt(args[i]);
            }
        }

        List<Bot> bots = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Bot bot = new Bot("Bot" + i, host, port, join ? List.of("ctb join") : List.of());
            bot.connect();
            bots.add(bot);
            Thread.sleep(500);
        }
        System.out.println("[Bots] " + count + " bot(s) started. Type a command for all bots (without /) or 'stop'.");

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = console.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("stop")) {
                bots.forEach(Bot::disconnect);
                Thread.sleep(500);
                System.exit(0);
            }
            String command = line.startsWith("/") ? line.substring(1) : line;
            bots.forEach(bot -> bot.runCommand(command));
        }
        // no console (e.g. started in the background): keep the bots online until the process is stopped
        Thread.currentThread().join();
    }

}
