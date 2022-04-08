package com.runicrealms.plugin.runicplayermanager.runicauthenticator.commands;

import com.runicrealms.plugin.runicplayermanager.runicauthenticator.Main;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public abstract class AuthCommand {
    @Getter
    private final Main instance;
    @Getter
    private final String name;
    @Getter
    private final String permission;
    @Getter
    private final String desc;

    public AuthCommand(Main instance, String name, String permission, String description) {
        this.instance = instance;
        this.name = name;
        this.permission = permission;
        this.desc = description;
    }

    public abstract boolean execute(Command command, CommandSender commandSender, String[] args);

    public boolean isConsole(CommandSender s) {
        return s instanceof ConsoleCommandSender;
    }
}
