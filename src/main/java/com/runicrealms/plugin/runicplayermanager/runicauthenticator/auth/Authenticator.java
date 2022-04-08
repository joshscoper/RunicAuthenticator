package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import org.bukkit.entity.Player;

public interface Authenticator {

    boolean authenticate(User user, Player p, String input) throws Exception;

    boolean isFormat(String s);

    void initUser(User u, Player p);

    void quitUser(User u, Player p);

}
