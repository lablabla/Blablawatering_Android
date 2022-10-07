package com.lablabla.blablawatering.bluetooth

import android.bluetooth.BluetoothDevice
import com.lablabla.blablawatering.model.Station

interface BlablaBTCallback {

    fun onUpdateStations(stations: List<Station>)

    fun onDeviceConnected(name: String, address: String)

    fun onDeviceDisconnected(name: String, address: String)
}