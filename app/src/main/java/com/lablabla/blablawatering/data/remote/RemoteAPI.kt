package com.lablabla.blablawatering.data.remote

import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station

interface RemoteAPI {

    fun getStations() : List<Station>
    fun setStations(stations : List<Station>)

    fun getEvents() : List<Event>
    fun setEvents(events : List<Event>)
}