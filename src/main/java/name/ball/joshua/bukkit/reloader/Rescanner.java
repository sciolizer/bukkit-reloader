package name.ball.joshua.bukkit.reloader;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class Rescanner {

    private final String rootFolder;
    private Map<String,Long> modificationTimes;

    public Rescanner(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public boolean hasChanged() {
        Map<String, Long> newModificationTimes = scan();
        try {
            if (modificationTimes == null && newModificationTimes == null) {
                return false;
            }
            if (modificationTimes == null || newModificationTimes == null) {
                return true;
            }
            for (Map.Entry<String, Long> entry : newModificationTimes.entrySet()) {
                Long previousModificationTime = modificationTimes.get(entry.getKey());
                if (previousModificationTime == null || !previousModificationTime.equals(entry.getValue())) {
                    return true;
                }
            }
            return false;
        } finally {
            modificationTimes = newModificationTimes;
        }
    }

    private Map<String,Long> scan() {
        File rootFile = new File(rootFolder);
        if (!rootFile.exists()) {
            return null;
        }
        return listFileTree(rootFile);
    }

    private Map<String,Long> listFileTree(File dir) {
        Map<String,Long> result = new LinkedHashMap<String, Long>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File entry : files) {
                if (entry.isDirectory()) {
                    result.putAll(listFileTree(entry));
                } else if (entry.isFile()) {
                    result.put(entry.getAbsolutePath(), entry.lastModified());
                } else {
                    System.err.println("Warning: unrecognized filetype: " + entry);
                }
            }
        }
        return result;
    }
}
