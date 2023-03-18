package com.lablabla.blablawatering.bluetooth.messages

data class TimeMessage(
    val tv_sec: Long,
    val tv_usec: Long,

    val tz_str: String
)
