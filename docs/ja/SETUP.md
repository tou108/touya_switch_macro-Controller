# セットアップガイド

## 必要なもの

| 商品 | 備考 |
|---|---|
| ESP32開発ボード（38ピン・技適付き） | Amazonで¥1,000〜1,500 |
| HDMIキャプチャーカード（MS2130チップ・USB-C対応） | Amazonで¥1,500〜2,300 |
| USB-C OTGアダプター（USB-Aメス付き） | Amazonで¥500〜800 |
| Switchドック + HDMIケーブル | 手持ちでOK |
| Android 11以上のスマートフォン | USB OTG対応必須 |

---

## Step 1：ESP32にファームウェアを書き込む

> PCが必要なのはこのステップだけです

### 1-1. Arduino IDE をインストール
[https://www.arduino.cc/en/software](https://www.arduino.cc/en/software) からダウンロード

### 1-2. ESP32ボードを追加
`ファイル > 環境設定 > 追加のボードマネージャのURL` に以下を追加：
```
https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
```

`ツール > ボード > ボードマネージャ` で `esp32` を検索してインストール

### 1-3. ライブラリをインストール
`ツール > ライブラリを管理` で以下をインストール：
- **WebSockets** by Markus Sattler
- **ArduinoJson** by Benoit Blanchon

### 1-4. ファームウェアを書き込む
1. `esp32-firmware/nx_controller/nx_controller.ino` を開く
2. `ツール > ボード` で `ESP32 Dev Module` を選択
3. ESP32をPCにUSBで接続
4. `ツール > シリアルポート` で ESP32のポートを選択
5. `→`（書き込み）ボタンをクリック

書き込み完了後、シリアルモニタに以下が表示されれば成功：
```
✅ 準備完了！
  1. AndroidをWiFi「NX_Switch_Macro」に接続
  2. Switchのコントローラー設定からペアリング
```

---

## Step 2：Androidアプリをインストール

1. [Releases](https://github.com/tou108/touya_switch_macro-Controller/releases) から最新の `nx_switch_macro_v*.apk` をダウンロード
2. `設定 > セキュリティ > 提供元不明のアプリ` を許可
3. APKをタップしてインストール

---

## Step 3：接続する

### 3-1. ハードウェアを接続
```
Switch ──HDMI──▶ キャプチャーカード ──USB-C OTG──▶ Android
```

### 3-2. ESP32の電源を入れる
モバイルバッテリーか USB アダプターで給電

### 3-3. AndroidのWiFiを切り替える
`設定 > WiFi` で **NX_Switch_Macro** に接続  
パスワード: `nxswitch123`

### 3-4. アプリを起動
起動すると自動的にESP32に接続されます  
ステータスバーの **ESP32** が緑になれば OK

### 3-5. SwitchとBluetoothペアリング
1. Switch のコントローラー設定を開く
2. `コントローラーとセンサー > コントローラーの持ち方/順番を変える`
3. **Pro コントローラー** として ESP32 が表示される
4. ペアリングするとステータスバーの **Switch** が緑になる

---

## Step 4：使い方

### コントローラー操作
画面のボタンをタップ／長押しするだけ

### マクロ録画
1. `● 録画開始` をタップ
2. Switchを操作する
3. `■ 録画停止・保存` をタップ → マクロ名を入力して保存

### マクロ再生
保存済みマクロの `▶` をタップ

### 自動操作（画像認識）
（第3週実装予定）画面の状態を認識して自動でマクロを実行

---

## トラブルシューティング

| 症状 | 対処法 |
|---|---|
| ESP32に接続できない | WiFiが「NX_Switch_Macro」になっているか確認 |
| Switchに認識されない | SwitchのBTをOFFにして再度ペアリング |
| キャプチャーが映らない | OTGアダプターとUSBケーブルの接続を確認 |
| アプリがクラッシュ | Android 11以上のOTG対応端末か確認 |
