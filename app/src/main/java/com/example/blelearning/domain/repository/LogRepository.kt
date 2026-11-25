package com.example.blelearning.domain.repository

import com.example.blelearning.domain.model.LogEntry
import com.example.blelearning.domain.model.LogLevel
import kotlinx.coroutines.flow.StateFlow

interface LogRepository {
    val logs: StateFlow<List<LogEntry>>
    fun log(level: LogLevel, tag: String, message: String)
    fun clear()
}

fun LogRepository.d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
fun LogRepository.i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
fun LogRepository.w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
fun LogRepository.e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)
