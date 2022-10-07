package com.lablabla.blablawatering.di

import android.content.Context
import com.lablabla.blablawatering.bluetooth.BluetoothManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

//    @Singleton
//    @Provides
//    fun providesBtManager(@ActivityContext context: Context): BluetoothManager {
//        return BluetoothManager(context)
//    }
}