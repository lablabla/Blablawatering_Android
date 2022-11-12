package com.lablabla.blablawatering.model

data class SetStationStateMessage(
    val station_id: Int,
    val is_on: Boolean
)
