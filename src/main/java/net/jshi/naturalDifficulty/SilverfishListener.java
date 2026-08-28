package net.jshi.naturalDifficulty;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SilverfishListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey swarmKey;

    public SilverfishListener(JavaPlugin plugin)
    {
        this.plugin = plugin;
        this.swarmKey = new NamespacedKey(plugin, "swarm_silverfish");
    }

    @EventHandler
    public void onSilverfishSpawn(EntitySpawnEvent event)
    {
        if (!(event.getEntity() instanceof Silverfish original)) return;

        if (original.getPersistentDataContainer().has(swarmKey, PersistentDataType.BYTE)) {
            scaleSilverfish(original);
            return;
        }

        // Mark the original Silverfish and scale it
        original.getPersistentDataContainer().set(swarmKey, PersistentDataType.BYTE, (byte) 1);
        scaleSilverfish(original);

        for (int i = 0; i < 19; i++) {
            original.getWorld().spawn(original.getLocation(), Silverfish.class, extra -> {
                extra.getPersistentDataContainer().set(swarmKey, PersistentDataType.BYTE, (byte) 1);
                scaleSilverfish(extra);
            });
        }
    }

    @EventHandler
    public void onSilverfishEnterBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Silverfish) {
            // Emerging from a block sets event.getTo() to AIR (allowed).
            // Hiding inside a block sets event.getTo() to an infested block (cancelled).
            if (event.getTo() != Material.AIR) {
                event.setCancelled(true);
            }
        }
    }

    private void scaleSilverfish(Silverfish silverfish) {
        // Sets entity scale to 50% on modern Minecraft/Spigot versions
        AttributeInstance scaleAttribute = silverfish.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(0.5);
        }
    }
}
