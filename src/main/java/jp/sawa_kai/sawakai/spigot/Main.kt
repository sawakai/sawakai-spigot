package jp.sawa_kai.sawakai.spigot

import jp.sawa_kai.sawakai.spigot.skyway.SkyWayClient
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    override fun onEnable() {
        super.onEnable()

        this.saveDefaultConfig()
        val client = SkyWayClient(
            config.getString("api-key") ?: "",
            config.getString("http-origin") ?: "",
            config.getString("room-id") ?: "",
            config.getBoolean("debug")
        )
        client.onOpened {
            this.server.pluginManager.registerEvents(PlayerEventListener(client), this)
        }
        client.connect()
    }
}
