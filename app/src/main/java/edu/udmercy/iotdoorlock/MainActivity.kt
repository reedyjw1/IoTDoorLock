package edu.udmercy.iotdoorlock

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import edu.udmercy.iotdoorlock.cryptography.DHKeyExchange
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import edu.udmercy.iotdoorlock.bluetooth.BTDialogFragment
import edu.udmercy.iotdoorlock.bluetooth.CommunicationInterface
import edu.udmercy.iotdoorlock.bluetooth.btWifiDialogFragment.BTWifiDialogFragment
import edu.udmercy.iotdoorlock.bluetooth.btWifiDialogFragment.WifiInformationInterface
import edu.udmercy.iotdoorlock.utils.SingleEvent
import edu.udmercy.iotdoorlock.utils.fromJson
import edu.udmercy.iotdoorlock.view.*


class MainActivity : AppCompatActivity(), CommunicationInterface {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Initializes the ViewModel
    private val viewModel by viewModels<MainViewModel>()

    // These observers are responsible for calling code to update the UI
    // When the value in the LiveData (in the ViewModel) is changed
    private val lockListObserver =
        Observer { list: List<UiLock> ->
            // Refreshes the lock list with a new list from the ViewModel
            this.updateLockList(list)
        }

    // Updates the currently connected bluetooth device
    private val bluetoothDeviceObserver =
        Observer { device: BluetoothDevice? ->
            Log.i(TAG, "BluetoothDeviceObserver: Updated=$device")
        }

    // Adapter that is responsible for presenting the IoT Locks
    private val adapter by lazy {
        RecyclerAdapter().apply {
            // When a lock is clicked, send the command to the ViewModel
            onDeviceClick = {
                Log.i(TAG, "lockState: ${it.locked}")
                if (it.locked == 0) {
                    viewModel.ioTDeviceClicked(it.ipAddress, 1)
                } else {
                    viewModel.ioTDeviceClicked(it.ipAddress, 0)
                }
            }
        }
    }

    // Shows user notification when the Bluetooth device becomes connected or disconnected
    private val isConnectedObserver = Observer { event: SingleEvent<Boolean> ->
        event.getContentIfNotHandledOrNull()?.let { status ->
            if (status) {
                Toast.makeText(this, "Connected to Device!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Disconnected from Device!", Toast.LENGTH_LONG).show()
            }

        }

    }

    // Shows an error notification to the user if the SSID or Password they entered is incorrect
    private val internetStatusOnEsp32 = Observer { event: SingleEvent<Boolean?> ->
        event.getContentIfNotHandledOrNull()?.let { status: Boolean? ->
            if (status == null) {
                Log.i(TAG, "internestStatusOnEsp32: null")
            } else if(!status) {
                Toast.makeText(this, "Invalid Wifi Name or Password. Please try again!", Toast.LENGTH_LONG).show()
            }

        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Assigns the adapter to the RecyclerView UI element
        recyclerView.adapter = adapter

        // Get the lock list if it has been saved to local storage before
        viewModel.getMostRecentLockList(null)

        // Sets the bluetooth button click listener for presenting the DialogFragment for the
        // user to choose a Bluetooth device
        bluetoothFab.setOnClickListener {
            BTDialogFragment().setCommunicationInterface(this)
                .show(supportFragmentManager, "bluetoothDevice")
        }
        // Hides a test button for development
        testFab.visibility = View.INVISIBLE

        // If the phone is on an Android version above S, these permission need to be requested
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,

                    )
            )
        } else {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }

        // Code for executing the Diffie-Hellman key exchange locally between to instances locally on the device (for testing purposes)
        lifecycleScope.launch(Dispatchers.IO) {
            val client = DHKeyExchange()
            val server = DHKeyExchange(client.pubKey.encoded)

            client.setReceivedPublicKey(server.pubKey.encoded)
            server.setReceivedPublicKey(client.pubKey.encoded)

            val msg = "Hello World!"
            val encrypted = client.encrypt(msg)
            Log.i(TAG, "onCreate: encrypted=$encrypted")

            val recvMesg = server.decrypt(encrypted)
            Log.i(TAG, "onCreate: ServerDecrypt=${recvMesg}")


        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateLockList(list: List<UiLock>) {
        // Updates the list that is associated with the recycler view
        Log.i(TAG, "updateLockList: updating lock list values")
        adapter.submitList(list)
        adapter.notifyDataSetChanged()
    }

    // Called when the application is resumed
    override fun onResume() {
        super.onResume()
        // Assigns the observers to the live data in the view model
        viewModel.lockList.observe(this, lockListObserver)
        viewModel.bluetoothDevice.observe(this, bluetoothDeviceObserver)
        viewModel.connectionStatus.observe(this, isConnectedObserver)
        viewModel.isSsidAndPasswordGood.observe(this, internetStatusOnEsp32)
    }

    // Called when the application is exited
    override fun onPause() {
        super.onPause()
        // Un-registers the observers from the live data in the viewmodel
        viewModel.lockList.removeObserver(lockListObserver)
        viewModel.bluetoothDevice.removeObserver(bluetoothDeviceObserver)
        viewModel.connectionStatus.removeObserver(isConnectedObserver)
        viewModel.isSsidAndPasswordGood.removeObserver(internetStatusOnEsp32)
    }

    // Request the permissions using an Android API
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.e("DEBUG", "${it.key} = ${it.value}")
            }
        }

    // When the Bluetooth device is clicked in the list, show the Wifi information dialog. When the information is recieved,
    // Start the bluetooth connection
    override fun updateSelectedBluetoothDevice(device: BluetoothDevice) {
        val wifiInfoObject = object : WifiInformationInterface {
            override fun wifiInformation(ssid: String, password: String) {
                viewModel.startBluetoothConnection(device, ssid, password)
                Log.i(TAG, "updateSelectedBluetoothDevice: $device")
            }

        }
        BTWifiDialogFragment().setCommunicationInterface(wifiInfoObject)
            .show(supportFragmentManager, "WifiInformation")

    }

    // Disconnect the Bluetooth device from the phone when the application is killed.
    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectFromDevice()
    }
}