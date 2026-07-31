package net.jshi.naturalDifficulty;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalDifficulty extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NaturalDifficulty] Enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NaturalDifficulty] Disabled");
    }
}
