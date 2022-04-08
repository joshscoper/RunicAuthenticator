package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.Authenticator;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class User {

    public User(UUID playerId, UserData userData,
                Main main) {
        this.playerId = playerId;
        this.userData = userData;
        this.main = main;
    }

    private final UUID playerId;

    @Getter
    private UserData userData;
    private final Main main;

    @Getter
    private boolean isInit = false;

    //Whether or not the user is viewed as 'authenticated'
    private boolean authenticated = false;
    private boolean lastAuthStatusUpstream = false;
    private boolean hasSentAuthStatusRemote = false;

    private ItemStack[] inventory = null;

    public boolean authenticated() {
        return !isInit && (userData == null || authenticated);
    }

    public boolean mustSetUp2FA() {
        return userData != null && userData.getSecret() == null;
    }

    public boolean authenticate(Player p) {
        if (authenticated()) return true;
        if (!main.getC().isEnforceSameIPAuth() &&
                userData.getLastAddress() != null &&
                userData.getLastAddress().equals(p.getAddress().getAddress())) {

            isInit = false;
            authenticated = true;
        }
        sendAuthenticatedUpstream(p);
        return authenticated;
    }

    public boolean is2fa() {
        return userData != null;
    }

    public boolean authenticate(String message, Player player)
            throws Exception {
        boolean authenticate = false;
        for (Authenticator a : main.getAuthenticators()) {
            if (a.isFormat(message)) {
                authenticate = a.authenticate(this, player, message);
                if (authenticate) break;
            }
        }

        if (!authenticate) {
            sendAuthenticatedUpstream(player);
            return false;
        }

        isInit = false;
        authenticated = true;

        reverseInventory(player);
        userData.setLastAddress(player.getAddress().getAddress());
        userData.setLocked(player.hasPermission("main.lock"));
        main.save();
        sendAuthenticatedUpstream(player);
        return true;
    }

    public void remoteAuthenticated() {
        isInit = false;
        authenticated = true;
    }

    public void invalidateKey(Player p) {
        main.getDataSource().destroyUser(userData.getId());
        userData = null;
        authenticated = false;
        sendAuthenticatedUpstream(p);
    }

    public boolean init2fa(Player p) {
        if (userData != null && userData.getSecret() != null) { //Already has a secret
            return false;
        }

        for (Authenticator a : main.getAuthenticators()) {
            a.initUser(this, p);
        }

        isInit = true;
        sendAuthenticatedUpstream(p);

        return true;
    }

    public void setUserInfo(String secret, int authtype, Player p) {
        if (userData == null)
            userData = main.getDataSource().createUser(playerId);
        userData.setSecret(secret, authtype);
        sendAuthenticatedUpstream(p);
    }

    public boolean isLocked(Player p) {
        return userData != null && userData.isLocked(p);
    }

    public boolean isInventoryStored() {
        return inventory != null;
    }

    public void storeInventory(Player p) {
        if (inventory != null)
            throw new IllegalStateException("Cannot double store inventory!");
        inventory = p.getInventory().getContents();
        p.getInventory().setContents(new ItemStack[36]);
    }

    public void reverseInventory(Player p) {
        if (inventory != null) {
            p.getInventory().setContents(inventory);
            inventory = null;
        }
    }

    public void sendAuthenticatedUpstream(final Player p) {
        final String authChannel = main.getC().getBungeePluginChannel();
        if (authChannel != null && p != null) {
            final boolean authStatus = authenticated();
            if (!hasSentAuthStatusRemote || lastAuthStatusUpstream != authStatus) {
                hasSentAuthStatusRemote = true;
                main.sync(new Runnable() {
                    @Override
                    public void run() {
                        p.sendPluginMessage(main, authChannel, (authStatus ? new byte[]{0x00} : new byte[]{0x01}));
                    }
                }, 3); /// 3 ticks seems to be the magic time. For some reason, we can't send upstream immediately on log in....?
                lastAuthStatusUpstream = authStatus;
            }
        }
    }
}
