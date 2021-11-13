package edu.udmercy.iotdoorlock.view

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val lockList: MutableLiveData<List<UiLock>> = MutableLiveData(emptyList())
    var scanningFlag: Boolean = false
    val bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
}