package com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth;

import com.google.zxing.WriterException;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.qrmap.ImageMapRenderer;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class RFC6238 implements Authenticator {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private static final String googleFormat =
            "https://www.google.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=" +
                    "otpauth://totp/%s@%s%%3Fsecret%%3D%s";
    private static final Pattern properInputPattern = Pattern.compile("[0-9][0-9][0-9] ?[0-9][0-9][0-9]");

    private final String serverIp;
    private final Map<User, String> temporarySecrets = new HashMap<>();
    private final Main main;

    public RFC6238(String serverIp, Main main) {
        this.serverIp = serverIp;
        this.main = main;
        validateServerTime();
    }

    @Override
    public boolean authenticate(User u, Player p, String input) {

        // GH-29 - Google Authenticator appears to have 2 3 digit numbers instead
        // of 1 6 digit number - some users misconstrue this by putting a space in
        // between the code. Allow this behavior

        if (input.charAt(3) == ' ')
            input = input.substring(0, 3) + input.substring(4);

        Integer code;
        try {
            code = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return false;
        }

        String authSecret = temporarySecrets.get(u);
        boolean temp = authSecret != null;
        if (!temp) {
            if (u.getUserData() == null || u.getUserData().getAuthType() != 0)
                return false; //Isn't 2fa.
            authSecret = u.getUserData().getSecret();
        }
        boolean result = gAuth.authorize(authSecret, code);
        if (temp && result) {
            temporarySecrets.remove(u);
            u.setUserInfo(authSecret, 0, p);
        }

        return result;
    }

    @Override
    public boolean isFormat(String s) {
        return properInputPattern.matcher(s).matches();
    }

    @Override
    public void initUser(User u, Player p) {
        String newKey = createNewKey();
        temporarySecrets.put(u, newKey);

        String msg = main.getC().message("sendAuthCode");
        msg = msg.replaceAll("%code%", newKey);
        try {
            msg = msg.replaceAll("%url%", getQRUrl(p.getName(), newKey));
        } catch (UnsupportedEncodingException ignored) { // will not be thrown
        }
        main.getC().send(p, msg);

        if (!main.getC().isInventoryTampering()) return;

        sendAndRenderMap(u, p, newKey);
    }

    private void sendAndRenderMap(User u, Player p, String newKey) {
        ImageMapRenderer mapRenderer;
        try {
            mapRenderer = new ImageMapRenderer(p.getName(), newKey, serverIp);
        } catch (WriterException e) {
            main.getC().sendDirect(p,
                    "&cThere was an error rendering your 2FA QR code!");
            main.handleException(e);
            return;
        }

        if (!u.isInventoryStored())
            u.storeInventory(p);

        MapView map = Bukkit.createMap(p.getWorld());
        map.getRenderers().forEach(map::removeRenderer);
        map.addRenderer(mapRenderer);

        ItemStack mapItem = new ItemStack(Material.MAP, 1, map.getId());
        p.getInventory().setHeldItemSlot(0);
        p.getInventory().setItemInMainHand(mapItem);
        p.sendMap(map);
    }

    @Override
    public void quitUser(User u, Player p) {
        temporarySecrets.remove(u);
        u.reverseInventory(p);
    }

    private String getQRUrl(String username, String secret) throws UnsupportedEncodingException {
        if (secret == null)
            return null;
        return String.format(googleFormat, username, URLEncoder.encode(serverIp, "UTF-8"), secret);
    }

    private String createNewKey() {
        return gAuth.createCredentials().getKey();
    }

    private void validateServerTime() {
        // Since 1.0.2
        try {
            String TIME_SERVER = "http://icanhazepoch.com";
            HttpURLConnection timeCheckQuery =
                    (HttpURLConnection) new URL(TIME_SERVER).openConnection();
            timeCheckQuery.setReadTimeout(4000);
            timeCheckQuery.setConnectTimeout(4000);
            timeCheckQuery.addRequestProperty("User-Agent", "Main (time check)");
            timeCheckQuery.connect();
            int responseCode = timeCheckQuery.getResponseCode();
            if (responseCode != 200) {
                main.getLogger().info("Could not validate the server's time! Ensure" +
                        " that the server's time is within specification!");
                return;
            }
            byte[] response = new byte[1024]; // Response should never be over 1kB
            InputStream inputStream = timeCheckQuery.getInputStream();
            int len = inputStream.read(response);
            String rsp = new String(response, 0, len, Charset.defaultCharset()).trim();
            Long unixSeconds = Long.parseLong(rsp);
            long myUnixSeconds = (System.currentTimeMillis() / 1000);
            int diff = (int) (unixSeconds - myUnixSeconds);
            if (Math.abs(diff) > 30) {
                main.getLogger().severe("Your server's Unix time is off by "
                        + Math.abs(diff) + " seconds! 2FA may not work! Please "
                        + "correct this to make sure 2FA works.");
            }
        } catch (IOException | NumberFormatException e) {
            main.getLogger().log(Level.WARNING, "Was not able to validate the server's" +
                    " Unix time against an external service: Please ensure your" +
                    " server's time is set correctly or 2FA may not operate right.", e);
        }
    }
}
