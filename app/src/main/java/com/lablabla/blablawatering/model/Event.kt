package com.lablabla.blablawatering.model

import java.io.Serializable

data class Event(
    val id: Int,
    val name: String,
    val station_ids: List<Int>,
    val cron_expr: String,
    val duration: Int,
    var isExpanded: Boolean = false,
    var rotationAngle: Float = 0.0f
) : Serializable
