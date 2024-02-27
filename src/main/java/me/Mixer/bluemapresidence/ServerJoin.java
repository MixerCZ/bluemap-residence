package me.Mixer.bluemapresidence;

import org.bukkit.ChatColor;
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
				if(!plugin.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
					e.getPlayer().sendMessage(plugin.Placeholder(e.getPlayer(), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.new_update", "New update available."))));
					e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + plugin.BMResLinkSpigot));
				}
			});
		}
	}
	
}
