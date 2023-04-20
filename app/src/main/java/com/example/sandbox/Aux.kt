package com.example.sandbox

import android.content.Context
import android.util.Log
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import timber.log.Timber

object Aux {

    val USERNAME = "username"

    fun buildPeerConnectionFactory(context: Context, rootEglBase: EglBase): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
        )

        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    rootEglBase.eglBaseContext,
                    true,
                    true
                )
            )
            .createPeerConnectionFactory()
    }

    fun buildPeerConnectionLocal(
        peerConnectionFactory: PeerConnectionFactory,
        rtcConfig: RTCConfiguration,
        gotIce: (IceCandidate) -> Unit,
        gotStream: (MediaStream) -> Unit
    ): PeerConnection {
        return peerConnectionFactory.createPeerConnection(rtcConfig, object :
            PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                Timber.d("signalling changeLocal $newState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Timber.d("OnIceStateChanged $newState")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                TODO("Not yet implemented")
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                Timber.d("OnIceGatheringChange $newState")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null) {
                    Timber.d("EndOfIce")
                    return
                }
                Timber.d("OnIce")
                gotIce.invoke(candidate)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                TODO("Not yet implemented")
            }

            override fun onAddStream(stream: MediaStream) {
                Timber.d("gotStream")
                gotStream.invoke(stream)
            }

            override fun onRemoveStream(stream: MediaStream?) {
                TODO("Not yet implemented")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                TODO("Not yet implemented")
            }

            override fun onRenegotiationNeeded() {
                Timber.d("Renegotiation")
            }

            override fun onAddTrack(
                receiver: RtpReceiver?,
                mediaStreams: Array<out MediaStream>
            ) {
                Timber.d("onAddTrack")
                gotStream.invoke(mediaStreams.first())
            }
        })!!
    }
}