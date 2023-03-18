package com.lablabla.blablawatering.data.repository

import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station

interface RepositoryAPI {

    // Actions
    fun requestStations()
    fun setStations(stations : List<Station>)

    fun requestEvents()
    fun setEvents(events : List<Event>)
}
