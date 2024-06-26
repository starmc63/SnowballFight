package me.xginko.snowballfight.modules;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.snowballfight.SnowballConfig;
import me.xginko.snowballfight.SnowballFight;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.time.Duration;
import java.util.UUID;

public class ThrowCoolDown implements SnowballModule, Listener {

    private final Cache<UUID, Boolean> player_cooldowns, entity_cooldowns;
    private final Cache<Location, Boolean> block_cooldowns;
    private final boolean blockCooldownEnabled, entityCooldownEnabled;

    protected ThrowCoolDown() {
        shouldEnable();
        SnowballConfig config = SnowballFight.getConfiguration();
        config.master().addComment("settings.cooldown",
                "Configure a cooldown delay between throwing snowballs for players.");
        this.player_cooldowns = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(
                Math.max(1, config.getInt("settings.cooldown.player-delay-in-ticks", 10) * 50L)
        )).build();
        this.entityCooldownEnabled = config.getBoolean("settings.cooldown.entities.enable", false);
        this.entity_cooldowns = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(
                Math.max(1, config.getInt("settings.cooldown.entities.delay-in-ticks", 10) * 50L)
        )).build();
        this.blockCooldownEnabled = config.getBoolean("settings.cooldown.blocks.enable", false);
        this.block_cooldowns = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(
                Math.max(1, config.getInt("settings.cooldown.blocks.delay-in-ticks", 20) * 50L)
        )).build();
    }

    @Override
    public boolean shouldEnable() {
        return SnowballFight.getConfiguration().getBoolean("settings.cooldown.enable", false);
    }

    @Override
    public void enable() {
        SnowballFight plugin = SnowballFight.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onPlayerLaunchSnowball(PlayerLaunchProjectileEvent event) {
        if (!event.getProjectile().getType().equals(EntityType.SNOWBALL)) return;

        final UUID playerUniqueId = event.getPlayer().getUniqueId();

        if (player_cooldowns.getIfPresent(playerUniqueId) != null) {
            event.setShouldConsume(false);
            event.setCancelled(true);
        } else {
            player_cooldowns.put(playerUniqueId, true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onEntityLaunch(ProjectileLaunchEvent event) {
        if (!event.getEntityType().equals(EntityType.SNOWBALL)) return;

        final ProjectileSource shooter = event.getEntity().getShooter();

        if (entityCooldownEnabled && shooter instanceof LivingEntity) {
            LivingEntity livingShooter = (LivingEntity) shooter;
            if (livingShooter.getType().equals(EntityType.PLAYER)) return; // Players in a different event due to item consumption.
            final UUID entityUniqueId = livingShooter.getUniqueId();
            if (entity_cooldowns.getIfPresent(entityUniqueId) != null) event.setCancelled(true);
            else entity_cooldowns.put(entityUniqueId, true);
            return;
        }

        if (blockCooldownEnabled && shooter instanceof BlockProjectileSource) {
            BlockProjectileSource blockShooter = (BlockProjectileSource) shooter;
            final Location blockLocation = blockShooter.getBlock().getLocation();
            if (block_cooldowns.getIfPresent(blockLocation) != null) event.setCancelled(true);
            else block_cooldowns.put(blockLocation, true);
        }
    }
}