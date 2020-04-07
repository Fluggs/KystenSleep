package kystensleep;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Collectors;

public class KystenSleep extends JavaPlugin {
    FileConfiguration config = getConfig();
    BedListener bl;

    @Override
    public void onEnable() {
        bl = new BedListener(this);
        getServer().getPluginManager().registerEvents(bl, this);
        System.out.println("KystenSleep active.");

        // Config
        config.addDefault("sleepPercentage", 0.5);
        config.addDefault("sleepDuration", 70L);
        config.options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public void onDisable() {
        System.out.println("KystenSleep deactivated.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("sleeplist")) {
            if(sender instanceof Player) {
                sender.sendMessage("Players sleeping in your world: " + bl.playersInBed.get(((Player) sender).getWorld()).stream().map(HumanEntity::getName).collect(Collectors.joining(", ")));
                return true;
            }
        }

        return false;
    }
}
