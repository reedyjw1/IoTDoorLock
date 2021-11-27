package edu.udmercy.iotdoorlock.model

data class SavedDevice(
    val name: String,
    val ipAddress: String,
    val initialState: Int
)

data class SavedDeviceList(
    val savedDevices: MutableList<SavedDevice>
)