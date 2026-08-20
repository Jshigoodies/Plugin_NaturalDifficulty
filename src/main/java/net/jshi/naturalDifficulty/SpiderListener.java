package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.concurrent.ThreadLocalRandom;

public class SpiderListener implements Listener {
    @EventHandler
    public void onSpiderSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Spider spider) {

            // 1. Scale down visual model and hitbox (Default is 1.0)
            AttributeInstance scaleAttribute = spider.getAttribute(Attribute.SCALE);
            if (scaleAttribute != null) {
                scaleAttribute.setBaseValue(0.1); // 30% of normal size
            }

            // 2. Boost movement speed (Makes tiny spiders nimble and hard to hit)
            AttributeInstance speedAttribute = spider.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.setBaseValue(0.38); // Default is ~0.3
            }

            // 3. Set follow range
            AttributeInstance rangeAttribute = spider.getAttribute(Attribute.FOLLOW_RANGE);
            if (rangeAttribute != null) {
                rangeAttribute.setBaseValue(64.0);
            }
        }
    }
}
