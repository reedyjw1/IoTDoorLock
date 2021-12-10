package edu.udmercy.iotdoorlock.network

interface IoTDeviceStateInterface {
    // Interface to notify class if the state of the lock has been changed.
    fun onDeviceStateUpdated(locked: Int, ipAddress: String)
}