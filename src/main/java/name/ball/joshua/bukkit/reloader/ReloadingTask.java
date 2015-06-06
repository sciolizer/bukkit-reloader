package name.ball.joshua.bukkit.reloader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// doesn't work very well with the debugger?
public class ReloadingTask implements Runnable {

    private final PluginTracker pluginTracker;
    private final Plugin plugin;

    public ReloadingTask(PluginTracker pluginTracker, Plugin plugin) {
        this.pluginTracker = pluginTracker;
        this.plugin = plugin;
    }

    private final Map<String,Plugin> loadedPlugins = new LinkedHashMap<String, Plugin>();

    @Override
    public void run() {
        final List<Watcher.JarReplacement> jarReplacements;
        try {
            jarReplacements = pluginTracker.call();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (jarReplacements != null && !jarReplacements.isEmpty()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    PluginManager pluginManager = Bukkit.getPluginManager();
                    for (Watcher.JarReplacement jarReplacement : jarReplacements) {
                        File originalJar = jarReplacement.originalJar;
                        if (originalJar != null) {
                            String originalPath = originalJar.getAbsolutePath();
                            if (loadedPlugins.containsKey(originalPath)) {
                                // Might be able to work around the problem of commands not getting re-pointed by
                                // hooking PlayerCommandPreprocessEvent
//                                Bukkit.getPluginCommand("").
//                                pluginManager.clearPlugins();
                                pluginManager.disablePlugin(loadedPlugins.get(originalPath));
                                loadedPlugins.remove(originalPath);
                            }
                            if (!originalJar.delete()) {
                                System.err.println("Warning: unable to delete: " + originalPath);
                            }
                        }
                        File newJar = jarReplacement.newJar;
                        if (newJar != null) {
                            Plugin p;
                            String newPath = newJar.getAbsolutePath();
                            try {
                                p = pluginManager.loadPlugin(newJar);
                            } catch (InvalidPluginException e) {
                                warnOfPluginLoadFailure(newPath, e);
                                continue;
                            } catch (InvalidDescriptionException e) {
                                warnOfPluginLoadFailure(newPath, e);
                                continue;
                            } catch (UnknownDependencyException e) {
                                warnOfPluginLoadFailure(newPath, e);
                                continue;
                            }
                            if (p != null) {
                                loadedPlugins.put(newPath, p);
                                pluginManager.enablePlugin(p);
                            }
                        }
                    }
                }
            });
        }
    }

    private void warnOfPluginLoadFailure(String newPath, Exception e) {
        System.err.println("Warning: unable to load plugin " + newPath + " (" + e.getClass().getSimpleName() + "): " + e.getMessage());
    }
}
