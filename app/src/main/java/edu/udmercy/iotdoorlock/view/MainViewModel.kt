package edu.udmercy.iotdoorlock.view

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.udmercy.iotdoorlock.bluetooth.BluetoothHandler
import edu.udmercy.iotdoorlock.bluetooth.BluetoothReceiver
import edu.udmercy.iotdoorlock.utils.SingleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.util.*

class MainViewModel: ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    val lockList: MutableLiveData<List<UiLock>> = MutableLiveData(emptyList())
    var scanningFlag: Boolean = false
    val bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
    private var bluetoothHandler: BluetoothHandler? = null
    val connectionStatus:  MutableLiveData<SingleEvent<Boolean>> = MutableLiveData()

    private val bluetoothListener = object : BluetoothReceiver {
        override fun receivedBluetoothMessage(msg: String) {
            Log.i(TAG, "receivedBluetoothMessage: $msg")
        }

        override fun errorSending(e: String) {
            Log.i(TAG, "errorSending: $e")
        }

        override fun errorReading(e: String) {
            Log.i(TAG, "errorReading: $e")
        }

        override fun connected(isConnected: Boolean) {
            connectionStatus.postValue(SingleEvent(isConnected))
            if (!isConnected) {
                bluetoothDevice.postValue(null)
            }
        }
    }

    fun startBluetoothConnection(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothHandler = BluetoothHandler(device)
            bluetoothHandler?.setBluetoothReceiverListener(this@MainViewModel.bluetoothListener)
            bluetoothHandler?.connect()
        }
    }

    fun sendMsg(msg: String) {
        bluetoothHandler?.sendMessage(msg)
    }

    fun disconnectFromDevice() {
        bluetoothHandler?.disconnect()
    }


}