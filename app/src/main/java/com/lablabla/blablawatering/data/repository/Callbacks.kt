package com.lablabla.blablawatering.data.repository

import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station

interface Callbacks {
    fun onDeviceConnected(name: String, address: String)
    fun onDeviceDisconnected(name: String, address: String)
    fun onStationStateNotification(stations: List<Station>)
    fun onUpdateStations(stations: List<Station>)
    fun onUpdateEvents(events: List<Event>)
}