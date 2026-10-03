package com.christian34.catchthebeacon.bot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * One test player: logs in (offline mode), confirms teleports, respawns after dying and reconnects if the connection
 * is lost (e.g. while the server restarts).
 *
 * @author Christian34
 */
public class Bot {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "bot-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private static final int RECONNECT_SECONDS = 5;
    private final String name;
    private final InetSocketAddress address;
    private final List<String> commandsOnJoin;
    private volatile ClientSession session;
    private volatile boolean stopped;

    public Bot(String name, String host, int port, List<String> commandsOnJoin) {
        this.name = name;
        this.address = new InetSocketAddress(host, port);
        this.commandsOnJoin = commandsOnJoin;
    }

    private static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private void log(String message) {
        System.out.println("[" + name + "] " + message);
    }

    public void connect() {
        ClientSession session = ClientNetworkSessionFactory.factory()
                .setRemoteSocketAddress(address)
                .setProtocol(new MinecraftProtocol(name))
                .create();
        session.addListener(new SessionAdapter() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                handle(packet);
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                if (stopped) return;
                log("disconnected (" + plain(event.getReason()) + ") - reconnecting in " + RECONNECT_SECONDS + "s");
                SCHEDULER.schedule(Bot.this::connect, RECONNECT_SECONDS, TimeUnit.SECONDS);
            }
        });
        this.session = session;
        session.connect(false);
    }

    public void disconnect() {
        stopped = true;
        session.disconnect(Component.text("Bot stopped"));
    }

    public void runCommand(String command) {
        session.send(new ServerboundChatCommandPacket(command));
    }

    private void handle(Packet packet) {
        switch (packet) {
            case ClientboundLoginPacket ignored -> {
                log("joined the server");
                // the server treats a player as loading (invulnerable) until the client says it has loaded the world
                session.send(ServerboundPlayerLoadedPacket.INSTANCE);
                for (int i = 0; i < commandsOnJoin.size(); i++) {
                    String command = commandsOnJoin.get(i);
                    SCHEDULER.schedule(() -> runCommand(command), 2 + i, TimeUnit.SECONDS);
                }
            }
            // without the answer the server stops sending chunks
            case ClientboundChunkBatchFinishedPacket ignored -> session.send(new ServerboundChunkBatchReceivedPacket(64f));
            case ClientboundPlayerPositionPacket position ->
                    session.send(new ServerboundAcceptTeleportationPacket(position.getId()));
            case ClientboundPlayerCombatKillPacket ignored -> {
                log("died - respawning");
                SCHEDULER.schedule(() -> {
                    session.send(new ServerboundClientCommandPacket(ClientCommand.PERFORM_RESPAWN));
                    session.send(ServerboundPlayerLoadedPacket.INSTANCE);
                }, 1, TimeUnit.SECONDS);
            }
            case ClientboundSystemChatPacket chat when !chat.isOverlay() -> log("chat: " + plain(chat.getContent()));
            default -> {
            }
        }
    }

}
