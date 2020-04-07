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

    private void checkSkipNight(World world) {
        if(playersInBed.get(world).size() >= playersNeeded.get(world)) {
            actbarMsg(world, "§2Enough players sleeping ✓");
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(ks, () -> skipNight(world), ks.config.getLong("sleepDuration"));
        }
    }

    private void skipNight(World world) {
        if(playersInBed.get(world).size() >= playersNeeded.get(world)) {
            world.setTime(world.getTime() + 24000 - (world.getTime() % 24000));
            for(Player player : world.getPlayers()) {
                player.sendMessage("§b" + playersInBed.get(world).stream().map(HumanEntity::getName).collect(Collectors.joining(", ")) + "§3 slept.");
            }
            playersInBed.get(world).clear();
        }
    }

    @EventHandler
    private void onBedEnter(PlayerBedEnterEvent e) {
        if(e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            World world = e.getPlayer().getWorld();

            playersInBed.get(world).add(e.getPlayer());
            actbarMsg(world, "§b" + e.getPlayer().getName() + "§3 entered bed. (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
            checkSkipNight(world);
        }
    }

    @EventHandler
    private void onBedLeave(PlayerBedLeaveEvent e) {
        World world = e.getPlayer().getWorld();
        playersInBed.get(world).remove(e.getPlayer());

        if((world.getTime() % 24000) < 1000) {
            actbarMsg(world, "§2Good Morning :)");
        } else if(playersInBed.get(world).size() + sleepPct >= playersNeeded.get(world)) {
            actbarMsg(world, "§c" + e.getPlayer().getName() + "§4 left bed. (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
        } else {
            actbarMsg(world, "§b" + e.getPlayer().getName() + "§3 left bed. (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
        }

    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent e) {
        World worldFrom = e.getFrom();
        World worldTo = e.getPlayer().getWorld();
        playersNeeded.put(worldFrom, playersNeeded.get(worldFrom) - sleepPct);
        playersNeeded.put(worldTo, playersNeeded.get(worldTo) + sleepPct);

        if(playersInBed.get(worldFrom).size() > 0) {
            actbarMsg(worldFrom, "§b" + e.getPlayer().getName() + "§3 left the world. (" + playersInBed.get(worldFrom).size() + "/" + (int) Math.ceil(playersNeeded.get(worldFrom)) + ")");
            checkSkipNight(worldFrom);
        }
        if(playersInBed.get(worldTo).size() > 0) {
            if(playersInBed.get(worldTo).size() + sleepPct >= playersNeeded.get(worldTo)) {
                actbarMsg(worldTo, "§c" + e.getPlayer().getName() + "§4 joined the world. (" + playersInBed.get(worldTo).size() + "/" + (int) Math.ceil(playersNeeded.get(worldTo)) + ")");
            } else {
                actbarMsg(worldTo, "§b" + e.getPlayer().getName() + "§3 joined the world. (" + playersInBed.get(worldTo).size() + "/" + (int) Math.ceil(playersNeeded.get(worldTo)) + ")");
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        World world = e.getPlayer().getWorld();
        playersNeeded.put(world, playersNeeded.get(world) + sleepPct);

        if(playersInBed.get(world).size() > 0) {
            if(playersInBed.get(world).size() + sleepPct >= playersNeeded.get(world)) {
                actbarMsg(world, "§c" + e.getPlayer().getName() + "§4 joined the server. (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
            } else {
                actbarMsg(world, "§b" + e.getPlayer().getName() + "§3 joined the server. (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
            }
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        World world = e.getPlayer().getWorld();
        playersNeeded.put(world, playersNeeded.get(world) - sleepPct);

        if(playersInBed.get(world).size() > 0) {
            actbarMsg(world, "§b" + e.getPlayer().getName() + "§3 left the server. (" + playersInBed.get(world).size() + "/" + (int) Math.ceil(playersNeeded.get(world)) + ")");
            checkSkipNight(world);
        }
    }

    @EventHandler
    private void worldLoad(WorldLoadEvent e) {
        playersInBed.put(e.getWorld(), new ArrayList<>());
        playersNeeded.put(e.getWorld(), 0.0);
    }
}
