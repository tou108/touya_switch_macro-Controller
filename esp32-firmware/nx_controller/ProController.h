#pragma once

// ============================================================
//  ProController.h
//  Nintendo Switch Pro Controller エミュレーション
//  Bluetooth Classic HID Device (ESP-IDF API)
// ============================================================

#include "esp_bt.h"
#include "esp_bt_main.h"
#include "esp_gap_bt_api.h"
#include "esp_bt_device.h"
#include "esp_log.h"
#include <stdint.h>
#include <stdbool.h>

// ─── ボタン定義（Androidアプリと共通） ───────────────────
#define BTN_Y        (1 << 0)
#define BTN_B        (1 << 1)
#define BTN_A        (1 << 2)
#define BTN_X        (1 << 3)
#define BTN_L        (1 << 4)
#define BTN_R        (1 << 5)
#define BTN_ZL       (1 << 6)
#define BTN_ZR       (1 << 7)
#define BTN_MINUS    (1 << 8)
#define BTN_PLUS     (1 << 9)
#define BTN_LS       (1 << 10)  // 左スティック押し込み
#define BTN_RS       (1 << 11)  // 右スティック押し込み
#define BTN_HOME     (1 << 12)
#define BTN_CAPTURE  (1 << 13)

// ─── 十字キー（HAT）定義 ─────────────────────────────────
#define HAT_UP        0
#define HAT_UP_RIGHT  1
#define HAT_RIGHT     2
#define HAT_DOWN_RIGHT 3
#define HAT_DOWN      4
#define HAT_DOWN_LEFT 5
#define HAT_LEFT      6
#define HAT_UP_LEFT   7
#define HAT_NEUTRAL   8

// ─── スティック中央値 ────────────────────────────────────
#define STICK_CENTER  128

// ─── コントローラーレポート構造体 ────────────────────────
struct ControllerReport {
    uint16_t buttons;   // ボタン状態（ビットマスク）
    uint8_t  hat;       // 十字キー (0-7, 8=ニュートラル)
    uint8_t  lx;        // 左スティックX (0-255, 中央=128)
    uint8_t  ly;        // 左スティックY (0-255, 中央=128)
    uint8_t  rx;        // 右スティックX (0-255, 中央=128)
    uint8_t  ry;        // 右スティックY (0-255, 中央=128)
};

// ─── ProController クラス ────────────────────────────────
class ProController {
public:
    // 初期化（setup()で1度だけ呼ぶ）
    static bool init();

    // HIDレポートをSwitchに送信
    static bool sendReport(const ControllerReport& report);

    // 全ボタンを離した状態を送信
    static void sendNeutral();

    // Switch との接続状態
    static bool isConnected();

    // 内部コールバック（直接呼ばない）
    static void _gapCallback(esp_bt_gap_cb_event_t event, esp_bt_gap_cb_param_t* param);
    static void _hidCallback(esp_hidd_cb_event_t event, esp_hidd_cb_param_t* param);

private:
    static bool     _connected;
    static uint32_t _hidHandle;

    static void _registerApp();
    static void _buildHidDescriptor();
};
