package com.runicrealms.plugin.runicplayermanager.runicauthenticator.events;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public final class PluginMessageEvent implements PluginMessageListener {

    private final Main instance;

    public PluginMessageEvent(Main instance) {
        this.instance = instance;
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        if (!instance.getC().getBungeePluginChannel().equals(s)) return;
        User user = instance.getCache().get(player.getUniqueId());
        if (bytes.length < 1) return;
        if (bytes[0] == 0x00) {
            user.remoteAuthenticated();
            // } else if (bytes[0] == 0x01) { // NO OP
        } else if (bytes[0] == 0x02) {
            boolean authenticate = false;
            try {
                authenticate = user.authenticate(new String(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8), player);
            } catch (Exception e) {
                instance.getC().sendDirect(player, "&cThere was a fatal exception when trying to authenticate you!");
            }

            if (authenticate) {
                instance.getC().send(player, instance.getC().message("authenticated"));
            } else {
                instance.getC().send(player, instance.getC().message("authFailed"));
            }
        }
    }
}
