package com.example.sandbox

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import timber.log.Timber

abstract class SdbObserverSingle(val where: String) : SdpObserver {
    abstract override fun onCreateSuccess(sdp: SessionDescription)

    override fun onSetSuccess() {
        Timber.d("OnSetSuccess $where")
    }

    override fun onCreateFailure(error: String) {
        Timber.d("$where $error")
    }

    override fun onSetFailure(error: String) {
        Timber.d("$where $error")
    }
}