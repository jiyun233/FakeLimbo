package cc.moeneko.limbo.listener

import cc.moeneko.limbo.service.LimboPlayerService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.plugin.java.JavaPlugin

class LimboListener(
    private val plugin: JavaPlugin,
    private val playerService: LimboPlayerService
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        playerService.apply(event.player)
        playerService.isolate(event.player)
        plugin.server.scheduler.runTask(plugin, Runnable {
            playerService.apply(event.player)
            playerService.isolate(event.player)
        })
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        event.respawnLocation = plugin.server.worlds.first().spawnLocation
        plugin.server.scheduler.runTask(plugin, Runnable {
            playerService.apply(event.player)
            playerService.isolate(event.player)
        })
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to ?: return
        if (from.x != to.x || from.y != to.y || from.z != to.z) {
            val locked = from.clone()
            locked.yaw = to.yaw
            locked.pitch = to.pitch
            event.setTo(locked)
        }
    }

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is Player) event.isCancelled = true
    }

    @EventHandler
    fun onInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked is Player) event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.entity is Player || event.damager is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        if (event.entity is Player) event.isCancelled = true
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        event.isCancelled = true
        playerService.clear(event.player)
    }

    @EventHandler
    fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked is Player) event.isCancelled = true
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.whoClicked is Player) event.isCancelled = true
    }
}
