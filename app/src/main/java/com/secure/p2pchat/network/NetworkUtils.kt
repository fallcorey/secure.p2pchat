package com.secure.p2pchat.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class NetworkUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkUtils"
    }
    
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "No Network"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }
    
    fun getLocalNetworkInfo(): NetworkInfo {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        var wifiIp: String? = null
        var cellularIp: String? = null
        
        for (intf in interfaces) {
            for (addr in Collections.list(intf.inetAddresses)) {
                if (!addr.isLoopbackAddress && addr.hostAddress != null) {
                    val sAddr = addr.hostAddress
                    if (sAddr != null && sAddr.indexOf(':') < 0) {
                        when {
                            intf.name.startsWith("wlan") -> wifiIp = sAddr
                            intf.name.startsWith("rmnet") || intf.name.startsWith("pdp") -> cellularIp = sAddr
                        }
                    }
                }
            }
        }
        
        return NetworkInfo(
            wifiIp = wifiIp,
            cellularIp = cellularIp,
            networkType = getNetworkType(),
            isAvailable = isNetworkAvailable()
        )
    }
    
    fun isValidIPAddress(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip) != null
        } catch (e: Exception) {
            false
        }
    }
    
    data class NetworkInfo(
        val wifiIp: String?,
        val cellularIp: String?,
        val networkType: String,
        val isAvailable: Boolean
    ) {
        fun getBestAvailableIp(): String? {
            return wifiIp ?: cellularIp
        }
        
        override fun toString(): String {
            return "Network: $networkType, Available: $isAvailable, " +
                   "WiFi IP: ${wifiIp ?: "N/A"}, " +
                   "Cellular IP: ${cellularIp ?: "N/A"}"
        }
    }
}
