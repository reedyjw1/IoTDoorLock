package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothDevice

interface CommunicationInterface {
    // Interface for relaying information about the clicked Bluetooth Device between classes.
    fun updateSelectedBluetoothDevice(device: BluetoothDevice)
}