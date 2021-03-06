package edu.udmercy.iotdoorlock.bluetooth

interface BluetoothReceiver {
    // Interface for handling communication between BluetoothHandler and Class that started it.
    fun receivedBluetoothMessage(msg: String)
    fun connected(isConnected: Boolean, ssid: String, password: String)
    fun errorSending(e: String)
    fun errorReading(e: String)
}