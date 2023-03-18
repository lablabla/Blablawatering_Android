package com.lablabla.blablawatering.bluetooth.messages

data class SetStationStateMessage(
    val station_id: Int,
    val is_on: Boolean
)
