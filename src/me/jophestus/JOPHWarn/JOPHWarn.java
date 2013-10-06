package me.jophestus.JOPHWarn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JOPHWarn extends JavaPlugin {
    public static JOPHWarn plugin;
    Logger log = Logger.getLogger("Minecraft");
    private FileConfiguration customConfig = null;
    private File customConfigFile = null;
    public final JOPHWarnListener Listener = new JOPHWarnListener(this);
    CommandSender warnedby = null;
    Player warnee = null;
    String reason = null;
    List<String> warnings = null;
    List<String> infractions = null;
    String rawName = null;
    String warnQueue = "";

    public void reloadCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "warnings.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // Look for defaults in the jar
        InputStream defConfigStream = this.getResource("warnings.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(defConfigStream);
            customConfig.setDefaults(defConfig);
        }
    }

    public FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            this.reloadCustomConfig();
        }
        return customConfig;
    }

    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            getCustomConfig().save(customConfigFile);
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE,
                    "Could not save config to " + customConfigFile, ex);
        }
    }

    public void onEnable() {
        SetupConfig();
        loadConfiguration();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this.Listener, this);

    }

    private void SetupConfig() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
    }

    private void loadConfiguration() {

        getCustomConfig().options().copyDefaults(true);
        saveCustomConfig();
    }


    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (command.getName().equalsIgnoreCase("warn")) {

            if (sender.hasPermission("JOPHWarn.warn")) {
                Calendar currentDate = Calendar.getInstance();
                SimpleDateFormat formatter = new SimpleDateFormat(
                        "dd/MMM HH:mm:ss");
                String dateNow = formatter.format(currentDate.getTime());
                if (args.length == 0 || args.length == 1) {
                    sender.sendMessage(ChatColor.RED
                            + "[JOPHWarn] "
                            + ChatColor.GREEN
                            + "I'm sorry "
                            + sender.getName()
                            + ", You haven't provided enough arguments for this command.");

                    return false;
                }

                StringBuilder b = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i != 1)
                        b.append(" ");
                    b.append(args[i]);
                }
                warnings = getCustomConfig().getStringList(
                        args[0] + ".warnings");
                rawName = args[0];
                infractions = this.getCustomConfig().getStringList(
                        rawName + ".infractions");
                reason = b.toString();
                warnedby = sender;
                int warningCount = warnings.size();
                warnee = Bukkit.getServer().getPlayer(args[0]);
                reloadCustomConfig();
                if (warningCount >= 1) {
                    sender.sendMessage(ChatColor.RED
                            + "[JOPHWarn] "
                            + ChatColor.GREEN
                            + args[0]
                            + " already has 1 or more warnings. Would you like to issue an infraction instead? '/confim' to infract, '/deny' to warn");
                    sender.sendMessage("The previous warnings were for:");
                    reloadCustomConfig();
                    List<String> warns = getCustomConfig().getStringList(
                            args[0] + ".warnings");

                    for (String s : warns) {

                        sender.sendMessage(ChatColor.GOLD + s);


                        warnQueue = sender.getName();

                    }
                    return true;

                } else {


                    if (warnee == null) {
                        OfflinePlayer offline = Bukkit.getServer()
                                .getOfflinePlayer(args[0]);

                        warnings.add(b.toString() + " - By: " + sender.getName()
                                + " " + dateNow);
                        getCustomConfig().set(offline.getName() + "offline",
                                warnings);
                        saveCustomConfig();
                        reloadCustomConfig();
                        sender.sendMessage(ChatColor.RED
                                + "[JOPHWarn] "
                                + ChatColor.GREEN
                                + args[0]
                                + " is not online. The warning will be received when they logon");
                        warnings.clear();
                        return true;
                    }


                    warnUser();
                }
                if (getConfig().getBoolean("notifyadmins", true)) {
                    for (Player plr : Bukkit.getServer().getOnlinePlayers())
                        if ((plr.hasPermission("JOPHWarn.notify"))
                                || (plr.isOp())) {
                            plr.sendMessage(warnee.getName() + ChatColor.GREEN
                                    + " Was warned by " + sender.getName()
                                    + " for:");
                            plr.sendMessage(ChatColor.GOLD + b.toString());
                        }
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }

        }
        if (command.getName().equalsIgnoreCase("confirm")) {

            if (sender.hasPermission("JOPHWarn.warn")) {
                if (warnQueue.equals("")) {
                    sender.sendMessage("There's nothing to confirm.");
                    return true;

                } else {
                    infractUser();
                    warnQueue = "";
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }

        }
        if (command.getName().equalsIgnoreCase("deny")) {

            if (sender.hasPermission("JOPHWarn.warn")) {
                if (warnQueue.equals("")) {
                    sender.sendMessage("There's nothing to deny.");
                    return true;


                } else {
                    warnUser();
                    warnQueue = "";
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }

        }
        if (command.getName().equalsIgnoreCase("warnings")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN
                        + "You have not provided enough arguments :(");
                return false;
            }
            if (sender.hasPermission("JOPHWarn.view")) {

                if (args[0].equalsIgnoreCase("view")) {
                    reloadCustomConfig();
                    List<String> warns = getCustomConfig().getStringList(
                            args[1] + ".warnings");
                    sender.sendMessage(ChatColor.BLACK
                            + "+++++++++++++++++++++++++++++++++++++++");
                    sender.sendMessage(ChatColor.GREEN + "Viewing " + args[1]
                            + "'s Warnings");
                    for (String s : warns) {

                        sender.sendMessage(ChatColor.GOLD + s);

                    }
                    sender.sendMessage(ChatColor.BLACK
                            + "+++++++++++++++++++++++++++++++++++++++");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }


            if (sender.hasPermission("JOPHWarn.warnings.clearall")) {
                if (args[0].equalsIgnoreCase("clear")) {
                    getCustomConfig().set(args[1] + ".warnings", null);
                    saveCustomConfig();
                    reloadCustomConfig();
                    sender.sendMessage(ChatColor.RED + "[JOPHWarn]"
                            + ChatColor.GREEN + " You have cleared " + args[1]
                            + "'s warnings");
                    return true;

                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("infractions")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN
                        + "You have not provided enough arguments :(");
                return false;
            }
            if (sender.hasPermission("JOPHWarn.infractions.view")) {

                if (args[0].equalsIgnoreCase("view")) {
                    reloadCustomConfig();
                    List<String> infractions = getCustomConfig().getStringList(
                            args[1] + ".infractions");
                    sender.sendMessage(ChatColor.BLACK
                            + "+++++++++++++++++++++++++++++++++++++++");
                    sender.sendMessage(ChatColor.GREEN + "Viewing " + args[1]
                            + "'s Infractions");
                    for (String s : infractions) {

                        sender.sendMessage(ChatColor.GOLD + s);

                    }
                    sender.sendMessage(ChatColor.BLACK
                            + "+++++++++++++++++++++++++++++++++++++++");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }
            if (sender.hasPermission("JOPHWarn.infractions.clearall")) {
                if (args[0].equalsIgnoreCase("clear")) {
                    getCustomConfig().set(args[1] + ".infractions", null);
                    saveCustomConfig();
                    reloadCustomConfig();
                    sender.sendMessage(ChatColor.RED + "[JOPHWarn]"
                            + ChatColor.GREEN + " You have cleared " + args[1]
                            + "'s infractions");
                    return true;

                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("jophwarn")) {

            sender.sendMessage(ChatColor.RED + "[JOPHWarn]" + ChatColor.GREEN
                    + " JOPHWarn, by JOPHESTUS & Drew1080. Version 2.0");

        }

        if (command.getName().equalsIgnoreCase("infract")) {

            if (sender.hasPermission("JOPHWarn.infract")) {

                Calendar currentDate = Calendar.getInstance();
                SimpleDateFormat formatter = new SimpleDateFormat(
                        "dd/MMM HH:mm:ss");
                String dateNow = formatter.format(currentDate.getTime());

                if (args.length == 0 || args.length == 1) {
                    sender.sendMessage(ChatColor.RED
                            + "[JOPHWarn] "
                            + ChatColor.GREEN
                            + "I'm sorry "
                            + sender.getName()
                            + ", You haven't provided enough arguments for this command.");

                    return false;
                }
                rawName = args[0];
                StringBuilder b = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i != 1)
                        b.append(" ");
                    b.append(args[i]);
                }
                warnee = Bukkit.getServer().getPlayer(args[0]);
                reason = b.toString();
                warnedby = sender;

                infractions = this.getCustomConfig().getStringList(
                        args[0] + ".infractions");
                if (warnee == null) {
                    OfflinePlayer offline = Bukkit.getServer()
                            .getOfflinePlayer(args[0]);

                    infractions.add(b.toString() + " - By: " + sender.getName()
                            + " " + dateNow);
                    this.getCustomConfig()
                            .set(offline.getName() + "offline_infractions",
                                    infractions);
                    this.saveCustomConfig();
                    this.reloadCustomConfig();
                    sender.sendMessage(ChatColor.RED
                            + "[JOPHWarn] "
                            + ChatColor.GREEN
                            + args[0]
                            + " is not online. The infraction will be received when they logon");
                    infractions.clear();
                    return true;
                } else {


                    if (this.getConfig().getBoolean("notifyadmins", true)) {
                        for (Player plr : Bukkit.getServer().getOnlinePlayers())
                            if ((plr.hasPermission("JOPHWarn.notify"))
                                    || (plr.isOp())) {
                                plr.sendMessage(warnee.getName() + ChatColor.GREEN
                                        + " Was infracted by " + sender.getName()
                                        + " for:");
                                plr.sendMessage(ChatColor.GOLD + b.toString());
                            }
                    }
                    infractUser();
                }
            } else {
                sender.sendMessage(ChatColor.RED + "[JOPHWarn] "
                        + ChatColor.GREEN + "I'm sorry " + sender.getName()
                        + ", but you can't do that.");
            }

        }

        return super.onCommand(sender, command, label, args);
    }

    private void infractUser() {

        ConsoleCommandSender console = Bukkit.getServer()
                .getConsoleSender();
        int maxinfractions = this.getConfig().getInt("kickafter");
        int maxinfractionsBan = getConfig().getInt("banafter");
        int custom1 = getConfig().getInt("custom1infractions");
        int custom2 = getConfig().getInt("custom2infractions");
        int custom3 = getConfig().getInt("custom3infractions");
        int custom4 = getConfig().getInt("custom4infractions");
        int custom5 = getConfig().getInt("custom5infractions");
        int custom6 = getConfig().getInt("custom6infractions");

        String custom1command = getConfig().getString("custom1command");
        String custom2command = getConfig().getString("custom2command");
        String custom3command = getConfig().getString("custom3command");
        String custom4command = getConfig().getString("custom4command");
        String custom5command = getConfig().getString("custom5command");
        String custom6command = getConfig().getString("custom6command");


        custom1command = custom1command.replace("%p", warnee.getName());
        custom2command = custom2command.replace("%p", warnee.getName());
        custom3command = custom3command.replace("%p", warnee.getName());
        custom4command = custom4command.replace("%p", warnee.getName());
        custom5command = custom5command.replace("%p", warnee.getName());
        custom6command = custom6command.replace("%p", warnee.getName());

        custom1command = custom1command.replace("%w", reason);
        custom2command = custom2command.replace("%w", reason);
        custom3command = custom3command.replace("%w", reason);
        custom4command = custom4command.replace("%w", reason);
        custom5command = custom5command.replace("%w", reason);
        custom6command = custom6command.replace("%w", reason);
        Calendar currentDate = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MMM HH:mm:ss");
        String dateNow = formatter.format(currentDate.getTime());

        warnedby.sendMessage(ChatColor.RED + "[JOPHWarn] "
                + ChatColor.GREEN + "Infraction sent to "
                + warnee.getName());
        warnee.sendMessage(ChatColor.BLACK
                + "+++++++++++++++++++++++++++++++++++++++");
        warnee.sendMessage(ChatColor.RED
                + "You have been infracted by " + ChatColor.BLUE
                + warnedby.getName() + ChatColor.RED + " for:");
        warnee.sendMessage(ChatColor.GREEN + reason);
        warnee.sendMessage(ChatColor.BLACK
                + "+++++++++++++++++++++++++++++++++++++++");

        infractions.add(reason + " - By: " + warnedby.getName() + " "
                + dateNow);
        getCustomConfig().set(warnee.getName() + ".infractions",
                infractions);
        saveCustomConfig();
        this.reloadCustomConfig();
        int infractionCount = infractions.size();
        infractions.clear();

        this.log.info("JOPHWarn:: " + warnedby.getName() + " infracted "
                + warnee.getName() + " for:");
        this.log.info("JOPHWarn:: " + reason);
        if (getConfig().getBoolean("enablekick")) {
            if (infractionCount == maxinfractions) {
                warnee.kickPlayer(getConfig().getString("kickmessage"));
                log.info(warnee.getName()
                        + " reached the max warnings amount and was kicked");
            }
        }
        if (getConfig().getBoolean("enableban")) {
            if (infractionCount == maxinfractionsBan) {
                Bukkit.getOfflinePlayer(rawName).setBanned(true);
                if (getServer().getPlayer(rawName) != null) {
                    warnee.setBanned(true);
                    warnee.kickPlayer(getConfig().getString(
                            "banmessage"));
                    this.log.info(warnee.getName()
                            + " reached the max warnings amount and was banned");
                }

            }
        }
        if (getConfig().getBoolean("enablecustom1")) {
            if (infractionCount == custom1) {

                Bukkit.dispatchCommand(console, custom1command);
            }
        }
        if (getConfig().getBoolean("enablecustom2")) {
            if (infractionCount == custom2) {

                Bukkit.dispatchCommand(console, custom2command);
            }
        }
        if (getConfig().getBoolean("enablecustom3")) {
            if (infractionCount == custom3) {

                Bukkit.dispatchCommand(console, custom3command);
            }
        }
        if (getConfig().getBoolean("enablecustom4")) {
            if (infractionCount == custom4) {

                Bukkit.dispatchCommand(console, custom4command);
            }
        }
        if (getConfig().getBoolean("enablecustom5")) {
            if (infractionCount == custom5) {

                Bukkit.dispatchCommand(console, custom5command);
            }
        }
        if (getConfig().getBoolean("enablecustom6")) {
            if (infractionCount == custom6) {

                Bukkit.dispatchCommand(console, custom6command);
            }
        }
    }

    private void warnUser() {
        Calendar currentDate = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MMM HH:mm:ss");
        String dateNow = formatter.format(currentDate.getTime());


        warnedby.sendMessage(ChatColor.RED + "[JOPHWarn] "
                + ChatColor.GREEN + "Warning sent to "
                + warnee.getName());
        warnee.sendMessage(ChatColor.BLACK
                + "+++++++++++++++++++++++++++++++++++++++");
        warnee.sendMessage(ChatColor.RED + "You have been warned by "
                + ChatColor.BLUE + warnedby.getName() + ChatColor.RED
                + " for:");
        warnee.sendMessage(ChatColor.GREEN + reason);
        warnee.sendMessage(ChatColor.BLACK
                + "+++++++++++++++++++++++++++++++++++++++");

        warnings.add(reason + " - By: " + warnedby.getName() + " "
                + dateNow);
        this.getCustomConfig().set(warnee.getName() + ".warnings",
                warnings);
        saveCustomConfig();
        reloadCustomConfig();

        warnings.clear();
        this.log.info("JOPHWarn:: " + warnedby.getName() + " warned "
                + warnee.getName() + " for:");
        this.log.info("JOPHWarn:: " + reason);
    }

}