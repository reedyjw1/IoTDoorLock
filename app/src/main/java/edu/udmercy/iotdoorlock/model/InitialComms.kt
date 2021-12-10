package edu.udmercy.iotdoorlock.model

data class InitialComms(
    val username: String,
    val password: String,
    val ssid: String,
    val ssidPassword: String
)
