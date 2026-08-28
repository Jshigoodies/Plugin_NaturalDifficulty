package net.jshi.naturalDifficulty;

import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EndListener implements Listener {
    @EventHandler
    public void onEndermanSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Enderman enderman) {
            // Cancel spawn only if the entity is in the End dimension
            if (enderman.getWorld().getEnvironment() == World.Environment.THE_END) {
                event.setCancelled(true);
            }
        }
    }
}
