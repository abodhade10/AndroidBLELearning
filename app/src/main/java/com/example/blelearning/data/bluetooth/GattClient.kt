package com.example.blelearning.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import com.example.blelearning.core.bluetooth.BleUuids
import com.example.blelearning.core.bluetooth.GattStatus
import com.example.blelearning.core.common.BleConfig
import com.example.blelearning.core.common.ValueFormatter.toHex
import com.example.blelearning.data.mapper.toDomain
import com.example.blelearning.domain.model.*
import com.example.blelearning.domain.repository.LogRepository
import com.example.blelearning.domain.repository.d
import com.example.blelearning.domain.repository.e
import com.example.blelearning.domain.repository.i
import com.example.blelearning.domain.repository.w
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The heart of the project: a coroutine-friendly wrapper around ONE BluetoothGatt connection.
 *
 * Why a serial operation queue?
 *   Android allows only ONE outstanding GATT operation (read / write / descriptor write / MTU...).
 *   Starting a second one before the first callback arrives silently fails. [execute] enforces this with a
 *   Mutex, stores a CompletableDeferred, starts the operation, and the matching BluetoothGattCallback method
 *   completes the deferred.
 */
@SuppressLint("MissingPermission") // The UI requests permissions before any call reaches this class.
@Singleton
class GattClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val log: LogRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _device = MutableStateFlow<BleDevice?>(null)
    val device: StateFlow<BleDevice?> = _device.asStateFlow()

    private val _services = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    val services: StateFlow<List<GattServiceInfo>> = _services.asStateFlow()

    private val _mtu = MutableStateFlow(BleConfig.DEFAULT_MTU)
    val mtu: StateFlow<Int> = _mtu.asStateFlow()

    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()

    private val _notifications = MutableSharedFlow<CharacteristicValue>(extraBufferCapacity = 64)
    val notifications: SharedFlow<CharacteristicValue> = _notifications.asSharedFlow()

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var userRequestedDisconnect = false
    @Volatile private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    // ---------------------------------------------------------------- connection lifecycle

    fun connect(target: BleDevice) {
        if (_state.value.isActive) {
            log.w(TAG, "connect() ignored, already ${_state.value}")
            return
        }
        userRequestedDisconnect = false
        reconnectAttempts = 0
        _device.value = target
        openGatt(target.address)
    }

    private fun openGatt(address: String) {
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            _state.value = ConnectionState.Failed(BleException.BluetoothDisabled)
            return
        }
        val remote = runCatching { adapter.getRemoteDevice(address) }.getOrElse {
            _state.value = ConnectionState.Failed(BleException.ConnectionFailed(-1, "invalid address $address"))
            return
        }
        _state.value =
            if (reconnectAttempts == 0) ConnectionState.Connecting
            else ConnectionState.Reconnecting(reconnectAttempts, BleConfig.MAX_RECONNECT_ATTEMPTS)
        log.i(TAG, "connectGatt($address, autoConnect=false, TRANSPORT_LE)")
        // autoConnect=false -> fast, direct connection attempt (times out ~30 s).
        // autoConnect=true  -> background "connect whenever it shows up", slower to start.
        gatt = remote.connectGatt(context, false, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        log.i(TAG, "disconnect() requested by user")
        userRequestedDisconnect = true
        reconnectJob?.cancel()
        val g = gatt
        if (g == null) {
            _state.value = ConnectionState.Disconnected
            return
        }
        _state.value = ConnectionState.Disconnecting
        g.disconnect() // result arrives in onConnectionStateChange, where close() is called
        // Safety net: some stacks never deliver the callback if the link is already dead.
        scope.launch {
            delay(3_000)
            if (_state.value is ConnectionState.Disconnecting) {
                log.w(TAG, "No disconnect callback after 3 s, forcing close()")
                cleanUp(g)
                _state.value = ConnectionState.Disconnected
            }
        }
    }

    /** ALWAYS close(): disconnect() alone leaks the client interface (limit ~7-32 per process). */
    private fun cleanUp(g: BluetoothGatt) {
        failPending(BleException.NotConnected)
        runCatching { g.close() }
        if (gatt === g) gatt = null
        _services.value = emptyList()
        _mtu.value = BleConfig.DEFAULT_MTU
        _rssi.value = null
    }

    private fun handleDisconnected(g: BluetoothGatt, status: Int) {
        cleanUp(g)
        val address = _device.value?.address
        when {
            userRequestedDisconnect -> _state.value = ConnectionState.Disconnected
            address != null && reconnectAttempts < BleConfig.MAX_RECONNECT_ATTEMPTS -> {
                reconnectAttempts++
                val delayMs = BleConfig.RECONNECT_BASE_DELAY_MS shl (reconnectAttempts - 1) // 1s, 2s, 4s
                _state.value = ConnectionState.Reconnecting(reconnectAttempts, BleConfig.MAX_RECONNECT_ATTEMPTS)
                log.w(TAG, "Unexpected disconnect, status=${GattStatus.describe(status)}. Retry #$reconnectAttempts in ${delayMs}ms")
                reconnectJob = scope.launch {
                    delay(delayMs)
                    if (!userRequestedDisconnect) openGatt(address)
                }
            }
            else -> {
                log.e(TAG, "Giving up after $reconnectAttempts retries, status=${GattStatus.describe(status)}")
                _state.value = ConnectionState.Failed(
                    BleException.ConnectionFailed(status, GattStatus.describe(status))
                )
            }
        }
    }

    // ---------------------------------------------------------------- serial operation queue

    private class OpResult(val status: Int, val value: ByteArray? = null, val intValue: Int? = null)

    private val opMutex = Mutex()
    @Volatile private var pending: CompletableDeferred<OpResult>? = null

    private fun completePending(result: OpResult) { pending?.complete(result) }
    private fun failPending(e: BleException) { pending?.completeExceptionally(e) }

    private suspend fun execute(name: String, start: (BluetoothGatt) -> Boolean): Result<OpResult> =
        opMutex.withLock {
            val g = gatt
            if (g == null || _state.value !is ConnectionState.Ready) {
                return@withLock Result.failure(BleException.NotConnected)
            }
            val deferred = CompletableDeferred<OpResult>()
            pending = deferred
            try {
                log.d(TAG, "-> $name")
                if (!start(g)) {
                    return@withLock Result.failure(BleException.OperationFailed("$name could not be started"))
                }
                val r = withTimeout(BleConfig.OPERATION_TIMEOUT_MS) { deferred.await() }
                if (r.status == BluetoothGatt.GATT_SUCCESS) {
                    Result.success(r)
                } else {
                    log.e(TAG, "<- $name FAILED ${GattStatus.describe(r.status)}")
                    Result.failure(BleException.OperationFailed("$name failed: ${GattStatus.describe(r.status)}", r.status))
                }
            } catch (e: TimeoutCancellationException) {
                log.e(TAG, "<- $name TIMEOUT")
                Result.failure(BleException.Timeout(name))
            } catch (e: BleException) {
                Result.failure(e)
            } finally {
                pending = null
            }
        }

    // ---------------------------------------------------------------- public GATT operations

    private fun find(service: UUID, characteristic: UUID): BluetoothGattCharacteristic? =
        gatt?.getService(service)?.getCharacteristic(characteristic)

    suspend fun read(service: UUID, characteristic: UUID): Result<ByteArray> {
        val ch = find(service, characteristic)
            ?: return Result.failure(BleException.CharacteristicNotFound(characteristic.toString()))
        return execute("read $characteristic") { it.readCharacteristic(ch) }
            .map { it.value ?: ByteArray(0) }
    }

    suspend fun write(service: UUID, characteristic: UUID, value: ByteArray, withResponse: Boolean): Result<Unit> {
        val ch = find(service, characteristic)
            ?: return Result.failure(BleException.CharacteristicNotFound(characteristic.toString()))
        val type = if (withResponse) BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return execute("write $characteristic [${value.toHex()}]") { g ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(ch, value, type) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    ch.writeType = type
                    ch.value = value
                    g.writeCharacteristic(ch)
                }
            }
        }.map { }
    }

    /**
     * Enabling notifications is TWO steps:
     *  1. setCharacteristicNotification()  -> tells the LOCAL stack to forward notifications to us
     *  2. write the CCCD descriptor (0x2902) -> tells the REMOTE peripheral to start sending them
     * Skipping step 2 is the #1 "why don't I get notifications" bug.
     */
    suspend fun setNotification(service: UUID, characteristic: UUID, enable: Boolean): Result<Unit> {
        val ch = find(service, characteristic)
            ?: return Result.failure(BleException.CharacteristicNotFound(characteristic.toString()))
        val g = gatt ?: return Result.failure(BleException.NotConnected)
        val props = ch.properties
        val supportsNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
        val supportsIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        if (!supportsNotify && !supportsIndicate) {
            return Result.failure(BleException.OperationFailed("Characteristic does not support notify/indicate"))
        }
        val cccd = ch.getDescriptor(BleUuids.CCCD)
            ?: return Result.failure(BleException.OperationFailed("CCCD (0x2902) descriptor not found"))
        if (!g.setCharacteristicNotification(ch, enable)) {
            return Result.failure(BleException.OperationFailed("setCharacteristicNotification failed"))
        }
        val payload = when {
            !enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            supportsNotify -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        return execute("write CCCD ${if (enable) "enable" else "disable"} $characteristic") { gg ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gg.writeDescriptor(cccd, payload) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    cccd.value = payload
                    gg.writeDescriptor(cccd)
                }
            }
        }.map { }
    }

    suspend fun requestMtu(mtu: Int): Result<Int> =
        execute("requestMtu($mtu)") { it.requestMtu(mtu) }.map { it.intValue ?: _mtu.value }

    suspend fun readRssi(): Result<Int> =
        execute("readRemoteRssi") { it.readRemoteRssi() }.map { it.intValue ?: 0 }

    // ---------------------------------------------------------------- the callback (binder thread!)

    /**
     * Callbacks run on a Binder thread, never the main thread. We only touch thread-safe objects here
     * (StateFlow, SharedFlow, CompletableDeferred), so no extra synchronisation is needed.
     */
    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            log.d(TAG, "onConnectionStateChange status=${GattStatus.describe(status)} newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                reconnectAttempts = 0
                _state.value = ConnectionState.Connected
                _state.value = ConnectionState.DiscoveringServices
                if (!g.discoverServices()) {
                    log.e(TAG, "discoverServices() returned false")
                    g.disconnect()
                }
            } else {
                // STATE_DISCONNECTED, or any error status (e.g. 133) even with newState CONNECTED.
                handleDisconnected(g, status)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log.e(TAG, "onServicesDiscovered failed ${GattStatus.describe(status)}")
                g.disconnect()
                return
            }
            val mapped = g.services.map { it.toDomain() }
            log.i(TAG, "onServicesDiscovered: ${mapped.size} services")
            _services.value = mapped
            _state.value = ConnectionState.Ready
        }

        // API 33+ variant (value delivered as a parameter, no shared mutable characteristic.value)
        override fun onCharacteristicRead(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            log.d(TAG, "onCharacteristicRead ${ch.uuid} status=$status value=${value.toHex()}")
            completePending(OpResult(status, value))
        }

        // API 26-32 variant
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) {
            @Suppress("DEPRECATION")
            val value = ch.value ?: ByteArray(0)
            log.d(TAG, "onCharacteristicRead(legacy) ${ch.uuid} status=$status value=${value.toHex()}")
            completePending(OpResult(status, value.copyOf()))
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) {
            log.d(TAG, "onCharacteristicWrite ${ch.uuid} status=$status")
            completePending(OpResult(status))
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            log.d(TAG, "onDescriptorWrite ${d.uuid} status=$status")
            completePending(OpResult(status))
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray) {
            emitNotification(ch, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            emitNotification(ch, ch.value?.copyOf() ?: ByteArray(0))
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            log.i(TAG, "onMtuChanged mtu=$mtu status=$status (max payload = ${mtu - 3} bytes)")
            if (status == BluetoothGatt.GATT_SUCCESS) _mtu.value = mtu
            completePending(OpResult(status, intValue = mtu))
        }

        override fun onReadRemoteRssi(g: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) _rssi.value = rssi
            completePending(OpResult(status, intValue = rssi))
        }
    }

    private fun emitNotification(ch: BluetoothGattCharacteristic, value: ByteArray) {
        log.d(TAG, "onCharacteristicChanged ${ch.uuid} value=${value.toHex()}")
        _notifications.tryEmit(CharacteristicValue(ch.service.uuid, ch.uuid, value))
    }

    private companion object { const val TAG = "Gatt" }
}
