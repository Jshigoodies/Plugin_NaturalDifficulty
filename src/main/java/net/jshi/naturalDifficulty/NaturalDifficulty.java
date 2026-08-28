package net.jshi.naturalDifficulty;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalDifficulty extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Register the event listener with Spigot

        //hostile overworld
        getServer().getPluginManager().registerEvents(new CreeperListener(), this);
        getServer().getPluginManager().registerEvents(new SkeletonListener(this), this);
        getServer().getPluginManager().registerEvents(new ZombieListener(this), this);
        getServer().getPluginManager().registerEvents(new SpiderListener(), this);
        getServer().getPluginManager().registerEvents(new PillagerListener(this), this);
        getServer().getPluginManager().registerEvents(new EndermanListener(this), this);
        getServer().getPluginManager().registerEvents(new WitchListener(this), this);
        getServer().getPluginManager().registerEvents(new SilverfishListener(this), this);
        getServer().getPluginManager().registerEvents(new RavagerListener(this), this);

        //hostile nether
        getServer().getPluginManager().registerEvents(new GhastListener(this), this);
        getServer().getPluginManager().registerEvents(new PiglinListener(), this);
        getServer().getPluginManager().registerEvents(new PiglinBruteListener(), this);
        getServer().getPluginManager().registerEvents(new BlazeListener(this), this);
        getServer().getPluginManager().registerEvents(new WitherSkeletonListener(this), this);


        //dragon
        getServer().getPluginManager().registerEvents(new EnderDragonListener(this), this);

        //passive
        getServer().getPluginManager().registerEvents(new FishListener(), this);
        getServer().getPluginManager().registerEvents(new FarmAnimalListener(), this);


        //end fix
        getServer().getPluginManager().registerEvents(new EndListener(), this);

        this.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[NaturalDifficulty] Enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "[NaturalDifficulty] Disabled");
    }
}
