package edu.udmercy.iotdoorlock.network

interface IoTDeviceStateInterface {
    fun onDeviceStateUpdated(locked: Int, ipAddress: String)
}