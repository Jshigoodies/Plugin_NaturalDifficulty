package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerBuffListener implements Listener {

    private final JavaPlugin plugin;

    public PlayerBuffListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startRegenTask();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Only set max health without forcing a full heal on every login
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() != 60.0) {
            maxHealth.setBaseValue(60.0);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // When they genuinely die and respawn, they *should* be healed to full 60 HP
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(60.0);
                player.setHealth(60.0);
            }
        }, 5L);
    }

    private void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.REGENERATION,
                            100,
                            2,
                            true,
                            false
                    ));
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }
}