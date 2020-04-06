package kystensleep;

import java.util.*;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class BedListener implements Listener {
    private Map<World, List<Player>> playersInBed = new HashMap<>();
    private Map<World, Integer> playersNeeded = new HashMap<>();
    private double sleepPct;

    public BedListener(KystenSleep instance) {
        sleepPct = instance.config.getDouble("sleepPercentage");
        for(World world : Bukkit.getWorlds()) {
            playersInBed.put(world, new ArrayList<>());
        }
    }

    //function that checks if the needed percentage in the world is reached and if yes, skips the night
    private void skipNight(World world) {
        playersNeeded.put(world, (int) Math.ceil(world.getPlayers().size() * sleepPct));
        if(playersInBed.get(world).size() >= playersNeeded.get(world)) {
            world.setTime(world.getTime() + 24000 - (world.getTime() % 24000));
            playersInBed.get(world).clear();
        }
    }

    @EventHandler
    private void onBedEnter(PlayerBedEnterEvent e) {
        if(e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            World world = e.getPlayer().getWorld();
            playersInBed.get(world).add(e.getPlayer());
            Bukkit.broadcastMessage("Sleeping: " + playersInBed.get(world) + " / In World: " + world.getPlayers().size() + " / Needed: " + playersNeeded.get(world));

            for(Player player : world.getPlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(e.getPlayer().getName() + " entered bed. (" + playersInBed.get(world).size() + "/" + playersNeeded.get(world) + ")"));
            }

            skipNight(world);
        }
    }

    @EventHandler
    private void onBedLeave(PlayerBedLeaveEvent e) {
        World world = e.getPlayer().getWorld();
        playersInBed.get(world).remove(e.getPlayer());
        for(Player player : world.getPlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(e.getPlayer().getName() + " left bed. (" + playersInBed.get(world).size() + "/" + playersNeeded.get(world) + ")"));
        }
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent e) {
        World worldFrom = e.getFrom();
        World worldTo = e.getPlayer().getWorld();
        playersNeeded.put(worldFrom, (int) Math.ceil(worldFrom.getPlayers().size() * sleepPct));
        playersNeeded.put(worldTo, (int) Math.ceil(worldTo.getPlayers().size() * sleepPct));

        if(playersInBed.get(worldFrom).size() > 0) {
            for(Player player : worldFrom.getPlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(e.getPlayer().getName() + " left the world. (" + playersInBed.get(worldFrom).size() + "/" + playersNeeded.get(worldFrom) + ")"));
            }
        }
        if(playersInBed.get(worldTo).size() > 0) {
            for(Player player : worldTo.getPlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(e.getPlayer().getName() + " left the world. (" + playersInBed.get(worldTo).size() + "/" + playersNeeded.get(worldTo) + ")"));
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        World world = e.getPlayer().getWorld();
        playersNeeded.put(world, (int) Math.ceil(world.getPlayers().size() * sleepPct));

        if(playersInBed.get(world).size() > 0) {
            for(Player player : world.getPlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(e.getPlayer().getName() + " joined the server. (" + playersInBed.get(world).size() + "/" + playersNeeded.get(world) + ")"));
            }
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        World world = e.getPlayer().getWorld();
        playersNeeded.put(world, (int) Math.ceil(world.getPlayers().size() * sleepPct));
        if(playersInBed.get(world).size() > 0) {
            for(Player player : world.getPlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(e.getPlayer().getName() + " left the server. (" + playersInBed.get(world).size() + "/" + playersNeeded.get(world) + ")"));
            }
            skipNight(world);
        }
    }
}
