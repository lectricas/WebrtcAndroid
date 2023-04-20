package com.example.sandbox

import com.google.gson.Gson
import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit
import timber.log.Timber


class SocketClient(private val messageListener: ServerResponse) : WebSocketListener() {

    private lateinit var ws: WebSocket
    val gson = Gson()

    private lateinit var username: String

    fun openAndConnect(username: String) {
        this.username = username
//        val token = "c822d612086340957a9517acec2d8204ba531f40"
//        val url = "ws://10.0.2.2:8000/ws/call/?token=$token"
        val url = "ws://10.0.2.2:8000/ws/socket-server/"
        val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        val request: Request = Request.Builder()
            .url(url)
            .build()

        ws = client.newWebSocket(request, this)

        sendCreateOrJoin()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {

    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val response = gson.fromJson(text, WsMessage::class.java)
        Timber.d("gotMessage $response")
        when (response.type) {
            "join" -> {
                messageListener.join()
            }
            "disconnected" -> {
                ws.close(0, "a")
                messageListener.disconnected()
            }
            "joined" -> {
                messageListener.joined()
            }
            "created" -> {
                messageListener.created()
            }
            "message" -> {
                when (response.rtcMessage.type) {
                    "offer" -> {
                        messageListener.offerMessage(response)
                    }
                    "answer" -> {
                        messageListener.answerMessage(response)
                    }
                    "candidate" -> {
                        messageListener.candidateMessage(response)
                    }
                    "got user media" -> {
                        messageListener.gotMediaMessage()
                    }
                }
            }
        }
    }

    private fun sendCreateOrJoin() {
        val json = JSONObject()
        json.put("type", "create or join")
        json.put("roomName", "foo")
        json.put("username", username)
        ws.send(json.toString())
    }

    fun sendOffer(sdp: SessionDescription) {
        val offer = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }
        val json = JSONObject()

        json.put("type", "message")
        json.put("rtcMessage", offer)

        ws.send(json.toString())
    }


    fun sendAnswer(sdp: SessionDescription) {
        val ans = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp.description)
        }

        val json = JSONObject()

        json.put("type", "message")
        json.put("rtcMessage", ans)

        ws.send(json.toString())
    }

    fun sendCandidate(candidate: IceCandidate) {
        val cand = JSONObject().apply {
            put("type", "candidate")
            put("id", candidate.sdpMid)
            put("label", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }

        val json = JSONObject()

        json.put("type", "message")
        json.put("rtcMessage", cand)
        ws.send(json.toString())
    }

    fun gotUserMedia() {
        val ansMedia = JSONObject().apply {
            put("type", "got user media")
        }

        val jsonMedia = JSONObject()

        jsonMedia.put("type", "message")
        jsonMedia.put("rtcMessage", ansMedia)

        ws.send(jsonMedia.toString())
    }

    fun close() {
        ws.close(1000, "a")
    }

    interface ServerResponse {
        fun join()
        fun joined()
        fun created()
        fun offerMessage(response: WsMessage)
        fun answerMessage(response: WsMessage)
        fun candidateMessage(response: WsMessage)
        fun gotMediaMessage()
        fun disconnected()
    }
}