package net.jshi.naturalDifficulty;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WitchListener implements Listener {
    private final JavaPlugin plugin;
    private final Set<UUID> customThrowingWitches = new HashSet<>();

    public WitchListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWitchSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Witch witch) {
            AttributeInstance followAttribute = witch.getAttribute(Attribute.FOLLOW_RANGE);
            if (followAttribute != null) {
                followAttribute.setBaseValue(64.0);
            }
            startWitchTask(witch);
        }
    }

    private void startWitchTask(Witch witch) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (witch.isDead() || !witch.isValid()) {
                    this.cancel();
                    return;
                }

                Player targetPlayer = null;
                if (witch.getTarget() instanceof Player p && !p.isDead() && p.isValid()) {
                    targetPlayer = p;
                } else {
                    double closestDistSq = 4096.0;
                    for (Player player : witch.getWorld().getPlayers()) {
                        if (player.isDead() || !player.isValid()) continue;
                        double distSq = player.getLocation().distanceSquared(witch.getLocation());
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            targetPlayer = player;
                        }
                    }
                    if (targetPlayer != null) {
                        witch.setTarget(targetPlayer);
                    }
                }

                if (targetPlayer != null && witch.hasLineOfSight(targetPlayer)) {
                    Location spawnLoc = witch.getEyeLocation().add(witch.getEyeLocation().getDirection().multiply(0.5));
                    Location targetLoc = targetPlayer.getEyeLocation();

                    Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector());
                    double distance = direction.length();

                    if (distance <= 32.0) {
                        direction.normalize();
                        direction.setY(direction.getY() + (distance * 0.020));

                        customThrowingWitches.add(witch.getUniqueId());
                        ThrownPotion potion = witch.launchProjectile(ThrownPotion.class, direction.multiply(1.1));
                        customThrowingWitches.remove(witch.getUniqueId());

                        // Set payload to a randomly selected lingering potion effect
                        potion.setItem(createRandomLingeringPotion());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 13L);
    }

    @EventHandler
    public void onPotionLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion && potion.getShooter() instanceof Witch witch) {
            // If the launch did not originate from our custom task, cancel the vanilla throw
            if (!customThrowingWitches.contains(witch.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    private ItemStack createRandomLingeringPotion() {
        ItemStack potionItem = new ItemStack(Material.LINGERING_POTION);
        PotionMeta meta = (PotionMeta) potionItem.getItemMeta();

        if (meta == null) return potionItem;

        int roll = ThreadLocalRandom.current().nextInt(100);

        if(roll < 10)
        {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INFESTED, 400, 2), true);
        }
        else if (roll < 35) {
            // 35% Chance: Harming II (Instant effect)
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
        } else if (roll < 55) {
            // 20% Chance: Blindness I (140 ticks = 7 seconds)
            meta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 4), true);
        } else if (roll < 75) {
            // 20% Chance: Nausea / Confusion I (200 ticks = 10 seconds)
            meta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 400, 4), true);
        } else if (roll < 90) {
            // 15% Chance: Slowness II (160 ticks = 8 seconds)
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 4), true);
        } else {
            // 10% Chance: Poison II (100 ticks = 5 seconds)
            meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 400, 1), true);
        }

        potionItem.setItemMeta(meta);
        return potionItem;
    }
}