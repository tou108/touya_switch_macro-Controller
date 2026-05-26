# nx_switch_マクロ (NX Switch Macro Controller)

> AndroidをNintendo Switchのコントローラーとして使用するアプリ  
> Use your Android device as a Nintendo Switch controller

---

## 🎮 概要 / Overview

**nx_switch_マクロ** は、AndroidスマートフォンをNintendo Switchのコントローラーとして使用できるオープンソースアプリです。  
画面キャプチャ・画像認識・マクロ自動操作機能を搭載しています。

**nx_switch_マクロ** is an open-source app that lets you use your Android smartphone as a Nintendo Switch controller, with screen capture, image recognition, and macro automation features.

---

## ✨ 機能 / Features

- 🎮 **コントローラー操作** — AndroidでSwitchを操作
- 📸 **画面キャプチャ** — Switch画面をAndroidにリアルタイム表示
- 🔁 **マクロ録画・再生** — ボタン操作を記録して自動再生
- 👁️ **画像認識** — 画面の状態を認識して自動操作
- 📡 **WiFi接続** — PCなし、Android単体で動作

---

## 🛒 必要なもの / Hardware Required

| 商品 | 用途 | 目安価格 |
|---|---|---|
| ESP32開発ボード（38ピン・技適付き） | BT ProControllerエミュレート | ¥1,000〜1,500 |
| HDMIキャプチャーカード（MS2130・USB-C対応） | Switch画面取り込み | ¥1,500〜2,300 |
| USB-C OTGアダプター（USB-Aメス付き） | キャプチャーカード接続 | ¥500〜800 |
| Switchドック + HDMIケーブル | Switch映像出力 | 手持ちでOK |

---

## 📡 接続図 / Connection Diagram

```
Switch ──HDMI──▶ キャプチャーカード ──USB-C OTG──▶ Android
                                                        │ WiFi
                                                        ▼
                                                      ESP32
                                                        │ Bluetooth Classic
                                                        ▼
                                                      Switch（コントローラー入力）
```

---

## 🚀 セットアップ / Setup

### 1. ESP32ファームウェアの書き込み

1. [Arduino IDE](https://www.arduino.cc/en/software) をPCにインストール
2. ESP32ボードを追加（`ファイル > 環境設定 > 追加のボードマネージャのURL`）
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
3. ライブラリをインストール：
   - `WebSockets` by Markus Sattler
   - `ArduinoJson` by Benoit Blanchon
4. `esp32-firmware/nx_controller/nx_controller.ino` を開いて書き込む

### 2. Androidアプリのインストール

[Releases](https://github.com/tou108/touya_switch_macro-Controller/releases) から最新の `.apk` をダウンロードしてインストール

> ⚙️ 設定 > セキュリティ > 「提供元不明のアプリ」を許可してください

### 3. 接続手順

1. ESP32の電源を入れる
2. AndroidのWiFiで **`NX_Switch_Macro`** に接続（パスワード: `nxswitch123`）
3. アプリを起動 → 自動接続
4. SwitchのBluetoothで ESP32 を「Proコントローラー」としてペアリング
5. HDMIキャプチャーカードをAndroidに接続

---

## 📱 動作環境 / Requirements

- Android 11以上（Android 15推奨）
- USB OTG対応端末
- ESP32開発ボード（技適マーク付き）

---

## ⚠️ 免責事項 / Disclaimer

本アプリはNintendo社の非公式アプリです。Nintendo・任天堂・Nintendo Switchは任天堂株式会社の商標です。  
This is an unofficial app and is not affiliated with or endorsed by Nintendo Co., Ltd.

---

## 📄 ライセンス / License

MIT License — 詳細は [LICENSE](LICENSE) を参照

---

## 🤝 コントリビュート / Contributing

Issue・PR歓迎です！詳細は [docs/ja/CONTRIBUTING.md](docs/ja/CONTRIBUTING.md) を参照。
