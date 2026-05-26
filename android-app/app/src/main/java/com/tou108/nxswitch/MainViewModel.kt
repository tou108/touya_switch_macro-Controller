package com.tou108.nxswitch

// ============================================================
//  MainViewModel.kt
//  アプリ全体の状態を管理するViewModel
// ============================================================

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tou108.nxswitch.controller.ControllerState
import com.tou108.nxswitch.macro.MacroManager
import com.tou108.nxswitch.macro.MacroStatus
import com.tou108.nxswitch.network.ConnectionState
import com.tou108.nxswitch.network.ESP32WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ─── WebSocket クライアント ────────────────────────────
    val wsClient = ESP32WebSocketClient()

    // ─── マクロマネージャー ───────────────────────────────
    val macroManager = MacroManager(application, wsClient)

    // ─── コントローラー現在状態 ───────────────────────────
    private val _controllerState = MutableStateFlow(ControllerState.NEUTRAL)
    val controllerState: StateFlow<ControllerState> = _controllerState

    // ─── 接続状態（WS経由でESP32へ） ─────────────────────
    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    // ─── Switch接続状態（BT経由） ─────────────────────────
    val switchConnected: StateFlow<Boolean> = wsClient.switchConnected
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─── マクロ状態 ───────────────────────────────────────
    val macroStatus: StateFlow<MacroStatus> = macroManager.status
        .stateIn(viewModelScope, SharingStarted.Eagerly, MacroStatus.IDLE)

    val savedMacros: StateFlow<List<String>> = macroManager.savedMacros
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ─── ESP32に接続 ──────────────────────────────────────
    fun connect() = wsClient.connect()
    fun disconnect() = wsClient.disconnect()

    // ─── ボタン操作 ───────────────────────────────────────
    fun onButtonDown(button: Int) {
        val newState = _controllerState.value.press(button)
        _controllerState.value = newState
        macroManager.recordFrame(newState)
        wsClient.sendControllerState(newState)
    }

    fun onButtonUp(button: Int) {
        val newState = _controllerState.value.release(button)
        _controllerState.value = newState
        macroManager.recordFrame(newState)
        wsClient.sendControllerState(newState)
    }

    // ─── スティック操作 ───────────────────────────────────
    fun onLeftStick(x: Int, y: Int) {
        val newState = _controllerState.value.copy(lx = x, ly = y)
        _controllerState.value = newState
        macroManager.recordFrame(newState)
        wsClient.sendControllerState(newState)
    }

    fun onRightStick(x: Int, y: Int) {
        val newState = _controllerState.value.copy(rx = x, ry = y)
        _controllerState.value = newState
        macroManager.recordFrame(newState)
        wsClient.sendControllerState(newState)
    }

    // ─── 十字キー ─────────────────────────────────────────
    fun onHat(hat: com.tou108.nxswitch.controller.Hat) {
        val newState = _controllerState.value.copy(hat = hat)
        _controllerState.value = newState
        macroManager.recordFrame(newState)
        wsClient.sendControllerState(newState)
    }

    // ─── マクロ操作 ───────────────────────────────────────
    fun startRecording() = macroManager.startRecording()
    fun stopRecording(name: String) = macroManager.stopRecording(name)
    fun playMacro(name: String, repeat: Int = 1) = macroManager.playMacro(name, repeat)
    fun stopMacro() = macroManager.stopPlaying()
    fun deleteMacro(name: String) = macroManager.deleteMacro(name)

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
