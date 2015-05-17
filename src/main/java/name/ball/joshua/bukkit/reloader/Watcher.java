package name.ball.joshua.bukkit.reloader;

import java.io.File;
import java.io.IOException;

public class Watcher {

    private final JarCreator jarCreator = new JarCreator();

    private final String rootPath;
    private final Rescanner rescanner;

    private File previousJar;

    public Watcher(String rootPath) {
        this.rootPath = rootPath;
        this.rescanner = new Rescanner(rootPath);
    }

    public JarReplacement getNewJar() throws IOException {
        if (!rescanner.hasChanged()) {
            return null;
        }
        File outputJar;
        while (true) {
            outputJar = makeJar();
            if (rescanner.hasChanged()) {
                if (!outputJar.delete()) {
                    System.err.println("Warning: unable to delete jar: " + outputJar.getAbsolutePath());
                }
            } else {
                break;
            }
        }
        try {
            return new JarReplacement(previousJar, outputJar);
        } finally {
            previousJar = outputJar;
        }
    }

    private File makeJar() throws IOException {
        File outputJar;
        for (long i = System.currentTimeMillis(); true; i++) {
            outputJar = new File("/tmp/plugin-" + i + ".jar");
            if (!outputJar.exists()) {
                break;
            }
        }
        jarCreator.createJar(new File(rootPath), outputJar);
        return outputJar;
    }

    public JarReplacement close() {
        if (previousJar == null) return null;
        return new JarReplacement(previousJar, null);
    }

    public static class JarReplacement {
        public final File originalJar;
        public final File newJar;

        public JarReplacement(File originalJar, File newJar) {
            this.originalJar = originalJar;
            this.newJar = newJar;
        }
    }

}
