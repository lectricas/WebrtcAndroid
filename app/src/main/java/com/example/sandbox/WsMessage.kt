package com.example.sandbox

data class WsMessage(
    val type: String,
    val rtcMessage: RtcMessage
)

data class RtcMessage(
    val sdp: String?,
    val type: String?,
    val id: String?,
    val label: Int?,
    val candidate: String?
)