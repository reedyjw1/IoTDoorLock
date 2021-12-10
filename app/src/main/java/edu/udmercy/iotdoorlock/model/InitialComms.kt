package edu.udmercy.iotdoorlock.model

// Data class for passing the information from the phone to the ESP32 (will be used for easily converting information to JSON)
data class InitialComms(
    val username: String,
    val password: String,
    val ssid: String,
    val ssidPassword: String
)
