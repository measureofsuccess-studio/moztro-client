package com.moztro.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object WakeOnLanHelper {
    private const val TAG = "WakeOnLanHelper"

    suspend fun sendWakeOnLan(context: Context, macAddress: String): Boolean = withContext(Dispatchers.IO) {
        var multicastLock: WifiManager.MulticastLock? = null
        var socket: DatagramSocket? = null

        try {
            val cleanMac = macAddress.replace(":", "").replace("-", "").trim()
            if (cleanMac.length != 12) {
                Log.e(TAG, "Invalid MAC Address format: $macAddress")
                return@withContext false
            }

            val macBytes = ByteArray(6)
            for (i in 0..5) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            // Build 102-byte magic packet: 6 bytes 0xFF + 16x MAC
            val bytes = ByteArray(6 + 16 * 6)
            for (i in 0..5) {
                bytes[i] = 0xff.toByte()
            }
            var i = 6
            while (i < bytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                i += macBytes.size
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("MoztroWolLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            socket = DatagramSocket().apply {
                broadcast = true
            }

            val targetAddresses = mutableSetOf<InetAddress>()
            targetAddresses.add(InetAddress.getByName("255.255.255.255"))

            // Add all subnet broadcast addresses
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (netIf in interfaces) {
                    if (netIf.isLoopback || !netIf.isUp) continue
                    for (addr in netIf.interfaceAddresses) {
                        val bcast = addr.broadcast
                        if (bcast != null) {
                            targetAddresses.add(bcast)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get interface broadcast IPs", e)
            }

            // Send on port 9 and port 7
            for (port in listOf(9, 7)) {
                for (addr in targetAddresses) {
                    try {
                        val packet = DatagramPacket(bytes, bytes.size, addr, port)
                        socket.send(packet)
                        Log.d(TAG, "Sent WOL packet to $addr:$port")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed sending WOL to $addr:$port", e)
                    }
                }
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WOL packet", e)
            return@withContext false
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
}
