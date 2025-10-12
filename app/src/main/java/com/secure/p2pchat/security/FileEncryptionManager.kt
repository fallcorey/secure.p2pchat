package com.secure.p2pchat.security

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class FileEncryptionManager(private val context: Context) {
    
    companion object {
        private const val ALGORITHM = "AES"
        private const TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        private const val BUFFER_SIZE = 8192
    }
    
    private val secureRandom = SecureRandom()
    
    fun encryptFile(inputUri: Uri, outputFile: File, key: SecretKey): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(inputUri)
            val outputStream = FileOutputStream(outputFile)
            
            // Generate random IV
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)
            
            // Write IV to beginning of file
            outputStream.write(iv)
            
            // Initialize cipher
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            
            // Encrypt file content
            inputStream?.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) {
                            output.write(encrypted)
                        }
                    }
                    val finalEncrypted = cipher.doFinal()
                    output.write(finalEncrypted)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun decryptFile(inputFile: File, outputFile: File, key: SecretKey): Boolean {
        return try {
            val inputStream = FileInputStream(inputFile)
            val outputStream = FileOutputStream(outputFile)
            
            // Read IV from beginning of file
            val iv = ByteArray(IV_LENGTH)
            inputStream.read(iv)
            
            // Initialize cipher
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            // Decrypt file content
            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val decrypted = cipher.update(buffer, 0, bytesRead)
                        if (decrypted != null) {
                            output.write(decrypted)
                        }
                    }
                    val finalDecrypted = cipher.doFinal()
                    output.write(finalDecrypted)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun generateFileKey(): SecretKey {
        val keyBytes = ByteArray(KEY_SIZE / 8)
        secureRandom.nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    fun keyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.DEFAULT)
    }
    
    fun stringToKey(keyString: String): SecretKey {
        val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    fun calculateFileHash(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            Base64.encodeToString(digest.digest(), Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error calculating hash"
        }
    }
}
