package edu.udmercy.iotdoorlock.view

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.udmercy.iotdoorlock.R
import edu.udmercy.iotdoorlock.bluetooth.BluetoothHandler
import edu.udmercy.iotdoorlock.bluetooth.BluetoothReceiver
import edu.udmercy.iotdoorlock.model.SavedDevice
import edu.udmercy.iotdoorlock.model.SavedDeviceList
import edu.udmercy.iotdoorlock.network.NetworkManager
import edu.udmercy.iotdoorlock.utils.SingleEvent
import edu.udmercy.iotdoorlock.utils.fromJson
import edu.udmercy.iotdoorlock.utils.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.util.*

class MainViewModel(app: Application): AndroidViewModel(app) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    val lockList: MutableLiveData<List<UiLock>> = MutableLiveData(emptyList())
    var scanningFlag: Boolean = false
    val bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
    private var bluetoothHandler: BluetoothHandler? = null
    private var networkManager: NetworkManager? = null
    val connectionStatus: MutableLiveData<SingleEvent<Boolean>> = MutableLiveData()

    private val bluetoothListener = object : BluetoothReceiver {
        override fun receivedBluetoothMessage(msg: String) {
            Log.i(TAG, "receivedBluetoothMessage: $msg")
            saveToSharedPrefs(msg.fromJson<SavedDevice>())
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

    fun startWifiTcpConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            networkManager = NetworkManager()
            networkManager?.run()
        }
    }

    fun sendNetworkRequest(msg: String) {
        networkManager?.sendNetworkMessage(msg)
    }

    fun sendBluetoothMsg(msg: String) {
        bluetoothHandler?.sendMessage(msg)
    }

    fun disconnectFromDevice() {
        bluetoothHandler?.disconnect()
        networkManager?.onDisconnect()
    }

    private fun saveToSharedPrefs(lock: SavedDevice?) {
        lock ?: return
        val context = getApplication<Application>().applicationContext
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_key),
            Context.MODE_PRIVATE
        ) ?: return
        val devices =
            sharedPref.getString(context.getString(R.string.saved_devices_sp), "") ?: return
        if (devices != "") {
            val savedDevices = devices.fromJson<SavedDeviceList>() ?: return
            savedDevices.savedDevices.forEach {
                if(it.ipAddress == lock.ipAddress) {
                    return
                }
            }
            savedDevices.savedDevices.add(lock)
            with(sharedPref.edit()) {
                putString(context.getString(R.string.saved_devices_sp), savedDevices.toJson())
                apply()
            }
            getMostRecentLockList(savedDevices)
        } else {
            // Create new shared prefs list
            val deviceList = mutableListOf<SavedDevice>()
            deviceList.add(lock)
            val newDeviceList = SavedDeviceList(deviceList)
            with(sharedPref.edit()) {
                putString(context.getString(R.string.saved_devices_sp), newDeviceList.toJson())
                apply()
            }
            getMostRecentLockList(newDeviceList)
        }

    }

    fun getMostRecentLockList(ogList: SavedDeviceList?) {
        if (ogList != null) {
            lockList.postValue(ogList?.savedDevices?.map {
                UiLock(UUID.randomUUID().toString(), it.name, it.ipAddress, it.initialState)
            })
        }

        val context = getApplication<Application>().applicationContext
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_key),
            Context.MODE_PRIVATE
        ) ?: return
        val devices =
            sharedPref.getString(context.getString(R.string.saved_devices_sp), "") ?: return
        if (devices != "") {
            val savedDevicesList = devices.fromJson<SavedDeviceList>()
            lockList.postValue(savedDevicesList?.savedDevices?.map {
                UiLock(UUID.randomUUID().toString(), it.name, it.ipAddress, it.initialState)
            })
        }
    }
}