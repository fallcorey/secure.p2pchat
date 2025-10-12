package com.secure.p2pchat

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConnectionActivity : AppCompatActivity() {
    
    private lateinit var p2pManager: P2PManager
    private lateinit var deviceIdText: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var discoveryToggle: ToggleButton
    private lateinit var connectButton: Button
    private lateinit var remoteIdInput: EditText
    private lateinit var devicesList: ListView
    private lateinit var chatButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        
        p2pManager = P2PManager(this)
        initializeViews()
        setupListeners()
        updateUI()
    }
    
    private fun initializeViews() {
        deviceIdText = findViewById(R.id.deviceIdText)
        connectionStatus = findViewById(R.id.connectionStatus)
        discoveryToggle = findViewById(R.id.discoveryToggle)
        connectButton = findViewById(R.id.connectButton)
        remoteIdInput = findViewById(R.id.remoteIdInput)
        devicesList = findViewById(R.id.devicesList)
        chatButton = findViewById(R.id.chatButton)
        
        deviceIdText.text = "Your ID: ${p2pManager.getDeviceName()}"
    }
    
    private fun setupListeners() {
        discoveryToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                p2pManager.startDiscovery()
                Toast.makeText(this, "Discovery enabled", Toast.LENGTH_SHORT).show()
            } else {
                p2pManager.stopDiscovery()
                Toast.makeText(this, "Discovery disabled", Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
        
        connectButton.setOnClickListener {
            val remoteId = remoteIdInput.text.toString()
            if (remoteId.isNotEmpty()) {
                Toast.makeText(this, "Connecting to $remoteId", Toast.LENGTH_SHORT).show()
                // Simulate connection
                connectionStatus.text = "Connected to $remoteId"
            } else {
                Toast.makeText(this, "Enter device ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Mock devices list
        val devices = arrayOf(
            "Device_ABC123 (192.168.1.101)", 
            "Device_DEF456 (192.168.1.102)",
            "Device_GHI789 (192.168.1.103)"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        devicesList.adapter = adapter
        
        devicesList.setOnItemClickListener { _, _, position, _ ->
            remoteIdInput.setText(devices[position].split(" ")[0])
        }
        
        chatButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun updateUI() {
        connectionStatus.text = p2pManager.getConnectionStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        p2pManager.stopDiscovery()
    }
}
