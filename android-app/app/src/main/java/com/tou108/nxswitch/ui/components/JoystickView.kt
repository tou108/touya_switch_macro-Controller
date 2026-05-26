package com.tou108.nxswitch.ui.components

// ============================================================
//  JoystickView.kt
//  タッチ操作できるアナログスティックUIコンポーネント
// ============================================================

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

// ─── スティック値（-1.0f 〜 1.0f） ────────────────────────
data class StickValue(val x: Float, val y: Float) {
    // 0-255 の整数値に変換（Switchへ送信する形式）
    fun toByteX(): Int = ((x * 127f) + 128f).toInt().coerceIn(0, 255)
    fun toByteY(): Int = ((y * 127f) + 128f).toInt().coerceIn(0, 255)

    companion object {
        val CENTER = StickValue(0f, 0f)
    }
}

@Composable
fun JoystickView(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    baseColor: Color = Color(0xFF303030),
    thumbColor: Color = Color(0xFF606060),
    activeColor: Color = Color(0xFF00E5FF),
    onValueChanged: (StickValue) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging  by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            updateThumb(offset, size.toPx() / 2, size.toPx()) { newOffset, value ->
                                thumbOffset = newOffset
                                onValueChanged(value)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateThumb(change.position, size.toPx() / 2, size.toPx()) { newOffset, value ->
                                thumbOffset = newOffset
                                onValueChanged(value)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            thumbOffset = Offset.Zero
                            onValueChanged(StickValue.CENTER)
                        },
                        onDragCancel = {
                            isDragging = false
                            thumbOffset = Offset.Zero
                            onValueChanged(StickValue.CENTER)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            updateThumb(offset, size.toPx() / 2, size.toPx()) { newOffset, value ->
                                thumbOffset = newOffset
                                onValueChanged(value)
                            }
                            tryAwaitRelease()
                            isDragging = false
                            thumbOffset = Offset.Zero
                            onValueChanged(StickValue.CENTER)
                        }
                    )
                }
        ) {
            val centerX   = this.size.width / 2
            val centerY   = this.size.height / 2
            val baseRadius = this.size.width / 2
            val thumbRadius = this.size.width / 6

            // ベース（外枠）
            drawCircle(
                color  = baseColor,
                radius = baseRadius,
                center = Offset(centerX, centerY)
            )

            // ベース内側の円（ガイド）
            drawCircle(
                color  = baseColor.copy(alpha = 0.4f),
                radius = baseRadius * 0.6f,
                center = Offset(centerX, centerY),
                style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            // スティック（サム）
            drawCircle(
                color  = if (isDragging) activeColor else thumbColor,
                radius = thumbRadius,
                center = Offset(centerX + thumbOffset.x, centerY + thumbOffset.y)
            )
        }
    }
}

// ─── タッチ位置からスティック値を計算 ────────────────────
private fun updateThumb(
    touchPos: Offset,
    center: Float,
    canvasSize: Float,
    onResult: (Offset, StickValue) -> Unit
) {
    val maxRadius = canvasSize / 2 * 0.75f  // ベース半径の75%が移動上限

    // 中心からの差分
    val dx = touchPos.x - center
    val dy = touchPos.y - center
    val distance = sqrt(dx * dx + dy * dy)

    // 最大半径でクランプ
    val clampedDist = min(distance, maxRadius)
    val angle       = atan2(dy, dx)

    val clampedX = clampedDist * kotlin.math.cos(angle)
    val clampedY = clampedDist * kotlin.math.sin(angle)

    // -1.0f〜1.0f に正規化
    val normalizedX = clampedX / maxRadius
    val normalizedY = clampedY / maxRadius

    onResult(
        Offset(clampedX, clampedY),
        StickValue(normalizedX, normalizedY)
    )
}
