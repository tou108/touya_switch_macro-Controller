// ============================================================
//  ProController.cpp
//  Nintendo Switch Pro Controller BT Classic HID エミュレーション
// ============================================================

#include "ProController.h"
#include "esp_hidd_api.h"

static const char* TAG = "ProController";

// ─── 静的メンバー初期化 ─────────────────────────────────
bool     ProController::_connected = false;
uint32_t ProController::_hidHandle  = 0;

// ─── Switch Pro Controller HID デスクリプター ────────────
//  Switchが認識する標準ゲームパッドデスクリプター
static const uint8_t hid_descriptor[] = {
    0x05, 0x01,        // Usage Page (Generic Desktop)
    0x09, 0x05,        // Usage (Gamepad)
    0xA1, 0x01,        // Collection (Application)

    // ── ボタン 16個 ────────────────────────────────────
    0x15, 0x00,        //   Logical Minimum (0)
    0x25, 0x01,        //   Logical Maximum (1)
    0x35, 0x00,        //   Physical Minimum (0)
    0x45, 0x01,        //   Physical Maximum (1)
    0x75, 0x01,        //   Report Size (1)
    0x95, 0x10,        //   Report Count (16)
    0x05, 0x09,        //   Usage Page (Button)
    0x19, 0x01,        //   Usage Minimum (Button 1)
    0x29, 0x10,        //   Usage Maximum (Button 16)
    0x81, 0x02,        //   Input (Data, Variable, Absolute)

    // ── 十字キー（HAT Switch） ──────────────────────────
    0x05, 0x01,        //   Usage Page (Generic Desktop)
    0x25, 0x07,        //   Logical Maximum (7)
    0x46, 0x3B, 0x01,  //   Physical Maximum (315)
    0x75, 0x04,        //   Report Size (4)
    0x95, 0x01,        //   Report Count (1)
    0x65, 0x14,        //   Unit (English Rotation)
    0x09, 0x39,        //   Usage (Hat Switch)
    0x81, 0x42,        //   Input (Data, Variable, Absolute, Null State)
    0x65, 0x00,        //   Unit (None)

    // パディング 4bit
    0x95, 0x01,        //   Report Count (1)
    0x75, 0x04,        //   Report Size (4)
    0x81, 0x01,        //   Input (Constant)

    // ── アナログスティック 4軸 ──────────────────────────
    0x26, 0xFF, 0x00,  //   Logical Maximum (255)
    0x46, 0xFF, 0x00,  //   Physical Maximum (255)
    0x09, 0x30,        //   Usage (X)   左スティックX
    0x09, 0x31,        //   Usage (Y)   左スティックY
    0x09, 0x32,        //   Usage (Z)   右スティックX
    0x09, 0x35,        //   Usage (Rz)  右スティックY
    0x75, 0x08,        //   Report Size (8)
    0x95, 0x04,        //   Report Count (4)
    0x81, 0x02,        //   Input (Data, Variable, Absolute)

    0xC0               // End Collection
};

// ─── GAP イベントコールバック ────────────────────────────
void ProController::_gapCallback(esp_bt_gap_cb_event_t event,
                                   esp_bt_gap_cb_param_t* param) {
    switch (event) {
        case ESP_BT_GAP_AUTH_CMPL_EVT:
            if (param->auth_cmpl.stat == ESP_BT_STATUS_SUCCESS) {
                ESP_LOGI(TAG, "ペアリング成功: %s", param->auth_cmpl.device_name);
            } else {
                ESP_LOGE(TAG, "ペアリング失敗: %d", param->auth_cmpl.stat);
            }
            break;
        default:
            break;
    }
}

// ─── HID イベントコールバック ────────────────────────────
void ProController::_hidCallback(esp_hidd_cb_event_t event,
                                   esp_hidd_cb_param_t* param) {
    switch (event) {
        case ESP_HIDD_OPEN_EVT:
            if (param->open.status == ESP_HIDD_SUCCESS) {
                _connected = true;
                _hidHandle = param->open.conn_handle;
                ESP_LOGI(TAG, "Switch と接続しました！");
            }
            break;

        case ESP_HIDD_CLOSE_EVT:
            _connected = false;
            _hidHandle = 0;
            ESP_LOGI(TAG, "Switch との接続が切れました");
            break;

        case ESP_HIDD_REG_EVT:
            if (param->reg.status == ESP_HIDD_SUCCESS) {
                ESP_LOGI(TAG, "HID デバイス登録完了");
                // ペアリング可能状態にする
                esp_bt_gap_set_scan_mode(ESP_BT_CONNECTABLE,
                                          ESP_BT_GENERAL_DISCOVERABLE);
            }
            break;

        default:
            break;
    }
}

// ─── 初期化 ─────────────────────────────────────────────
bool ProController::init() {
    esp_err_t ret;

    // BT コントローラー初期化
    esp_bt_controller_config_t bt_cfg = BT_CONTROLLER_INIT_CONFIG_DEFAULT();
    bt_cfg.mode = ESP_BT_MODE_CLASSIC_BT;

    ret = esp_bt_controller_init(&bt_cfg);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "BT controller init 失敗: %s", esp_err_to_name(ret));
        return false;
    }

    ret = esp_bt_controller_enable(ESP_BT_MODE_CLASSIC_BT);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "BT controller enable 失敗: %s", esp_err_to_name(ret));
        return false;
    }

    // Bluedroid スタック初期化
    ret = esp_bluedroid_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Bluedroid init 失敗: %s", esp_err_to_name(ret));
        return false;
    }

    ret = esp_bluedroid_enable();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Bluedroid enable 失敗: %s", esp_err_to_name(ret));
        return false;
    }

    // デバイス名を "Pro Controller" に設定（Switchが認識する名前）
    esp_bt_dev_set_device_name("Pro Controller");

    // GAP コールバック登録
    esp_bt_gap_register_callback(_gapCallback);

    // SSP（Secure Simple Pairing）設定
    esp_bt_sp_param_t param_type = ESP_BT_SP_IOCAP_MODE;
    esp_bt_io_cap_t iocap = ESP_BT_IO_CAP_NONE;
    esp_bt_gap_set_security_param(param_type, &iocap, sizeof(iocap));

    // HID デバイス設定
    esp_hidd_app_param_t app_param = {
        .name        = "Pro Controller",
        .description = "Nintendo Switch Pro Controller",
        .provider    = "Nintendo Co., Ltd.",
        .subclass    = 0x08,  // Gamepad
        .desc_list     = hid_descriptor,
        .desc_list_len = sizeof(hid_descriptor)
    };

    esp_hidd_qos_param_t qos_param = {
        .service_type     = 0,
        .token_rate       = 0,
        .token_bucket_size = 0,
        .peak_bandwidth   = 0,
        .access_latency   = 0xFFFFFFFF,
        .delay_variation  = 0xFFFFFFFF
    };

    // HID コールバック登録
    ret = esp_bt_hid_device_register_callback(_hidCallback);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "HID callback 登録失敗: %s", esp_err_to_name(ret));
        return false;
    }

    // HID デバイス初期化
    ret = esp_bt_hid_device_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "HID init 失敗: %s", esp_err_to_name(ret));
        return false;
    }

    // HID アプリ登録
    ret = esp_bt_hid_device_register_app(&app_param, &qos_param, &qos_param);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "HID app 登録失敗: %s", esp_err_to_name(ret));
        return false;
    }

    ESP_LOGI(TAG, "Pro Controller 初期化完了！Switchからペアリングしてください");
    return true;
}

// ─── HIDレポート送信 ────────────────────────────────────
bool ProController::sendReport(const ControllerReport& report) {
    if (!_connected) return false;

    // 8バイトのHIDレポートを構築
    uint8_t hidReport[8];

    // バイト0-1: ボタン
    hidReport[0] = (uint8_t)(report.buttons & 0xFF);
    hidReport[1] = (uint8_t)((report.buttons >> 8) & 0xFF);

    // バイト2: HAT(上位4bit) + パディング(下位4bit)
    hidReport[2] = (report.hat & 0x0F) << 4;

    // バイト3-6: アナログスティック
    hidReport[3] = report.lx;
    hidReport[4] = report.ly;
    hidReport[5] = report.rx;
    hidReport[6] = report.ry;

    // バイト7: 予約
    hidReport[7] = 0x00;

    esp_err_t ret = esp_bt_hid_device_send_report(
        ESP_HIDD_REPORT_TYPE_INTRDATA,
        0x01,           // Report ID
        sizeof(hidReport),
        hidReport
    );

    return ret == ESP_OK;
}

// ─── ニュートラル状態を送信 ──────────────────────────────
void ProController::sendNeutral() {
    ControllerReport neutral = {
        .buttons = 0,
        .hat     = HAT_NEUTRAL,
        .lx      = STICK_CENTER,
        .ly      = STICK_CENTER,
        .rx      = STICK_CENTER,
        .ry      = STICK_CENTER
    };
    sendReport(neutral);
}

// ─── 接続状態確認 ────────────────────────────────────────
bool ProController::isConnected() {
    return _connected;
}
