package com.secure.p2pchat.p2p

import android.content.Context
import android.util.Log
import com.secure.p2pchat.security.KeyStorage
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class RealP2PManager(private val context: Context) {
    
    private val keyStorage = KeyStorage(context)
    private val executor = Executors.newFixedThreadPool(4)
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var isServerRunning = false
    private var isConnected = false
    private var deviceName = "Device_${UUID.randomUUID().toString().substring(0, 8)}"
    private val connectedClients = HashMap<String, Socket>()
    private val discoveryPort = 8888
    private val messagePort = 8889
    
    companion object {
        private const val TAG = "RealP2PManager"
        private const val BUFFER_SIZE = 8192
        private const val DISCOVERY_TIMEOUT = 5000
    }
    
    interface P2PListener {
        fun onDeviceDiscovered(deviceInfo: DeviceInfo)
        fun onConnectionEstablished(deviceName: String, isHost: Boolean)
        fun onConnectionLost()
        fun onMessageReceived(message: String, fromDevice: String)
        fun onFileReceived(fileName: String, filePath: String, fromDevice: String)
        fun onError(error: String)
    }
    
    data class DeviceInfo(
        val deviceName: String,
        val ipAddress: String,
        val port: Int,
        val lastSeen: Long
    )
    
    private var p2pListener: P2PListener? = null
    
    fun setP2PListener(listener: P2PListener) {
        this.p2pListener = listener
    }
    
    // === Discovery ===
    fun startDiscovery() {
        executor.execute {
            try {
                // Broadcast discovery
                val broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = DISCOVERY_TIMEOUT
                }
                
                val broadcastMessage = "P2P_DISCOVERY:$deviceName:$messagePort"
                val broadcastData = broadcastMessage.toByteArray()
                
                // Send broadcast to all devices in network
                val broadcastPacket = DatagramPacket(
                    broadcastData,
                    broadcastData.size,
                    InetAddress.getByName("255.255.255.255"),
                    discoveryPort
                )
                
                broadcastSocket.send(broadcastPacket)
                Log.d(TAG, "Discovery broadcast sent: $broadcastMessage")
                
                // Listen for responses
                val buffer = ByteArray(1024)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT) {
                    try {
                        broadcastSocket.receive(responsePacket)
                        val response = String(responsePacket.data, 0, responsePacket.length)
                        if (response.startsWith("P2P_RESPONSE:")) {
                            val parts = response.split(":")
                            if (parts.size >= 3) {
                                val discoveredDevice = DeviceInfo(
                                    deviceName = parts[1],
                                    ipAddress = responsePacket.address.hostAddress,
                                    port = parts[2].toInt(),
                                    lastSeen = System.currentTimeMillis()
                                )
                                p2pListener?.onDeviceDiscovered(discoveredDevice)
                                Log.d(TAG, "Discovered device: ${discoveredDevice.deviceName} at ${discoveredDevice.ipAddress}")
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // Continue listening
                    }
                }
                
                broadcastSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Discovery failed", e)
                p2pListener?.onError("Discovery failed: ${e.message}")
            }
        }
        
        // Start listening for discovery requests
        startDiscoveryServer()
    }
    
    private fun startDiscoveryServer() {
        executor.execute {
            try {
                val serverSocket = DatagramSocket(discoveryPort).apply {
                    broadcast = true
                }
                
                val buffer = ByteArray(1024)
                
                while (!Thread.currentThread().isInterrupted) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    serverSocket.receive(packet)
                    
                    val message = String(packet.data, 0, packet.length)
                    if (message.startsWith("P2P_DISCOVERY:")) {
                        val parts = message.split(":")
                        if (parts.size >= 3) {
                            // Send response
                            val response = "P2P_RESPONSE:$deviceName:$messagePort"
                            val responseData = response.toByteArray()
                            val responsePacket = DatagramPacket(
                                responseData,
                                responseData.size,
                                packet.address,
                                packet.port
                            )
                            serverSocket.send(responsePacket)
                            
                            val discoveredDevice = DeviceInfo(
                                deviceName = parts[1],
                                ipAddress = packet.address.hostAddress,
                                port = parts[2].toInt(),
                                lastSeen = System.currentTimeMillis()
                            )
                            p2pListener?.onDeviceDiscovered(discoveredDevice)
                        }
                    }
                }
                
                serverSocket.close()
            } catch (e: Exception) {
                if (!Thread.currentThread().isInterrupted) {
                    Log.e(TAG, "Discovery server failed", e)
                }
            }
        }
    }
    
    // === Connection ===
    fun startServer() {
        executor.execute {
            try {
                serverSocket = ServerSocket(messagePort).apply {
                    soTimeout = 0
                }
                isServerRunning = true
                
                Log.d(TAG, "P2P server started on port $messagePort")
                
                while (isServerRunning) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        val clientAddress = clientSocket.inetAddress.hostAddress
                        
                        executor.execute {
                            handleClientConnection(clientSocket, clientAddress)
                        }
                    } catch (e: Exception) {
                        if (isServerRunning) {
                            Log.e(TAG, "Server accept failed", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
                p2pListener?.onError("Failed to start server: ${e.message}")
            }
        }
    }
    
    fun connectToDevice(ipAddress: String, port: Int = messagePort): Boolean {
        return try {
            executor.execute {
                try {
                    clientSocket = Socket(ipAddress, port).apply {
                        soTimeout = 30000
                    }
                    outputStream = DataOutputStream(clientSocket!!.getOutputStream())
                    inputStream = DataInputStream(clientSocket!!.getInputStream())
                    
                    isConnected = true
                    
                    // Send handshake
                    sendHandshake()
                    
                    p2pListener?.onConnectionEstablished(ipAddress, false)
                    Log.d(TAG, "Connected to device at $ipAddress:$port")
                    
                    // Start listening for messages
                    listenForMessages()
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    p2pListener?.onError("Connection failed: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun handleClientConnection(socket: Socket, clientAddress: String) {
        try {
            val clientInputStream = DataInputStream(socket.getInputStream())
            val clientOutputStream = DataOutputStream(socket.getOutputStream())
            
            // Read handshake
            val handshake = clientInputStream.readUTF()
            if (handshake.startsWith("HANDSHAKE:")) {
                val clientName = handshake.removePrefix("HANDSHAKE:")
                connectedClients[clientAddress] = socket
                
                isConnected = true
                clientSocket = socket
                outputStream = clientOutputStream
                inputStream = clientInputStream
                
                p2pListener?.onConnectionEstablished(clientName, true)
                Log.d(TAG, "Client connected: $clientName from $clientAddress")
                
                // Start listening for messages from this client
                listenForMessages()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client connection handling failed", e)
            connectedClients.remove(clientAddress)
            socket.close()
        }
    }
    
    // === Messaging ===
    fun sendMessage(message: String): Boolean {
        return if (isConnected && outputStream != null) {
            try {
                val encryptedMessage = keyStorage.encrypt(message)
                val messageData = "MESSAGE:$encryptedMessage"
                outputStream!!.writeUTF(messageData)
                outputStream!!.flush()
                Log.d(TAG, "Message sent: ${message.take(50)}...")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                false
            }
        } else {
            false
        }
    }
    
    fun sendFile(filePath: String, fileName: String): Boolean {
        return if (isConnected && outputStream != null) {
            try {
                val file = File(filePath)
                if (!file.exists()) return false
                
                val fileInputStream = FileInputStream(file)
                val fileBuffer = ByteArray(BUFFER_SIZE)
                
                // Send file header
                outputStream!!.writeUTF("FILE_START:$fileName:${file.length()}")
                
                // Send file data
                var bytesRead: Int
                while (fileInputStream.read(fileBuffer).also { bytesRead = it } != -1) {
                    outputStream!!.write(fileBuffer, 0, bytesRead)
                }
                
                // Send file end marker
                outputStream!!.writeUTF("FILE_END:$fileName")
                outputStream!!.flush()
                fileInputStream.close()
                
                Log.d(TAG, "File sent: $fileName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send file", e)
                false
            }
        } else {
            false
        }
    }
    
    private fun listenForMessages() {
        executor.execute {
            try {
                while (isConnected && inputStream != null) {
                    val message = inputStream!!.readUTF()
                    
                    when {
                        message.startsWith("MESSAGE:") -> {
                            val encryptedMessage = message.removePrefix("MESSAGE:")
                            val decryptedMessage = keyStorage.decrypt(encryptedMessage)
                            p2pListener?.onMessageReceived(decryptedMessage ?: "Decryption failed", "Remote Device")
                        }
                        message.startsWith("FILE_START:") -> {
                            // Handle file reception
                            val parts = message.removePrefix("FILE_START:").split(":")
                            if (parts.size >= 2) {
                                receiveFile(parts[0], parts[1].toLong())
                            }
                        }
                        message == "FILE_END" -> {
                            // File transfer complete
                        }
                    }
                }
            } catch (e: Exception) {
                if (isConnected) {
                    Log.e(TAG, "Message listening failed", e)
                    disconnect()
                }
            }
        }
    }
    
    private fun receiveFile(fileName: String, fileSize: Long) {
        executor.execute {
            try {
                val outputDir = File(context.filesDir, "received_files")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                val outputFile = File(outputDir, fileName)
                val fileOutputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(BUFFER_SIZE)
                
                var remainingBytes = fileSize
                while (remainingBytes > 0 && inputStream != null) {
                    val bytesToRead = minOf(remainingBytes, BUFFER_SIZE.toLong()).toInt()
                    val bytesRead = inputStream!!.read(buffer, 0, bytesToRead)
                    if (bytesRead == -1) break
                    
                    fileOutputStream.write(buffer, 0, bytesRead)
                    remainingBytes -= bytesRead
                }
                
                fileOutputStream.close()
                p2pListener?.onFileReceived(fileName, outputFile.absolutePath, "Remote Device")
                Log.d(TAG, "File received: $fileName")
            } catch (e: Exception) {
                Log.e(TAG, "File reception failed", e)
            }
        }
    }
    
    private fun sendHandshake() {
        outputStream?.writeUTF("HANDSHAKE:$deviceName")
        outputStream?.flush()
    }
    
    // === Management ===
    fun disconnect() {
        isConnected = false
        isServerRunning = false
        
        try {
            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
            
            connectedClients.values.forEach { it.close() }
            connectedClients.clear()
            
            p2pListener?.onConnectionLost()
            Log.d(TAG, "Disconnected from all devices")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }
    
    fun setDeviceName(name: String) {
        deviceName = name
    }
    
    fun getDeviceName(): String {
        return deviceName
    }
    
    fun getConnectionStatus(): String {
        return when {
            isConnected -> "Connected"
            isServerRunning -> "Waiting for connections"
            else -> "Disconnected"
        }
    }
    
    fun isConnected(): Boolean {
        return isConnected
    }
    
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
