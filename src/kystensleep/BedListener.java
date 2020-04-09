package kystensleep;

import java.util.*;
import java.util.stream.Collectors;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitScheduler;

public class BedListener implements Listener {
    public Map<World, List<Player>> playersInBed = new HashMap<>();
    private Map<World, Double> playersNeeded = new HashMap<>();
    private double sleepPct;
    KystenSleep ks;

    public BedListener(KystenSleep instance) {
        ks = instance;
        sleepPct = instance.config.getDouble("sleepPercentage");
        for(World world : Bukkit.getWorlds()) {
            playersInBed.put(world, new ArrayList<>());
            playersNeeded.put(world, 0.0);
        }
    }

    private void actbarMsg(World world, String msg) {
        for(Player player : world.getPlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        }
    }

    private void scheduleSkipping(World world) {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ks, () -> skipNight(world), ks.config.getLong("sleepDuration"));
    }

    private void skipNight(World world) {
        if(playersInBed.get(world).size() >= playersNeeded.get(world)) {
            world.setTime(world.getTime() + 24000 - (world.getTime() % 24000));
            world.setStorm(false);
            world.setThundering(false);
            for(Player player : world.getPlayers()) {
                player.sendMessage(world.getName() + ": §3" + playersInBed.get(world).stream().map(HumanEntity::getName).collect(Collectors.joining(", ")) + "§r slept.");
            }
            playersInBed.get(world).clear();
        }
    }

    @EventHandler
    private void onBedEnter(PlayerBedEnterEvent e) {
        if(e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            World world = e.getPlayer().getWorld();

            playersInBed.get(world).add(e.getPlayer());
            if(playersInBed.get(world).size() >= playersNeeded.get(world)) {
                actbarMsg(world, "§2" + e.getPlayer().getName() + " " + ks.config.getString("bedEnterMsg") + " (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
                scheduleSkipping(world);
            } else
                actbarMsg(world, "§3" + e.getPlayer().getName() + "§r " + ks.config.getString("bedEnterMsg") + " §3(" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
        }
    }

    @EventHandler
    private void onBedLeave(PlayerBedLeaveEvent e) {
        World world = e.getPlayer().getWorld();
        playersInBed.get(world).remove(e.getPlayer());

        if((world.getTime() % 24000) < 1000) {
            actbarMsg(world, "§2Good Morning :)");
        } else if(playersInBed.get(world).size() + 1 >= playersNeeded.get(world)) {
            actbarMsg(world, "§c" + e.getPlayer().getName() + "§4 " + ks.config.getString("bedLeaveMsg") + " (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
        } else {
            actbarMsg(world, "§3" + e.getPlayer().getName() + "§r " + ks.config.getString("bedLeaveMsg") + " §3(" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
        }
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent e) {
        World worldFrom = e.getFrom();
        World worldTo = e.getPlayer().getWorld();
        playersNeeded.put(worldFrom, playersNeeded.get(worldFrom) - sleepPct);
        playersNeeded.put(worldTo, playersNeeded.get(worldTo) + sleepPct);

        if(playersInBed.get(worldFrom).size() > 0) {
            if(playersInBed.get(worldFrom).size() >= playersNeeded.get(worldFrom)) {
                actbarMsg(worldFrom, "§2" + e.getPlayer().getName() + " " + ks.config.getString("worldLeaveMsg") + " (" + playersInBed.get(worldFrom).size() + "/" + (int) Math.ceil(playersNeeded.get(worldFrom)) + ")");
                scheduleSkipping(worldFrom);
            } else actbarMsg(worldFrom, "§3" + e.getPlayer().getName() + "§r " + ks.config.getString("worldLeaveMsg") + " §3(" + playersInBed.get(worldFrom).size() + "/" + (int) Math.ceil(playersNeeded.get(worldFrom)) + ")");
        }
        if(playersInBed.get(worldTo).size() > 0) {
            if(playersInBed.get(worldTo).size() + sleepPct < playersNeeded.get(worldTo)) {
                actbarMsg(worldTo, "§3" + e.getPlayer().getName() + "§r " + ks.config.getString("worldJoinMsg") + " §3(" + playersInBed.get(worldTo).size() + "/" + (int) Math.ceil(playersNeeded.get(worldTo)) + ")");
            } else if(playersInBed.get(worldTo).size() < playersNeeded.get(worldTo)) {
                actbarMsg(worldTo, "§c" + e.getPlayer().getName() + "§4 " + ks.config.getString("worldJoinMsg") + " (" + playersInBed.get(worldTo).size() + "/" + (int) Math.ceil(playersNeeded.get(worldTo)) + ")");
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        World world = e.getPlayer().getWorld();
        playersNeeded.put(world, playersNeeded.get(world) + sleepPct);

        if(playersInBed.get(world).size() > 0) {
            if(playersInBed.get(world).size() + sleepPct < playersNeeded.get(world)) {
                actbarMsg(world, "§3" + e.getPlayer().getName() + "§r " + ks.config.getString("serverJoinMsg") + " §3(" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
            } else if(playersInBed.get(world).size() < playersNeeded.get(world)) {
                actbarMsg(world, "§c" + e.getPlayer().getName() + "§4 " + ks.config.getString("serverJoinMsg") + " (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
            }
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        World world = e.getPlayer().getWorld();
        playersNeeded.put(world, playersNeeded.get(world) - sleepPct);

        if(playersInBed.get(world).size() > 0) {
            if(playersInBed.get(world).size() >= playersNeeded.get(world)) {
                actbarMsg(world, "§2" + e.getPlayer().getName() + " " + ks.config.getString("serverLeaveMsg") + " (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
                scheduleSkipping(world);
            } else actbarMsg(world, "§3" + e.getPlayer().getName() + "§r " + ks.config.getString("serverLeaveMsg") + " §3(" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
        }
    }

    @EventHandler
    private void worldLoad(WorldLoadEvent e) {
        playersInBed.put(e.getWorld(), new ArrayList<>());
        playersNeeded.put(e.getWorld(), 0.0);
    }
}
