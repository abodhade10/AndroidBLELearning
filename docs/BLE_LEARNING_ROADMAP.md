# BLE Learning Roadmap

Each phase lists the concepts, where to see them in this project, and a hands-on exercise.
Verify everything on a **physical phone** with nRF Connect (phone #2 as peripheral) or the ESP32 sketch in `docs/esp32/`.

## Phase 1 – BLE Fundamentals
**Concepts:** Classic vs LE (low power, small packets, GATT-based), roles (Central / Peripheral, Observer / Broadcaster), GAP (advertising, scanning, connection), GATT (client/server, services, characteristics, descriptors), ATT (the protocol under GATT), UUIDs (16-bit vs 128-bit), advertising packet (31 bytes) + scan response.
**In the project:** `core/bluetooth/BleUuids.kt`, `domain/model/GattModels.kt`.
**Exercise:** Open nRF Connect, advertise from phone #2, and read the raw advertising record. Identify flags, name, service UUIDs.

## Phase 2 – Android BLE APIs
**Concepts:** `BluetoothManager`, `BluetoothAdapter`, `BluetoothLeScanner`, `ScanCallback`, `ScanSettings` (modes, callback types), `ScanFilter`, runtime permissions (Android 12 `BLUETOOTH_SCAN/CONNECT` vs ≤11 location), why scans are location-gated before Android 12, scan throttling (5 starts / 30 s).
**In the project:** `data/bluetooth/BleScanner.kt`, `core/bluetooth/BlePermissions.kt`, `presentation/scanner/*`.
**Exercises:**
1. Add a `ScanFilter` for `BleUuids.LEARNING_SERVICE` and confirm only the ESP32 shows.
2. Deny the permission, observe the error path. Turn Bluetooth off mid-scan, observe the crash risk in `stopScan` (already guarded).

## Phase 3 – GATT Communication
**Concepts:** `connectGatt`, `autoConnect`, `discoverServices`, one-operation-at-a-time rule, `readCharacteristic`, `writeCharacteristic` (with / without response), descriptors, the API 33 callback/signature changes.
**In the project:** `data/bluetooth/GattClient.kt` (`execute`, `read`, `write`), `presentation/gatt/*`.
**Exercises:**
1. Write `48 65 6C 6C 6F` to Echo, read it back as ASCII.
2. Fire two reads concurrently from two coroutines, check the log to see them serialised.
3. Write 30 bytes before and after requesting MTU 247. What changes?

## Phase 4 – Advanced BLE
**Concepts:** Notifications vs indications, CCCD, MTU (ATT payload = MTU − 3), RSSI, connection parameters, reconnection strategy (backoff), long writes, packet reassembly, bonding vs pairing.
**In the project:** `setNotification`, `requestMtu`, `handleDisconnected`, `presentation/notification/*`.
**Exercises:**
1. Enable Counter notifications, watch 1 Hz updates. Disable them, confirm the stream stops.
2. Power-cycle the ESP32 and observe `Reconnecting (attempt n of 3)` with 1 s / 2 s / 4 s delays.
3. Remove the CCCD write from `setNotification` and observe nothing arrives.

## Phase 5 – Architecture
**Concepts:** MVVM, Clean Architecture dependency rule (presentation → domain ← data), repository pattern, use cases, `Flow`/`StateFlow`/`SharedFlow`, `callbackFlow`, bridging callback APIs to coroutines (`CompletableDeferred`), Hilt.
**In the project:** the package layout, `ScanDevicesUseCase`, `GattClient.execute`.
**Exercises:** Write a fake `BleConnectionRepository` and unit-test `GattViewModel`. Replace `InMemoryLogRepository` with a file logger without touching any ViewModel.

## Phase 6 – Production Readiness
**Concepts:** foreground service for long connections (`connectedDevice` type on Android 14), Companion Device Manager, battery (scan modes, duty cycles, filters), bonding / encryption (LE Secure Connections), error taxonomy, retry policy, OEM quirks (status 133, cache), `refresh()` hidden API trade-offs, testing strategy (fakes, instrumented tests with a real peripheral), logging without leaking user identifiers, Play Console Bluetooth declaration.
**Exercises:** Move `GattClient` ownership into a foreground service. Add a connection watchdog. Add an instrumented smoke test that connects to the ESP32.
