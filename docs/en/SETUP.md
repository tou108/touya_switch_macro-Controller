# Setup Guide

## Requirements

| Item | Notes |
|---|---|
| ESP32 Development Board (38-pin, Japan approved) | ~$8-12 on Amazon |
| HDMI Capture Card (MS2130 chip, USB-C) | ~$12-18 on Amazon |
| USB-C OTG Adapter (with USB-A female) | ~$4-6 on Amazon |
| Nintendo Switch Dock + HDMI Cable | You probably have these |
| Android 11+ Smartphone | Must support USB OTG |

---

## Step 1: Flash ESP32 Firmware

> A PC is only needed for this step

### 1-1. Install Arduino IDE
Download from [https://www.arduino.cc/en/software](https://www.arduino.cc/en/software)

### 1-2. Add ESP32 Board
Go to `File > Preferences > Additional Board Manager URLs` and add:
```
https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
```
Then go to `Tools > Board > Board Manager`, search `esp32` and install.

### 1-3. Install Libraries
Go to `Tools > Manage Libraries` and install:
- **WebSockets** by Markus Sattler
- **ArduinoJson** by Benoit Blanchon

### 1-4. Flash Firmware
1. Open `esp32-firmware/nx_controller/nx_controller.ino`
2. Select `ESP32 Dev Module` under `Tools > Board`
3. Connect ESP32 via USB
4. Select the correct port under `Tools > Port`
5. Click Upload (→)

Success message in Serial Monitor:
```
✅ Ready!
  1. Connect Android to WiFi "NX_Switch_Macro"
  2. Pair Switch via Bluetooth settings
```

---

## Step 2: Install Android App

1. Download the latest `nx_switch_macro_v*.apk` from [Releases](https://github.com/tou108/touya_switch_macro-Controller/releases)
2. Enable `Settings > Security > Install unknown apps`
3. Tap the APK to install

---

## Step 3: Connect Everything

### 3-1. Hardware Connection
```
Switch ──HDMI──▶ Capture Card ──USB-C OTG──▶ Android
```

### 3-2. Power the ESP32
Use a USB power bank or adapter

### 3-3. Connect Android to ESP32 WiFi
Go to `Settings > WiFi` and connect to **NX_Switch_Macro**  
Password: `nxswitch123`

### 3-4. Launch the App
It will auto-connect to ESP32.  
The **ESP32** indicator turns green when connected.

### 3-5. Pair Switch via Bluetooth
1. On Switch: `System Settings > Controllers and Sensors > Change Grip/Order`
2. ESP32 appears as **Pro Controller**
3. Pair it — the **Switch** indicator in the app turns green

---

## Step 4: Usage

### Controller
Tap and hold buttons on screen to control your Switch

### Macro Recording
1. Tap `● Start Recording`
2. Play your game
3. Tap `■ Stop & Save` → Enter a name

### Macro Playback
Tap `▶` next to any saved macro

### Auto Play (Image Recognition)
_(Coming in Week 3)_ Automatically triggers macros based on screen content

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Can't connect to ESP32 | Make sure WiFi is set to "NX_Switch_Macro" |
| Switch doesn't detect controller | Turn Switch BT off and re-pair |
| No capture video | Check OTG adapter and USB cable connections |
| App crashes | Verify Android 11+ and OTG support |
