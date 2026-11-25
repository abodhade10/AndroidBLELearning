package com.example.blelearning.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelearning.domain.model.LogEntry
import com.example.blelearning.domain.usecase.ClearLogsUseCase
import com.example.blelearning.domain.usecase.ObserveLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    observeLogs: ObserveLogsUseCase,
    private val clearLogs: ClearLogsUseCase,
) : ViewModel() {
    /** Newest first. */
    val logs: StateFlow<List<LogEntry>> = observeLogs()
        .map { it.asReversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() = clearLogs()
}
