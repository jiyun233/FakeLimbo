package cc.moeneko.limbo.service

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class LimboPlayerService(private val plugin: JavaPlugin) {

    fun applyToOnlinePlayers() {
        plugin.server.onlinePlayers.forEach(::apply)
    }

    fun restoreOnlinePlayers() {
        plugin.server.onlinePlayers.forEach { player ->
            plugin.server.onlinePlayers
                .filter { other -> other != player }
                .forEach { other -> player.showPlayer(plugin, other) }
        }
    }

    fun apply(player: Player) {
        clear(player)
        player.teleport(limboLocation())
        player.gameMode = GameMode.SPECTATOR
        player.isInvulnerable = true
        player.isSilent = true
        player.allowFlight = true
        player.isFlying = true
        player.flySpeed = 0f
        player.walkSpeed = 0f
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        player.foodLevel = 20
        player.saturation = 20f
        player.fireTicks = 0
        player.fallDistance = 0f
        plugin.server.scoreboardManager?.let { manager ->
            player.scoreboard = manager.newScoreboard
        }
        player.updateInventory()
    }

    fun isolate(player: Player) {
        plugin.server.onlinePlayers
            .filter { other -> other != player }
            .forEach { other ->
                player.hidePlayer(plugin, other)
                other.hidePlayer(plugin, player)
            }
    }

    fun clear(player: Player) {
        player.closeInventory()
        player.inventory.clear()
        player.inventory.setArmorContents(arrayOfNulls(4))
        player.inventory.setExtraContents(arrayOfNulls(1))
        player.inventory.heldItemSlot = 0
        player.setItemOnCursor(ItemStack(Material.AIR))

        player.activePotionEffects.forEach { effect ->
            player.removePotionEffect(effect.type)
        }

        player.exp = 0f
        player.level = 0
        player.totalExperience = 0
        player.absorptionAmount = 0.0
        player.arrowsInBody = 0
    }

    private fun limboLocation(): Location {
        val world = plugin.server.worlds.first()
        return Location(world, 0.5, world.minHeight.toDouble() + 32.0, 0.5, 0f, 0f)
    }
}
