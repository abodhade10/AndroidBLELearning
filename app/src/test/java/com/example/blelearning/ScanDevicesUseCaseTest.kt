package com.example.blelearning

import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.repository.BleScanRepository
import com.example.blelearning.domain.usecase.ScanDevicesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanDevicesUseCaseTest {
    private fun device(addr: String, rssi: Int) =
        BleDevice(name = addr, address = addr, rssi = rssi, isConnectable = true)

    @Test fun deduplicatesByAddress_keepsLatestRssi_sortsStrongestFirst() = runTest {
        val fake = object : BleScanRepository {
            override fun scan(): Flow<BleDevice> = flowOf(
                device("A", -80), device("B", -60), device("A", -40),
            )
        }
        val result = ScanDevicesUseCase(fake)().last()
        assertEquals(listOf("A", "B"), result.map { it.address })
        assertEquals(-40, result.first().rssi)
    }
}
