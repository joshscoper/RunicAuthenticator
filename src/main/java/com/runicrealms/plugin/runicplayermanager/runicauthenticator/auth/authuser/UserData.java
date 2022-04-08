package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser;

import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.UUID;

public interface UserData {
    UUID getId();
    InetAddress getLastAddress();
    void setLastAddress(InetAddress inetAddress);
    String getSecret();
    int getAuthType();
    void setSecret(String secret, int authtype);
    boolean isLocked(Player player);
    void setLocked(boolean lock);
}
