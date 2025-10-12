package com.secure.p2pchat

import com.secure.p2pchat.security.KeyStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SecurityTest {

    private lateinit var keyStorage: KeyStorage

    @Before
    fun setup() {
        // Mock context for testing
        keyStorage = KeyStorage(object : android.content.Context() {
            override fun getApplicationContext(): android.content.Context = this
            override fun getPackageName(): String = "com.secure.p2pchat.test"
            override fun getPackageManager(): android.content.pm.PackageManager {
                throw UnsupportedOperationException()
            }
            override fun getResources(): android.content.res.Resources {
                throw UnsupportedOperationException()
            }
            override fun getTheme(): android.content.res.Resources.Theme {
                throw UnsupportedOperationException()
            }
            override fun getSystemService(name: String): Any? {
                return null
            }
            // Add other required abstract methods...
        })
    }

    @Test
    fun testEncryptionDecryption() {
        val originalText = "Hello, Secure P2P World!"
        
        val encrypted = keyStorage.encrypt(originalText)
        val decrypted = keyStorage.decrypt(encrypted)
        
        assertEquals(originalText, decrypted)
        assertNotEquals(originalText, encrypted)
    }

    @Test
    fun testEmptyStringEncryption() {
        val originalText = ""
        
        val encrypted = keyStorage.encrypt(originalText)
        val decrypted = keyStorage.decrypt(encrypted)
        
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testSpecialCharacters() {
        val originalText = "🔐 Secure Chat! 123 @#\$%"
        
        val encrypted = keyStorage.encrypt(originalText)
        val decrypted = keyStorage.decrypt(encrypted)
        
        assertEquals(originalText, decrypted)
    }
}
