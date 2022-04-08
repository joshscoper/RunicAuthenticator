package com.runicrealms.plugin.runicplayermanager.runicauthenticator.events;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

@RequiredArgsConstructor
public class InteractEvent implements Listener {
    private final Main instance;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        User u = instance.getCache().get(player.getUniqueId());

        if (u != null && u.authenticated()) return;

        event.setCancelled(true);
    }
}
