package com.tou108.nxswitch.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tou108.nxswitch.MainViewModel
import com.tou108.nxswitch.controller.Button
import com.tou108.nxswitch.controller.Hat
import com.tou108.nxswitch.macro.MacroStatus
import com.tou108.nxswitch.network.ConnectionState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ─── カラー定義 ───────────────────────────────────────────
private val BG       = Color(0xFF0F0F0F)
private val PANEL    = Color(0xFF1C1C1C)
private val GRAY     = Color(0xFF383838)
private val BTN_A    = Color(0xFFE53935)
private val BTN_B    = Color(0xFFFFD600)
private val BTN_X    = Color(0xFF1E88E5)
private val BTN_Y    = Color(0xFF43A047)
private val GREEN    = Color(0xFF00E676)
private val RED      = Color(0xFFFF1744)
private val ORANGE   = Color(0xFFFF6D00)
private val CYAN     = Color(0xFF00E5FF)

// ─── メイン画面 ───────────────────────────────────────────
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connState   by viewModel.connectionState.collectAsState()
    val swConnected by viewModel.switchConnected.collectAsState()
    val macroStatus by viewModel.macroStatus.collectAsState()
    val macros      by viewModel.savedMacros.collectAsState()
    var showSave    by remember { mutableStateOf(false) }
    var saveName    by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(BG)
    ) {
        // ── ステータスバー ─────────────────────────────────
        StatusBar(connState, swConnected, macroStatus) { viewModel.connect() }

        // ── コントローラー本体 ────────────────────────────
        // 3カラム構成: 左(weight=2) | 中央(weight=3) | 右(weight=2)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ════════ 左パネル ════════
            Column(
                modifier = Modifier.weight(2f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // ZL / L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NxBtn("ZL", GRAY, Modifier.weight(1f).height(34.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.ZL) else viewModel.onButtonUp(Button.ZL)
                    }
                    NxBtn("L", GRAY, Modifier.weight(1f).height(34.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.L) else viewModel.onButtonUp(Button.L)
                    }
                }

                // 左スティック
                Box(contentAlignment = Alignment.Center) {
                    Joystick(accentColor = CYAN) { x, y -> viewModel.onLeftStick(x, y) }
                }

                // 十字キー
                DPad { viewModel.onHat(it) }

                // − / LS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NxBtn("−", GRAY, Modifier.weight(1f).height(30.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.MINUS) else viewModel.onButtonUp(Button.MINUS)
                    }
                    NxBtn("LS", GRAY, Modifier.weight(1f).height(30.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.LS) else viewModel.onButtonUp(Button.LS)
                    }
                }
            }

            // ════════ 中央パネル ════════
            Column(
                modifier = Modifier.weight(3f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // キャプチャー画面
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF080808)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "📺 キャプチャー画面\nキャプチャーカードを接続してください",
                        color = Color(0xFF3A3A3A),
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }

                // マクロパネル
                MacroPanel(macroStatus, macros,
                    onStartRec = { viewModel.startRecording() },
                    onStopRec  = { showSave = true },
                    onPlay     = { viewModel.playMacro(it) },
                    onStop     = { viewModel.stopMacro() },
                    onDelete   = { viewModel.deleteMacro(it) }
                )

                // HOME / キャプチャボタン
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NxBtn("HOME", Color(0xFF282828), Modifier.size(42.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.HOME) else viewModel.onButtonUp(Button.HOME)
                    }
                    NxBtn("⏺", Color(0xFF282828), Modifier.size(42.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.CAPTURE) else viewModel.onButtonUp(Button.CAPTURE)
                    }
                }
            }

            // ════════ 右パネル ════════
            Column(
                modifier = Modifier.weight(2f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // R / ZR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NxBtn("R", GRAY, Modifier.weight(1f).height(34.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.R) else viewModel.onButtonUp(Button.R)
                    }
                    NxBtn("ZR", GRAY, Modifier.weight(1f).height(34.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.ZR) else viewModel.onButtonUp(Button.ZR)
                    }
                }

                // 右スティック
                Box(contentAlignment = Alignment.Center) {
                    Joystick(accentColor = CYAN) { x, y -> viewModel.onRightStick(x, y) }
                }

                // ABXY
                ABXYPad(viewModel)

                // RS / +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NxBtn("RS", GRAY, Modifier.weight(1f).height(30.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.RS) else viewModel.onButtonUp(Button.RS)
                    }
                    NxBtn("+", GRAY, Modifier.weight(1f).height(30.dp)) { d ->
                        if (d) viewModel.onButtonDown(Button.PLUS) else viewModel.onButtonUp(Button.PLUS)
                    }
                }
            }
        }
    }

    // マクロ保存ダイアログ
    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            containerColor   = PANEL,
            title = { Text("マクロを保存", color = Color.White) },
            text  = {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    label = { Text("マクロ名") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (saveName.isNotBlank()) {
                        viewModel.stopRecording(saveName)
                        showSave = false
                        saveName = ""
                    }
                }) { Text("保存", color = CYAN) }
            },
            dismissButton = {
                TextButton(onClick = { showSave = false }) {
                    Text("キャンセル", color = Color.Gray)
                }
            }
        )
    }
}

// ─── ステータスバー ───────────────────────────────────────
@Composable
fun StatusBar(
    conn: ConnectionState,
    sw: Boolean,
    macro: MacroStatus,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181818))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("nx_switch_マクロ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusDot("ESP32",  conn == ConnectionState.CONNECTED)
            StatusDot("Switch", sw)
            when (macro) {
                MacroStatus.RECORDING -> Text("● REC",  color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                MacroStatus.PLAYING   -> Text("▶ PLAY", color = GREEN,  fontWeight = FontWeight.Bold, fontSize = 12.sp)
                else -> {}
            }
            if (conn != ConnectionState.CONNECTED) {
                TextButton(
                    onClick = onConnect,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) { Text("接続", color = CYAN, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun StatusDot(label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (active) GREEN else RED))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

// ─── マクロパネル ─────────────────────────────────────────
@Composable
fun MacroPanel(
    status: MacroStatus,
    macros: List<String>,
    onStartRec: () -> Unit,
    onStopRec:  () -> Unit,
    onPlay:     (String) -> Unit,
    onStop:     () -> Unit,
    onDelete:   (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("マクロ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        when (status) {
            MacroStatus.IDLE ->
                Button(
                    onClick = onStartRec,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ORANGE)
                ) { Text("● 録画開始", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            MacroStatus.RECORDING ->
                Button(
                    onClick = onStopRec,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("■ 録画停止・保存", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            MacroStatus.PLAYING ->
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("■ 再生停止", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        if (macros.isEmpty()) {
            Text("マクロなし", color = Color(0xFF505050), fontSize = 11.sp)
        } else {
            macros.take(4).forEach { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onPlay(name) }, contentPadding = PaddingValues(4.dp)) {
                        Text("▶", color = GREEN, fontSize = 14.sp)
                    }
                    TextButton(onClick = { onDelete(name) }, contentPadding = PaddingValues(4.dp)) {
                        Text("✕", color = RED, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─── アナログスティック ───────────────────────────────────
@Composable
fun Joystick(accentColor: Color, onMove: (Int, Int) -> Unit) {
    var thumbPos by remember { mutableStateOf(Offset.Zero) }
    val sizeDp   = 88.dp

    Canvas(
        modifier = Modifier
            .size(sizeDp)
            .pointerInput(Unit) {
                val sizePx = sizeDp.toPx()
                detectDragGestures(
                    onDragStart = { pos ->
                        thumbPos = clampStick(pos, sizePx)
                        val (bx, by) = stickToBytes(thumbPos, sizePx)
                        onMove(bx, by)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        thumbPos = clampStick(change.position, sizePx)
                        val (bx, by) = stickToBytes(thumbPos, sizePx)
                        onMove(bx, by)
                    },
                    onDragEnd    = { thumbPos = Offset.Zero; onMove(128, 128) },
                    onDragCancel = { thumbPos = Offset.Zero; onMove(128, 128) }
                )
            }
            .pointerInput(Unit) {
                val sizePx = sizeDp.toPx()
                detectTapGestures(onPress = { pos ->
                    thumbPos = clampStick(pos, sizePx)
                    val (bx, by) = stickToBytes(thumbPos, sizePx)
                    onMove(bx, by)
                    tryAwaitRelease()
                    thumbPos = Offset.Zero
                    onMove(128, 128)
                })
            }
    ) {
        val cx      = size.width  / 2f
        val cy      = size.height / 2f
        val baseR   = size.width  / 2f - 3f
        val thumbR  = size.width  / 5.5f
        val isActive = thumbPos != Offset.Zero

        // ベース円（外枠）
        drawCircle(color = Color(0xFF252525), radius = baseR, center = Offset(cx, cy))
        // アクセントリング
        drawCircle(
            color  = accentColor.copy(alpha = if (isActive) 0.6f else 0.25f),
            radius = baseR,
            center = Offset(cx, cy),
            style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
        // 内側ガイド円
        drawCircle(
            color  = Color(0xFF303030),
            radius = baseR * 0.5f,
            center = Offset(cx, cy),
            style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
        // 十字ガイド線
        drawLine(Color(0xFF2A2A2A), Offset(cx - baseR * 0.45f, cy), Offset(cx + baseR * 0.45f, cy), strokeWidth = 1f)
        drawLine(Color(0xFF2A2A2A), Offset(cx, cy - baseR * 0.45f), Offset(cx, cy + baseR * 0.45f), strokeWidth = 1f)

        // サムスティック
        drawCircle(
            color  = if (isActive) accentColor else Color(0xFF484848),
            radius = thumbR,
            center = Offset(cx + thumbPos.x, cy + thumbPos.y)
        )
        // サム内側ハイライト
        drawCircle(
            color  = Color.White.copy(alpha = if (isActive) 0.25f else 0.1f),
            radius = thumbR * 0.45f,
            center = Offset(cx + thumbPos.x - thumbR * 0.15f, cy + thumbPos.y - thumbR * 0.15f)
        )
    }
}

private fun clampStick(pos: Offset, sizePx: Float): Offset {
    val cx   = sizePx / 2f
    val cy   = sizePx / 2f
    val maxR = sizePx / 2f * 0.70f
    val dx   = pos.x - cx
    val dy   = pos.y - cy
    val dist = sqrt(dx * dx + dy * dy)
    val r    = min(dist, maxR)
    val a    = atan2(dy, dx)
    return Offset((r * cos(a)), (r * sin(a)))
}

private fun stickToBytes(offset: Offset, sizePx: Float): Pair<Int, Int> {
    val maxR = sizePx / 2f * 0.70f
    val bx   = ((offset.x / maxR) * 127f + 128f).toInt().coerceIn(0, 255)
    val by   = ((offset.y / maxR) * 127f + 128f).toInt().coerceIn(0, 255)
    return Pair(bx, by)
}

// ─── 十字キー ─────────────────────────────────────────────
@Composable
fun DPad(onDir: (Hat) -> Unit) {
    val btnSize = 34.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NxBtn("▲", GRAY, Modifier.size(btnSize)) { d -> onDir(if (d) Hat.UP    else Hat.NEUTRAL) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NxBtn("◀", GRAY, Modifier.size(btnSize)) { d -> onDir(if (d) Hat.LEFT  else Hat.NEUTRAL) }
            Box(Modifier.size(btnSize).background(Color(0xFF181818)))
            NxBtn("▶", GRAY, Modifier.size(btnSize)) { d -> onDir(if (d) Hat.RIGHT else Hat.NEUTRAL) }
        }
        NxBtn("▼", GRAY, Modifier.size(btnSize)) { d -> onDir(if (d) Hat.DOWN  else Hat.NEUTRAL) }
    }
}

// ─── ABXYボタン ───────────────────────────────────────────
@Composable
fun ABXYPad(vm: MainViewModel) {
    val s = 38.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NxBtn("X", BTN_X, Modifier.size(s)) { d -> if (d) vm.onButtonDown(Button.X) else vm.onButtonUp(Button.X) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NxBtn("Y", BTN_Y, Modifier.size(s)) { d -> if (d) vm.onButtonDown(Button.Y) else vm.onButtonUp(Button.Y) }
            Spacer(Modifier.size(s))
            NxBtn("A", BTN_A, Modifier.size(s)) { d -> if (d) vm.onButtonDown(Button.A) else vm.onButtonUp(Button.A) }
        }
        NxBtn("B", BTN_B, Modifier.size(s)) { d -> if (d) vm.onButtonDown(Button.B) else vm.onButtonUp(Button.B) }
    }
}

// ─── 汎用ボタン ───────────────────────────────────────────
@Composable
fun NxBtn(
    label:    String,
    color:    Color,
    modifier: Modifier = Modifier,
    onPress:  (Boolean) -> Unit
) {
    val src     = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(src) {
        src.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press   -> { pressed = true;  onPress(true)  }
                is PressInteraction.Release,
                is PressInteraction.Cancel  -> { pressed = false; onPress(false) }
            }
        }
    }

    Button(
        onClick           = {},
        modifier          = modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        shape             = CircleShape,
        colors            = ButtonDefaults.buttonColors(
            containerColor = if (pressed) color.copy(alpha = 0.55f) else color
        ),
        contentPadding    = PaddingValues(2.dp),
        interactionSource = src
    ) {
        Text(
            text       = label,
            color      = Color.White,
            fontSize   = 11.sp,
            maxLines   = 1,
            fontWeight = FontWeight.Bold
        )
    }
}
