package com.example.blelearning.presentation.gatt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelearning.core.common.ValueFormatter
import com.example.blelearning.domain.model.CharacteristicValue
import com.example.blelearning.domain.model.GattCharacteristicInfo
import com.example.blelearning.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class GattUiState(
    val lastValue: CharacteristicValue? = null,
    val isNotifying: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class GattViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeServices: ObserveServicesUseCase,
    observeNotifications: ObserveNotificationsUseCase,
    private val readUseCase: ReadCharacteristicUseCase,
    private val writeUseCase: WriteCharacteristicUseCase,
    private val notifyUseCase: SetNotificationUseCase,
) : ViewModel() {

    // Navigation arguments arrive in SavedStateHandle under the names declared in the route.
    private val serviceUuid = UUID.fromString(checkNotNull(savedStateHandle.get<String>("service")))
    private val charUuid = UUID.fromString(checkNotNull(savedStateHandle.get<String>("characteristic")))

    val characteristic: StateFlow<GattCharacteristicInfo?> = observeServices()
        .map { services ->
            services.firstOrNull { it.uuid == serviceUuid }
                ?.characteristics?.firstOrNull { it.uuid == charUuid }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(GattUiState())
    val ui: StateFlow<GattUiState> = _ui.asStateFlow()

    init {
        // Notifications for this characteristic update the displayed value live.
        viewModelScope.launch {
            observeNotifications().filter { it.characteristicUuid == charUuid }
                .collect { v -> _ui.update { it.copy(lastValue = v) } }
        }
    }

    fun read() = launchOp {
        readUseCase(serviceUuid, charUuid)
            .onSuccess { bytes ->
                _ui.update { it.copy(lastValue = CharacteristicValue(serviceUuid, charUuid, bytes), message = "Read OK") }
            }
            .onFailure { e -> _ui.update { it.copy(message = e.message) } }
    }

    fun write(hexInput: String, withResponse: Boolean) = launchOp {
        val bytes = ValueFormatter.parseHex(hexInput)
        if (bytes == null) {
            _ui.update { it.copy(message = "Enter valid hex, e.g. 48 65 6C 6C 6F") }
            return@launchOp
        }
        writeUseCase(serviceUuid, charUuid, bytes, withResponse)
            .onSuccess { _ui.update { it.copy(message = "Wrote ${bytes.size} byte(s)") } }
            .onFailure { e -> _ui.update { it.copy(message = e.message) } }
    }

    fun toggleNotifications() = launchOp {
        val enable = !_ui.value.isNotifying
        notifyUseCase(serviceUuid, charUuid, enable)
            .onSuccess { _ui.update { it.copy(isNotifying = enable, message = if (enable) "Notifications ON" else "Notifications OFF") } }
            .onFailure { e -> _ui.update { it.copy(message = e.message) } }
    }

    private fun launchOp(block: suspend () -> Unit) {
        if (_ui.value.busy) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true) }
            try { block() } finally { _ui.update { it.copy(busy = false) } }
        }
    }
}
