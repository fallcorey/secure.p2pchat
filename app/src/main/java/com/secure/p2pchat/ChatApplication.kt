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
            // Security provider initialization
            // This ensures we're using the most secure crypto providers
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            Log.d(TAG, "Security providers initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Conscrypt not available, using default providers")
        }
    }
    
    private fun logAppInfo() {
        Log.i(TAG, """
            === Secure P2P Chat ===
            Version: 1.0
            Build Type: ${BuildConfig.BUILD_TYPE}
            Debug: ${BuildConfig.DEBUG}
            Security: Enabled
            Features: Text, Voice, Video, File Sharing
        """.trimIndent())
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminating")
    }
}
