// ============================================================
//  nx_controller.ino
//  nx_switch_マクロ — ESP32ファームウェア メインエントリー
//
//  【必要なライブラリ】Arduino IDEでインストール
//    - WebSockets by Markus Sattler
//    - ArduinoJson by Benoit Blanchon
//
//  【動作フロー】
//    1. ESP32がWiFi APを起動（SSID: NX_Switch_Macro）
//    2. AndroidがそのWiFiに接続
//    3. AndroidからWebSocketでボタン状態を受信
//    4. ESP32がBluetooth Classic HIDでSwitchにレポート送信
// ============================================================

#include <Arduino.h>
#include <WiFi.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>
#include "ProController.h"

// ─── WiFi AP 設定 ────────────────────────────────────────
const char* AP_SSID     = "NX_Switch_Macro";
const char* AP_PASSWORD = "nxswitch123";
const IPAddress AP_IP(192, 168, 4, 1);
const IPAddress AP_SUBNET(255, 255, 255, 0);

// ─── WebSocket サーバー（ポート81） ──────────────────────
WebSocketsServer webSocket(81);

// ─── LED ピン（ESP32内蔵LED） ────────────────────────────
const int LED_PIN = 2;

// ─── 現在のコントローラー状態 ────────────────────────────
ControllerReport currentReport = {
    .buttons = 0,
    .hat     = HAT_NEUTRAL,
    .lx      = STICK_CENTER,
    .ly      = STICK_CENTER,
    .rx      = STICK_CENTER,
    .ry      = STICK_CENTER
};

// ─── WebSocket イベントハンドラー ─────────────────────────
void onWebSocketEvent(uint8_t clientId, WStype_t type,
                      uint8_t* payload, size_t length) {
    switch (type) {

        case WStype_CONNECTED: {
            IPAddress clientIP = webSocket.remoteIP(clientId);
            Serial.printf("[WS] Android 接続: %d.%d.%d.%d\n",
                          clientIP[0], clientIP[1], clientIP[2], clientIP[3]);
            digitalWrite(LED_PIN, HIGH);

            // 接続確認メッセージを送信
            StaticJsonDocument<64> doc;
            doc["status"] = "connected";
            doc["switch_connected"] = ProController::isConnected();
            String msg;
            serializeJson(doc, msg);
            webSocket.sendTXT(clientId, msg);
            break;
        }

        case WStype_DISCONNECTED:
            Serial.printf("[WS] Android 切断: %d\n", clientId);
            digitalWrite(LED_PIN, LOW);
            ProController::sendNeutral();  // 全ボタン離す
            break;

        case WStype_TEXT: {
            // JSON パース
            // 受信フォーマット:
            // {"b":0,"h":8,"lx":128,"ly":128,"rx":128,"ry":128}
            // b=buttons, h=hat, lx/ly/rx/ry=スティック
            StaticJsonDocument<128> doc;
            DeserializationError err = deserializeJson(doc, payload, length);
            if (err) {
                Serial.printf("[WS] JSON エラー: %s\n", err.c_str());
                return;
            }

            currentReport.buttons = doc["b"]  | 0;
            currentReport.hat     = doc["h"]  | HAT_NEUTRAL;
            currentReport.lx      = doc["lx"] | STICK_CENTER;
            currentReport.ly      = doc["ly"] | STICK_CENTER;
            currentReport.rx      = doc["rx"] | STICK_CENTER;
            currentReport.ry      = doc["ry"] | STICK_CENTER;

            // Switchへ送信
            if (ProController::isConnected()) {
                ProController::sendReport(currentReport);
            }
            break;
        }

        default:
            break;
    }
}

// ─── 定期ステータス送信タスク ─────────────────────────────
unsigned long lastStatusTime = 0;
const unsigned long STATUS_INTERVAL = 2000;  // 2秒ごと

void sendStatus() {
    if (millis() - lastStatusTime < STATUS_INTERVAL) return;
    lastStatusTime = millis();

    StaticJsonDocument<64> doc;
    doc["status"] = "ok";
    doc["switch_connected"] = ProController::isConnected();

    String msg;
    serializeJson(doc, msg);
    webSocket.broadcastTXT(msg);
}

// ─── setup ───────────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    delay(500);

    Serial.println("\n========================================");
    Serial.println("  nx_switch_マクロ — ESP32 ファーム");
    Serial.println("========================================");

    // LED 初期化
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, LOW);

    // ── WiFi AP 起動 ──────────────────────────────────────
    Serial.println("[WiFi] AP を起動中...");
    WiFi.softAPConfig(AP_IP, AP_IP, AP_SUBNET);
    WiFi.softAP(AP_SSID, AP_PASSWORD);

    Serial.printf("[WiFi] SSID     : %s\n", AP_SSID);
    Serial.printf("[WiFi] Password : %s\n", AP_PASSWORD);
    Serial.printf("[WiFi] IP アドレス: %s\n", AP_IP.toString().c_str());

    // ── WebSocket サーバー起動 ────────────────────────────
    webSocket.begin();
    webSocket.onEvent(onWebSocketEvent);
    Serial.println("[WS] WebSocket サーバー起動（ポート81）");

    // ── Bluetooth Pro Controller 初期化 ───────────────────
    Serial.println("[BT] Pro Controller 初期化中...");
    if (ProController::init()) {
        Serial.println("[BT] 初期化完了！SwitchのBTからペアリングしてください");
    } else {
        Serial.println("[BT] 初期化失敗！再起動します...");
        delay(3000);
        ESP.restart();
    }

    Serial.println("\n✅ 準備完了！");
    Serial.println("  1. AndroidをWiFi「NX_Switch_Macro」に接続");
    Serial.println("  2. Switchのコントローラー設定からペアリング");
}

// ─── loop ────────────────────────────────────────────────
void loop() {
    webSocket.loop();
    sendStatus();

    // Switch接続状態をLEDで表示（点滅=未接続、点灯=接続中）
    if (!ProController::isConnected()) {
        static unsigned long lastBlink = 0;
        static bool ledState = false;
        if (millis() - lastBlink > 500) {
            lastBlink = millis();
            ledState = !ledState;
            digitalWrite(LED_PIN, ledState);
        }
    }
}
