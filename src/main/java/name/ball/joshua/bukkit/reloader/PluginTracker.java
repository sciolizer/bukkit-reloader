package name.ball.joshua.bukkit.reloader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

public class PluginTracker implements Callable<List<Watcher.JarReplacement>> {

    private final Map<String,Watcher> watchers = new TreeMap<String, Watcher>();
    private final List<String> droppedPaths = new ArrayList<String>();

    public synchronized void addPath(String path) {
        watchers.put(path, new Watcher(path));
    }

    public synchronized Set<String> getPaths() {
        return new TreeSet<String>(watchers.keySet());
    }

    public synchronized void removePath(String path) {
        droppedPaths.add(path);
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
        }
        return replacements;
    }

}
