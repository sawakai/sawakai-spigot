package jp.sawa_kai.sawakai.spigot

import jp.sawa_kai.sawakai.spigot.skyway.SkyWayClient
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector
import org.json.JSONObject
import kotlin.math.max
import kotlin.system.measureTimeMillis

class PlayerEventListener(private val client: SkyWayClient) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val millis = measureTimeMillis {
            sendToSkyWay(event.player)
        }
        if (50 < millis) {
            Bukkit.getLogger().warning("sendToSkyWay is too slow! $millis ms")
        }
    }

    private fun sendToSkyWay(player: Player) {
        if (!client.connected) {
            return
        }

        val obj = JSONObject()
        obj.put("gameUserID", player.name)
        obj.put("gameClientID", "minecraft")
        obj.put("position", JSONObject().apply {
            put("x", player.eyeLocation.x)
            put("y", player.eyeLocation.y)
            put("z", player.eyeLocation.z)
        })

        val eyeDirection = player.eyeLocation.direction
        val y = Vector(0.0, 1.0, 0.0)
        val topDirection = Vector().copy(eyeDirection).crossProduct(y.crossProduct(eyeDirection)).normalize()
        obj.put("faceDirection", eyeDirection.toJSONObject())
        obj.put("upDirection", topDirection.toJSONObject())

        client.send(obj)
    }

    private fun Vector.toJSONObject(): JSONObject {
        return JSONObject().also {
            it.put("x", x)
            it.put("y", y)
            it.put("z", z)
        }
    }
}
