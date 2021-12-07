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

    private val viewModel by viewModels<MainViewModel>()
    private val btAdapter = BluetoothAdapter.getDefaultAdapter()

    private val lockListObserver =
        Observer { list: List<UiLock> ->
            this.updateLockList(list)
        }

    private val bluetoothDeviceObserver =
        Observer { device: BluetoothDevice? ->
            Log.i(TAG, "BluetoothDeviceObserver: Updated=$device")
        }

    private val adapter by lazy {
        RecyclerAdapter().apply {
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

    private val isConnectedObserver = Observer { event: SingleEvent<Boolean> ->
        event.getContentIfNotHandledOrNull()?.let { status ->
            if (status) {
                Toast.makeText(this, "Connected to Device!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Disconnected from Device!", Toast.LENGTH_LONG).show()
            }

        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.adapter = adapter

        viewModel.getMostRecentLockList(null)

        bluetoothFab.setOnClickListener {
            BTDialogFragment().setCommunicationInterface(this)
                .show(supportFragmentManager, "bluetoothDevice")
        }

        testFab.visibility = View.INVISIBLE

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
        Log.i(TAG, "updateLockList: updating lock list values")
        adapter.submitList(list)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        viewModel.lockList.observe(this, lockListObserver)
        viewModel.bluetoothDevice.observe(this, bluetoothDeviceObserver)
        viewModel.connectionStatus.observe(this, isConnectedObserver)
    }

    override fun onPause() {
        super.onPause()
        viewModel.lockList.removeObserver(lockListObserver)
        viewModel.bluetoothDevice.removeObserver(bluetoothDeviceObserver)
        viewModel.connectionStatus.removeObserver(isConnectedObserver)
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.e("DEBUG", "${it.key} = ${it.value}")
            }
        }

    override fun updateSelectedBluetoothDevice(device: BluetoothDevice) {
        val wifiInfoObject = object : WifiInformationInterface {
            override fun wifiInformation(ssid: String, password: String) {
                viewModel.startBluetoothConnection(device)
                Log.i(TAG, "updateSelectedBluetoothDevice: $device")
            }

        }
        BTWifiDialogFragment().setCommunicationInterface(wifiInfoObject)
            .show(supportFragmentManager, "WifiInformation")

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectFromDevice()
    }
}