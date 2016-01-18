package name.ball.joshua.bukkit.reloader;

import com.google.common.io.Files;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// doesn't work very well with the debugger?
public class ReloadingTask implements Runnable {

    private final PluginTracker pluginTracker;
    private final Plugin plugin;
    private BukkitTask bukkitTask;
    private volatile boolean stopped = false;
    private String pluginFolder;
    private boolean firstRun = true;

    public ReloadingTask(PluginTracker pluginTracker, Plugin plugin) {
        this.pluginTracker = pluginTracker;
        this.plugin = plugin;
        this.pluginFolder = plugin.getDataFolder().getParent();
    }

    public void start() {
        bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, 20l, 20l);
    }

    public synchronized void stop() {
        stopped = true;
        bukkitTask.cancel();
    }

    private final Map<String,Plugin> loadedPlugins = new LinkedHashMap<String, Plugin>();

    @Override
    public synchronized void run() {
        if (stopped) return;
        final List<Watcher.JarReplacement> jarReplacements;
        try {
            jarReplacements = pluginTracker.call();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (firstRun) {
            firstRun = false;
            return;
        }
        if (jarReplacements != null && !jarReplacements.isEmpty()) {
            for (Watcher.JarReplacement jarReplacement : jarReplacements) {
                File file = new File(pluginFolder, "watched.jar"); // todo: allow for more than one jar
                if (file.exists()) {
                    if (!file.delete()) {
                        System.err.println("Warning: unable to delete: " + file);
                    }
                }
                try {
                    Files.copy(jarReplacement.newJar, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.getServer().reload();
                }
            });
        }
    }

    private void warnOfPluginLoadFailure(String newPath, Exception e) {
        System.err.println("Warning: unable to load plugin " + newPath + " (" + e.getClass().getSimpleName() + "): " + e.getMessage());
    }
}
