package me.xginko.snowballfight.modules;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.snowballfight.SnowballConfig;
import me.xginko.snowballfight.SnowballFight;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class DamageOnHit implements SnowballModule, Listener {

    private final ServerImplementation scheduler;
    private final HashSet<EntityType> configuredTypes;
    private final double damage;
    private final boolean isFolia, onlyForSpecificEntities, asBlacklist;

    protected DamageOnHit() {
        shouldEnable();
        FoliaLib foliaLib = SnowballFight.getFoliaLib();
        this.isFolia = foliaLib.isFolia();
        this.scheduler = isFolia ? foliaLib.getImpl() : null;
        SnowballConfig config = SnowballFight.getConfiguration();
        config.master().addComment("settings.damage", "\nEnable snowballs dealing damage when they hit an entity.");
        this.damage = config.getDouble("settings.damage.damage", 3.0,
                "Configure the damage that entities take from getting hit by a snowball.");
        this.onlyForSpecificEntities = config.getBoolean("settings.damage.only-for-specific-entities", false,
                "When enabled, only configured entities will take extra damage when hit by a snowball.");
        this.asBlacklist = config.getBoolean("settings.damage.use-list-as-blacklist", false,
                "All entities except the ones on this list will take damage when hit by a snowball if set to true.");
        this.configuredTypes = config.getList("settings.damage.specific-entity-types", Collections.singletonList("PLAYER"),
                "Please use correct enums from: https://jd.papermc.io/paper/1.20/org/bukkit/entity/EntityType.html")
                .stream()
                .map(configuredType -> {
                    try {
                        return EntityType.valueOf(configuredType);
                    } catch (IllegalArgumentException e) {
                        SnowballFight.getLog().warn("(Damage) Configured entity type '"+configuredType+"' not recognized. " +
                                "Please use correct values from: https://jd.papermc.io/paper/1.20/org/bukkit/entity/EntityType.html");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public boolean shouldEnable() {
        return SnowballFight.getConfiguration().getBoolean("settings.damage.enable", false);
    }

    @Override
    public void enable() {
        SnowballFight plugin = SnowballFight.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onSnowballHit(ProjectileHitEvent event) {
        if (!event.getEntityType().equals(EntityType.SNOWBALL)) return;
        if(!(event.getEntity().getShooter() instanceof Player))return;        if (!(event.getHitEntity() instanceof LivingEntity)) return;

        final LivingEntity living = (LivingEntity) event.getHitEntity();
        if (onlyForSpecificEntities && (asBlacklist == configuredTypes.contains(living.getType()))) return;

        if (isFolia) {
            scheduler.runAtEntity(living, dmg -> living.damage(damage, event.getEntity()));
        } else {
            living.damage(damage, event.getEntity());
        }
    }
}