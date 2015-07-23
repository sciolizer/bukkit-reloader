package name.ball.joshua.bukkit.reloader;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

// doesn't work very well with the debugger?
public class ReloadingTask implements Runnable {

    private final PluginTracker pluginTracker;
    private final Plugin plugin;
    private final PluginRemover pluginRemover;

    public ReloadingTask(PluginTracker pluginTracker, Plugin plugin, PluginRemover pluginRemover) {
        this.pluginTracker = pluginTracker;
        this.plugin = plugin;
        this.pluginRemover = pluginRemover;
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
                                pluginRemover.removePlugin(pluginManager, loadedPlugins.get(originalPath));
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

    // Based on https://github.com/alexandreblin/PluginReloader/blob/master/src/net/madjawa/pluginreloader/PluginReloader.java
    private static class PluginRemover {

        private final List<Plugin> plugins;
        private final Map<String, Plugin> lookupNames;
        private final Map<Type, SortedSet<RegisteredListener>> listeners;
        private final SimpleCommandMap commandMap;

        public PluginRemover() throws IllegalAccessException, NoSuchFieldException {
            PluginManager pluginManager = Bukkit.getPluginManager();
            Class<? extends PluginManager> pluginManagerClass = pluginManager.getClass();

            Field pluginsField = pluginManagerClass.getDeclaredField("plugins");
            Field lookupNamesField = pluginManagerClass.getDeclaredField("lookupNames");
            Field listenersField = pluginManagerClass.getDeclaredField("listeners");
            Field commandMapField = pluginManagerClass.getDeclaredField("commandMap");

            pluginsField.setAccessible(true);
            lookupNamesField.setAccessible(true);
            listenersField.setAccessible(true);
            commandMapField.setAccessible(true);

            plugins = (List<Plugin>) pluginsField.get(pluginManager);
            lookupNames = (Map<String, Plugin>) lookupNamesField.get(pluginManager);
            listeners = (Map<Type, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
            commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);
        }

        private void removePlugin(Plugin pl) {

            PluginManager manager = Bukkit.getPluginManager();

            // disable the plugin itself
            manager.disablePlugin(pl);

            // removing all traces of the plugin in the private structures (so it won't appear in the plugin list twice)
            if (plugins != null && plugins.contains(pl)) {
                plugins.remove(pl);
            }
            
            String pluginName = pl.getDescription().getName();

            if (lookupNames != null && lookupNames.containsKey(pluginName)) {
                lookupNames.remove(pluginName);
            }

            // removing registered listeners to avoid registering them twice when reloading the plugin
            if (listeners != null) {
                for (SortedSet<RegisteredListener> set : listeners.values()) {
                    for (Iterator<RegisteredListener> it = set.iterator(); it.hasNext();) {
                        RegisteredListener value = it.next();

                        if (value.getPlugin() == pl) {
                            it.remove();
                        }
                    }
                }
            }

            // removing registered commands, if we don't do this they can't get re-registered when the plugin is reloaded
            if (commandMap != null) {
                for (Iterator<Map.Entry<String, Command>> it = knownCommands.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Command> entry = it.next();

                    if (entry.getValue() instanceof PluginCommand) {
                        PluginCommand c = (PluginCommand) entry.getValue();

                        if (c.getPlugin() == pl) {
                            c.unregister(commandMap);

                            it.remove();
                        }
                    }
                }
            }

            try {
                List<Permission> permissionlist = pl.getDescription().getPermissions();
                for (Permission aPermissionlist : permissionlist) {
                    manager.removePermission(aPermissionlist.toString());
                }
            } catch (NoSuchMethodError e) {
                // Do nothing
            }

            // ta-da! we're done (hopefully)
            // I don't know if there are more things that need to be reset
            // I'll take a more in-depth look into the bukkit source if it doesn't work well
        }

    }

    private void warnOfPluginLoadFailure(String newPath, Exception e) {
        System.err.println("Warning: unable to load plugin " + newPath + " (" + e.getClass().getSimpleName() + "): " + e.getMessage());
    }
}
