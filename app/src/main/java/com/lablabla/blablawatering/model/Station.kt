package com.lablabla.blablawatering.model

import java.io.Serializable

data class Station(
    val gpio_pin: Int,
    val id: Int,
    val is_on: Boolean,
    val name: String
) : Serializable