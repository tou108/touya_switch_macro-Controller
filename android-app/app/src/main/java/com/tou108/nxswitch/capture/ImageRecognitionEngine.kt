package com.tou108.nxswitch.capture

// ============================================================
//  ImageRecognitionEngine.kt
//  ML Kit を使ったSwitch画面の画像認識・自動操作エンジン
// ============================================================

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "ImageRecognition"

// ─── 認識結果 ─────────────────────────────────────────────
data class RecognitionResult(
    val labels: List<LabelResult>,
    val objects: List<ObjectResult>
)

data class LabelResult(
    val text: String,
    val confidence: Float      // 0.0 〜 1.0
)

data class ObjectResult(
    val trackingId: Int?,
    val boundingBox: Rect,
    val labels: List<LabelResult>
)

// ─── 自動操作トリガー ─────────────────────────────────────
data class AutoTrigger(
    val id: String,
    val description: String,
    val condition: TriggerCondition,
    val macroName: String,    // 条件を満たしたとき実行するマクロ名
    val enabled: Boolean = true
)

sealed class TriggerCondition {
    // ラベルが指定の信頼度以上で検出されたとき
    data class LabelDetected(val label: String, val minConfidence: Float = 0.7f) : TriggerCondition()
    // 指定色が指定領域に存在するとき（ピクセル検出）
    data class ColorInRegion(val region: Rect, val targetColor: Int, val tolerance: Int = 30) : TriggerCondition()
}

class ImageRecognitionEngine {

    // ─── ML Kit 初期化 ────────────────────────────────────
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    // ─── 登録済みトリガー ─────────────────────────────────
    private val triggers = mutableListOf<AutoTrigger>()

    // ─── 画像認識（サスペンド関数） ───────────────────────
    suspend fun analyze(bitmap: Bitmap): RecognitionResult =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)

            // ラベリングとオブジェクト検出を並行実行
            var labelsResult:  List<LabelResult>?  = null
            var objectsResult: List<ObjectResult>? = null

            fun checkAndResume() {
                val l = labelsResult
                val o = objectsResult
                if (l != null && o != null) {
                    cont.resume(RecognitionResult(l, o))
                }
            }

            // ラベリング
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    labelsResult = labels.map {
                        LabelResult(it.text, it.confidence)
                    }
                    checkAndResume()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ラベリングエラー: ${e.message}")
                    labelsResult = emptyList()
                    checkAndResume()
                }

            // オブジェクト検出
            objectDetector.process(image)
                .addOnSuccessListener { objects ->
                    objectsResult = objects.map { obj ->
                        ObjectResult(
                            trackingId  = obj.trackingId,
                            boundingBox = obj.boundingBox,
                            labels      = obj.labels.map {
                                LabelResult(it.text, it.confidence)
                            }
                        )
                    }
                    checkAndResume()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "オブジェクト検出エラー: ${e.message}")
                    objectsResult = emptyList()
                    checkAndResume()
                }
        }

    // ─── 特定色の検出（ピクセルレベル） ──────────────────
    fun detectColor(bitmap: Bitmap, region: Rect, targetColor: Int, tolerance: Int): Boolean {
        val targetR = android.graphics.Color.red(targetColor)
        val targetG = android.graphics.Color.green(targetColor)
        val targetB = android.graphics.Color.blue(targetColor)

        // 領域内のピクセルをサンプリング（全ピクセルだと重いのでグリッドで）
        val step = 4
        var matchCount = 0
        var sampleCount = 0

        val left   = region.left.coerceIn(0, bitmap.width - 1)
        val top    = region.top.coerceIn(0, bitmap.height - 1)
        val right  = region.right.coerceIn(0, bitmap.width)
        val bottom = region.bottom.coerceIn(0, bitmap.height)

        for (y in top until bottom step step) {
            for (x in left until right step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)

                if (Math.abs(r - targetR) <= tolerance &&
                    Math.abs(g - targetG) <= tolerance &&
                    Math.abs(b - targetB) <= tolerance) {
                    matchCount++
                }
                sampleCount++
            }
        }

        // サンプルの30%以上がマッチすれば検出
        return sampleCount > 0 && matchCount.toFloat() / sampleCount >= 0.3f
    }

    // ─── トリガー管理 ─────────────────────────────────────
    fun addTrigger(trigger: AutoTrigger) {
        triggers.removeAll { it.id == trigger.id }
        triggers.add(trigger)
        Log.d(TAG, "トリガー追加: ${trigger.id} - ${trigger.description}")
    }

    fun removeTrigger(id: String) {
        triggers.removeAll { it.id == id }
    }

    fun clearTriggers() = triggers.clear()

    fun getTriggers(): List<AutoTrigger> = triggers.toList()

    // ─── 認識結果からトリガーを判定 ──────────────────────
    fun evaluateTriggers(
        result: RecognitionResult,
        bitmap: Bitmap
    ): List<AutoTrigger> {
        val fired = mutableListOf<AutoTrigger>()

        for (trigger in triggers) {
            if (!trigger.enabled) continue

            val triggered = when (val cond = trigger.condition) {
                is TriggerCondition.LabelDetected -> {
                    result.labels.any { label ->
                        label.text.contains(cond.label, ignoreCase = true) &&
                        label.confidence >= cond.minConfidence
                    }
                }
                is TriggerCondition.ColorInRegion -> {
                    detectColor(bitmap, cond.region, cond.targetColor, cond.tolerance)
                }
            }

            if (triggered) {
                Log.d(TAG, "トリガー発火: ${trigger.id}")
                fired.add(trigger)
            }
        }

        return fired
    }

    // ─── リソース解放 ─────────────────────────────────────
    fun close() {
        labeler.close()
        objectDetector.close()
    }
}
