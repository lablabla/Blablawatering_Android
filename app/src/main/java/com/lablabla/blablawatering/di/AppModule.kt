package com.lablabla.blablawatering.di

import android.content.Context
import com.lablabla.blablawatering.bluetooth.BluetoothManager
import com.lablabla.blablawatering.data.remote.RemoteAPI
import com.lablabla.blablawatering.data.remote.RemoteApiIml
import com.lablabla.blablawatering.model.Station
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesBtManager(@ApplicationContext context: Context): BluetoothManager {
        return BluetoothManager(context)
    }

    @Singleton
    @Provides
    fun providesRemoteApi(bt: BluetoothManager): RemoteAPI {
        return RemoteApiIml(bt)
    }

    @Singleton
    @Provides
    fun provideDebugStations() : List<Station> {
        return listOf(
            Station(0, 0, false, "Test Station 1"),
            Station(1, 1, true, "Test Station 2"),
        )
    }
}