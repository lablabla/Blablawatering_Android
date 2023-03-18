package com.lablabla.blablawatering.data.remote

import com.lablabla.blablawatering.bluetooth.BluetoothManager
import com.lablabla.blablawatering.data.repository.RepositoryAPI
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station

class RemoteApiBTImpl(
    private val bt: BluetoothManager
) : RepositoryAPI {
    override fun requestStations() {
        TODO("Not yet implemented")
    }

    override fun setStations(stations: List<Station>) {
        TODO("Not yet implemented")
    }

    override fun requestEvents() {
        TODO("Not yet implemented")
    }

    override fun setEvents(events: List<Event>) {
        TODO("Not yet implemented")
    }
}