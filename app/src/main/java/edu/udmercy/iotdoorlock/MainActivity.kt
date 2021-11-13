package edu.udmercy.iotdoorlock

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import edu.udmercy.iotdoorlock.cryptography.DHKeyExchange
import edu.udmercy.iotdoorlock.view.LockState
import edu.udmercy.iotdoorlock.view.MainViewModel
import edu.udmercy.iotdoorlock.view.RecyclerAdapter
import edu.udmercy.iotdoorlock.view.UiLock
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import edu.udmercy.iotdoorlock.bluetooth.BTDialogFragment
import edu.udmercy.iotdoorlock.bluetooth.CommunicationInterface


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
        Observer { device: BluetoothDevice ->
            Log.i(TAG, "BluetoothDeviceObserver: Updated=$device")
        }

    private val adapter by lazy {
        RecyclerAdapter()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.adapter = adapter
        Log.i(TAG, "onCreate: Bluetooth adapter available=${btAdapter!=null}")
        Log.i(TAG, "onCreate: Bluetooth adapter enabled=${btAdapter.isEnabled}")

        viewModel.lockList.postValue(mutableListOf(UiLock("test", "IoT Door Lock 1", LockState.LOCKED)))
        bluetoothFab.setOnClickListener {
            BTDialogFragment().setCommunicationInterface(this).show(supportFragmentManager, "bluetoothDevice")
        }

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
        adapter.submitList(list)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        viewModel.lockList.observe(this, lockListObserver)
        viewModel.bluetoothDevice.observe(this, bluetoothDeviceObserver)
    }

    override fun onPause() {
        super.onPause()
        viewModel.lockList.removeObserver(lockListObserver)
        viewModel.bluetoothDevice.removeObserver(bluetoothDeviceObserver)
    }

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.e("DEBUG", "${it.key} = ${it.value}")
        }
    }

    override fun updateSelectedBluetoothDevice(device: BluetoothDevice) {
        Log.i(TAG, "updateSelectedBluetoothDevice: $device")
    }
}