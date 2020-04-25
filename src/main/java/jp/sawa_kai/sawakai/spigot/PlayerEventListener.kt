package jp.sawa_kai.sawakai.spigot

import jp.sawa_kai.sawakai.spigot.skyway.SkyWayClient
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector
import org.json.JSONObject
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class PlayerEventListener(private val client: SkyWayClient) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        sendToSkyWay(event.player)
    }

    private fun sendToSkyWay(player: Player) {
        if (!client.connected) {
            return
        }

        val obj = JSONObject()
        obj.put("gameUserID", player.name)
        obj.put("gameClientID", "minecraft")
        obj.put("position", JSONObject().apply {
            put("x", player.location.x)
            put("y", player.location.y)
            put("z", player.location.z)
        })
        obj.put("faceRotation", toQuaternion(player.eyeLocation))
        client.send(obj)
    }

    private fun toQuaternion(eyeLocation: Location): JSONObject {
        val base = Vector(1.0, 0.0, 0.0)
        val direction = eyeLocation.direction
        val axis = base.crossProduct(direction).normalize()
        val theta = acos(base.dot(direction))
        return JSONObject().apply {
            put("x", axis.x * sin(theta / 2.0))
            put("y", axis.y * sin(theta / 2.0))
            put("z", axis.z * sin(theta / 2.0))
            put("w", cos(theta / 2.0))
        }
    }
}
