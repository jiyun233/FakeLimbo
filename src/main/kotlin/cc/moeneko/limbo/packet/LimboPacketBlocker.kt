package cc.moeneko.limbo.packet

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.plugin.java.JavaPlugin

class LimboPacketBlocker(private val plugin: JavaPlugin) {

    private val protocolManager = ProtocolLibrary.getProtocolManager()
    private var listener: PacketAdapter? = null

    fun enable() {
        if (listener != null) return

        val adapter = object : PacketAdapter(
            plugin,
            ListenerPriority.HIGHEST,
            *BlockedPackets.worldAndEntityPackets.toTypedArray()
        ) {
            override fun onPacketSending(event: PacketEvent) {
                event.isCancelled = true
            }
        }

        protocolManager.addPacketListener(adapter)
        listener = adapter
    }

    fun disable() {
        listener?.let(protocolManager::removePacketListener)
        listener = null
    }
}
