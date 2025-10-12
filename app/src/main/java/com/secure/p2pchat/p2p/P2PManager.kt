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
    private var isConnected = false
    private var deviceName = "Android_${UUID.randomUUID().toString().substring(0, 8)}"
    private var connectedDevice: String? = null
    
    companion object {
        private const val TAG = "P2PManager"
    }
    
    interface ConnectionListener {
        fun onConnectionEstablished(deviceName: String)
        fun onConnectionLost()
        fun onMessageReceived(message: String)
        fun onError(error: String)
    }
    
    private var connectionListener: ConnectionListener? = null
    
    fun setConnectionListener(listener: ConnectionListener) {
        this.connectionListener = listener
    }
    
    fun startDiscovery() {
        isDiscoverable = true
        Log.d(TAG, "P2P Discovery started for device: $deviceName")
        // Симуляция обнаружения устройств
        simulateDeviceDiscovery()
    }
    
    fun stopDiscovery() {
        isDiscoverable = false
        Log.d(TAG, "P2P Discovery stopped")
    }
    
    fun connectToDevice(deviceId: String): Boolean {
        return try {
            // Симуляция успешного подключения
            Thread {
                Thread.sleep(1000) // Имитация задержки сети
                isConnected = true
                connectedDevice = deviceId
                connectionListener?.onConnectionEstablished(deviceId)
            }.start()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device", e)
            connectionListener?.onError("Connection failed: ${e.message}")
            false
        }
    }
    
    fun disconnect() {
        isConnected = false
        connectedDevice = null
        connectionListener?.onConnectionLost()
        Log.d(TAG, "Disconnected from device")
    }
    
    fun sendMessage(message: String): Boolean {
        return if (isConnected) {
            try {
                val encryptedMessage = keyStorage.encrypt(message)
                Log.d(TAG, "Message encrypted: $encryptedMessage")
                
                // Симуляция отправки сообщения
                Thread {
                    Thread.sleep(500) // Имитация сетевой задержки
                    // Симуляция получения ответа
                    val response = "Echo: $message"
                    connectionListener?.onMessageReceived(response)
                }.start()
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                false
            }
        } else {
            Log.w(TAG, "Not connected, cannot send message")
            false
        }
    }
    
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress != null) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr
                        }
                    }
                }
            }
            null
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting IP address", ex)
            null
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
        Log.d(TAG, "Device name updated to: $name")
    }
    
    fun getDeviceName(): String {
        return deviceName
    }
    
    fun getConnectionStatus(): String {
        return when {
            isConnected && connectedDevice != null -> "Connected to $connectedDevice"
            isDiscoverable -> "Discoverable - ${getLocalIpAddress()}"
            else -> "Not connected"
        }
    }
    
    fun isConnected(): Boolean {
        return isConnected
    }
    
    fun getConnectedDevice(): String? {
        return connectedDevice
    }
    
    private fun simulateDeviceDiscovery() {
        // Симуляция обнаружения устройств в сети
        Thread {
            Thread.sleep(2000)
            if (isDiscoverable) {
                Log.d(TAG, "Simulated devices discovered in network")
                // В реальном приложении здесь был бы callback с найденными устройствами
            }
        }.start()
    }
    
    fun simulateIncomingMessage(message: String) {
        // Метод для тестирования входящих сообщений
        connectionListener?.onMessageReceived(message)
    }
    
    fun simulateIncomingConnection(deviceName: String) {
        // Метод для тестирования входящих подключений
        isConnected = true
        connectedDevice = deviceName
        connectionListener?.onConnectionEstablished(deviceName)
    }
}
