package edu.udmercy.iotdoorlock.model

// Data classes to be used to save information to SharedPreferences
data class SavedDevice(
    val name: String,
    val ipAddress: String,
    val initialState: Int
)

data class SavedDeviceList(
    val savedDevices: MutableList<SavedDevice>
)