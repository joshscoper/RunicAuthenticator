package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.datasource;

import com.google.gson.*;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.UserData;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.UserDataSource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SingleFileUserDataSource implements UserDataSource {

    @Override
    public String toString() {
        return "(SingleFileDataSource: "+ dataFile.getPath()+")";
    }

    public SingleFileUserDataSource(File f) throws IOException {
        if (!f.exists()) {
            if (!f.createNewFile()) {
                throw new IOException("There was an issue creating the player data file "+f.getPath());
            }
        }
        this.dataFile = f;
        invalidateCache();
    }

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create(); //Users like pretty json!

    private Map<UUID, BasicUserData> data = new HashMap<>();
    private final File dataFile;

    @Override
    public UserData getUser(UUID id) {
        return data.get(id);
    }

    @Override
    public UserData createUser(UUID id) {
        BasicUserData d = new BasicUserData(id, null, null, -1, false);
        data.put(id, d);
        return d;
    }

    @Override
    public void destroyUser(UUID id) {
        data.remove(id);
    }

    @Override
    public void save() throws IOException {
        JsonArray a = new JsonArray();
        for (BasicUserData d : data.values()) {
            JsonObject element = new JsonObject();
            element.addProperty("uuid", d.getId().toString());
            InetAddress iAdd = d.getLastAddress();
            element.addProperty("ip", iAdd != null ? iAdd.getHostAddress() : null);
            element.addProperty("secret", d.getSecret());
            element.addProperty("locked", d.isLocked(null));
            element.addProperty("authtype", d.getAuthType());
            a.add(element);
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(a, writer);
        }
    }

    @Override
    public void invalidateCache() throws IOException {

        //We basically reload the data, since its only one file anyways!

        Map<UUID, BasicUserData> basicUserData = new HashMap<>();
        try (FileReader fileReader = new FileReader(dataFile)) {
            JsonArray data = gson.fromJson(fileReader, JsonArray.class);
            for (int i = 0; data != null && i < data.size(); i++) {
                JsonObject obj = data.get(i).getAsJsonObject();
                JsonElement ip1 = obj.get("ip");
                String ip = ip1 != null ? ip1.getAsString() : null;
                JsonElement secret = obj.get("secret");
                JsonElement authtype = obj.get("authtype");
                BasicUserData adata = new BasicUserData(
                        UUID.fromString(obj.get("uuid").getAsString()),
                        ip != null ? InetAddress.getByName(ip) : null,
                        secret != null ? secret.getAsString() : null,
                        authtype != null ? authtype.getAsInt() : 0,
                        obj.get("locked").getAsBoolean());
                basicUserData.put(adata.getId(), adata);
            }
        }
        this.data = basicUserData;
    }
}
