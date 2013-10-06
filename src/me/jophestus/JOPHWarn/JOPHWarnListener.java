package me.jophestus.JOPHWarn;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;


public class JOPHWarnListener
        implements Listener {
    public static JOPHWarn plugin;

    public JOPHWarnListener(JOPHWarn instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        List<String> warnings = plugin.getCustomConfig().getStringList(p.getName() + "offline");
        List<String> infractions = plugin.getCustomConfig().getStringList(p.getName() + "offline_infractions");
        int size = warnings.size();
        if (size > 0) {
            p.sendMessage(ChatColor.GREEN + "While you were away, " + p.getName() + ". You were warned " + size + " times. For:");
            for (String s : warnings) {

                p.sendMessage(ChatColor.GOLD + s);
                plugin.getCustomConfig().set(p.getName() + "offline", null);
                plugin.saveCustomConfig();
                plugin.reloadCustomConfig();
            }
        }
        int infractsize = infractions.size();
        if (infractsize > 0) {
            p.sendMessage(ChatColor.GREEN + "While you were away, " + p.getName() + ", You were infracted " + infractsize + " times. For:");
            for (String s : infractions) {

                p.sendMessage(ChatColor.GOLD + s);
                plugin.getCustomConfig().set(p.getName() + "offline_infractions", null);
                plugin.saveCustomConfig();
                plugin.reloadCustomConfig();
            }
        }
    }


}

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
