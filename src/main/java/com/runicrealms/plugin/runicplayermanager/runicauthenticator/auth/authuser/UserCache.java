package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UserCache {

    private final Map<UUID, User> userMap = new ConcurrentHashMap<>();
    private final Main main;

    public UserCache(Main authenticator) {
        this.main = authenticator;
    }

    public User get(UUID id) {
        return userMap.get(id);
    }

    public User join(UUID id, UserData data) throws IOException, SQLException {
        User user = new User(id, data, main);
        userMap.put(id, user);
        return user;
    }

    public User leave(UUID id) {
        return userMap.remove(id);
    }

    public void invalidate() {
        userMap.clear();
    }

}
