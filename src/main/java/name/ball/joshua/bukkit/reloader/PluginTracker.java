package name.ball.joshua.bukkit.reloader;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

public class PluginTracker implements Callable<List<Watcher.JarReplacement>> {

    private final Plugin plugin;
    private final Map<String,Watcher> watchers = new TreeMap<String, Watcher>();
    private final List<String> droppedPaths = new ArrayList<String>();

    public PluginTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void addPath(String path) {
        watchers.put(path, new Watcher(path));
    }

    public synchronized void addPathAndSaveConfig(String path) {
        addPath(path);
        updateConfig(watchers.keySet());
    }

    public synchronized Set<String> getPaths() {
        return new TreeSet<String>(watchers.keySet());
    }

    public synchronized void removePath(String path) {
        droppedPaths.add(path);
        List<String> ws = new ArrayList<String>(watchers.keySet());
        ws.removeAll(droppedPaths);
        updateConfig(ws);
    }

    private void updateConfig(Collection<String> paths) {
        plugin.getConfig().set("watchers", new ArrayList<String>(paths));
        plugin.saveConfig();
    }

    public List<Watcher.JarReplacement> call() throws IOException {
        List<Watcher.JarReplacement> replacements = new ArrayList<Watcher.JarReplacement>();
        Map<String, Watcher> snapshot;
        synchronized (this) {
            snapshot = new TreeMap<String, Watcher>(watchers);
        }
        for (Watcher watcher : snapshot.values()) {
            Watcher.JarReplacement newJar = watcher.getNewJar();
            if (newJar != null) {
                replacements.add(newJar);
            }
        }
        synchronized (this) {
            for (String droppedPath : droppedPaths) {
                Watcher watcher = watchers.get(droppedPath);
                if (watcher != null) {
                    Watcher.JarReplacement jarReplacement = watcher.close();
                    if (jarReplacement != null) {
                        replacements.add(jarReplacement);
                    }
                    watchers.remove(droppedPath);
                }
            }
            droppedPaths.clear();
        }
        return replacements;
    }

}
