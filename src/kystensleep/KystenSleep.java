package kystensleep;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class KystenSleep extends JavaPlugin {
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BedListener(this), this);
        System.out.println("KystenSleep active.");

        // Config
        config.addDefault("sleepPercentage", 0.5);
        config.options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public void onDisable() {
        System.out.println("KystenSleep deactivated.");
    }
}
