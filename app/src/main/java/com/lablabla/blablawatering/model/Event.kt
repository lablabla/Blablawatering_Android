package com.lablabla.blablawatering.model

import java.io.Serializable

data class Event(
    val name: String,
    val stations: List<Station>,
    val days: Byte,
    val startHours: Int,
    val startMinutes: Int,
    val endHours: Int,
    val endMinutes: Int,
    var isExpanded: Boolean = false,
    var rotationAngle: Float = 0.0f
) : Serializable
