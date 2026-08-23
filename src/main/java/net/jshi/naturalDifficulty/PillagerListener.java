package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PillagerListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> activePillagers = new HashSet<>();

    public PillagerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPillagerSpawn(EntitySpawnEvent event)
    {
        if(event.getEntity() instanceof Pillager pillager)
        {
            AttributeInstance rangeAttribute = pillager.getAttribute(Attribute.FOLLOW_RANGE);
            if (rangeAttribute != null) {
                rangeAttribute.setBaseValue(128.0);
            }
        }
    }

    @EventHandler
    public void onPillagerTarget(EntityTargetLivingEntityEvent event)
    {
        if (!(event.getEntity() instanceof Pillager pillager)) return;
        if (!(event.getTarget() instanceof Player)) return;


        UUID pillagerId = pillager.getUniqueId();
        if (activePillagers.contains(pillagerId)) return;

        activePillagers.add(pillagerId);


        new BukkitRunnable() {
            @Override
            public void run() {
                if (pillager.isDead() || !pillager.isValid()
                        || pillager.getTarget() == null || pillager.getTarget().isDead()) {
                    activePillagers.remove(pillagerId);
                    this.cancel();
                    return;
                }

                LivingEntity target = pillager.getTarget();

                if (pillager.hasLineOfSight(target)) {
                    Vector direction = target.getEyeLocation().toVector()
                            .subtract(pillager.getEyeLocation().toVector())
                            .normalize();

                    // Near-instant sniper arrow
                    Arrow arrow = pillager.launchProjectile(Arrow.class, direction.multiply(4.5));
                    arrow.setShooter(pillager);
                    arrow.setGravity(false);
                }

            }
        }.runTaskTimer(plugin, 0L, 35L);
    }

    @EventHandler
    public void onVanillaShoot(EntityShootBowEvent event) {
        // Cancel vanilla crossbow shooting logic to prevent double firing
        if (event.getEntity() instanceof Pillager) {
            event.setCancelled(true);
        }
    }

}
