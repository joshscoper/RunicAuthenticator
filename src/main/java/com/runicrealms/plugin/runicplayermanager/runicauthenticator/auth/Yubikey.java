package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.UserData;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import org.bukkit.entity.Player;

public class Yubikey implements Authenticator {

    private final YubicoClient authClient;
    private final Main a;

    public Yubikey(Integer clientId, String secret, Main a) {
        this.a = a;
        this.authClient = YubicoClient.getClient(clientId, secret);
    }

    @Override
    public boolean authenticate(User user, Player p, String input) throws Exception {
        UserData userData = user.getUserData();
        if (userData != null && userData.getSecret() != null
                && userData.getAuthType() != 1) return false;
        String publicId = userData != null ? userData.getSecret() : null;
        try {
            VerificationResponse resp = authClient.verify(input);
            if (resp.isOk() && (publicId == null ||
                    resp.getPublicId().equals(publicId))) {
                if (publicId == null) {
                    user.setUserInfo(resp.getPublicId(), 1, p);
                }
                return true;
            } else {
                return false;
            }
        } catch (YubicoValidationFailure yvf) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (YubicoVerificationException e) {
            return false;
        }
    }

    @Override
    public boolean isFormat(String s) {
        return YubicoClient.isValidOTPFormat(s);
    }

    @Override
    public void initUser(User u, Player p) {
        // Send a pretty message
        a.getC().send(p, a.getC().message("yubikeyInitPrompt"));
    }

    @Override
    public void quitUser(User u, Player p) {
        // Don't care
    }
}
