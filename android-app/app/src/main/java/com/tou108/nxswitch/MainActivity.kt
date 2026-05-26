package com.tou108.nxswitch

// ============================================================
//  MainActivity.kt
//  アプリのエントリーポイント
// ============================================================

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tou108.nxswitch.ui.screens.MainScreen
import com.tou108.nxswitch.ui.theme.NxSwitchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NxSwitchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }

        // アプリ起動時に自動接続
        viewModel.connect()
    }

    override fun onStop() {
        super.onStop()
        // バックグラウンドでも接続維持（マクロ再生中のため）
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }
}
