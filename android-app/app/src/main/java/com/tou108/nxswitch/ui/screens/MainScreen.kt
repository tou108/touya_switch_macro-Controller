package com.tou108.nxswitch.ui.screens

// ============================================================
//  MainScreen.kt
//  メイン画面（コントローラーUI + 状態表示）
// ============================================================

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tou108.nxswitch.MainViewModel
import com.tou108.nxswitch.controller.Button
import com.tou108.nxswitch.controller.Hat
import com.tou108.nxswitch.macro.MacroStatus
import com.tou108.nxswitch.network.ConnectionState

// ─── カラー定義 ───────────────────────────────────────────
private val SwitchGray   = Color(0xFF2D2D2D)
private val SwitchDark   = Color(0xFF1A1A1A)
private val BtnA         = Color(0xFFE02020)  // 赤
private val BtnB         = Color(0xFFFFDD00)  // 黄
private val BtnX         = Color(0xFF2060E0)  // 青
private val BtnY         = Color(0xFF20A020)  // 緑
private val BtnGray      = Color(0xFF505050)
private val Connected    = Color(0xFF00E676)
private val Disconnected = Color(0xFFFF1744)
private val Recording    = Color(0xFFFF6D00)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val switchConnected by viewModel.switchConnected.collectAsState()
    val macroStatus     by viewModel.macroStatus.collectAsState()
    val savedMacros     by viewModel.savedMacros.collectAsState()
    var showMacroDialog by remember { mutableStateOf(false) }
    var macroSaveName   by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwitchDark)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── ステータスバー ─────────────────────────────
            StatusBar(
                connectionState = connectionState,
                switchConnected = switchConnected,
                macroStatus     = macroStatus,
                onConnect       = { viewModel.connect() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── メインコントローラーレイアウト ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // 左側（十字キー + L/ZL + - + LS）
                LeftPanel(viewModel)

                // 中央（キャプチャー画面 / マクロパネル）
                CenterPanel(
                    viewModel       = viewModel,
                    macroStatus     = macroStatus,
                    savedMacros     = savedMacros,
                    onSaveRequest   = { showMacroDialog = true }
                )

                // 右側（ABXYボタン + R/ZR + + + RS）
                RightPanel(viewModel)
            }
        }

        // マクロ名入力ダイアログ
        if (showMacroDialog) {
            AlertDialog(
                onDismissRequest = { showMacroDialog = false },
                title = { Text("マクロを保存") },
                text = {
                    OutlinedTextField(
                        value = macroSaveName,
                        onValueChange = { macroSaveName = it },
                        label = { Text("マクロ名") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (macroSaveName.isNotBlank()) {
                            viewModel.stopRecording(macroSaveName)
                            showMacroDialog = false
                            macroSaveName = ""
                        }
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showMacroDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}

// ─── ステータスバー ───────────────────────────────────────
@Composable
fun StatusBar(
    connectionState: ConnectionState,
    switchConnected: Boolean,
    macroStatus: MacroStatus,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SwitchGray)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // アプリ名
        Text(
            text = "nx_switch_マクロ",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // ESP32接続状態
            StatusDot(
                label  = "ESP32",
                active = connectionState == ConnectionState.CONNECTED
            )

            // Switch接続状態
            StatusDot(
                label  = "Switch",
                active = switchConnected
            )

            // マクロ状態
            if (macroStatus == MacroStatus.RECORDING) {
                Text("● REC", color = Recording, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            } else if (macroStatus == MacroStatus.PLAYING) {
                Text("▶ PLAY", color = Connected, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // 未接続時は再接続ボタン
            if (connectionState == ConnectionState.DISCONNECTED ||
                connectionState == ConnectionState.ERROR) {
                TextButton(onClick = onConnect) {
                    Text("接続", color = Color.Cyan, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusDot(label: String, active: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) Connected else Disconnected)
        )
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

// ─── 左パネル（十字キー + L/ZL） ─────────────────────────
@Composable
fun LeftPanel(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.width(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ZL / L ボタン
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchButton("ZL", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.ZL)
                else viewModel.onButtonUp(Button.ZL)
            }
            SwitchButton("L", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.L)
                else viewModel.onButtonUp(Button.L)
            }
        }

        // 十字キー
        DPad(
            onDirection = { hat -> viewModel.onHat(hat) }
        )

        // MINUS + LS
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchButton("−", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.MINUS)
                else viewModel.onButtonUp(Button.MINUS)
            }
            SwitchButton("LS", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.LS)
                else viewModel.onButtonUp(Button.LS)
            }
        }
    }
}

// ─── 右パネル（ABXY + R/ZR） ─────────────────────────────
@Composable
fun RightPanel(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.width(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ZR / R ボタン
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchButton("R", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.R)
                else viewModel.onButtonUp(Button.R)
            }
            SwitchButton("ZR", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.ZR)
                else viewModel.onButtonUp(Button.ZR)
            }
        }

        // ABXY ボタン
        ABXYPad(viewModel)

        // PLUS + RS
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchButton("RS", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.RS)
                else viewModel.onButtonUp(Button.RS)
            }
            SwitchButton("+", BtnGray, Modifier.weight(1f)) { down ->
                if (down) viewModel.onButtonDown(Button.PLUS)
                else viewModel.onButtonUp(Button.PLUS)
            }
        }
    }
}

// ─── 中央パネル（画面 + マクロ） ─────────────────────────
@Composable
fun CenterPanel(
    viewModel: MainViewModel,
    macroStatus: MacroStatus,
    savedMacros: List<String>,
    onSaveRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // キャプチャー画面プレースホルダー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📺 キャプチャー画面\n（第2週実装予定）",
                color = Color(0xFF666666),
                fontSize = 12.sp
            )
        }

        // マクロコントロール
        MacroControls(
            macroStatus = macroStatus,
            savedMacros = savedMacros,
            onStartRec  = { viewModel.startRecording() },
            onStopRec   = onSaveRequest,
            onPlay      = { name -> viewModel.playMacro(name) },
            onStop      = { viewModel.stopMacro() },
            onDelete    = { name -> viewModel.deleteMacro(name) },
            viewModel   = viewModel
        )

        // HOME / CAPTURE ボタン
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SwitchButton("HOME", Color(0xFF404040), Modifier.size(48.dp)) { down ->
                if (down) viewModel.onButtonDown(Button.HOME)
                else viewModel.onButtonUp(Button.HOME)
            }
            SwitchButton("⏺", Color(0xFF404040), Modifier.size(48.dp)) { down ->
                if (down) viewModel.onButtonDown(Button.CAPTURE)
                else viewModel.onButtonUp(Button.CAPTURE)
            }
        }
    }
}

// ─── マクロコントロール ───────────────────────────────────
@Composable
fun MacroControls(
    macroStatus: MacroStatus,
    savedMacros: List<String>,
    onStartRec: () -> Unit,
    onStopRec: () -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onDelete: (String) -> Unit,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SwitchGray)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("マクロ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        // 録画ボタン
        when (macroStatus) {
            MacroStatus.IDLE -> {
                Button(
                    onClick = onStartRec,
                    colors = ButtonDefaults.buttonColors(containerColor = Recording),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("● 録画開始", fontSize = 12.sp)
                }
            }
            MacroStatus.RECORDING -> {
                Button(
                    onClick = onStopRec,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("■ 録画停止・保存", fontSize = 12.sp)
                }
            }
            MacroStatus.PLAYING -> {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("■ 再生停止", fontSize = 12.sp)
                }
            }
        }

        // 保存済みマクロ一覧
        if (savedMacros.isNotEmpty()) {
            savedMacros.take(3).forEach { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onPlay(name) },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("▶", color = Connected, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onDelete(name) },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("✕", color = Disconnected, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Text("マクロなし", color = Color(0xFF666666), fontSize = 11.sp)
        }
    }
}

// ─── 十字キーコンポーネント ───────────────────────────────
@Composable
fun DPad(onDirection: (Hat) -> Unit) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SwitchButton("▲", BtnGray, Modifier.size(32.dp)) { down ->
                onDirection(if (down) Hat.UP else Hat.NEUTRAL)
            }
            Row {
                SwitchButton("◀", BtnGray, Modifier.size(32.dp)) { down ->
                    onDirection(if (down) Hat.LEFT else Hat.NEUTRAL)
                }
                Box(modifier = Modifier.size(32.dp).background(SwitchGray))
                SwitchButton("▶", BtnGray, Modifier.size(32.dp)) { down ->
                    onDirection(if (down) Hat.RIGHT else Hat.NEUTRAL)
                }
            }
            SwitchButton("▼", BtnGray, Modifier.size(32.dp)) { down ->
                onDirection(if (down) Hat.DOWN else Hat.NEUTRAL)
            }
        }
    }
}

// ─── ABXYボタン ───────────────────────────────────────────
@Composable
fun ABXYPad(viewModel: MainViewModel) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SwitchButton("X", BtnX, Modifier.size(32.dp)) { down ->
                if (down) viewModel.onButtonDown(Button.X) else viewModel.onButtonUp(Button.X)
            }
            Row {
                SwitchButton("Y", BtnY, Modifier.size(32.dp)) { down ->
                    if (down) viewModel.onButtonDown(Button.Y) else viewModel.onButtonUp(Button.Y)
                }
                Box(modifier = Modifier.size(32.dp).background(SwitchGray))
                SwitchButton("A", BtnA, Modifier.size(32.dp)) { down ->
                    if (down) viewModel.onButtonDown(Button.A) else viewModel.onButtonUp(Button.A)
                }
            }
            SwitchButton("B", BtnB, Modifier.size(32.dp)) { down ->
                if (down) viewModel.onButtonDown(Button.B) else viewModel.onButtonUp(Button.B)
            }
        }
    }
}

// ─── 汎用ボタンコンポーネント ─────────────────────────────
@Composable
fun SwitchButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onPress: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Button(
        onClick = {},
        modifier = modifier
            .clip(CircleShape)
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) color.copy(alpha = 0.6f) else color
        ),
        contentPadding = PaddingValues(2.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            .also { source ->
                LaunchedEffect(source) {
                    source.interactions.collect { interaction ->
                        when (interaction) {
                            is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                                pressed = true
                                onPress(true)
                            }
                            is androidx.compose.foundation.interaction.PressInteraction.Release,
                            is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                                pressed = false
                                onPress(false)
                            }
                        }
                    }
                }
            }
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}
