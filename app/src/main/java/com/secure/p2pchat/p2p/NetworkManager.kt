package com.secure.p2pchat.p2p

import android.content.Context
import android.util.Log
import com.secure.p2pchat.security.KeyStorage
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class NetworkManager(private val context: Context) {
    
    private val keyStorage = KeyStorage(context)
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var isConnected = false
    private val messageListeners = mutableListOf<MessageListener>()
    
    companion object {
        private const val TAG = "NetworkManager"
        private const val WS_SERVER = "wss://echo.websocket.org" // Заглушка для тестирования
    }
    
    interface MessageListener {
        fun onMessageReceived(message: String)
        fun onConnectionStatusChanged(connected: Boolean)
        fun onError(error: String)
    }
    
    fun addMessageListener(listener: MessageListener) {
        messageListeners.add(listener)
    }
    
    fun removeMessageListener(listener: MessageListener) {
        messageListeners.remove(listener)
    }
    
    fun connect(serverUrl: String = WS_SERVER) {
        try {
            client = OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            
            val request = Request.Builder()
                .url(serverUrl)
                .build()
            
            webSocket = client?.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connection opened")
                    isConnected = true
                    messageListeners.forEach { it.onConnectionStatusChanged(true) }
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Message received: $text")
                    try {
                        val json = JSONObject(text)
                        val encryptedMessage = json.getString("message")
                        val decryptedMessage = keyStorage.decrypt(encryptedMessage)
                        messageListeners.forEach { it.onMessageReceived(decryptedMessage ?: "Decryption failed") }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing message", e)
                    }
                }
                
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Log.d(TAG, "Binary message received")
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $code - $reason")
                    isConnected = false
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code - $reason")
                    isConnected = false
                    messageListeners.forEach { it.onConnectionStatusChanged(false) }
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket connection failed", t)
                    isConnected = false
                    messageListeners.forEach { 
                        it.onConnectionStatusChanged(false)
                        it.onError(t.message ?: "Connection failed")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            messageListeners.forEach { it.onError(e.message ?: "Connection failed") }
        }
    }
    
    fun sendMessage(message: String): Boolean {
        return if (isConnected && webSocket != null) {
            try {
                val encryptedMessage = keyStorage.encrypt(message)
                val json = JSONObject().apply {
                    put("message", encryptedMessage)
                    put("timestamp", System.currentTimeMillis())
                    put("type", "text")
                }
                webSocket?.send(json.toString())
                Log.d(TAG, "Message sent successfully")
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
    
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client?.dispatcher?.executorService?.shutdown()
        isConnected = false
        messageListeners.forEach { it.onConnectionStatusChanged(false) }
        Log.d(TAG, "Disconnected from server")
    }
    
    fun getConnectionStatus(): String {
        return if (isConnected) "Connected to server" else "Disconnected"
    }
    
    fun isConnected(): Boolean {
        return isConnected
    }
    
    fun simulateP2PConnection(targetDevice: String): Boolean {
        // Симуляция P2P подключения
        Log.d(TAG, "Simulating P2P connection to: $targetDevice")
        return true
    }
}
