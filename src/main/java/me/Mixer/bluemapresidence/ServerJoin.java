package me.Mixer.bluemapresidence;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ServerJoin implements Listener {
	
	static Main plugin;
	public ServerJoin(Main instance) {
		plugin = instance;
	}

	@EventHandler
	public void Join(PlayerJoinEvent e)  {
		if(e.getPlayer().hasPermission("blueres.updatecheck")) {
			new UpdateChecker(plugin, 107389).getVersion(version -> {
				if(!plugin.getDescription().getVersion().equalsIgnoreCase(version)) {
					e.getPlayer().sendMessage(plugin.Placeholder(e.getPlayer(), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.new_update", "New update available."))));
					e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + plugin.BMResLinkSpigot));
				}
			});
		}
	}
	
}
