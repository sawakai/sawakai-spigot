package jp.sawa_kai.sawakai.spigot.skyway

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.bukkit.Bukkit
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom


class SkyWayClient(
    private val apiKey: String,
    private val origin: String,
    val roomId: String,
    private val debug: Boolean
) {

    val connected
        get() = socket?.connected() ?: false

    private val okHttpClient = {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Bukkit.getLogger().info("okhttp: $message")
            }
        }).apply {
            setLevel(
                if (debug) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            )
        }

        OkHttpClient.Builder()
            .cookieJar(CookieJar())
            .addInterceptor {
                val newRequest = it.request().newBuilder()
                    .addHeader("Origin", origin)
                    .build()

                val resp = it.proceed(newRequest)
                resp
            }
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }()

    private val signalingServerDomain by lazy {
        val req = Request.Builder()
            .url("https://dispatcher.webrtc.ecl.ntt.com/signaling")
            .build()
        okHttpClient.newCall(req).execute().use {
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
        "https://${signalingServerDomain}"
    }

    private var socket: Socket? = null

    private var onConnectedHandler: (() -> Unit) = {}
    private var onOpenedHandler: (() -> Unit) = {}
    private var onErrorHandler: ((messages: List<String>) -> Unit) = {
        throw IOException("failed to connect skyway: $it")
    }

    fun connect() {
        Bukkit.getLogger().info("Signaling server is $signalingServerUrl")

        val token = SecureRandom.getInstanceStrong()
            .nextLong()
            .toString(36)
            .substring(2)

        IO.setDefaultOkHttpCallFactory(okHttpClient)
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
        socket = IO.socket(signalingServerUrl, IO.Options().apply {
            query = "apiKey=${apiKey}&token=${token}"
            timestampRequests = true
            secure = true
            forceNew = true
        })
        socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
            Bukkit.getLogger().info("connected to skyway")
            onConnectedHandler?.invoke()
        })?.on(Socket.EVENT_ERROR, Emitter.Listener { errors ->
            onErrorHandler?.invoke(errors.map { it.toString() })
        })?.on("OPEN") {
            Bukkit.getLogger().info("skyway peer is opened")
            joinRoom()
            onOpenedHandler()
        }
        socket?.connect()

        GlobalScope.launch {
            while (true) {
                delay(25_000)
                socket?.emit("PING")
            }
        }
    }

    fun onConnected(handler: () -> Unit): SkyWayClient {
        onConnectedHandler = handler
        return this
    }

    fun onOpened(handler: () -> Unit): SkyWayClient {
        onOpenedHandler = handler
        return this
    }

    fun onError(handler: (messages: List<String>) -> Unit): SkyWayClient {
        onErrorHandler = handler
        return this
    }

    fun send(message: JSONObject) {
        val socket = socket
        if (!connected || socket == null) {
            throw IllegalStateException("no connection to skyway")
        }
        socket.emit("ROOM_SEND_DATA", JSONObject().apply {
            put("roomName", roomId)
            put("data", message)
        })
    }

    private fun joinRoom() {
        val obj = JSONObject().apply {
            put("roomName", roomId)
            put("roomType", "sfu")
        }
        socket?.emit("ROOM_JOIN", obj)
    }
}
