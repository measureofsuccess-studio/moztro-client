package com.moztro.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.moztro.app.data.DiscoveredServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class UdpDiscoveryClient(private val context: Context) {
    private val tag = "UdpDiscoveryClient"
    private val broadcastPort = 8766

    suspend fun discoverServers(
        timeoutMs: Long = 6000,
        onServerFound: (DiscoveredServer) -> Unit
    ) = withContext(Dispatchers.IO) {
        var multicastLock: WifiManager.MulticastLock? = null
        var socket: DatagramSocket? = null

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("MoztroDiscoveryLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 1500
            }

            val discoverMsg = "MOZTRO_DISCOVER".toByteArray()
            val broadcastAddresses = mutableSetOf<InetAddress>()
            broadcastAddresses.add(InetAddress.getByName("255.255.255.255"))

            // Enumerate standard network interfaces to get exact subnet broadcast IPs
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (netIf in interfaces) {
                    if (netIf.isLoopback || !netIf.isUp) continue
                    for (addr in netIf.interfaceAddresses) {
                        val bcast = addr.broadcast
                        if (bcast != null) {
                            broadcastAddresses.add(bcast)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to get interface broadcast addresses", e)
            }

            Log.d(tag, "Target broadcast addresses: $broadcastAddresses")

            // Send discovery packet to all target broadcast addresses
            fun sendBroadcastPackets() {
                for (addr in broadcastAddresses) {
                    try {
                        val packet = DatagramPacket(discoverMsg, discoverMsg.size, addr, broadcastPort)
                        socket?.send(packet)
                        Log.d(tag, "Sent discovery broadcast to $addr:$broadcastPort")
                    } catch (e: Exception) {
                        Log.w(tag, "Broadcast send failed to $addr", e)
                    }
                }
            }

            sendBroadcastPackets()

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - startTime) < timeoutMs) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    val senderIp = responsePacket.address.hostAddress ?: ""
                    val responseText = String(responsePacket.data, 0, responsePacket.length)
                    Log.d(tag, "Received UDP response from $senderIp: $responseText")

                    val json = JSONObject(responseText)
                    if (json.optString("type") == "MOZTRO_SERVER_ANNOUNCE") {
                        val server = DiscoveredServer(
                            hostname = json.optString("hostname", "PC Server"),
                            ip = json.optString("ip", senderIp),
                            mac = json.optString("mac", null),
                            wsPort = json.optInt("wsPort", 8765),
                            platform = json.optString("platform", "Windows"),
                            lastSeen = System.currentTimeMillis()
                        )
                        withContext(Dispatchers.Main) {
                            onServerFound(server)
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    // Re-broadcast
                    sendBroadcastPackets()
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(tag, "UDP Receive exception", e)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Discovery error", e)
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun sendPresenceBeacon(deviceId: String, deviceName: String) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            val json = JSONObject().apply {
                put("type", "MOZTRO_CLIENT_BEACON")
                put("deviceId", deviceId)
                put("deviceName", deviceName)
                put("timestamp", System.currentTimeMillis())
            }
            val data = json.toString().toByteArray()
            socket = DatagramSocket().apply { broadcast = true }

            val target = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(data, data.size, target, broadcastPort)
            socket.send(packet)
        } catch (e: Exception) {
            Log.w(tag, "Failed sending presence beacon", e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun sendPresenceBye(deviceId: String) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            val json = JSONObject().apply {
                put("type", "MOZTRO_CLIENT_BYE")
                put("deviceId", deviceId)
                put("timestamp", System.currentTimeMillis())
            }
            val data = json.toString().toByteArray()
            socket = DatagramSocket().apply { broadcast = true }

            val target = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(data, data.size, target, broadcastPort)
            socket.send(packet)
        } catch (e: Exception) {
            Log.w(tag, "Failed sending presence bye", e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
