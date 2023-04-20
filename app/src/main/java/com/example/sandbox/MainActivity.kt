package com.example.sandbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.webrtc.*
import timber.log.Timber
import java.util.logging.Level


const val VIDEO_TRACK_ID = "ARDAMSv0"

class MainActivity : AppCompatActivity() {

    private var isInitiator = false
    private var isChannelReady = false
    private var isStarted = false

    private lateinit var localVideoTrack: VideoTrack
    private val socketClient = SocketClient(object : SocketClient.ServerResponse {
        override fun join() {
            isChannelReady = true
        }

        override fun disconnected() {
            Timber.d("Disconnected")
            finish()
        }

        override fun joined() {
            isChannelReady = true
        }

        override fun created() {
            isInitiator = true
        }

        override fun offerMessage(response: WsMessage) {
            handleOnOfferCalled(response)
        }

        override fun answerMessage(response: WsMessage) {
            localPeerConnection.setRemoteDescription(
                object : SdbObserverSingle("4") {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        Timber.d("LocalRemote4")
                    }
                }, SessionDescription(
                    SessionDescription.Type.ANSWER, response.rtcMessage.sdp
                )
            )
        }

        override fun candidateMessage(response: WsMessage) {
            val sdpMid = response.rtcMessage.id
            val sdpMLineIndex = response.rtcMessage.label!!
            val candidate = response.rtcMessage.candidate
            val ice = IceCandidate(sdpMid, sdpMLineIndex, candidate)
            localPeerConnection.addIceCandidate(ice)
        }

        override fun gotMediaMessage() {
            maybeStart()
        }
    })

    private val rootEglBase = EglBase.create()
    private val surfaceTextureHelper =
        SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)

    private val peerConnectionFactory by lazy {
        Aux.buildPeerConnectionFactory(this@MainActivity, rootEglBase)
    }

    private val iceServer = mutableListOf<PeerConnection.IceServer>(
        PeerConnection.IceServer.builder("stun:relay.metered.ca:80").createIceServer(),

        PeerConnection.IceServer.builder("turn:relay.metered.ca:80")
            .setUsername("79d5ba37349dbd6d2eedb196").setPassword("oxnfWPJ0NQcp0FJC")
            .createIceServer(),

        PeerConnection.IceServer.builder("turn:relay.metered.ca:443")
            .setUsername("79d5ba37349dbd6d2eedb196").setPassword("oxnfWPJ0NQcp0FJC")
            .createIceServer(),

        PeerConnection.IceServer.builder("turn:relay.metered.ca:443?transport=tcp")
            .setUsername("79d5ba37349dbd6d2eedb196").setPassword("oxnfWPJ0NQcp0FJC")
            .createIceServer(),
    )

    val localPeerConnection: PeerConnection by lazy {
        Aux.buildPeerConnectionLocal(peerConnectionFactory,
            PeerConnection.RTCConfiguration(iceServer).apply {},
            { candidate: IceCandidate ->
                socketClient.sendCandidate(candidate)
            },
            { stream ->
                stream.videoTracks.getOrNull(0)?.apply {
                    Timber.d("add sink")
                    addSink((findViewById<SurfaceViewRenderer>(R.id.surfaceViewRemote)))
                    setEnabled(true)
                }
            })
    }

    private val constraints = MediaConstraints().apply {
        optional.add(MediaConstraints.KeyValuePair("RtpDataChannels", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        java.util.logging.Logger.getLogger("org.webrtc.Logging").level = Level.OFF

        val username = intent.getStringExtra(Aux.USERNAME)!!
        socketClient.openAndConnect(username)
        initializeSurfaceViews()
        createVideoTrackFromCameraAndShowIt()
    }

    private fun initializeSurfaceViews() {
        (findViewById<SurfaceViewRenderer>(R.id.surfaceViewLocal)).init(
            rootEglBase.eglBaseContext, null
        )

        (findViewById<SurfaceViewRenderer>(R.id.surfaceViewRemote)).init(
            rootEglBase.eglBaseContext, null
        )
    }

    private fun createVideoTrackFromCameraAndShowIt() {
        val videoCaptureManager = VideoCapturer.getVideoCapture(this)
        val videoSource: VideoSource = peerConnectionFactory.createVideoSource(false)
        videoCaptureManager.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        videoCaptureManager.startCapture(720, 1280, 30)
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        localVideoTrack.setEnabled(true)
        localVideoTrack.addSink(findViewById(R.id.surfaceViewLocal))
        localPeerConnection.addTrack(localVideoTrack)
        socketClient.gotUserMedia()
    }


    private fun maybeStart() {
        if (!isStarted && isChannelReady) {
            isStarted = true
            if (isInitiator) {
                localPeerConnection.createOffer(object : SdbObserverSingle("0") {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        Timber.d("descriptionCreated")
                        localPeerConnection.setLocalDescription(object : SdbObserverSingle("1") {
                            override fun onCreateSuccess(sdp: SessionDescription) {
                                Timber.d("LocalLocal")
                            }

                        }, sdp)

                        socketClient.sendOffer(sdp)
                    }
                }, constraints)
                Timber.d("CreateOffer")
            }
        }
    }

    private fun handleOnOfferCalled(response: WsMessage) {
        if (!isInitiator && !isStarted && isChannelReady) {
            isStarted = true
        }
        localPeerConnection.setRemoteDescription(
            object : SdbObserverSingle("5") {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    Timber.d("LocalRemote")
                }
            }, SessionDescription(
                SessionDescription.Type.OFFER, response.rtcMessage.sdp
            )
        )

        localPeerConnection.createAnswer(object : SdbObserverSingle("3") {
            override fun onCreateSuccess(sdp: SessionDescription) {
                localPeerConnection.setLocalDescription(object :
                    SdbObserverSingle("4") {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        Timber.d("LocalRemote4")
                    }
                }, sdp)

                socketClient.sendAnswer(sdp)
            }

        }, constraints)
    }

    override fun onPause() {
        super.onPause()
        socketClient.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("OnDestroy")
    }
}

