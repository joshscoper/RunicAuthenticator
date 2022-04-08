package com.runicrealms.plugin.runicplayermanager.runicauthenticator.commands;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;

public final class EnableCommand extends AuthCommand {
    public EnableCommand(Main instance) {
        super(instance, "enable", "runicauth.enable", "Enables 2FA on an account");
    }

    @Override
    public boolean execute(final Command command, final CommandSender commandSender, String[] args) {
        if (args.length == 0) {
            //Self
            if (!(commandSender instanceof Player)) {
                getInstance().getC().sendDirect(commandSender, "&cYou must specify a player to enable 2FA on.\n" +
                        "&c    /auth enable <player>");
                return true;
            }

            Player sndr = ((Player) commandSender);
            User u = getInstance().getCache().get(sndr.getUniqueId());
            if (u.is2fa()) {
                getInstance().getC().send(commandSender, getInstance().getC().message("alreadyEnabled"));
                return true;
            }

            //Start the 2fa process.
            u.init2fa(sndr);
        } else if (args.length == 1) {
            if (!commandSender.hasPermission("mcauthenticator.enable.other") && !isConsole(commandSender)) {
                getInstance().getC().sendDirect(commandSender, "&cYou are not permitted to enable other people's 2FA!");
                return true;
            }
            final String playerQuery = args[0];
            getInstance().async(new Runnable() {
                @Override
                public void run() {
                    final OfflinePlayer player = Bukkit.getOfflinePlayer(playerQuery);
                    if (player == null) {
                        getInstance().getC().sendDirect(commandSender, "&cThe player &4'" + playerQuery + "'&c does not exist!");
                        return;
                    }

                    if (player.isOnline()) {
                        final User u = getInstance().getCache().get(player.getUniqueId());
                        if (u.is2fa()) {
                            getInstance().getC().sendDirect(commandSender, "&cThe player &4'" + playerQuery + "'&c already has 2FA enabled.");
                            return;
                        }
                        getInstance().sync(new Runnable() {
                            @Override
                            public void run() {
                                getInstance().getC().send((CommandSender) player, getInstance().getC().message("otherEnable").replaceAll("%player%", commandSender.getName()));
                                getInstance().getC().sendDirect(commandSender, "&7You have enabled 2FA on "+player.getName()+"'s account.");
                                u.init2fa((Player) player);
                            }
                        });
                    } else {
                        //Player isn't online, so lets just kinda 'help them along'
                        //Lets first check if they already have a record.

                        UserData d;

                        try {
                            d = getInstance().getDataSource().getUser(player.getUniqueId());
                        } catch (IOException | SQLException e) {
                            commandSender.sendMessage(ChatColor.RED + "There was an issue retrieving the userdata. Check console.");
                            getInstance().handleException(e);
                            return;
                        }

                        if (d == null) {
                            //No 2fa, lets enable a blank data
                            getInstance().getDataSource().createUser(player.getUniqueId());
                            getInstance().getC().sendDirect(commandSender, "&7Enabled 2FA for " + player.getName() + ". They will be set up next time they log in.");
                            getInstance().save();
                        } else {
                            getInstance().getC().sendDirect(commandSender, "&4'" + player.getName() + "'&c already has 2FA enabled!");
                        }
                    }
                }
            });
        } else {
            getInstance().getC().sendDirect(commandSender, "&c Invalid usage: /auth enable" + (commandSender.hasPermission("mcauthenticator.enable.other") ? " [player]" : ""));
        }
        return true;
    }
}
