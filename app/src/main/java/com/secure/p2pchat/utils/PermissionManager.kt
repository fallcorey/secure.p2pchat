package com.secure.p2pchat.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    companion object {
        const val REQUEST_CODE_ALL_PERMISSIONS = 1001
        
        // Permission groups
        val AUDIO_PERMISSIONS = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        
        val STORAGE_PERMISSIONS = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val NETWORK_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        )
        
        val CAMERA_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA
        )
    }
    
    fun checkAudioPermissions(): Boolean {
        return AUDIO_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun checkStoragePermissions(): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun checkNetworkPermissions(): Boolean {
        return NETWORK_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun checkCameraPermissions(): Boolean {
        return CAMERA_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestAudioPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            AUDIO_PERMISSIONS,
            REQUEST_CODE_ALL_PERMISSIONS
        )
    }
    
    fun requestStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            STORAGE_PERMISSIONS,
            REQUEST_CODE_ALL_PERMISSIONS
        )
    }
    
    fun requestCameraPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            CAMERA_PERMISSIONS,
            REQUEST_CODE_ALL_PERMISSIONS
        )
    }
    
    fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    fun getMissingPermissions(): List<String> {
        val allPermissions = AUDIO_PERMISSIONS + STORAGE_PERMISSIONS + NETWORK_PERMISSIONS + CAMERA_PERMISSIONS
        return allPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun getPermissionStatus(): Map<String, Boolean> {
        val allPermissions = AUDIO_PERMISSIONS + STORAGE_PERMISSIONS + NETWORK_PERMISSIONS + CAMERA_PERMISSIONS
        return allPermissions.associateWith { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun areAllPermissionsGranted(): Boolean {
        return getMissingPermissions().isEmpty()
    }
}
