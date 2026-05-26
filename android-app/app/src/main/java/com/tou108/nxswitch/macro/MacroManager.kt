package com.tou108.nxswitch.macro

// ============================================================
//  MacroManager.kt
//  マクロの録画・保存・再生を管理
// ============================================================

import android.content.Context
import android.util.Log
import com.tou108.nxswitch.controller.ControllerState
import com.tou108.nxswitch.network.ESP32WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val TAG = "MacroManager"

// ─── マクロの1フレーム ────────────────────────────────────
data class MacroFrame(
    val state: ControllerState,
    val holdMs: Long   // この状態を保持するミリ秒
)

// ─── 録画・再生の状態 ─────────────────────────────────────
enum class MacroStatus { IDLE, RECORDING, PLAYING }

class MacroManager(
    private val context: Context,
    private val wsClient: ESP32WebSocketClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── 状態Flow ───────────────────────────────────────
    private val _status = MutableStateFlow(MacroStatus.IDLE)
    val status: StateFlow<MacroStatus> = _status

    private val _savedMacros = MutableStateFlow<List<String>>(emptyList())
    val savedMacros: StateFlow<List<String>> = _savedMacros

    // ─── 録画バッファ ────────────────────────────────────
    private val recordBuffer = mutableListOf<MacroFrame>()
    private var lastFrameTime = 0L
    private var lastState = ControllerState.NEUTRAL
    private var playJob: Job? = null

    // ─── 録画開始 ────────────────────────────────────────
    fun startRecording() {
        if (_status.value != MacroStatus.IDLE) return
        recordBuffer.clear()
        lastFrameTime = System.currentTimeMillis()
        lastState = ControllerState.NEUTRAL
        _status.value = MacroStatus.RECORDING
        Log.d(TAG, "録画開始")
    }

    // ─── 録画中に状態を記録 ──────────────────────────────
    fun recordFrame(state: ControllerState) {
        if (_status.value != MacroStatus.RECORDING) return
        val now = System.currentTimeMillis()
        val holdMs = now - lastFrameTime

        // 状態が変化したときだけ記録（容量節約）
        if (state != lastState) {
            recordBuffer.add(MacroFrame(lastState, holdMs))
            lastState = state
            lastFrameTime = now
        }
    }

    // ─── 録画停止・保存 ──────────────────────────────────
    fun stopRecording(macroName: String): Boolean {
        if (_status.value != MacroStatus.RECORDING) return false

        // 最後のフレームを追加
        val now = System.currentTimeMillis()
        recordBuffer.add(MacroFrame(lastState, now - lastFrameTime))

        _status.value = MacroStatus.IDLE
        Log.d(TAG, "録画停止: ${recordBuffer.size}フレーム")

        return saveMacro(macroName, recordBuffer.toList())
    }

    // ─── マクロ保存（JSON形式） ───────────────────────────
    private fun saveMacro(name: String, frames: List<MacroFrame>): Boolean {
        return try {
            val jsonArray = JSONArray()
            frames.forEach { frame ->
                val obj = JSONObject().apply {
                    put("b",  frame.state.buttons)
                    put("h",  frame.state.hat.value)
                    put("lx", frame.state.lx)
                    put("ly", frame.state.ly)
                    put("rx", frame.state.rx)
                    put("ry", frame.state.ry)
                    put("ms", frame.holdMs)
                }
                jsonArray.put(obj)
            }

            val file = File(context.filesDir, "macro_$name.json")
            file.writeText(jsonArray.toString())
            loadMacroList()
            Log.d(TAG, "マクロ保存: ${file.path}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "マクロ保存失敗: ${e.message}")
            false
        }
    }

    // ─── マクロ一覧を読み込み ─────────────────────────────
    fun loadMacroList() {
        val files = context.filesDir.listFiles { f ->
            f.name.startsWith("macro_") && f.name.endsWith(".json")
        } ?: emptyArray()

        _savedMacros.value = files.map { f ->
            f.name.removePrefix("macro_").removeSuffix(".json")
        }.sorted()
    }

    // ─── マクロ再生 ──────────────────────────────────────
    fun playMacro(name: String, repeatCount: Int = 1) {
        if (_status.value != MacroStatus.IDLE) return

        val file = File(context.filesDir, "macro_$name.json")
        if (!file.exists()) {
            Log.e(TAG, "マクロが見つかりません: $name")
            return
        }

        playJob = scope.launch {
            _status.value = MacroStatus.PLAYING
            Log.d(TAG, "マクロ再生開始: $name x$repeatCount")

            try {
                val jsonArray = JSONArray(file.readText())

                repeat(repeatCount) { iteration ->
                    if (_status.value != MacroStatus.PLAYING) return@launch
                    Log.d(TAG, "再生 ${iteration + 1}/$repeatCount 回目")

                    for (i in 0 until jsonArray.length()) {
                        if (_status.value != MacroStatus.PLAYING) return@launch

                        val obj = jsonArray.getJSONObject(i)
                        val state = ControllerState(
                            buttons = obj.getInt("b"),
                            hat     = com.tou108.nxswitch.controller.Hat.entries
                                          .firstOrNull { it.value == obj.getInt("h") }
                                          ?: com.tou108.nxswitch.controller.Hat.NEUTRAL,
                            lx      = obj.getInt("lx"),
                            ly      = obj.getInt("ly"),
                            rx      = obj.getInt("rx"),
                            ry      = obj.getInt("ry")
                        )
                        val holdMs = obj.getLong("ms")

                        wsClient.sendControllerState(state)
                        delay(holdMs)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "マクロ再生エラー: ${e.message}")
            } finally {
                wsClient.sendNeutral()
                _status.value = MacroStatus.IDLE
                Log.d(TAG, "マクロ再生完了")
            }
        }
    }

    // ─── 再生停止 ────────────────────────────────────────
    fun stopPlaying() {
        playJob?.cancel()
        playJob = null
        wsClient.sendNeutral()
        _status.value = MacroStatus.IDLE
    }

    // ─── マクロ削除 ──────────────────────────────────────
    fun deleteMacro(name: String) {
        File(context.filesDir, "macro_$name.json").delete()
        loadMacroList()
    }

    init {
        loadMacroList()
    }
}
