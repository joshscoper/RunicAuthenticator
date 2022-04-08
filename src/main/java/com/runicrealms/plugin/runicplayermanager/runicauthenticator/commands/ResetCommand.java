package com.runicrealms.plugin.runicplayermanager.runicauthenticator.commands;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.User;
import com.runicrealms.plugin.runicplayermanager.runicauthenticator.auth.authuser.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;

public final class ResetCommand extends AuthCommand {
    public ResetCommand(Main instance) {
        super(instance, "reset", "runicauth.reset", "Resets a player's 2FA code.");
    }

    @Override
    public boolean execute(final Command command, final CommandSender commandSender, String[] args) {
        if (args.length == 0) {
            //Self
            if (!(commandSender instanceof Player)) {
                getInstance().getC().sendDirect(commandSender, "&cYou must specify a player to reset 2FA on.\n" +
                        "&c    /auth reset <player>");
                return true;
            }

            Player sndr = ((Player) commandSender);
            User u = getInstance().getCache().get(sndr.getUniqueId());
            if (!(u.is2fa())) {
                getInstance().getC().send(commandSender, getInstance().getC().message("resetDisabled"));
                return true;
            }

            //If the user isn't authenticated, they can't run commands.

            if (u.isLocked(sndr)) {
                getInstance().getC().send(commandSender, getInstance().getC().message("resetForced"));
                return true;
            }

            u.invalidateKey(sndr);
            u.init2fa(sndr);
            getInstance().getC().send(commandSender, getInstance().getC().message("selfReset"));
        } else if (args.length == 1) {
            if (!commandSender.hasPermission("mcauthenticator.reset.other") && !isConsole(commandSender)) {
                getInstance().getC().sendDirect(commandSender, "&cYou are not permitted to enable other people's 2FA!");
                return true;
            }
            final String playerQuery = args[0];
            getInstance().async(new Runnable() {
                @Override
                public void run() {
                    //getOfflinePlayer can make a web query, and isn't safe to run in sync.
                    final OfflinePlayer player = Bukkit.getOfflinePlayer(playerQuery);
                    if (player == null) {
                        getInstance().getC().sendDirect(commandSender, "&cThe player &4'" + playerQuery + "'&c does not exist!");
                        return;
                    }

                    if (player.isOnline()) {
                        final User u = getInstance().getCache().get(player.getUniqueId());
                        if (!u.is2fa()) {
                            getInstance().getC().sendDirect(commandSender, "&cThe player &4'" + playerQuery + "'&c does not have 2FA enabled.");
                            return;
                        }

                        if (u.mustSetUp2FA() || !u.authenticated()) {
                            getInstance().getC().sendDirect(commandSender, "&cThe player is already in reset mode.");
                            return;
                        }

                        getInstance().sync(new Runnable() {
                            @Override
                            public void run() {
                                u.invalidateKey((Player) player);
                                u.init2fa((Player) player);
                                getInstance().getC().send(commandSender, getInstance().getC().message("otherReset").replaceAll("%player%", commandSender.getName()));
                                getInstance().getC().sendDirect(commandSender, "&7You have reset 2FA on " + player.getName() + "'s account.");
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
                            getInstance().getC().sendDirect(commandSender, "&4" + player.getName() + " doesn't have 2FA enabled.");
                        } else {
                            if (d.getSecret() == null) {
                                getInstance().getC().sendDirect(commandSender, "&4" + player.getName() + " is already in reset mode. They will reset their code when they next log in.");
                            } else {
                                if (d.isLocked(null) && !(commandSender instanceof ConsoleCommandSender)) {
                                    getInstance().getC().sendDirect(commandSender, "&c This user has the locked permission! You cannot reset 2FA for this person unless you are in console!");
                                } else {
                                    d.setSecret(null, -1);
                                    d.setLastAddress(null);
                                    getInstance().getC().sendDirect(commandSender, "&7You have reset this player's 2FA. They will reset their code when they next log in.");
                                    getInstance().save();
                                }
                            }
                        }
                    }
                }
            });
        } else {
            getInstance().getC().sendDirect(commandSender, "&c Invalid usage: /auth reset" + (commandSender.hasPermission("mcauthenticator.reset.other") ? " [player]" : ""));
        }
        return true;
    }
}
