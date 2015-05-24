Reloader plugin
===============

This is a tool for developing bukkit plugins faster. At the craft bukkit console, type:

    /reloader watch /path/to/my/bukkit/plugin/target/classes

When reloader sees any change in the contents of the given path, it creates a new jar file and instructs bukkit
to load the new jar as a plugin.

This is still very much a work in progress. It doesn't handle plugins that have commands very well, and it
leaks memory.

Installation
============

```
git clone https://github.com/Bukkit/Bukkit.git
cd Bukkit
git checkout 1.8.1-R4
mvn clean install
cd ..

git clone https://github.com/sciolizer/bukkit-reloader.git
cd bukkit-reloader
mvn clean package
cp target/*.jar ~/CraftBukkit/plugins
```

Then start craft-bukkit, and run `/reloader`.
