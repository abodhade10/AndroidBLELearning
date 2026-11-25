package com.example.blelearning.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelearning.core.bluetooth.BleUuids
import com.example.blelearning.core.common.ValueFormatter
import com.example.blelearning.domain.usecase.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class NotificationItem(val id: Long, val time: String, val source: String, val text: String)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    observeNotifications: ObserveNotificationsUseCase,
) : ViewModel() {

    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var nextId = 0L

    init {
        viewModelScope.launch {
            observeNotifications().collect { n ->
                val item = NotificationItem(
                    id = nextId++,
                    time = timeFormat.format(Date(n.timestampMillis)),
                    source = BleUuids.nameOf(n.characteristicUuid) ?: BleUuids.shortOrFull(n.characteristicUuid),
                    text = ValueFormatter.describe(n.characteristicUuid, n.value),
                )
                // Newest first, bounded so an unbounded stream can never exhaust memory.
                _items.update { (listOf(item) + it).take(200) }
            }
        }
    }

    fun clear() { _items.value = emptyList() }
}
