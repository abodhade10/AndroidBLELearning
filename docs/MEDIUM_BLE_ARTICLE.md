# Building a Production-Style BLE Client on Android with Kotlin, Compose and Clean Architecture

*Draft for Medium. Replace the placeholders with your own screenshots and measured results from the ESP32.*

Bluetooth Low Energy has a reputation on Android: status 133, silent failures, callbacks that arrive on the wrong thread. Most of that pain comes from one fact. The platform API is a thin, callback-based wrapper over an asynchronous radio stack with strict rules. In this article we build a small BLE client that respects those rules and keeps the rest of the app clean.

## 1. The mental model

- **GAP** gets two devices to find each other: one advertises, one scans.
- **GATT** is a tiny database exposed by the peripheral: *services* contain *characteristics* which have a *value*, *properties* (read / write / notify) and *descriptors*.
- The phone (central) is the GATT **client**. The sensor (peripheral) is the GATT **server**.

## 2. Architecture

```
presentation (Compose + ViewModel)  ->  domain (models, use cases, repository interfaces)  <-  data (BLE + repositories)
```

The domain layer has zero `android.bluetooth` imports. That is what makes `ScanDevicesUseCase` unit-testable with a three-line fake.

## 3. Scanning as a cold Flow

```kotlin
fun scan(): Flow<BleDevice> = callbackFlow {
    val cb = object : ScanCallback() {
        override fun onScanResult(type: Int, r: ScanResult) { trySend(r.toBleDevice()) }
        override fun onScanFailed(code: Int) { close(BleException.ScanFailed(code)) }
    }
    scanner.startScan(null, settings, cb)
    awaitClose { scanner.stopScan(cb) }
}
```

Collecting starts the scan; cancelling the collector stops it. A ViewModel can no longer leak a scan. The use case then de-duplicates by MAC address and sorts by RSSI.

## 4. The rule that breaks everyone: one operation at a time

Android allows a single outstanding GATT operation. We turn that rule into code:

```kotlin
private suspend fun execute(name: String, start: (BluetoothGatt) -> Boolean) = opMutex.withLock {
    val deferred = CompletableDeferred<OpResult>().also { pending = it }
    if (!start(gatt)) return Result.failure(...)
    withTimeout(10_000) { deferred.await() }   // completed by the BluetoothGattCallback
}
```

Every read, write, descriptor write, MTU change and RSSI read goes through this function. Callers write straight-line suspend code, the queue, timeout and error mapping live in one place.

## 5. Notifications are a two-step dance

1. `setCharacteristicNotification()` tells the *local* stack to deliver them.
2. Writing `0x0001` to the CCCD descriptor (0x2902) tells the *peripheral* to send them.

Forget step 2 and nothing arrives, with no error.

## 6. Reconnection

On an unexpected disconnect we `close()` the GATT object (never reuse it), wait 1 s, 2 s, 4 s with exponential backoff and surface `Reconnecting(attempt, max)` to the UI. A user-initiated disconnect cancels pending retries.

## 7. Testing on real hardware

An ESP32 sketch (included in the repository) exposes an echo characteristic and a 1 Hz counter notification. nRF Connect is your second pair of eyes: if it works in nRF Connect and not in your app, the bug is yours.

## 8. Lessons

- Always `close()` GATT objects.
- Treat status 133 as "retry after a delay", not as a bug in your logic.
- Keep Android types out of your domain layer.
- Log every callback with its status, you will need it.

*Next steps: foreground service, bonding, and a DFU flow.*
