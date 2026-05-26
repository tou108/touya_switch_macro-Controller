package com.tou108.nxswitch.network

// ============================================================
//  ESP32WebSocketClient.kt
//  ESP32とのWebSocket通信を管理
// ============================================================

import android.util.Log
import com.tou108.nxswitch.controller.ControllerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

private const val TAG = "ESP32WsClient"

// 接続状態
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class ESP32WebSocketClient {

    // ─── 設定 ─────────────────────────────────────────────
    private val esp32Ip   = "192.168.4.1"   // ESP32 AP モードのIP
    private val esp32Port = 81

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ─── 状態Flow（UIが購読） ──────────────────────────────
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _switchConnected = MutableStateFlow(false)
    val switchConnected: StateFlow<Boolean> = _switchConnected

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    // ─── 接続 ─────────────────────────────────────────────
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING
        Log.d(TAG, "ESP32に接続中: ws://$esp32Ip:$esp32Port")

        val request = Request.Builder()
            .url("ws://$esp32Ip:$esp32Port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket接続成功")
                _connectionState.value = ConnectionState.CONNECTED
                _lastError.value = null
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.v(TAG, "受信: $text")
                // {"status":"ok","switch_connected":true}
                if (text.contains("\"switch_connected\":true")) {
                    _switchConnected.value = true
                } else if (text.contains("\"switch_connected\":false")) {
                    _switchConnected.value = false
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket エラー: ${t.message}")
                _connectionState.value = ConnectionState.ERROR
                _lastError.value = t.message
                _switchConnected.value = false
                // 3秒後に再接続
                scope.launch {
                    delay(3000)
                    if (_connectionState.value == ConnectionState.ERROR) {
                        connect()
                    }
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket クローズ: $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                _switchConnected.value = false
            }
        })
    }

    // ─── 切断 ─────────────────────────────────────────────
    fun disconnect() {
        webSocket?.close(1000, "ユーザーが切断")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _switchConnected.value = false
    }

    // ─── コントローラー状態を送信 ─────────────────────────
    fun sendControllerState(state: ControllerState): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        val json = state.toJson()
        Log.v(TAG, "送信: $json")
        return webSocket?.send(json) ?: false
    }

    // ─── ニュートラル送信（離す） ─────────────────────────
    fun sendNeutral() {
        sendControllerState(ControllerState.NEUTRAL)
    }
}
