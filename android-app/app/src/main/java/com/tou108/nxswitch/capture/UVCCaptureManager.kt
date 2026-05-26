package com.tou108.nxswitch.capture

// ============================================================
//  UVCCaptureManager.kt
//  USBキャプチャーカード（UVC）からSwitch画面を取り込む
//  対応チップ: MS2130, MS2109 など UVC規格準拠デバイス
// ============================================================

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.usb.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

private const val TAG = "UVCCapture"

// UVC クラス定数
private const val UVC_CLASS         = 14    // USB_CLASS_VIDEO
private const val UVC_SC_VIDEOCONTROL    = 1
private const val UVC_SC_VIDEOSTREAMING  = 2

// 取り込み解像度設定
private const val CAPTURE_WIDTH  = 1280
private const val CAPTURE_HEIGHT = 720
private const val TARGET_FPS     = 30

// キャプチャーの状態
enum class CaptureState { IDLE, CONNECTING, STREAMING, ERROR }

class UVCCaptureManager(private val context: Context) {

    private val usbManager  = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope        = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var usbDevice:     UsbDevice?     = null
    private var usbConnection: UsbDeviceConnection? = null
    private var videoInterface: UsbInterface? = null
    private var videoEndpoint:  UsbEndpoint?  = null
    private var captureJob:     Job?          = null

    // ─── 状態Flow（UIが購読） ──────────────────────────────
    private val _captureState  = MutableStateFlow(CaptureState.IDLE)
    val captureState: StateFlow<CaptureState> = _captureState

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps

    // ─── 接続されたUSBデバイス一覧からUVCを検索 ──────────
    fun findUVCDevice(): UsbDevice? {
        val deviceList = usbManager.deviceList
        for ((_, device) in deviceList) {
            if (isUVCDevice(device)) {
                Log.d(TAG, "UVCデバイス検出: ${device.productName} " +
                      "(VID=0x${device.vendorId.toString(16)}, " +
                      "PID=0x${device.productId.toString(16)})")
                return device
            }
        }
        Log.w(TAG, "UVCデバイスが見つかりません")
        return null
    }

    // UVCデバイスかどうか判定
    private fun isUVCDevice(device: UsbDevice): Boolean {
        // インターフェースを走査してVideo Streamingがあれば UVC
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UVC_CLASS &&
                iface.interfaceSubclass == UVC_SC_VIDEOSTREAMING) {
                return true
            }
        }
        // または既知ベンダーID（MACROSILICON = 0x534D）
        return device.vendorId == 0x534D
    }

    // ─── USBパーミッション確認 ────────────────────────────
    fun hasPermission(device: UsbDevice): Boolean =
        usbManager.hasPermission(device)

    // ─── キャプチャー開始 ────────────────────────────────
    fun startCapture(device: UsbDevice): Boolean {
        if (_captureState.value == CaptureState.STREAMING) return true

        usbDevice = device
        _captureState.value = CaptureState.CONNECTING

        // USB接続を開く
        usbConnection = usbManager.openDevice(device)
        if (usbConnection == null) {
            Log.e(TAG, "USB接続失敗")
            _captureState.value = CaptureState.ERROR
            return false
        }

        // Video Streamingインターフェースを取得
        videoInterface = findVideoStreamingInterface(device)
        if (videoInterface == null) {
            Log.e(TAG, "Video Streamingインターフェースが見つかりません")
            _captureState.value = CaptureState.ERROR
            return false
        }

        // インターフェースをクレーム
        if (!usbConnection!!.claimInterface(videoInterface, true)) {
            Log.e(TAG, "インターフェースのクレームに失敗")
            _captureState.value = CaptureState.ERROR
            return false
        }

        // ISOchronousまたはBulkエンドポイントを取得
        videoEndpoint = findVideoEndpoint(videoInterface!!)
        if (videoEndpoint == null) {
            Log.e(TAG, "Videoエンドポイントが見つかりません")
            _captureState.value = CaptureState.ERROR
            return false
        }

        Log.d(TAG, "UVC初期化完了 → ストリーミング開始")
        _captureState.value = CaptureState.STREAMING

        // フレーム受信ループ
        startFrameLoop()
        return true
    }

    // ─── Video Streamingインターフェースを検索 ─────────────
    private fun findVideoStreamingInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UVC_CLASS &&
                iface.interfaceSubclass == UVC_SC_VIDEOSTREAMING &&
                iface.endpointCount > 0) {
                return iface
            }
        }
        return null
    }

    // ─── Videoエンドポイントを検索 ────────────────────────
    private fun findVideoEndpoint(iface: UsbInterface): UsbEndpoint? {
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.direction == UsbConstants.USB_DIR_IN) {
                return ep
            }
        }
        return null
    }

    // ─── フレーム受信ループ（コルーチン） ────────────────
    private fun startFrameLoop() {
        captureJob = scope.launch {
            val endpoint   = videoEndpoint ?: return@launch
            val connection = usbConnection ?: return@launch
            val bufferSize = endpoint.maxPacketSize * 32
            val buffer     = ByteBuffer.allocate(bufferSize)
            val rawBuffer  = ByteArray(bufferSize)

            var frameBuffer  = ByteArray(0)
            var frameCount   = 0
            var lastFpsTime  = System.currentTimeMillis()

            Log.d(TAG, "フレーム受信ループ開始 (バッファ: ${bufferSize}バイト)")

            while (isActive && _captureState.value == CaptureState.STREAMING) {
                val received = connection.bulkTransfer(
                    endpoint, rawBuffer, rawBuffer.size, 1000
                )

                if (received <= 0) continue

                // MJPEG / UVC ペイロードを蓄積
                val chunk = rawBuffer.copyOf(received)
                frameBuffer += chunk

                // JPEGの終端（0xFF 0xD9）を検出
                val endIdx = findJpegEnd(frameBuffer)
                if (endIdx >= 0) {
                    val jpegData = frameBuffer.copyOf(endIdx + 2)
                    frameBuffer  = frameBuffer.copyOfRange(endIdx + 2, frameBuffer.size)

                    // Bitmapにデコード
                    val bitmap = decodeJpeg(jpegData)
                    if (bitmap != null) {
                        _latestFrame.value = bitmap
                        frameCount++

                        // FPS計算
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTime >= 1000) {
                            _fps.value = frameCount
                            frameCount = 0
                            lastFpsTime = now
                        }
                    }
                }

                // バッファが大きくなりすぎたらリセット（同期ズレ対策）
                if (frameBuffer.size > bufferSize * 4) {
                    Log.w(TAG, "バッファリセット")
                    frameBuffer = ByteArray(0)
                }
            }

            Log.d(TAG, "フレーム受信ループ終了")
        }
    }

    // ─── JPEGの終端を検索 ────────────────────────────────
    private fun findJpegEnd(data: ByteArray): Int {
        for (i in 0 until data.size - 1) {
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0xD9.toByte()) {
                return i
            }
        }
        return -1
    }

    // ─── JPEGデコード ────────────────────────────────────
    private fun decodeJpeg(data: ByteArray): Bitmap? {
        return try {
            android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (e: Exception) {
            Log.v(TAG, "JPEGデコードエラー（スキップ）")
            null
        }
    }

    // ─── キャプチャー停止 ────────────────────────────────
    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null

        videoInterface?.let { usbConnection?.releaseInterface(it) }
        usbConnection?.close()
        usbConnection = null

        _captureState.value = CaptureState.IDLE
        _latestFrame.value  = null
        _fps.value          = 0
        Log.d(TAG, "キャプチャー停止")
    }

    // ─── 最新フレームを画像認識用に取得 ──────────────────
    fun getLatestBitmap(): Bitmap? = _latestFrame.value

    fun release() {
        stopCapture()
        scope.cancel()
    }
}
