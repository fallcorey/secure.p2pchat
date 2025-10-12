package com.secure.p2pchat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.secure.p2pchat.security.KeyStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityTest {

    private lateinit var keyStorage: KeyStorage

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        keyStorage = KeyStorage(context)
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

    @Test
    fun testLargeTextEncryption() {
        val originalText = "A".repeat(1000)
        
        val encrypted = keyStorage.encrypt(originalText)
        val decrypted = keyStorage.decrypt(encrypted)
        
        assertEquals(originalText, decrypted)
    }
}
