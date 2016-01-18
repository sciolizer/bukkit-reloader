package name.ball.joshua.bukkit.reloader;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Set;

public class Reloader extends JavaPlugin {

    private PluginTracker pluginTracker = new PluginTracker(this);
    private ReloadingTask reloadingTask;

    public void onDisable() {
        reloadingTask.stop();
    }

    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.options().copyHeader(true);
        saveConfig();

        List<?> watchers = config.getList("watchers");
        for (Object watcher : watchers) {
            pluginTracker.addPath((String)watcher);
        }

        getCommand("reloader").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if (!(commandSender instanceof ConsoleCommandSender)) {
                    commandSender.sendMessage("reloader can only be run from the server console");
                    return false;
                }
                if (strings.length == 0) {
                    commandSender.sendMessage("Available subcommands are: 'list', 'watch', and 'ignore', e.g. 'reloader list'");
                    return false;
                }
                String subcommand = strings[0];
                if ("list".equals(subcommand)) {
                    Set<String> paths = pluginTracker.getPaths();
                    if (paths.isEmpty()) {
                        commandSender.sendMessage("No paths are currently being watched");
                        return true;
                    }
                    for (String watchedPath : paths) {
                        commandSender.sendMessage(watchedPath);
                    }
                    return true;
                } else if ("watch".equals(subcommand)) {
                    if (strings.length <= 1) {
                        commandSender.sendMessage("Please specify a folder, e.g. 'reloader watch /home/username/myPlugin/target/classes'");
                        return false;
                    }
                    String path = strings[1];
                    pluginTracker.addPathAndSaveConfig(path);
                    if (strings.length > 2) {
                        commandSender.sendMessage("Now watching path '" + path + "'.");
                    }
                    if (!new File(path).exists()) {
                        commandSender.sendMessage("Warning: path '" + path + "' does not exist.");
                    }
                    return true;
                } else if ("ignore".equals(subcommand)) {
                    if (strings.length <= 1) {
                        commandSender.sendMessage("Please specify a folder, e.g. 'reloader ignore /home/username/myPlugin/target/classes");
                        return false;
                    }
                    String path = strings[1];
                    Set<String> paths = pluginTracker.getPaths();
                    if (!paths.contains(path)) {
                        commandSender.sendMessage("I am not watching '" + path + "'. Type 'reloader list' to see what I am watching.");
                        return true;
                    }
                    pluginTracker.removePath(path);
                    if (strings.length > 2) {
                        commandSender.sendMessage("No longer watching path '" + path + "'.");
                    }
                    return true;
                } else {
                    commandSender.sendMessage("Unrecognized subcommand '" + subcommand + "'. Try 'list', 'watch', or 'ignore'.");
                }
                return true;
            }
        });

        reloadingTask = new ReloadingTask(pluginTracker, this);
        reloadingTask.start();
    }

}
