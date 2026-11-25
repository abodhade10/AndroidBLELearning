package com.example.blelearning.data.repository

import android.util.Log
import com.example.blelearning.core.common.BleConfig
import com.example.blelearning.domain.model.LogEntry
import com.example.blelearning.domain.model.LogLevel
import com.example.blelearning.domain.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the last N log lines for the in-app Logs screen and mirrors them to Logcat (tag "BLE"). */
@Singleton
class InMemoryLogRepository @Inject constructor() : LogRepository {
    private val ids = AtomicLong()
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    override fun log(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d("BLE/$tag", message)
            LogLevel.INFO -> Log.i("BLE/$tag", message)
            LogLevel.WARN -> Log.w("BLE/$tag", message)
            LogLevel.ERROR -> Log.e("BLE/$tag", message)
        }
        val entry = LogEntry(ids.incrementAndGet(), System.currentTimeMillis(), level, tag, message)
        _logs.update { (it + entry).takeLast(BleConfig.MAX_LOG_ENTRIES) }
    }

    override fun clear() { _logs.value = emptyList() }
}
