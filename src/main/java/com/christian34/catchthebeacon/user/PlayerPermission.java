package com.christian34.catchthebeacon.user;

public enum PlayerPermission {
    ADMIN,
    VIP;

    private final static String PERMISSION_PREFIX = "ctb.";

    @Override
    public String toString() {
        return PERMISSION_PREFIX + name().toLowerCase().replace("_", ".");
    }

}