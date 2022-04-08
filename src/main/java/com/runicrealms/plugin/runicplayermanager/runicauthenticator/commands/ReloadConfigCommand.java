package com.runicrealms.plugin.runicplayermanager.runicauthenticator.commands;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public final class ReloadConfigCommand extends AuthCommand {
    public ReloadConfigCommand(Main instance) {
        super(instance, "reload", "runicauth.reload", "Reloads the configuration and data, and reauthenticates everyone if needed.");
    }

    @Override
    public boolean execute(Command command, CommandSender commandSender, String[] args) {
        Main i = getInstance();
        i.reload();

        i.getC().sendDirect(commandSender, "&7Successfully reloaded configuration and datasources!\n" +
                "&7All authenticated users will now be forced to reauthenticate.");
        return true;
    }
}
