# BLE Interview Questions (Senior Android)

## Fundamentals
1. **Classic vs BLE?** BLE trades throughput for very low power: short bursts, GATT data model, connection intervals measured in ms, ~1-2 Mbps PHY but practical app throughput far lower. Classic is stream oriented (A2DP, SPP).
2. **GAP vs GATT?** GAP controls discovery and connections (advertising, scanning, roles). GATT defines how data is organised and exchanged after connecting (services → characteristics → descriptors) on top of ATT.
3. **Central vs peripheral?** Central scans and initiates connections (the phone). Peripheral advertises and accepts a connection (the sensor). Android can do both, but this app is a central.
4. **What is in a characteristic?** Declaration (properties), value, and optional descriptors (e.g. CCCD 0x2902, user description 0x2901).
5. **16-bit vs 128-bit UUID?** 16-bit is shorthand for `0000XXXX-0000-1000-8000-00805F9B34FB`. Custom services use full 128-bit UUIDs.
6. **How large can a write be?** ATT MTU − 3 bytes per operation (default MTU 23 → 20 bytes). Request up to 517; the peer may grant less. Larger values need long writes or app-level chunking.

## Android APIs
7. **Permissions?** Android 12+: `BLUETOOTH_SCAN` (+ `neverForLocation` if applicable), `BLUETOOTH_CONNECT`. Android ≤ 11: `ACCESS_FINE_LOCATION` and location services on. Missing permission throws `SecurityException`.
8. **Why must I always call `close()`?** `disconnect()` only ends the link; `close()` releases the client interface. Leaking GATT clients eventually makes `connectGatt` fail (status 133 or silent).
9. **`autoConnect` true vs false?** false = direct attempt, fast, ~30 s timeout. true = background connection whenever the device is seen, slower to start, good for reconnection to bonded devices.
10. **Which thread do callbacks run on?** A Binder thread (not main). Post to a dispatcher / use thread-safe holders (`StateFlow`).
11. **What changed in API 33?** `writeCharacteristic(ch, value, type)`, `writeDescriptor(d, value)` return status codes; `onCharacteristicRead/Changed` deliver the value as a parameter; `characteristic.value` deprecated (shared mutable buffer race).
12. **Scan modes / battery?** `LOW_POWER`, `BALANCED`, `LOW_LATENCY`, `OPPORTUNISTIC`. Use filters, stop scanning promptly, background scans must use filters.

## GATT behaviour
13. **Why a GATT operation queue?** Android allows one pending operation. A second call before the callback typically returns false or is dropped. Serialise with a Mutex / queue and complete on the matching callback.
14. **How do you enable notifications?** `setCharacteristicNotification(ch, true)` (local) **and** write `ENABLE_NOTIFICATION_VALUE` to the CCCD (remote). Indications use `ENABLE_INDICATION_VALUE` and are acknowledged at the ATT layer.
15. **Notify vs indicate?** Notify: no ack, higher throughput, can be lost. Indicate: confirmed, one in flight at a time.
16. **Write with vs without response?** With response: reliable, callback after the peer acks. Without: fastest, no delivery confirmation; limited by the controller buffer, flow control is on you.
17. **Status 133?** Generic `GATT_ERROR`. Typical causes: device out of range, stale GATT cache, too many clients, connecting while scanning, connecting too soon after disconnect. Mitigate with retry + delay, `close()`, avoid concurrent connect attempts.
18. **Service discovery returns nothing / stale services?** Android caches GATT database per bonded address. Respect Service Changed indication; the hidden `BluetoothGatt.refresh()` is a last-resort hack.
19. **What is RSSI useful for?** Rough proximity / link quality. Noisy: smooth with a moving average; never use it as precise distance.
20. **Bonding vs pairing?** Pairing exchanges keys; bonding stores them for reuse. Characteristics with encryption permission trigger pairing on first access (status 5/15 otherwise).

## Architecture
21. **Why wrap callbacks in coroutines?** To remove callback nesting, make timeouts / cancellation / sequencing trivial. Use `callbackFlow` for streams (scan), `CompletableDeferred` for request-response (read/write).
22. **Why is `ScanDevicesUseCase` in the domain layer?** De-duplication / sorting is a business rule independent of Android APIs, testable with a fake repository.
23. **Where should the GATT connection live?** In a singleton / service scoped to the app process, not a ViewModel, so it survives configuration changes and navigation. Consider a foreground service for background operation.
24. **StateFlow vs SharedFlow here?** State (connection, services, MTU) = `StateFlow` (always has a value, conflated). Events/stream (notifications) = `SharedFlow` with buffer (no replay, every item matters).
25. **How would you test BLE code?** Hide Android types behind interfaces, unit-test use cases/ViewModels with fakes, test the GATT wrapper with a thin `GattTransport` fake, and run a small instrumented suite against a real ESP32.

## Scenario questions
26. **Notifications stop after a few minutes in the background.** Doze / OEM battery killers / process death. Use a foreground service (`connectedDevice` type), request ignore-battery-optimisations only if justified, resubscribe after reconnect (CCCD resets on disconnect).
27. **Throughput is 1 KB/s but you need 50 KB/s.** Request MTU 247+, request high connection priority (`requestConnectionPriority`), use write-without-response or notifications, ask the peer for 2M PHY / data length extension, avoid per-packet round-trips.
28. **Two devices at once?** One `BluetoothGatt` per device, each with its own operation queue. Controller limits apply (often ~7 concurrent links).
29. **Firmware update (DFU) over BLE.** Chunked write-no-response with flow control, CRC/validation, resume, high connection priority, keep screen/CPU awake via foreground service.
30. **How do you avoid leaking a scan?** Tie it to a coroutine / Flow (`awaitClose { stopScan }`), add a timeout, stop on `onStop`.
