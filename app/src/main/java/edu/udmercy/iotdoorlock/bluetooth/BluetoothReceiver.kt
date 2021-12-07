package edu.udmercy.iotdoorlock.bluetooth

interface BluetoothReceiver {
    fun receivedBluetoothMessage(msg: String)
    fun connected(isConnected: Boolean, ssid: String, password: String)
    fun errorSending(e: String)
    fun errorReading(e: String)
}