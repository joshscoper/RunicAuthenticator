package com.runicrealms.plugin.runicplayermanager.runicauthenticator;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.Authenticator;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.RFC6238;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.Yubikey;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.UserDataSource;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.datasource.DirectoryUserDataSource;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.datasource.MySQLUserDataSource;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.datasource.SingleFileUserDataSource;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Getter
public final class Config {

    @Getter
    private final UserDataSource dataSource;

    @Getter
    private final Set<Authenticator> authenticators;

    @Getter
    private boolean enforceSameIPAuth;

    @Getter
    private boolean inventoryTampering;

    private String bungeePluginChannel;

    private final Map<String, String> messages;
    private String prefix = color("&8[&4Auth&8] ");

    public Config(Main auth, ConfigurationSection section) throws SQLException, IOException {
        List<String> authenticators = section.getStringList("authenticators");
        this.authenticators = new HashSet<>();
        if (authenticators.contains("2fa")) {
            auth.getLogger().info("Using RFC6238 (Google Authenticator 2FA) based authentication.");
            String tempServerIP;
            tempServerIP = section.getString("2fa.serverIp", section.getString("serverIp"));
            if (tempServerIP == null) {
                auth.getLogger().info("Your serverIp within your MCAuthenticator configuration is not set! It defaults " +
                        "'MCAuthenticator', but you should consider changing it to your server name!");
                tempServerIP = "MCAuthenticator";
            }
            this.authenticators.add(new RFC6238(tempServerIP, auth));
        }
        if (authenticators.contains("yubikey")) {
            Integer clientId = section.getInt("yubikey.clientId");
            String clientSecret = section.getString("yubikey.clientSecret");
            auth.getLogger().info("Using Yubikey based authenticator.");
            if(clientSecret == null || (clientId == -1 && "secret".equals(clientSecret))) {
                auth.getLogger().warning("The Yubikey configuration appears to be the default configuration/not configured!" +
                        " In order for the Yubikey authentication to work, you must retrieve a client id and secret.");
                auth.getLogger().warning("These may be retrieved from here: https://upgrade.yubico.com/getapikey/");
            }
            this.authenticators.add(new Yubikey(clientId, clientSecret, auth));
        }

        String backing = section.getString("dataBacking.type", "single");
        switch (backing) {
            case "single":
                this.dataSource = new SingleFileUserDataSource(new File(auth.getDataFolder(), section.getString("dataBacking.file", "playerData.json")));
                break;
            case "directory":
                this.dataSource = new DirectoryUserDataSource(new File(auth.getDataFolder(), section.getString("dataBacking.directory", "playerData")));
                break;
            case "mysql":
                ConfigurationSection mysql = section.getConfigurationSection("dataBacking.mysql");
                this.dataSource = new MySQLUserDataSource(mysql.getString("url", "jdbc:mysql://localhost:3306/db"),
                        mysql.getString("username"),
                        mysql.getString("password"),
                        mysql.getInt("queryTimeout", 0));
                break;
            default:
                throw new IllegalArgumentException("The dataBacking type '" + backing + "' doesn't exist.");
        }

        auth.getLogger().info("Using data source: " + dataSource.toString());

        this.enforceSameIPAuth = section.getBoolean("forceSameIPAuthentication", false);

        this.inventoryTampering = section.getBoolean("inventoryTampering", true);

        if (section.getBoolean("bungee.enabled", false))
            this.bungeePluginChannel = section.getString("bungee.channel", "MCAuthenticator");
        else this.bungeePluginChannel = null;

        this.messages = new HashMap<>();
        ConfigurationSection msgCfg = section.getConfigurationSection("messages");
        for (String key : msgCfg.getKeys(false)) {
            if (key.equals("prefix")) {
                this.prefix = color(msgCfg.getString(key));
            }
            this.messages.put(key, color(msgCfg.getString(key)));
        }
    }

    public String message(String key) {
        return prefix + messages.get(key);
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(message.split("\n"));
    }

    public void sendDirect(CommandSender sender, String raw) {
        send(sender, prefixFormat(raw));
    }

    public String prefixFormat(String s) {
        return prefix + color(s);
    }

    private String color(String s) {
        if (s == null) return null;
        return ChatColor.translateAlternateColorCodes('&', s);
    }


}