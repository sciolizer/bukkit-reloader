My bukkit plugin
================

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

Then start craft bukkit, and run `/reloader help`.
