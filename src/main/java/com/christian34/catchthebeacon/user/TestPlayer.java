package com.christian34.catchthebeacon.user;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Fake {@link Player} for local debugging (fills up games without a second client).
 * <p>
 * Implemented as a dynamic proxy so it keeps compiling when the Player interface changes
 * between Minecraft versions. Only the methods the plugin relies on are emulated; every
 * other call returns a neutral default (null / false / 0).
 */
public final class TestPlayer implements InvocationHandler {
    private static final String ADMIN_NAME = "Christian34";

    private final String name;
    private final UUID uniqueID = UUID.randomUUID();
    private Location location;
    private GameMode gameMode = GameMode.SURVIVAL;

    private TestPlayer(String name) {
        this.name = name;
        this.location = getAdmin().getLocation();
    }

    public static Player create(String name) {
        return (Player) Proxy.newProxyInstance(TestPlayer.class.getClassLoader(),
                new Class<?>[]{Player.class},
                new TestPlayer(name));
    }

    private static Player getAdmin() {
        Player admin = Bukkit.getPlayerExact(ADMIN_NAME);
        if (admin != null) return admin;
        return Bukkit.getOnlinePlayers().stream().findAny()
                .orElseThrow(() -> new IllegalStateException("TestPlayer needs at least one real player online"));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return uniqueID.hashCode();
            case "toString":
                return "TestPlayer{" + name + "}";
            case "getName":
            case "getDisplayName":
            case "getPlayerListName":
                return name;
            case "getUniqueId":
                return uniqueID;
            case "getLocation":
            case "getEyeLocation":
                return location.clone();
            case "getWorld":
                return location.getWorld();
            case "getVelocity":
                return new Vector();
            case "teleport":
                if (args != null && args.length > 0 && args[0] instanceof Location) {
                    this.location = ((Location) args[0]).clone();
                    return true;
                }
                return false;
            case "getGameMode":
                return gameMode;
            case "setGameMode":
                this.gameMode = (GameMode) args[0];
                return null;
            case "getTargetBlock":
                return location.getBlock();
            case "isOnline":
            case "isValid":
                return true;
            case "getInventory":
            case "getEnderChest":
            case "getScoreboard":
            case "getItemInHand":
                try {
                    return method.invoke(getAdmin(), args);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            default:
                return defaultValue(method.getReturnType());
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        return 0d;
    }

}
