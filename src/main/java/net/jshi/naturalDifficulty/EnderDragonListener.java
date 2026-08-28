package net.jshi.naturalDifficulty;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EnderDragonListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> activeDragons = new HashSet<>();

    public EnderDragonListener(JavaPlugin plugin) {
        this.plugin = plugin;

        for (World world : Bukkit.getWorlds()) {
            for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                initializeDragon(dragon);
            }
        }
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon) {
            initializeDragon(dragon);
        }
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof EnderDragon dragon) {
                initializeDragon(dragon);
            }
        }
    }

    private void initializeDragon(EnderDragon dragon) {
        AttributeInstance health = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(600.0);
            if (!dragon.getScoreboardTags().contains("custom_boss")) {
                dragon.setHealth(600.0);
            }
        }

        dragon.addScoreboardTag("custom_boss");

        UUID id = dragon.getUniqueId();
        if (!activeDragons.contains(id)) {
            activeDragons.add(id);
            startBossAttackLoop(dragon);
        }
    }

    private void startBossAttackLoop(EnderDragon dragon) {
        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    activeDragons.remove(dragon.getUniqueId());
                    this.cancel();
                    return;
                }

                tick++;
                EnderDragon.Phase phase = dragon.getPhase();

                // 1. Perching Phase: Carpet Bombing & Lightning Storm
                if (phase == EnderDragon.Phase.LAND_ON_PORTAL
                        || phase == EnderDragon.Phase.BREATH_ATTACK
                        || phase == EnderDragon.Phase.ROAR_BEFORE_ATTACK) {

                    // Rain fireballs downward every 10 ticks (0.5 seconds)
                    if (tick % 10 == 0) {
                        Location center = dragon.getLocation().add(0, 8.0, 0);
                        for (int i = 0; i < 4; i++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-15.0, 15.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-15.0, 15.0);
                            Location spawnLoc = center.clone().add(ox, 0, oz);

                            Vector down = new Vector(0, -1.0, 0);
                            DragonFireball bomb = dragon.getWorld().spawn(spawnLoc, DragonFireball.class);
                            bomb.setShooter(dragon);
                            bomb.setDirection(down);
                            bomb.setVelocity(down.multiply(3.0));
                            bomb.setYield(10.0f);
                        }
                    }
                }
                // 2. Flight Phase: Shotgun Volley, Ambient Lightning, Void Pull, Meteor Swarm, & Lightning Ring
                else {
                    // Shotgun Volley every 40 ticks (~2 seconds)
                    if (tick % 40 == 0) {
                        Player target = getTargetPlayer(dragon, 80.0);
                        if (target != null) {
                            Location eyeLoc = dragon.getEyeLocation();
                            Vector baseDir = target.getEyeLocation().toVector().subtract(eyeLoc.toVector()).normalize();

                            for (int i = 0; i < 3; i++) {
                                Vector spreadDir = baseDir.clone().add(new Vector(
                                        ThreadLocalRandom.current().nextDouble(-0.15, 0.15),
                                        ThreadLocalRandom.current().nextDouble(-0.15, 0.15),
                                        ThreadLocalRandom.current().nextDouble(-0.15, 0.15)
                                )).normalize();

                                DragonFireball fireball = dragon.launchProjectile(DragonFireball.class, spreadDir.multiply(2.5));
                                fireball.setYield(8.0f);
                            }
                        }
                    }

                    // Ambient Lightning every 70 ticks (~3.5 seconds)
                    if (tick % 70 == 0) {
                        Player target = getTargetPlayer(dragon, 100.0);
                        if (target != null) {
                            target.getWorld().strikeLightning(target.getLocation());
                        }
                    }

                    // Void Pull Attack (Every 100 ticks / 5 seconds)
                    if (tick % 100 == 0) {
                        Location dragonLoc = dragon.getLocation();
                        dragon.getWorld().spawnParticle(Particle.PORTAL, dragonLoc, 100, 4.0, 2.0, 4.0, 0.5);
                        dragon.getWorld().playSound(dragonLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);

                        for (Player p : dragon.getWorld().getPlayers()) {
                            if (p.getLocation().distanceSquared(dragonLoc) <= 2500.0) { // within 50 blocks
                                Vector pullDir = dragonLoc.toVector().subtract(p.getLocation().toVector()).normalize();
                                p.setVelocity(pullDir.multiply(1.5).setY(0.4));
                            }
                        }
                    }

                    // Ender Meteor Swarm (Every 130 ticks / 6.5 seconds)
                    if (tick % 130 == 0) {
                        for (Player p : dragon.getWorld().getPlayers()) {
                            if (p.getLocation().distanceSquared(dragon.getLocation()) <= 4000.0) {
                                Location playerLoc = p.getLocation();
                                Location skyOrigin = playerLoc.clone().add(
                                        ThreadLocalRandom.current().nextDouble(-6.0, 6.0),
                                        20.0,
                                        ThreadLocalRandom.current().nextDouble(-6.0, 6.0)
                                );

                                Vector down = new Vector(0, -1.5, 0);
                                DragonFireball meteor = p.getWorld().spawn(skyOrigin, DragonFireball.class);
                                meteor.setShooter(dragon);
                                meteor.setDirection(down);
                                meteor.setVelocity(down.multiply(2.5));
                                meteor.setYield(12.0f);
                            }
                        }
                    }

                    // New Unique Attack: Lightning Ring (Every 110 ticks / 5.5 seconds)
                    // Spawns a ring of 6 lightning strikes in a circle around the dragon
                    if (tick % 110 == 0) {
                        Location dragonLoc = dragon.getLocation();
                        double radius = 10.0;
                        int points = 6;

                        for (int i = 0; i < points; i++) {
                            double angle = 2 * Math.PI * i / points;
                            double x = dragonLoc.getX() + radius * Math.cos(angle);
                            double z = dragonLoc.getZ() + radius * Math.sin(angle);

                            // Find highest solid block block coordinate matching world surface
                            Location strikeLoc = new Location(dragonLoc.getWorld(), x, dragonLoc.getWorld().getHighestBlockAt((int) x, (int) z).getY(), z);
                            dragonLoc.getWorld().strikeLightning(strikeLoc);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Player getTargetPlayer(EnderDragon dragon, double maxDist) {
        double maxDistSq = maxDist * maxDist;
        for (Player p : dragon.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(dragon.getLocation()) <= maxDistSq) {
                return p;
            }
        }
        return null;
    }
}