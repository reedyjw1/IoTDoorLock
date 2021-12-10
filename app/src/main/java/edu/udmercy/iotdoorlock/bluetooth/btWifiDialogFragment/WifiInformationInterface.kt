package edu.udmercy.iotdoorlock.bluetooth.btWifiDialogFragment

interface WifiInformationInterface {
    // Interface for passing the wifi name and password between fragments
    fun wifiInformation(ssid: String, password: String)
}