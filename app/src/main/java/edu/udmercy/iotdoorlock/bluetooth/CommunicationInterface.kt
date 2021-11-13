package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothDevice

interface CommunicationInterface {
    fun updateSelectedBluetoothDevice(device: BluetoothDevice)
}