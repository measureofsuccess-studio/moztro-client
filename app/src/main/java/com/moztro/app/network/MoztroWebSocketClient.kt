package com.moztro.app.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.moztro.app.data.ConnectionState
import com.moztro.app.data.PcDisplaySource
import com.moztro.app.data.PingRecord
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MoztroWebSocketClient(
    private val deviceId: String = UUID.randomUUID().toString(),
    private val onStateChanged: (ConnectionState, String?) -> Unit,
    private val onPingResult: (PingRecord) -> Unit,
    private val onMacReceived: ((String) -> Unit)? = null,
    private val onClipboardResponse: ((String) -> Unit)? = null,
    private val onClipboardGetRequest: (() -> Unit)? = null,
    private val onFileTransferStart: ((transferId: String, fileName: String, fileSize: Long, mimeType: String) -> Unit)? = null,
    private val onFileTransferChunk: ((transferId: String, chunkBase64: String, chunkIndex: Int, isLast: Boolean) -> Unit)? = null,
    private val onFileTransferCancel: ((transferId: String) -> Unit)? = null,
    private val onOverdriveFrame: ((Bitmap) -> Unit)? = null,
    private val onOverdriveAudio: ((ByteArray) -> Unit)? = null,
    private val onScreenSourcesReceived: ((List<PcDisplaySource>) -> Unit)? = null,
    private val onOverdriveStreamStarted: ((width: Int, height: Int, sourceId: String) -> Unit)? = null,
    private val onOverdriveStreamError: ((String) -> Unit)? = null,
    private val onCursorPos: ((x: Float, y: Float, screenW: Float, screenH: Float) -> Unit)? = null,
    private val onLog: (String) -> Unit
) {
    private val tag = "MoztroWebSocketClient"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable timeout for persistent connection
        .build()

    private val pingSeq = AtomicInteger(1)

    fun connect(ip: String, port: Int = 8765) {
        val url = "ws://$ip:$port"
        onLog("Connecting to $url...")
        onStateChanged(ConnectionState.CONNECTING, null)

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onLog("Connected to server WebSocket. Sending pairing request...")
                onStateChanged(ConnectionState.PAIRING_REQUESTED, null)
                sendPairRequest()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (bytes.size > 1) {
                    val header = bytes.get(0).toInt()
                    if (header == 0x01) { // Video JPEG Frame
                        try {
                            val data = bytes.toByteArray()
                            val bitmap = BitmapFactory.decodeByteArray(data, 1, data.size - 1)
                            if (bitmap != null) {
                                onOverdriveFrame?.invoke(bitmap)
                            }
                        } catch (_: Exception) {}
                    } else if (header == 0x02) { // Audio PCM Chunk
                        try {
                            val data = bytes.toByteArray()
                            val audioBytes = data.copyOfRange(1, data.size)
                            onOverdriveAudio?.invoke(audioBytes)
                        } catch (_: Exception) {}
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onLog("Server closing connection: $reason")
                onStateChanged(ConnectionState.DISCONNECTED, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onLog("WebSocket closed: $reason")
                onStateChanged(ConnectionState.DISCONNECTED, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onLog("WebSocket connection error: ${t.message}")
                onStateChanged(ConnectionState.FAILED, t.localizedMessage ?: "Connection failed")
            }
        })
    }

    private fun sendPairRequest() {
        val cleanDeviceName = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
            Build.MODEL
        } else {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        }
        val json = JSONObject().apply {
            put("type", "PAIR_REQUEST")
            put("deviceId", deviceId)
            put("deviceName", cleanDeviceName)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("osVersion", Build.VERSION.RELEASE)
        }
        webSocket?.send(json.toString())
    }

    fun sendPowerCommand(action: String): Boolean {
        val json = JSONObject().apply {
            put("type", "POWER_COMMAND")
            put("action", action)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendKeyEvent(key: String, modifiers: List<String> = emptyList()): Boolean {
        val json = JSONObject().apply {
            put("type", "KEY_EVENT")
            put("key", key)
            if (modifiers.isNotEmpty()) {
                val arr = org.json.JSONArray()
                modifiers.forEach { arr.put(it) }
                put("modifiers", arr)
            }
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendModifierEvent(modifier: String, isDown: Boolean): Boolean {
        val json = JSONObject().apply {
            put("type", "MODIFIER_EVENT")
            put("modifier", modifier)
            put("isDown", isDown)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendReleaseAllModifiers(): Boolean {
        val json = JSONObject().apply {
            put("type", "RELEASE_ALL_MODIFIERS")
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendMouseMove(dx: Float, dy: Float): Boolean {
        val json = JSONObject().apply {
            put("type", "MOUSE_MOVE")
            put("dx", dx)
            put("dy", dy)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendMouseClick(button: String): Boolean {
        val json = JSONObject().apply {
            put("type", "MOUSE_CLICK")
            put("button", button)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendMouseDown(button: String): Boolean {
        val json = JSONObject().apply {
            put("type", "MOUSE_DOWN")
            put("button", button)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendMouseUp(button: String): Boolean {
        val json = JSONObject().apply {
            put("type", "MOUSE_UP")
            put("button", button)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendMouseScroll(deltaY: Int): Boolean {
        val json = JSONObject().apply {
            put("type", "MOUSE_SCROLL")
            put("deltaY", deltaY)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendPing(): Int {
        val seq = pingSeq.getAndIncrement()
        val now = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("type", "PING")
            put("deviceId", deviceId)
            put("timestamp", now)
            put("seq", seq)
        }
        val sent = webSocket?.send(json.toString()) ?: false
        if (!sent) {
            onPingResult(PingRecord(seq = seq, rttMs = -1, timestamp = now, isSuccess = false))
        }
        return seq
    }

    fun sendClipboardText(text: String): Boolean {
        val json = JSONObject().apply {
            put("type", "CLIPBOARD_SEND")
            put("text", text)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendClipboardGetResponse(text: String): Boolean {
        val json = JSONObject().apply {
            put("type", "CLIPBOARD_GET_RESPONSE")
            put("text", text)
            put("success", true)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun requestClipboard(): Boolean {
        val json = JSONObject().apply {
            put("type", "CLIPBOARD_GET_REQUEST")
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendFileTransferProgress(transferId: String, status: String, progress: Float, speedFormatted: String, error: String? = null): Boolean {
        val json = JSONObject().apply {
            put("type", "FILE_TRANSFER_PROGRESS")
            put("transferId", transferId)
            put("status", status)
            put("progress", progress)
            put("speedFormatted", speedFormatted)
            if (error != null) put("error", error)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendVodStatus(enabled: Boolean, ftpUrl: String?, port: Int): Boolean {
        val json = JSONObject().apply {
            put("type", "VOD_STATUS")
            put("enabled", enabled)
            if (ftpUrl != null) put("ftpUrl", ftpUrl)
            put("port", port)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun requestScreenSources(): Boolean {
        val json = JSONObject().apply {
            put("type", "REQUEST_SCREEN_SOURCES")
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun startOverdriveStream(sourceId: String? = null, quality: String = "BALANCED", audioEnabled: Boolean = true): Boolean {
        val json = JSONObject().apply {
            put("type", "START_OVERDRIVE_STREAM")
            if (sourceId != null) put("sourceId", sourceId)
            put("quality", quality)
            put("audioEnabled", audioEnabled)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun stopOverdriveStream(): Boolean {
        val json = JSONObject().apply {
            put("type", "STOP_OVERDRIVE_STREAM")
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun switchOverdriveMonitor(sourceId: String): Boolean {
        val json = JSONObject().apply {
            put("type", "OVERDRIVE_CHANGE_MONITOR")
            put("sourceId", sourceId)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    fun sendOverdriveTouch(
        action: String,
        x: Float? = null,
        y: Float? = null,
        dx: Float? = null,
        dy: Float? = null,
        button: String? = null,
        deltaY: Int? = null
    ): Boolean {
        val json = JSONObject().apply {
            put("type", "OVERDRIVE_INPUT_TOUCH")
            put("action", action)
            if (x != null) put("x", x)
            if (y != null) put("y", y)
            if (dx != null) put("dx", dx)
            if (dy != null) put("dy", dy)
            if (button != null) put("button", button)
            if (deltaY != null) put("deltaY", deltaY)
            put("deviceId", deviceId)
            put("timestamp", System.currentTimeMillis())
        }
        return webSocket?.send(json.toString()) ?: false
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "PAIR_RESPONSE" -> {
                    val status = json.optString("status")
                    val message = json.optString("message")
                    val serverHostname = json.optString("serverHostname", "PC Host")
                    val mac = json.optString("mac")
                    if (!mac.isNullOrEmpty() && mac != "00:00:00:00:00:00") {
                        onMacReceived?.invoke(mac)
                    }

                    if (status == "ACCEPTED") {
                        onLog("Pairing accepted by $serverHostname")
                        onStateChanged(ConnectionState.CONNECTED, serverHostname)
                    } else if (status == "REJECTED") {
                        onLog("Pairing rejected by host PC: $message")
                        onStateChanged(ConnectionState.REJECTED, message)
                        disconnect()
                    } else {
                        onLog("Pairing timeout: $message")
                        onStateChanged(ConnectionState.FAILED, message)
                        disconnect()
                    }
                }
                "SCREEN_SOURCES_RESPONSE" -> {
                    val sourcesArray = json.optJSONArray("sources")
                    val list = mutableListOf<PcDisplaySource>()
                    if (sourcesArray != null) {
                        for (i in 0 until sourcesArray.length()) {
                            val sObj = sourcesArray.getJSONObject(i)
                            list.add(
                                PcDisplaySource(
                                    id = sObj.optString("id"),
                                    name = sObj.optString("name", "Display ${i + 1}"),
                                    width = sObj.optInt("width", 1920),
                                    height = sObj.optInt("height", 1080),
                                    isPrimary = sObj.optBoolean("isPrimary", false)
                                )
                            )
                        }
                    }
                    onScreenSourcesReceived?.invoke(list)
                }
                "OVERDRIVE_STREAM_STARTED" -> {
                    val width = json.optInt("width", 1920)
                    val height = json.optInt("height", 1080)
                    val sourceId = json.optString("sourceId", "")
                    onOverdriveStreamStarted?.invoke(width, height, sourceId)
                }
                "OVERDRIVE_STREAM_ERROR" -> {
                    val error = json.optString("error", "Stream error")
                    onOverdriveStreamError?.invoke(error)
                }
                "CURSOR_POS" -> {
                    val x = json.optDouble("x", 0.0).toFloat()
                    val y = json.optDouble("y", 0.0).toFloat()
                    val sw = json.optDouble("screenW", 1920.0).toFloat()
                    val sh = json.optDouble("screenH", 1080.0).toFloat()
                    onCursorPos?.invoke(x, y, sw, sh)
                }
                "CLIPBOARD_GET_REQUEST" -> {
                    onLog("Received CLIPBOARD_GET_REQUEST from PC")
                    onClipboardGetRequest?.invoke()
                }
                "CLIPBOARD_GET_RESPONSE" -> {
                    val clipboardText = json.optString("text", "")
                    onLog("Received clipboard content from PC (${clipboardText.length} chars)")
                    onClipboardResponse?.invoke(clipboardText)
                }
                "CLIPBOARD_SEND" -> {
                    val clipboardText = json.optString("text", "")
                    onLog("Received clipboard text from PC (${clipboardText.length} chars)")
                    onClipboardResponse?.invoke(clipboardText)
                }
                "CLIPBOARD_SEND_RESPONSE" -> {
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", "")
                    onLog("Clipboard send response: success=$success, $message")
                }
                "FILE_TRANSFER_START" -> {
                    val transferId = json.optString("transferId")
                    val fileName = json.optString("fileName")
                    val fileSize = json.optLong("fileSize")
                    val mimeType = json.optString("mimeType", "")
                    onLog("Incoming file transfer from PC: $fileName ($fileSize bytes)")
                    onFileTransferStart?.invoke(transferId, fileName, fileSize, mimeType)
                }
                "FILE_TRANSFER_CHUNK" -> {
                    val transferId = json.optString("transferId")
                    val chunkBase64 = json.optString("chunkBase64")
                    val chunkIndex = json.optInt("chunkIndex", 0)
                    val isLast = json.optBoolean("isLast", false)
                    onFileTransferChunk?.invoke(transferId, chunkBase64, chunkIndex, isLast)
                }
                "FILE_TRANSFER_CANCEL" -> {
                    val transferId = json.optString("transferId")
                    onLog("File transfer cancelled by PC: $transferId")
                    onFileTransferCancel?.invoke(transferId)
                }
                "PING" -> {
                    val timestamp = json.optLong("timestamp")
                    val seq = json.optInt("seq")
                    val pong = JSONObject().apply {
                        put("type", "PONG")
                        put("deviceId", deviceId)
                        put("clientTimestamp", timestamp)
                        put("seq", seq)
                    }
                    webSocket?.send(pong.toString())
                }
                "PONG" -> {
                    val clientTimestamp = json.optLong("clientTimestamp")
                    val seq = json.optInt("seq")
                    val now = System.currentTimeMillis()
                    val rtt = if (clientTimestamp > 0) (now - clientTimestamp) else 0L

                    onPingResult(PingRecord(seq = seq, rttMs = rtt, timestamp = now, isSuccess = true))
                }
                else -> {
                    onLog("Received message: $text")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse incoming message", e)
        }
    }

    fun disconnect() {
        try {
            val byeJson = JSONObject().apply {
                put("type", "DISCONNECT")
                put("deviceId", deviceId)
            }
            webSocket?.send(byeJson.toString())
            webSocket?.close(1000, "Client disconnect")
        } catch (_: Exception) {}
        webSocket = null
        onStateChanged(ConnectionState.DISCONNECTED, null)
    }
}
