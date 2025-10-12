package com.secure.p2pchat

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.secure.p2pchat.p2p.P2PManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var p2pManager: P2PManager
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatDisplay: TextView
    private lateinit var statusText: TextView
    private lateinit var discoveryToggle: ToggleButton
    private lateinit var deviceNameInput: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        p2pManager = P2PManager(this)
        initializeViews()
        setupClickListeners()
        updateStatus()
    }
    
    private fun initializeViews() {
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatDisplay = findViewById(R.id.chatDisplay)
        statusText = findViewById(R.id.statusText)
        discoveryToggle = findViewById(R.id.discoveryToggle)
        deviceNameInput = findViewById(R.id.deviceNameInput)
        
        deviceNameInput.setText(p2pManager.getDeviceName())
    }
    
    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageInput.text.toString()
            if (message.isNotEmpty()) {
                if (p2pManager.sendMessage(message)) {
                    displayMessage("You: $message")
                    messageInput.text.clear()
                } else {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        discoveryToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                p2pManager.startDiscovery()
            } else {
                p2pManager.stopDiscovery()
            }
            updateStatus()
        }
        
        deviceNameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newName = deviceNameInput.text.toString()
                if (newName.isNotEmpty()) {
                    p2pManager.setDeviceName(newName)
                    Toast.makeText(this, "Device name updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun displayMessage(message: String) {
        runOnUiThread {
            val currentText = chatDisplay.text.toString()
            chatDisplay.text = if (currentText.isEmpty()) {
                message
            } else {
                "$currentText\n$message"
            }
        }
    }
    
    private fun updateStatus() {
        statusText.text = p2pManager.getConnectionStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        p2pManager.stopDiscovery()
    }
}
