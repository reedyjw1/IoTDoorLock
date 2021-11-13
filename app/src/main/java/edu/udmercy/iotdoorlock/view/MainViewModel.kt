package edu.udmercy.iotdoorlock.view

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun startBluetoothConnection(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothHandler = BluetoothHandler(device).apply { connect() }
        }
    }

    fun sendMsg(msg: String) {
        bluetoothHandler?.sendMessage(msg)
    }

    fun disconnectFromDevice() {
        bluetoothHandler?.disconnect()
    }

    private inner class BluetoothHandler(private val device: BluetoothDevice) {

        var bluetoothSocket: BluetoothSocket? = null
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        fun sendMessage(msg: String) {
            val formatted = msg + "\n"
            bluetoothSocket?.outputStream?.write(formatted.toByteArray())
        }

        fun connect() {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val connectionDevice = adapter.getRemoteDevice(device.address)

            if (bluetoothSocket == null) {
                bluetoothSocket = connectionDevice.createInsecureRfcommSocketToServiceRecord(uuid)
                try {
                    bluetoothSocket!!.connect()
                } catch (e: Exception) {
                    Log.i(TAG, "connect: ${e.localizedMessage}")
                }
            }
        }

        fun disconnect() {
            bluetoothSocket?.close()
            bluetoothSocket = null
        }
    }
}