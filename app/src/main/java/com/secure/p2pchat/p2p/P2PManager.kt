package com.secure.p2pchat.p2p

import android.content.Context
import android.util.Log
import com.secure.p2pchat.security.KeyStorage
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class P2PManager(private val context: Context) {
    
    private val keyStorage = KeyStorage(context)
    private var isDiscoverable = false
    private var deviceName = "Android_${UUID.randomUUID().toString().substring(0, 8)}"
    
    companion object {
        private const val TAG = "P2PManager"
        private const val DISCOVERY_PORT = 8888
    }
    
    fun startDiscovery() {
        isDiscoverable = true
        Log.d(TAG, "P2P Discovery started for device: $deviceName")
        // TODO: Implement network discovery
    }
    
    fun stopDiscovery() {
        isDiscoverable = false
        Log.d(TAG, "P2P Discovery stopped")
    }
    
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting IP address", ex)
        }
        return null
    }
    
    fun sendMessage(message: String, targetIp: String? = null): Boolean {
        return try {
            val encryptedMessage = keyStorage.encrypt(message)
            Log.d(TAG, "Message encrypted and ready to send to: $targetIp")
            // TODO: Implement message sending
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }
    
    fun receiveMessage(encryptedMessage: String): String? {
        return try {
            keyStorage.decrypt(encryptedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting message", e)
            null
        }
    }
    
    fun setDeviceName(name: String) {
        deviceName = name
    }
    
    fun getDeviceName(): String {
        return deviceName
    }
    
    fun getConnectionStatus(): String {
        return if (isDiscoverable) "Discoverable - ${getLocalIpAddress()}" else "Not discoverable"
    }
}
