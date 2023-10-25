package me.Mixer.bluemapresidence;

import com.bekvon.bukkit.residence.event.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EventList implements Listener {
    static Main plugin;
    public EventList(Main instance) {
        plugin = instance;
    }

    @EventHandler(priority= EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceCreate(ResidenceCreationEvent event) {
        plugin.refreshPl();
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceFlagChange(ResidenceFlagChangeEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceDelete(ResidenceDeleteEvent event) {
        plugin.refreshPl();
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceOwnerChange(ResidenceOwnerChangeEvent event) {
        plugin.refreshPl();
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceRename(ResidenceRenameEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceAreaAdd(ResidenceAreaAddEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceAreaDelete(ResidenceAreaDeleteEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceSizeChange(ResidenceSizeChangeEvent event) {
        plugin.refreshPl();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onResidenceSubzoneCreate(ResidenceSubzoneCreationEvent event) {
        plugin.refreshPl();
    }
}
