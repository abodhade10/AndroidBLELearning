package com.example.blelearning.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.blelearning.data.repository.BleConnectionRepositoryImpl
import com.example.blelearning.data.repository.BleScanRepositoryImpl
import com.example.blelearning.data.repository.InMemoryLogRepository
import com.example.blelearning.domain.repository.BleConnectionRepository
import com.example.blelearning.domain.repository.BleScanRepository
import com.example.blelearning.domain.repository.LogRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {
    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager =
        context.getSystemService(BluetoothManager::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindScanRepository(impl: BleScanRepositoryImpl): BleScanRepository
    @Binds abstract fun bindConnectionRepository(impl: BleConnectionRepositoryImpl): BleConnectionRepository
    @Binds abstract fun bindLogRepository(impl: InMemoryLogRepository): LogRepository
}
