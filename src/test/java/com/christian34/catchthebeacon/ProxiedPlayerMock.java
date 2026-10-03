package com.christian34.catchthebeacon;

import com.christian34.catchthebeacon.network.NetworkManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.plugin.Plugin;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

/**
 * A player behind a proxy: remembers the servers the plugin sent him to ("Connect" messages).
 *
 * @author Christian34
 */
public class ProxiedPlayerMock extends PlayerMock {
    private final List<String> connects = new ArrayList<>();

    public ProxiedPlayerMock(ServerMock server, String name) {
        super(server, name);
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        // fails if the channel isn't registered
        super.sendPluginMessage(source, channel, message);
        if (!channel.equals(NetworkManager.CHANNEL)) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        if (in.readUTF().equals("Connect")) connects.add(in.readUTF());
    }

    /**
     * @return the servers the player was sent to
     */
    public List<String> getConnects() {
        return connects;
    }

}
