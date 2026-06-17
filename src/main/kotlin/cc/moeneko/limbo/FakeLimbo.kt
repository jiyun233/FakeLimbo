package cc.moeneko.limbo

import cc.moeneko.limbo.listener.LimboListener
import cc.moeneko.limbo.packet.LimboPacketBlocker
import cc.moeneko.limbo.service.LimboPlayerService
import org.bukkit.plugin.java.JavaPlugin

class FakeLimbo : JavaPlugin() {

    private lateinit var packetBlocker: LimboPacketBlocker
    private lateinit var playerService: LimboPlayerService
    private lateinit var limboListener: LimboListener

    override fun onEnable() {
        playerService = LimboPlayerService(this)
        packetBlocker = LimboPacketBlocker(this)
        limboListener = LimboListener(this, playerService)

        packetBlocker.enable()
        server.pluginManager.registerEvents(limboListener, this)
        playerService.applyToOnlinePlayers()

        logger.info("FakeLimbo enabled: messages are allowed, players are isolated, and spectator limbo is active.")
    }

    override fun onDisable() {
        packetBlocker.disable()
        playerService.restoreOnlinePlayers()
    }
}
