package com.example.blelearning.domain.usecase

import com.example.blelearning.domain.repository.LogRepository
import javax.inject.Inject

class ObserveLogsUseCase @Inject constructor(private val repo: LogRepository) {
    operator fun invoke() = repo.logs
}

class ClearLogsUseCase @Inject constructor(private val repo: LogRepository) {
    operator fun invoke() = repo.clear()
}
