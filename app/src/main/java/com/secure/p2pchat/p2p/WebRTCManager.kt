package com.secure.p2pchat.p2p

import android.content.Context
import android.util.Log
import com.secure.p2pchat.security.KeyStorage
import org.webrtc.*
import java.util.*
import org.webrtc.PeerConnectionFactory as PCFactory

class WebRTCManager(private val context: Context) {
    
    private val keyStorage = KeyStorage(context)
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )
    
    private var localSdpObserver = object : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {
            Log.d(TAG, "Local SDP created: ${desc?.description}")
            desc?.let { setLocalDescription(it) }
        }
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Log.e(TAG, "SDP create failed: $error")
        override fun onSetFailure(error: String?) = Log.e(TAG, "SDP set failed: $error")
    }
    
    private var remoteSdpObserver = object : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) = Unit
        override fun onSetSuccess() {
            Log.d(TAG, "Remote SDP set successfully")
            if (peerConnection?.remoteDescription != null) {
                createAnswer()
            }
        }
        override fun onCreateFailure(error: String?) = Log.e(TAG, "Remote SDP create failed: $error")
        override fun onSetFailure(error: String?) = Log.e(TAG, "Remote SDP set failed: $error")
    }
    
    companion object {
        private const val TAG = "WebRTCManager"
        private const val DATA_CHANNEL_LABEL = "secure_chat"
    }
    
    interface ConnectionListener {
        fun onConnectionSuccess()
        fun onConnectionFailed(error: String)
        fun onMessageReceived(message: String)
        fun onDataChannelReady()
    }
    
    private var connectionListener: ConnectionListener? = null
    
    fun initialize() {
        try {
            PCFactory.initialize(
                PCFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
            
            val options = PCFactory.Options().apply {
                networkIgnoreMask = 0
                disableEncryption = false
                disableNetworkMonitor = false
            }
            
            peerConnectionFactory = PCFactory.createPeerConnectionFactory(options)
            Log.d(TAG, "WebRTC initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "WebRTC initialization failed", e)
        }
    }
    
    fun createPeerConnection(listener: ConnectionListener) {
        this.connectionListener = listener
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            keyType = PeerConnection.KeyType.ECDSA
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
            enableDtlsSrtp = true
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state: $state")
            }
            
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        connectionListener?.onConnectionSuccess()
                        createDataChannel()
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        connectionListener?.onConnectionFailed("ICE connection failed")
                    }
                    else -> Unit
                }
            }
            
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gathering state: $state")
            }
            
            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d(TAG, "New ICE candidate: ${candidate?.sdp}")
            }
            
            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d(TAG, "Data channel received")
                dataChannel?.let { setupDataChannel(it) }
            }
            
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }
            
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                Log.d(TAG, "Track added")
            }
            
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "Peer connection state: $newState")
            }
            
            override fun onIceCandidateError(event: PeerConnection.IceCandidateErrorEvent?) {
                Log.e(TAG, "ICE candidate error: ${event?.errorCode} - ${event?.errorText}")
            }
            
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "Standardized ICE connection state: $newState")
            }
            
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "ICE candidates removed")
            }
            
            override fun onRemoveTrack(rtpReceiver: RtpReceiver?) {
                Log.d(TAG, "Track removed")
            }
            
            override fun onAddStream(mediaStream: MediaStream?) {
                Log.d(TAG, "Stream added")
            }
            
            override fun onRemoveStream(mediaStream: MediaStream?) {
                Log.d(TAG, "Stream removed")
            }
        })
    }
    
    fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        
        peerConnection?.createOffer(localSdpObserver, constraints)
    }
    
    fun setRemoteDescription(sdp: String, type: String) {
        val sessionDescription = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(type),
            sdp
        )
        peerConnection?.setRemoteDescription(remoteSdpObserver, sessionDescription)
    }
    
    fun setLocalDescription(sdp: SessionDescription) {
        peerConnection?.setLocalDescription(localSdpObserver, sdp)
    }
    
    fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        
        peerConnection?.createAnswer(localSdpObserver, constraints)
    }
    
    private fun createDataChannel() {
        val dataChannelInit = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = -1
            protocol = "secure-chat"
        }
        
        dataChannel = peerConnection?.createDataChannel(DATA_CHANNEL_LABEL, dataChannelInit)
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {
                Log.d(TAG, "Buffered amount changed: $amount")
            }
            
            override fun onStateChange() {
                Log.d(TAG, "Data channel state: ${dataChannel?.state()}")
                if (dataChannel?.state() == DataChannel.State.OPEN) {
                    connectionListener?.onDataChannelReady()
                }
            }
            
            override fun onMessage(buffer: DataChannel.Buffer) {
                val message = String(buffer.data.array(), Charsets.UTF_8)
                val decryptedMessage = keyStorage.decrypt(message)
                Log.d(TAG, "Message received: $decryptedMessage")
                connectionListener?.onMessageReceived(decryptedMessage ?: "Decryption failed")
            }
        })
    }
    
    private fun setupDataChannel(channel: DataChannel) {
        dataChannel = channel
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) = Unit
            override fun onStateChange() {
                if (dataChannel?.state() == DataChannel.State.OPEN) {
                    connectionListener?.onDataChannelReady()
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val message = String(buffer.data.array(), Charsets.UTF_8)
                val decryptedMessage = keyStorage.decrypt(message)
                connectionListener?.onMessageReceived(decryptedMessage ?: "Decryption failed")
            }
        })
    }
    
    fun sendMessage(message: String): Boolean {
        return try {
            val encryptedMessage = keyStorage.encrypt(message)
            dataChannel?.send(DataChannel.Buffer(encryptedMessage.toByteArray(Charsets.UTF_8), false))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            false
        }
    }
    
    fun getLocalDescription(): String? {
        return peerConnection?.localDescription?.description
    }
    
    fun close() {
        dataChannel?.close()
        peerConnection?.close()
        peerConnectionFactory?.dispose()
        Log.d(TAG, "WebRTC connection closed")
    }
}
