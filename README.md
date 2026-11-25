# Android BLE Master Project

Kotlin · Jetpack Compose · MVVM · Clean Architecture · Hilt · Coroutines/Flow · BLE GATT

A learning project that implements the five core BLE scenarios on a real device:

1. Scan and discover a nearby BLE device
2. Connect and discover GATT services
3. Read and write a characteristic
4. Enable notifications and receive continuous data
5. Handle disconnection, reconnection and errors

## Run it
1. Open the folder in **Android Studio** (Ladybug or newer). Let Gradle sync (it also generates the Gradle wrapper jar).
2. Connect a **physical Android phone** (API 26+). Emulators cannot do real BLE.
3. Flash `docs/esp32/ble_learning_peripheral.ino` to an ESP32, or use **nRF Connect → Advertiser** on a second phone.
4. Run the app → Scan → tap your device → expand a service → open a characteristic.

## Structure
The requested layout is implemented as **packages in a single `app` module** (easier to build and read while learning).
Each top-level folder maps 1:1 to a Gradle module if you want to split later.

```
app/src/main/java/com/example/blelearning/
├── core/common        ValueFormatter, BleConfig
├── core/bluetooth     BleUuids, GattStatus, BlePermissions
├── domain/model       BleDevice, ConnectionState, GattModels, BleException, LogEntry
├── domain/repository  interfaces (no Android types)
├── domain/usecase     one small class per action
├── data/bluetooth     BleScanner (callbackFlow), GattClient (operation queue)
├── data/repository    implementations
├── data/mapper        ScanResult / GATT -> domain
├── di                 Hilt modules
└── presentation       dashboard, scanner, device, gatt, notification, logs (+ navigation, theme)
docs/                  roadmap, interview questions, Medium draft, scanning flow, mentor prompt, ESP32 sketch
```

Dependency rule: `presentation → domain ← data`. `domain` never imports Android.

## Learning path
Read `docs/BLE_LEARNING_ROADMAP.md`, then paste `docs/MENTOR_PROMPT.md` into Cursor/Claude. `docs/FLOW_01_SCANNING.md` is the first lesson.

## Tests
`./gradlew testDebugUnitTest` runs `ValueFormatterTest` and `ScanDevicesUseCaseTest`.

## Known simplifications
- One connection at a time (`GattClient` is a singleton).
- No foreground service: the connection lives while the process lives.
- No bonding / encryption flow, no scan filters (both are roadmap exercises).
