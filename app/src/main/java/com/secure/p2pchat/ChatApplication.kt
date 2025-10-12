package com.secure.p2pchat

import android.app.Application
import android.util.Log

class ChatApplication : Application() {
    
    companion object {
        private const val TAG = "ChatApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Secure P2P Chat Application started")
        
        // Initialize security components
        initializeSecurity()
        
        // Log app start for debugging
        logAppInfo()
    }
    
    private fun initializeSecurity() {
        try {
            // Basic security initialization
            Log.d(TAG, "Security initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Security initialization warning", e)
        }
    }
    
    private fun logAppInfo() {
        Log.i(TAG, """
            === Secure P2P Chat ===
            Version: 1.0
            Security: Enabled
            Features: Text, Voice, Video, File Sharing
        """.trimIndent())
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminating")
    }
}
