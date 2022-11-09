package com.lablabla.blablawatering.data.remote

import com.lablabla.blablawatering.bluetooth.BluetoothManager
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station

class RemoteApiIml(
    private val bt: BluetoothManager
) : RemoteAPI {
    override fun getStations(): List<Station> {
        TODO("Not yet implemented")
    }

    override fun setStations(stations: List<Station>) {
        TODO("Not yet implemented")
    }

    override fun getEvents(): List<Event> {
        TODO("Not yet implemented")
    }

    override fun setEvents(events: List<Event>) {
        TODO("Not yet implemented")
    }

}