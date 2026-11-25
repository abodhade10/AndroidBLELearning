# Flow 01 – From the Scan button to scan results on screen

```
ScannerScreen (Compose)           onScanClicked()
   │  permission missing? ─────▶  RequestMultiplePermissions launcher ─▶ result ─▶ vm.startScan()
   ▼
ScannerViewModel.startScan()      viewModelScope.launch { withTimeout(15 s) { scanDevices().collect { ... } } }
   ▼
ScanDevicesUseCase.invoke()       repository.scan().runningFold(map by address).map(sort by RSSI).conflate()
   ▼
BleScanRepositoryImpl.scan()      delegates
   ▼
BleScanner.scan()  (callbackFlow) permission? adapter? enabled?  ──▶ close(BleException...) on failure
   ▼
BluetoothLeScanner.startScan(filters=null, settings, ScanCallback)
   ▼  ... radio hears advertising packets ...
ScanCallback.onScanResult()       trySend(result.toBleDevice())          (Binder thread)
   ▼
Flow operators (use case)         Map<address, BleDevice> → sorted List
   ▼
ViewModel collect                 _uiState.update { devices = list }
   ▼
Compose recomposition             LazyColumn(items(devices, key = address))
```

## Order of events
1. Click → permission check → `startScan()`.
2. `collect` begins → `callbackFlow` block runs → `startScan` on the controller.
3. `onScanResult` is invoked repeatedly (same device many times).
4. Stop: user taps Stop, timeout fires, or the ViewModel is cleared → coroutine cancelled → `awaitClose` runs → `stopScan`.

## Common bugs
| Symptom | Likely cause |
|---|---|
| No results on Android ≤ 11 | Location permission granted but **location services** switched off |
| `SecurityException` | Missing `BLUETOOTH_SCAN` at runtime (manifest alone is not enough) |
| `onScanFailed(2)` APPLICATION_REGISTRATION_FAILED / `6` SCANNING_TOO_FREQUENTLY | More than 5 scan starts in 30 s |
| UI janks while scanning | Emitting a new list per packet without `conflate()` |
| Scan keeps running after leaving screen | Scan not tied to a cancellable coroutine |

## Interview prompts
- Why `callbackFlow` and not `flow {}`?
- Why `awaitClose`?
- Which thread calls `onScanResult`, and is `trySend` safe there?
- What does `SCAN_MODE_LOW_LATENCY` cost you?

## Exercises
1. Add a `ScanFilter.Builder().setServiceUuid(ParcelUuid(BleUuids.LEARNING_SERVICE))` and observe results.
2. Log the time between two results of the same device. What is the advertising interval?
3. Start/stop scanning 6 times in 30 s and read the error in the Logs screen.
