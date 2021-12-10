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
import edu.udmercy.iotdoorlock.MainActivity
import edu.udmercy.iotdoorlock.R
import edu.udmercy.iotdoorlock.bluetooth.BluetoothHandler
import edu.udmercy.iotdoorlock.bluetooth.BluetoothReceiver
import edu.udmercy.iotdoorlock.model.InitialComms
import edu.udmercy.iotdoorlock.model.SavedDevice
import edu.udmercy.iotdoorlock.model.SavedDeviceList
import edu.udmercy.iotdoorlock.network.IoTDeviceStateInterface
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

// ViewModel is responsible for handling data before it is presented to UI
class MainViewModel(app: Application): AndroidViewModel(app) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    // Creates LiveData objects (this data can be observed, which means a function will be called everytime it changes,
    // this allows any UI fragment to listen for changes and make updates asynchronously
    val lockList: MutableLiveData<List<UiLock>> = MutableLiveData(emptyList())
    var scanningFlag: Boolean = false
    // Bluetooth Device that will be clicked
    val bluetoothDevice: MutableLiveData<BluetoothDevice> = MutableLiveData()
    // Declares and initialized the BluetoothHandler to Null
    private var bluetoothHandler: BluetoothHandler? = null
    // If the BluetoothDevice is Connected or disconnected, this will be used for automatic notifications to the user
    val connectionStatus: MutableLiveData<SingleEvent<Boolean>> = MutableLiveData()
    val isSsidAndPasswordGood: MutableLiveData<SingleEvent<Boolean?>> = MutableLiveData()
    // Pairs an IP address with a subsequent NetworkManager
    private val hashDeviceList: MutableMap<String, NetworkManager> = mutableMapOf()

    // Implements the BluetoothListener Interface, Used to update LiveData when Bluetooth information changes
    // When live data changes, then so will the UI
    private val bluetoothListener = object : BluetoothReceiver {
        override fun receivedBluetoothMessage(msg: String) {
            Log.i(TAG, "receivedBluetoothMessage: $msg")
            if (msg == "error") {
                // If an error has been recieved, send UI notification to the user
                isSsidAndPasswordGood.postValue(SingleEvent(null))
                isSsidAndPasswordGood.postValue(SingleEvent(false))
            } else {
                saveToSharedPrefs(msg.fromJson<SavedDevice>())
            }
        }

        override fun errorSending(e: String) {
            Log.i(TAG, "errorSending: $e")
        }

        override fun errorReading(e: String) {
            Log.i(TAG, "errorReading: $e")
        }

        override fun connected(isConnected: Boolean, ssid: String, password: String) {
            // If a device is connected or disconnected, notify the user
            connectionStatus.postValue(SingleEvent(isConnected))
            if (!isConnected) {
                // IF is is disconnected, remove the previous device from state
                bluetoothDevice.postValue(null)
            } else {
                // If it is connected, send the initial bluetooth message with the username,
                // password, ssid of the network for ESP32 connection, and the Network password
                sendBluetoothMsg(InitialComms(
                    "john123",
                    "adminpassword",
                    ssid,
                    password
                ).toJson())
            }
        }
    }

    fun startBluetoothConnection(device: BluetoothDevice, ssid: String, password: String) {
        // Function used to start the bluetooth connection on a new thread
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothHandler = BluetoothHandler(device, ssid, password)
            bluetoothHandler?.setBluetoothReceiverListener(this@MainViewModel.bluetoothListener)
            bluetoothHandler?.connect()
        }
    }

    fun sendBluetoothMsg(msg: String) {
        // Sends a message to the Device
        bluetoothHandler?.sendMessage(msg)
    }

    fun ioTDeviceClicked(ipAddress: String, msg: Int) {
        // If an IoT device in the list on the UI has the lock button clicked, get the required network manaager,
        // and send the request command
        hashDeviceList[ipAddress]?.sendNetworkMessage("john123", "adminpassword", msg)
    }

    fun disconnectFromDevice() {
        // Disconnects the device from the phone
        bluetoothHandler?.disconnect()
        //disconnectAndClearHashMap()
    }

    private fun saveToSharedPrefs(lock: SavedDevice?) {
        // This function is used to save a device to local storage once it is pared
        // If the device is null, just return out of the function
        lock ?: return

        // Gets an instance of SharedPreferences
        val context = getApplication<Application>().applicationContext
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_key),
            Context.MODE_PRIVATE
        ) ?: return

        // Gets any devices that may have already been saved to SharedPreferences
        val devices =
            sharedPref.getString(context.getString(R.string.saved_devices_sp), "") ?: return

        // If there have been previously saved devices
        if (devices != "") {
            // Gets the current list of devices in the form of a SavedDeviceList Object (this holds all of instances of SavedDevice)
            val savedDevices = devices.fromJson<SavedDeviceList>() ?: return
            // For each device that has been previously saved, check if the current one we are trying to save has already been saved
            savedDevices.savedDevices.forEach {
                if(it.ipAddress == lock.ipAddress) {
                    // If the device already exists, just leave the function
                    return
                }
            }
            // If not, add the new device to the list
            savedDevices.savedDevices.add(lock)
            // Save this new list to shared preferences
            with(sharedPref.edit()) {
                putString(context.getString(R.string.saved_devices_sp), savedDevices.toJson())
                apply()
            }
            // Function used to update the UI
            getMostRecentLockList(savedDevices)
        } else {
            // Create new shared prefs list
            val deviceList = mutableListOf<SavedDevice>()
            // Adds the new lock
            deviceList.add(lock)
            // Saves the new list to SharedPreferences in the required format
            val newDeviceList = SavedDeviceList(deviceList)
            with(sharedPref.edit()) {
                putString(context.getString(R.string.saved_devices_sp), newDeviceList.toJson())
                apply()
            }
            // Update the UI
            getMostRecentLockList(newDeviceList)
        }

    }

    fun getMostRecentLockList(ogList: SavedDeviceList?) {
        // Gets an instance of SharedPreferences
        val context = getApplication<Application>().applicationContext
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.shared_prefs_key),
            Context.MODE_PRIVATE
        ) ?: return

        // Gets the devices that have been saved to SharedPreferences
        val devices =
            sharedPref.getString(context.getString(R.string.saved_devices_sp), "") ?: return

        // If devices exist
        if (devices != "") {
            // Get the devices from the String saved in shared preferences
            val savedDevicesList = devices.fromJson<SavedDeviceList>()
            // Updates the LiveData with the new list (will automatically update the UI)
            lockList.postValue(savedDevicesList?.savedDevices?.map {
                UiLock(UUID.randomUUID().toString(), it.name, it.ipAddress, 0)
            })
            // For each device in the list, create a new communication interface
            savedDevicesList?.savedDevices?.forEach {
                viewModelScope.launch(Dispatchers.IO) {
                    // This interface is created for every IoT device to communicate which device needs its UI updated
                    val listenerObject = object: IoTDeviceStateInterface {
                        override fun onDeviceStateUpdated(locked: Int, ipAddress: String) {
                            var indexValue = -1
                            val tempList = lockList.value
                            tempList?.forEachIndexed { index, uiLock ->
                                if (uiLock.ipAddress == ipAddress)  {
                                    Log.i(TAG, "updateLockList: updating lock list values")
                                    // <May not get mutable list to update, if not need to copy list, update, then repalce entire list
                                    uiLock.locked = locked
                                }
                            }

                            lockList.postValue(tempList)

                        }

                    }
                    // If the IpAddress does not have a network manager associated with it (this means no TCP connection has been created for it)
                    if (hashDeviceList[it.ipAddress] == null) {
                        // Create the NetworkManager and start it
                        hashDeviceList[it.ipAddress] = NetworkManager(it.ipAddress, listenerObject)
                        hashDeviceList[it.ipAddress]?.run()
                    }
                }
            }
        } /*else {
            // If there are no devices
            val savedDevicesList = devices.fromJson<SavedDeviceList>()
            //disconnectAndClearHashMap()
            savedDevicesList?.savedDevices?.forEach {
                viewModelScope.launch(Dispatchers.IO) {
                    val listenerObject = object: IoTDeviceStateInterface {
                        override fun onDeviceStateUpdated(locked: Int, ipAddress: String) {
                            var indexValue = -1
                            val tempList = lockList.value
                            tempList?.forEachIndexed { index, uiLock ->
                                if (uiLock.ipAddress == ipAddress)  {
                                    Log.i(TAG, "updateLockList: updating lock list values")
                                    // <May not get mutable list to update, if not need to copy list, update, then repalce entire list
                                    uiLock.locked = locked
                                }
                            }
                            lockList.postValue(tempList)

                        }

                    }
                    if (hashDeviceList[it.ipAddress] == null) {
                        hashDeviceList[it.ipAddress] = NetworkManager(it.ipAddress, listenerObject)
                        hashDeviceList[it.ipAddress]?.run()
                    }
                }
            }
        }*/
    }

    private fun disconnectAndClearHashMap() {
        hashDeviceList.forEach {
            it.value.onDisconnect()
        }
        hashDeviceList.clear()
    }
}