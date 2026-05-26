package com.tou108.nxswitch.controller

// ============================================================
//  ControllerState.kt
//  コントローラーの状態を管理するデータクラス
//  ESP32ファームウェアの ProController.h と定義を合わせること
// ============================================================

// ─── ボタン定義（ビットマスク） ──────────────────────────
object Button {
    const val Y       = 1 shl 0
    const val B       = 1 shl 1
    const val A       = 1 shl 2
    const val X       = 1 shl 3
    const val L       = 1 shl 4
    const val R       = 1 shl 5
    const val ZL      = 1 shl 6
    const val ZR      = 1 shl 7
    const val MINUS   = 1 shl 8
    const val PLUS    = 1 shl 9
    const val LS      = 1 shl 10  // 左スティック押し込み
    const val RS      = 1 shl 11  // 右スティック押し込み
    const val HOME    = 1 shl 12
    const val CAPTURE = 1 shl 13
}

// ─── 十字キー定義 ─────────────────────────────────────────
enum class Hat(val value: Int) {
    UP(0),
    UP_RIGHT(1),
    RIGHT(2),
    DOWN_RIGHT(3),
    DOWN(4),
    DOWN_LEFT(5),
    LEFT(6),
    UP_LEFT(7),
    NEUTRAL(8)
}

// ─── コントローラー状態 ───────────────────────────────────
data class ControllerState(
    val buttons: Int = 0,
    val hat: Hat = Hat.NEUTRAL,
    val lx: Int = 128,    // 左スティックX (0-255, 中央=128)
    val ly: Int = 128,    // 左スティックY (0-255, 中央=128)
    val rx: Int = 128,    // 右スティックX
    val ry: Int = 128,    // 右スティックY
) {
    // ボタンが押されているか確認
    fun isPressed(button: Int): Boolean = (buttons and button) != 0

    // ボタンを押した新しい状態を返す
    fun press(button: Int): ControllerState =
        copy(buttons = buttons or button)

    // ボタンを離した新しい状態を返す
    fun release(button: Int): ControllerState =
        copy(buttons = buttons and button.inv())

    // ニュートラル状態
    fun isNeutral(): Boolean =
        buttons == 0 && hat == Hat.NEUTRAL &&
        lx == 128 && ly == 128 && rx == 128 && ry == 128

    // ESP32へ送信するJSONに変換
    fun toJson(): String =
        """{"b":$buttons,"h":${hat.value},"lx":$lx,"ly":$ly,"rx":$rx,"ry":$ry}"""

    companion object {
        val NEUTRAL = ControllerState()
    }
}
