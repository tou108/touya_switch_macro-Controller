package com.tou108.nxswitch.ui.theme

// ============================================================
//  Theme.kt  —  nx_switch_マクロ Compose テーマ
// ============================================================

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── カラーパレット ───────────────────────────────────────
val Primary       = Color(0xFF00E5FF)   // シアン（Switchっぽい）
val Secondary     = Color(0xFFE040FB)   // 紫
val Background    = Color(0xFF0F0F0F)   // ほぼ黒
val Surface       = Color(0xFF1E1E1E)   // ダークグレー
val Error         = Color(0xFFFF1744)
val OnPrimary     = Color(0xFF000000)
val OnBackground  = Color(0xFFE0E0E0)
val OnSurface     = Color(0xFFBDBDBD)

private val DarkColorScheme = darkColorScheme(
    primary      = Primary,
    secondary    = Secondary,
    background   = Background,
    surface      = Surface,
    error        = Error,
    onPrimary    = OnPrimary,
    onBackground = OnBackground,
    onSurface    = OnSurface,
)

// ─── タイポグラフィ ───────────────────────────────────────
val NxTypography = androidx.compose.material3.Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        color      = OnBackground
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        color      = OnSurface
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        color      = OnBackground
    )
)

// ─── テーマ ───────────────────────────────────────────────
@Composable
fun NxSwitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = NxTypography,
        content     = content
    )
}
