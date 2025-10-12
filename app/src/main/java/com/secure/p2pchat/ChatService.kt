package com.secure.p2pchat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.secure.p2pchat.av.AVCallManager
import com.secure.p2pchat.network.NetworkUtils
import com.secure.p2pchat.p2p.P2PManager
import com.secure.p2pchat.security.FileEncryptionManager

class ChatService : Service() {
    
    private lateinit var p2pManager: P2PManager
    private lateinit var avCallManager: AVCallManager
    private lateinit var fileEncryptionManager: FileEncryptionManager
    private lateinit var networkUtils: NetworkUtils
    
    companion object {
        private const val TAG = "ChatService"
        const val ACTION_START_DISCOVERY = "START_DISCOVERY"
        const val ACTION_STOP_DISCOVERY = "STOP_DISCOVERY"
        const val ACTION_START_VOICE_CALL = "START_VOICE_CALL"
        const val ACTION_END_CALL = "END_CALL"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ChatService created")
        
        p2pManager = P2PManager(this)
        avCallManager = AVCallManager(this)
        fileEncryptionManager = FileEncryptionManager(this)
        networkUtils = NetworkUtils(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DISCOVERY -> {
                p2pManager.startDiscovery()
                Log.d(TAG, "Discovery started via service")
            }
            ACTION_STOP_DISCOVERY -> {
                p2pManager.stopDiscovery()
                Log.d(TAG, "Discovery stopped via service")
            }
            ACTION_START_VOICE_CALL -> {
                val callId = avCallManager.startVoiceCall()
                Log.d(TAG, "Voice call started: $callId")
            }
            ACTION_END_CALL -> {
                avCallManager.endCall()
                Log.d(TAG, "Call ended via service")
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        p2pManager.stopDiscovery()
        avCallManager.endCall()
        Log.d(TAG, "ChatService destroyed")
    }
    
    fun getServiceStatus(): String {
        return buildString {
            append("Network: ${networkUtils.getNetworkType()}\n")
            append("P2P: ${p2pManager.getConnectionStatus()}\n")
            append("Call: ${avCallManager.getCallStatus()}\n")
            append("Features: ${avCallManager.getSupportedFeatures().size} available")
        }
    }
}
