package me.Mixer.bluemapresidence;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    static Main cfg;
    public ReloadCommand(Main instance) {
        cfg = instance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            if(!sender.hasPermission("blueres.reload")) {
                sender.sendMessage(cfg.Placeholder((Player) sender, ChatColor.translateAlternateColorCodes('&',cfg.getConfig().getString("messages.no_permissions", "No permissions!"))));
                return true;
            }
        }

        cfg.reloadConfig();
        cfg.refreshPl();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', cfg.getConfig().getString("messages.reload_successfully", "Reloaded successfully.")));
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}
