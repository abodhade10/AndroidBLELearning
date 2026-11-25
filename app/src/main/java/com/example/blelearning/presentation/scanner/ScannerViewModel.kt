package com.example.blelearning.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelearning.core.common.BleConfig
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.usecase.ConnectDeviceUseCase
import com.example.blelearning.domain.usecase.ScanDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanDevices: ScanDevicesUseCase,
    private val connectDevice: ConnectDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _uiState.value = ScannerUiState(isScanning = true)
        scanJob = viewModelScope.launch {
            try {
                // Cancelling this coroutine cancels the flow, which runs stopScan() in awaitClose.
                withTimeout(BleConfig.SCAN_TIMEOUT_MS) {
                    scanDevices().collect { list -> _uiState.update { it.copy(devices = list) } }
                }
            } catch (e: TimeoutCancellationException) {
                // Normal end of a timed scan.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { it.copy(error = e.message ?: "Scan failed") }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun stopScan() { scanJob?.cancel() }

    fun onPermissionDenied() {
        _uiState.update { it.copy(error = "Bluetooth permission is required to scan.") }
    }

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    fun connect(device: BleDevice) {
        stopScan()
        connectDevice(device)
    }
}
