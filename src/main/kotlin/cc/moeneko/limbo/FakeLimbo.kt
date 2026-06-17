package cc.moeneko.limbo

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class FakeLimbo : JavaPlugin(), Listener {

    private val protocolManager by lazy { ProtocolLibrary.getProtocolManager() }
    private var limboPacketAdapter: PacketAdapter? = null

    override fun onEnable() {
        registerLimboPackets()
        server.pluginManager.registerEvents(this, this)

        server.onlinePlayers.forEach { player ->
            applyLimboState(player)
            isolatePlayer(player)
        }
        logger.info("FakeLimbo enabled: world, entity, sound and UI packets are blocked by ProtocolLib.")
    }

    override fun onDisable() {
        limboPacketAdapter?.let(protocolManager::removePacketListener)
        limboPacketAdapter = null
        server.onlinePlayers.forEach { player ->
            server.onlinePlayers
                .filter { other -> other != player }
                .forEach { other -> player.showPlayer(this, other) }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        applyLimboState(event.player)
        isolatePlayer(event.player)
        server.scheduler.runTask(this, Runnable {
            applyLimboState(event.player)
            isolatePlayer(event.player)
        })
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        event.respawnLocation = limboLocation()
        server.scheduler.runTask(this, Runnable {
            applyLimboState(event.player)
            isolatePlayer(event.player)
        })
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
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
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.entity is Player || event.damager is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        if (event.entity is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        event.isCancelled = true
        clearPlayerState(event.player)
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.whoClicked is Player) {
            event.isCancelled = true
        }
    }

    private fun registerLimboPackets() {
        val adapter = object : PacketAdapter(
            this,
            ListenerPriority.HIGHEST,
            *blockedMapPackets.toTypedArray()
        ) {
            override fun onPacketSending(event: PacketEvent) {
                event.isCancelled = true
            }
        }

        protocolManager.addPacketListener(adapter)
        limboPacketAdapter = adapter
    }

    private fun applyLimboState(player: Player) {
        clearPlayerState(player)
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
        server.scoreboardManager?.let { manager ->
            player.scoreboard = manager.newScoreboard
        }
        player.updateInventory()
    }

    private fun isolatePlayer(player: Player) {
        server.onlinePlayers
            .filter { other -> other != player }
            .forEach { other ->
                player.hidePlayer(this, other)
                other.hidePlayer(this, player)
            }
    }

    private fun clearPlayerState(player: Player) {
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
        val world = server.worlds.first()
        return Location(world, 0.5, world.minHeight.toDouble() + 32.0, 0.5, 0f, 0f)
    }

    companion object {
        private val blockedMapPackets = setOf(
            PacketType.Play.Server.MAP_CHUNK,
            PacketType.Play.Server.MAP_CHUNK_BULK,
            PacketType.Play.Server.UNLOAD_CHUNK,
            PacketType.Play.Server.LIGHT_UPDATE,
            PacketType.Play.Server.CHUNK_BATCH_START,
            PacketType.Play.Server.CHUNK_BATCH_FINISHED,
            PacketType.Play.Server.CHUNKS_BIOMES,
            PacketType.Play.Server.MAP,
            PacketType.Play.Server.BLOCK_CHANGE,
            PacketType.Play.Server.MULTI_BLOCK_CHANGE,
            PacketType.Play.Server.TILE_ENTITY_DATA,
            PacketType.Play.Server.BLOCK_ACTION,
            PacketType.Play.Server.BLOCK_BREAK_ANIMATION,
            PacketType.Play.Server.BLOCK_CHANGED_ACK,
            PacketType.Play.Server.BLOCK_BREAK,
            PacketType.Play.Server.WORLD_EVENT,
            PacketType.Play.Server.WORLD_PARTICLES,
            PacketType.Play.Server.EXPLOSION,
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
            PacketType.Play.Server.NAMED_ENTITY_SPAWN,
            PacketType.Play.Server.SPAWN_ENTITY_LIVING,
            PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
            PacketType.Play.Server.SPAWN_ENTITY_WEATHER,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_LOOK,
            PacketType.Play.Server.ENTITY,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_HEAD_ROTATION,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ENTITY_STATUS,
            PacketType.Play.Server.ENTITY_EFFECT,
            PacketType.Play.Server.REMOVE_ENTITY_EFFECT,
            PacketType.Play.Server.ENTITY_SOUND,
            PacketType.Play.Server.NAMED_SOUND_EFFECT,
            PacketType.Play.Server.CUSTOM_SOUND_EFFECT,
            PacketType.Play.Server.STOP_SOUND,
            PacketType.Play.Server.ANIMATION,
            PacketType.Play.Server.HURT_ANIMATION,
            PacketType.Play.Server.DAMAGE_EVENT,
            PacketType.Play.Server.COLLECT,
            PacketType.Play.Server.MOUNT,
            PacketType.Play.Server.ATTACH_ENTITY,
            PacketType.Play.Server.CAMERA,
            PacketType.Play.Server.BOSS,
            PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE,
            PacketType.Play.Server.SCOREBOARD_OBJECTIVE,
            PacketType.Play.Server.SCOREBOARD_SCORE,
            PacketType.Play.Server.SCOREBOARD_TEAM,
            PacketType.Play.Server.RESET_SCORE,
            PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER,
            PacketType.Play.Server.DELETE_CHAT_MESSAGE,
            PacketType.Play.Server.PLAYER_COMBAT_ENTER,
            PacketType.Play.Server.PLAYER_COMBAT_END,
            PacketType.Play.Server.PLAYER_COMBAT_KILL,
            PacketType.Play.Server.UPDATE_TIME,
            PacketType.Play.Server.INITIALIZE_BORDER,
            PacketType.Play.Server.SET_BORDER_CENTER,
            PacketType.Play.Server.SET_BORDER_LERP_SIZE,
            PacketType.Play.Server.SET_BORDER_SIZE,
            PacketType.Play.Server.SET_BORDER_WARNING_DELAY,
            PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE,
            PacketType.Play.Server.WORLD_BORDER,
            PacketType.Play.Server.SERVER_DIFFICULTY,
            PacketType.Play.Server.SPAWN_POSITION,
            PacketType.Play.Server.VIEW_CENTRE,
            PacketType.Play.Server.VIEW_DISTANCE,
            PacketType.Play.Server.UPDATE_SIMULATION_DISTANCE,
            PacketType.Play.Server.ADVANCEMENTS,
            PacketType.Play.Server.RECIPE_UPDATE,
            PacketType.Play.Server.RECIPES,
            PacketType.Play.Server.AUTO_RECIPE,
            PacketType.Play.Server.SELECT_ADVANCEMENT_TAB,
            PacketType.Play.Server.SET_COOLDOWN,
            PacketType.Play.Server.STATISTIC,
            PacketType.Play.Server.STATISTICS,
            PacketType.Play.Server.OPEN_WINDOW,
            PacketType.Play.Server.OPEN_WINDOW_HORSE,
            PacketType.Play.Server.OPEN_WINDOW_MERCHANT,
            PacketType.Play.Server.OPEN_BOOK,
            PacketType.Play.Server.OPEN_SIGN_EDITOR,
            PacketType.Play.Server.OPEN_SIGN_ENTITY,
            PacketType.Play.Server.CLOSE_WINDOW,
            PacketType.Play.Server.WINDOW_DATA,
            PacketType.Play.Server.RESOURCE_PACK_SEND,
            PacketType.Play.Server.ADD_RESOURCE_PACK,
            PacketType.Play.Server.REMOVE_RESOURCE_PACK,
            PacketType.Play.Server.CUSTOM_PAYLOAD,
            PacketType.Play.Server.DEBUG_SAMPLE,
            PacketType.Play.Server.NBT_QUERY,
            PacketType.Play.Server.UPDATE_ENTITY_NBT,
            PacketType.Play.Server.ADD_VIBRATION_SIGNAL,
            PacketType.Play.Server.PROJECTILE_POWER,
            PacketType.Play.Server.LOOK_AT
        )
    }
}
