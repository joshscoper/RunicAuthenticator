package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public interface UserDataSource {

    UserData getUser(UUID id) throws IOException, SQLException;
    UserData createUser(UUID id);
    void destroyUser(UUID id);

    void save() throws IOException, SQLException;
    void invalidateCache() throws IOException;
}
