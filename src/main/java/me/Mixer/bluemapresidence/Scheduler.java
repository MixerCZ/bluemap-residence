package me.Mixer.bluemapresidence;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class Scheduler {

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            if (Bukkit.getVersion().contains("Folia") || Bukkit.getVersion().contains("Luminol")) return true;
            return false;
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia())
            Bukkit.getGlobalRegionScheduler()
                    .execute(Main.getInstance(), runnable);

        else
            Bukkit.getScheduler().runTask(Main.getInstance(), runnable);
    }

    public static Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia())

            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(Main.getInstance(), t -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskTimer(Main.getInstance(), runnable, delayTicks, periodTicks));
    }

    public static Task runAsync(Runnable runnable) {
        if (isFolia())
            return new Task(Bukkit.getAsyncScheduler().runNow(Main.getInstance(), t -> runnable.run()));
        else return new Task(Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), runnable));
    }
    public static class Task {

        private Object foliaTask;
        private BukkitTask bukkitTask;

        Task(Object foliaTask) {
            this.foliaTask = foliaTask;
        }

        Task(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }

        public void cancel() {
            if (foliaTask != null)
                ((ScheduledTask) foliaTask).cancel();
            else
                bukkitTask.cancel();
        }
    }
}