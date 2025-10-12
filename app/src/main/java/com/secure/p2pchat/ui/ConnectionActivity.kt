package com.secure.p2pchat.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.secure.p2pchat.R
import com.secure.p2pchat.p2p.P2PManager

class ConnectionActivity : AppCompatActivity() {
    
    private lateinit var p2pManager: P2PManager
    private lateinit var deviceIdText: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var discoveryToggle: ToggleButton
    private lateinit var connectButton: Button
    private lateinit var remoteIdInput: EditText
    private lateinit var devicesList: ListView
    
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
        
        deviceIdText.text = "Ваш ID: ${p2pManager.getDeviceName()}"
    }
    
    private fun setupListeners() {
        discoveryToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                p2pManager.startDiscovery()
                Toast.makeText(this, "Обнаружение включено", Toast.LENGTH_SHORT).show()
            } else {
                p2pManager.stopDiscovery()
                Toast.makeText(this, "Обнаружение выключено", Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
        
        connectButton.setOnClickListener {
            val remoteId = remoteIdInput.text.toString()
            if (remoteId.isNotEmpty()) {
                // TODO: Реализовать подключение
                Toast.makeText(this, "Подключаемся к $remoteId", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Введите ID устройства", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Заглушка для списка устройств
        val devices = arrayOf("Устройство 1 (192.168.1.101)", "Устройство 2 (192.168.1.102)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        devicesList.adapter = adapter
        
        devicesList.setOnItemClickListener { _, _, position, _ ->
            remoteIdInput.setText(devices[position])
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
