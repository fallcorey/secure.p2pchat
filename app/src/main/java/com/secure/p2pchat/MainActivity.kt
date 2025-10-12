package com.secure.p2pchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatDisplay: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatDisplay = findViewById(R.id.chatDisplay)
    }
    
    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageInput.text.toString()
            if (message.isNotEmpty()) {
                displayMessage("You: $message")
                messageInput.text.clear()
            }
        }
    }
    
    private fun displayMessage(message: String) {
        val currentText = chatDisplay.text.toString()
        chatDisplay.text = if (currentText.isEmpty()) {
            message
        } else {
            "$currentText\n$message"
        }
    }
}
