package jp.sawa_kai.sawakai.spigot.skyway

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.Transport
import jp.sawa_kai.sawakai.spigot.skyway.types.GetSignalingResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.addHeaderLenient
import org.bukkit.Bukkit
import java.io.IOException
import java.security.SecureRandom
import javax.xml.bind.JAXBElement
import kotlin.math.sign


class SkyWayClient(private val apiKey: String, private val roomId: String) {

    fun connect() {
        Bukkit.getLogger().info("Signaling server is $signalingServerUrl")

        val token = SecureRandom.getInstanceStrong()
            .nextLong()
            .toString(36)
            .substring(2)

        IO.setDefaultOkHttpCallFactory(okHttpClient)
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
        val socket = IO.socket(signalingServerUrl, IO.Options().apply {
            query = "apiKey=${apiKey}&token=${token}&platform=javascript&sdk_version=2.0.5"
            timestampRequests = true
        })
        socket.on(Socket.EVENT_CONNECT, Emitter.Listener {
            socket.emit("ROOM_JOIN", "{\"roomName\":\"${roomId}\",\"roomType\":\"sfu\"}")
            Bukkit.getLogger().info("connected!")
        }).on(Socket.EVENT_ERROR, Emitter.Listener {
            Bukkit.getLogger().info("errror")
            Bukkit.getLogger().info(it.map { it.toString() }.toString())
        }).on(Socket.EVENT_DISCONNECT) {
            Bukkit.getLogger().info("Socket was disconnected")
        }.on(Socket.EVENT_MESSAGE) {
            Bukkit.getLogger().info("message: ${it}")
        }.on(Socket.EVENT_PING) {
            Bukkit.getLogger().info("ping connected?: ${socket.connected()}")
            Bukkit.getLogger().info("{\"roomName\":\"${roomId}\",\"roomType\":\"sfu\"}")
            socket.emit("ROOM_JOIN", "{\"roomName\":\"${roomId}\",\"roomType\":\"sfu\"}")
        }.on(Socket.EVENT_PONG) {
            Bukkit.getLogger().info("pong")
        }.on(Socket.EVENT_RECONNECTING) {
            Bukkit.getLogger().info("reconnecting")
        }.on(Socket.EVENT_CONNECTING) {
            Bukkit.getLogger().info("connecting")
        }.on("ROOM_USER_JOIN") {
            Bukkit.getLogger().info("ROOM_USER_JOIN")
            socket.emit("SFU_GET_OFFER", "{\"roomName\":\"${roomId}\"}")
        }
        socket.connect()

        GlobalScope.launch {
            while (true) {
                socket.emit("PING")
                delay(25_000)
            }
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor {
                val newRequest = it.request().newBuilder()
                    .addHeader("Origin", "https://minecraft-voice-chat-test.web.app")
                    .addHeader("Referer", "https://minecraft-voice-chat-test.web.app/room/")
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537.36"
                    )
                    .addHeader(
                        "Host",
                        signalingServerDomain
                    )
                    .addHeader(
                        "Accept-Language",
                        "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7,zh-TW;q=0.6,zh;q=0.5,pt;q=0.4"
                    )
                    .addHeader(
                        "Sec-Fetch-Dest",
                        "empty"
                    )
                    .addHeader(
                        "Sec-Fetch-Mode",
                        "cors"
                    )
                    .addHeader(
                        "Sec-Fetch-Site",
                        "cross-site"
                    )
                    .addHeader(
                        "Accept",
                        "*/*"
                    )
                    .addHeader(
                        "Accept-Encoding",
                        "gzip, deflate, br"
                    )
                    .addHeader(
                        "Cache-Control",
                        "no-cache"
                    )
                    .addHeader("DNT", "1")
                    .addHeader("Pragma", "no-cache")
                    .build()

                Bukkit.getLogger().info("url: ${newRequest.url.toString()}")
                val resp = it.proceed(newRequest)
                resp
            }
            .build()
    }

    private val signalingServerDomain by lazy {
        val req = Request.Builder()
            .url("https://dispatcher.webrtc.ecl.ntt.com/signaling")
            .build()
        OkHttpClient().newCall(req).execute().use {
            if (!it.isSuccessful) {
                throw IOException("Failed to get /signaling $it")
            }

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val jsonAdapter = moshi.adapter(GetSignalingResponse::class.java)
            val json = it.body?.string() ?: throw IOException("Body is null $it")
            jsonAdapter.fromJson(json)?.domain ?: throw IOException("Failed to get /signaling $it")
        }
    }

    private val signalingServerUrl by lazy {
        "https://${signalingServerDomain}/socket.io/"
    }
}
