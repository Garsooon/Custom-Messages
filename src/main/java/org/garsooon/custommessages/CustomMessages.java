package org.garsooon.custommessages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CustomMessages extends JavaPlugin {
    private Properties messages;
    private File messagesFile;
    private File blacklistFile;
    private List<Pattern> blacklistPatterns;
    private final PlayerListener playerListener = new CustomMessageListener(this);

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        messagesFile = new File(getDataFolder(), "messages.properties");
        messages = new Properties();
        loadMessages();

        blacklistFile = new File(getDataFolder(), "blacklist.txt");
        blacklistPatterns = new ArrayList<>();
        loadBlacklist();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_KICK, playerListener, Event.Priority.Normal, this);

        getServer().getLogger().info("[CustomMessages] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveMessages();
        getServer().getLogger().info("[CustomMessages] Plugin disabled!");
    }

    private void loadBlacklist() {
        blacklistPatterns.clear();
        if (!blacklistFile.exists()) {
            try {
                blacklistFile.createNewFile();
            } catch (IOException e) {
                getServer().getLogger().warning("[CustomMessages] Could not create blacklist file!");
            }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(blacklistFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        blacklistPatterns.add(Pattern.compile(line, Pattern.CASE_INSENSITIVE));
                    } catch (Exception e) {
                        getServer().getLogger().warning("[CustomMessages] Invalid regex in blacklist.txt: " + line);
                    }
                }
            }
        } catch (IOException e) {
            getServer().getLogger().warning("[CustomMessages] Could not load blacklist file!");
        }
    }

    private List<Pattern> getMatchingBlacklistPatterns(String message) {
        List<Pattern> matches = new ArrayList<>();
        for (Pattern pattern : blacklistPatterns) {
            Matcher m = pattern.matcher(message);
            if (m.find()) {
                matches.add(pattern);
            }
        }
        return matches;
    }

    private void logBlacklistedUsage(CommandSender sender, String message, List<Pattern> matchedPatterns) {
        getServer().getLogger().warning(
                "[CustomMessages] Blacklisted word/regex used by: " + sender.getName() +
                        " Message: " + message +
                        " Matched: " + matchedPatterns
        );
    }

    private void notifyAdmins(CommandSender offender, String message, List<Pattern> matchedPatterns) {
        String warn = ChatColor.RED + "[CustomMessages] " + offender.getName()
                + " tried to use a blacklisted word/pattern in their message!";
        String msg = ChatColor.YELLOW + "Message: " + ChatColor.WHITE + message;
        String match = ChatColor.YELLOW + "Matched pattern(s): " + ChatColor.WHITE + patternsToString(matchedPatterns);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("custommessages.admin")) {
                p.sendMessage(warn);
                p.sendMessage(msg);
                p.sendMessage(match);
            }
        }
    }

    private String patternsToString(List<Pattern> patterns) {
        List<String> out = new ArrayList<>();
        for (Pattern p : patterns) out.add("\"" + p.pattern() + "\"");
        return String.join(", ", out);
    }



    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("setjoin")) {
            return handleSetJoin(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("setleave")) {
            return handleSetLeave(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("viewmessages")) {
            return handleViewMessages(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("setplayerjoin")) {
            return handleSetPlayerJoin(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("setplayerleave")) {
            return handleSetPlayerLeave(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("resetmessages")) {
            return handleResetMessages(sender, args);
        }
        return false;
    }

    private boolean isGoldColored(String message) {
        return message.startsWith("&") || message.startsWith("§");
    }

    private String applyDefaultGold(String message) {
        if (!isGoldColored(message)) {
            return ChatColor.GOLD + message;
        }
        return message;
    }

    private boolean validateMessage(CommandSender sender, String message) {
        if (!message.contains("%player%")) {
            sender.sendMessage(ChatColor.RED + "Your message must contain %player% somewhere!");
            return false;
        }
        List<Pattern> matches = getMatchingBlacklistPatterns(message);
        if (!matches.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Your message contains blacklisted words or patterns and was not saved!");
            logBlacklistedUsage(sender, message, matches);
            if (!sender.hasPermission("custommessages.admin")) {
                notifyAdmins(sender, message, matches);
            }
            return false;
        }
        return true;
    }



    private boolean handleSetJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("custommessages.setjoin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to set a join message!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /setjoin <message>");
            player.sendMessage(ChatColor.YELLOW + "Use %player% for your name");
            return true;
        }

        String message = String.join(" ", args);
        if (!validateMessage(sender, message)) return true;

        message = applyDefaultGold(message);
        setJoinMessage(player.getName(), message);
        player.sendMessage(ChatColor.GREEN + "Join message set to: " + ChatColor.WHITE + message.replace("%player%", player.getName()));
        return true;
    }

    private boolean handleSetLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("custommessages.setleave")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to set a leave message!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /setleave <message>");
            player.sendMessage(ChatColor.YELLOW + "Use %player% for your name");
            return true;
        }

        String message = String.join(" ", args);
        if (!validateMessage(sender, message)) return true;

        message = applyDefaultGold(message);
        setLeaveMessage(player.getName(), message);
        player.sendMessage(ChatColor.GREEN + "Leave message set to: " + ChatColor.WHITE + message.replace("%player%", player.getName()));
        return true;
    }

    private boolean handleSetPlayerJoin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommessages.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to modify other players' messages!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /setplayerjoin <player> <message>");
            return true;
        }

        String targetPlayer = args[0];
        String[] msgArgs = new String[args.length - 1];
        System.arraycopy(args, 1, msgArgs, 0, args.length - 1);
        String message = String.join(" ", msgArgs);

        if (!validateMessage(sender, message)) return true;

        message = applyDefaultGold(message);
        setJoinMessage(targetPlayer, message);
        sender.sendMessage(ChatColor.GREEN + "Set " + targetPlayer + "'s join message to: " + ChatColor.WHITE + message);
        return true;
    }

    private boolean handleSetPlayerLeave(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommessages.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to modify other players' messages!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /setplayerleave <player> <message>");
            return true;
        }

        String targetPlayer = args[0];
        String[] msgArgs = new String[args.length - 1];
        System.arraycopy(args, 1, msgArgs, 0, args.length - 1);
        String message = String.join(" ", msgArgs);

        if (!validateMessage(sender, message)) return true;

        message = applyDefaultGold(message);
        setLeaveMessage(targetPlayer, message);
        sender.sendMessage(ChatColor.GREEN + "Set " + targetPlayer + "'s leave message to: " + ChatColor.WHITE + message);
        return true;
    }

    private boolean handleViewMessages(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommessages.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view messages!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /viewmessages <player>");
            return true;
        }

        String targetPlayer = args[0];
        String joinMsg = getJoinMessage(targetPlayer);
        String leaveMsg = getLeaveMessage(targetPlayer);

        sender.sendMessage(ChatColor.GOLD + "=== Messages for " + targetPlayer + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Join: " + ChatColor.WHITE +
                (joinMsg != null ? joinMsg : ChatColor.GRAY + "(default)"));
        sender.sendMessage(ChatColor.YELLOW + "Leave: " + ChatColor.WHITE +
                (leaveMsg != null ? leaveMsg : ChatColor.GRAY + "(default)"));
        return true;
    }

    private boolean handleResetMessages(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommessages.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reset messages!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /resetmessages <player>");
            return true;
        }

        String targetPlayer = args[0];
        messages.remove(targetPlayer + ".join");
        messages.remove(targetPlayer + ".leave");
        saveMessages();
        sender.sendMessage(ChatColor.GREEN + "Reset messages for " + targetPlayer);
        return true;
    }



    public String getJoinMessage(String playerName) {
        return messages.getProperty(playerName + ".join");
    }

    public String getLeaveMessage(String playerName) {
        return messages.getProperty(playerName + ".leave");
    }

    public void setJoinMessage(String playerName, String message) {
        messages.setProperty(playerName + ".join", message);
        saveMessages();
    }

    public void setLeaveMessage(String playerName, String message) {
        messages.setProperty(playerName + ".leave", message);
        saveMessages();
    }



    private void loadMessages() {
        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            } catch (IOException e) {
                getServer().getLogger().warning("[CustomMessages] Could not create messages file!");
                e.printStackTrace();
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(messagesFile)) {
            messages.load(fis);
        } catch (IOException e) {
            getServer().getLogger().warning("[CustomMessages] Could not load messages file!");
            e.printStackTrace();
        }
    }

    private void saveMessages() {
        try (FileOutputStream fos = new FileOutputStream(messagesFile)) {
            messages.store(fos, "CustomMessages Data");
        } catch (IOException e) {
            getServer().getLogger().warning("[CustomMessages] Could not save messages file!");
            e.printStackTrace();
        }
    }

    private String translateColorCodes(String message) {
        if (message == null) return null;
        return message.replace("&0", ChatColor.BLACK.toString())
                .replace("&1", ChatColor.DARK_BLUE.toString())
                .replace("&2", ChatColor.DARK_GREEN.toString())
                .replace("&3", ChatColor.DARK_AQUA.toString())
                .replace("&4", ChatColor.DARK_RED.toString())
                .replace("&5", ChatColor.DARK_PURPLE.toString())
                .replace("&6", ChatColor.GOLD.toString())
                .replace("&7", ChatColor.GRAY.toString())
                .replace("&8", ChatColor.DARK_GRAY.toString())
                .replace("&9", ChatColor.BLUE.toString())
                .replace("&a", ChatColor.GREEN.toString())
                .replace("&b", ChatColor.AQUA.toString())
                .replace("&c", ChatColor.RED.toString())
                .replace("&d", ChatColor.LIGHT_PURPLE.toString())
                .replace("&e", ChatColor.YELLOW.toString())
                .replace("&f", ChatColor.WHITE.toString());
    }


// lazy, one mc project :P
    class CustomMessageListener extends PlayerListener {
        private final CustomMessages plugin;

        public CustomMessageListener(CustomMessages plugin) {
            this.plugin = plugin;
        }

        @Override
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            String customMsg = plugin.getJoinMessage(player.getName());

            if (customMsg != null) {
                event.setJoinMessage(plugin.translateColorCodes(
                        customMsg.replace("%player%", player.getName())));
            }
        }

        @Override
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            String customMsg = plugin.getLeaveMessage(player.getName());

            if (customMsg != null) {
                event.setQuitMessage(plugin.translateColorCodes(
                        customMsg.replace("%player%", player.getName())));
            }
        }

        @Override
        public void onPlayerKick(PlayerKickEvent event) {
            Player player = event.getPlayer();
            String customMsg = plugin.getLeaveMessage(player.getName());

            if (customMsg != null && !event.isCancelled()) {
                event.setLeaveMessage(plugin.translateColorCodes(
                        customMsg.replace("%player%", player.getName())));
            }
        }
    }
}