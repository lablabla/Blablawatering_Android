package com.lablabla.blablawatering.bluetooth

import com.lablabla.blablawatering.model.Station

interface BlablaBTCallback {

    fun onStationStateNotification(stations: List<Station>)

    fun onUpdateStations(stations: List<Station>)

    fun onDeviceConnected(name: String, address: String)

    fun onDeviceDisconnected(name: String, address: String)
}