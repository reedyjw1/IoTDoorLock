package edu.udmercy.iotdoorlock.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import edu.udmercy.iotdoorlock.R
import edu.udmercy.iotdoorlock.view.MainViewModel
import kotlinx.android.synthetic.main.dialog_fragment_bt.*
import java.lang.Exception

// Class responsible for showing the bluetooth devices around the user, and allowing them to select one
class BTDialogFragment: DialogFragment() {

    companion object {
        const val TAG = "BTDialog"
    }

    // Creates the adapter that handles showing every device nearby to the screen
    private val adapter by lazy {
        BluetoothRecyclerAdapter().apply {
            // If the UI cell in the adapter is clicked, then the relevant information is propagated back to the main fragment
            onDeviceClick = { device ->
                // Check if bonded, if so continue, if not pair
                communicationInterface.updateSelectedBluetoothDevice(device.device)
                Log.i(TAG, "clicked: ${device.device.bondState == BluetoothDevice.BOND_BONDED}")
                // Closes the DialogFragment
                dismissAllowingStateLoss()
            }
        }
    }

    // Initializes the BluetoothAdpater API from Android
    private val btAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothList = mutableListOf<BluetoothDevice>()
    private lateinit var communicationInterface: CommunicationInterface

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Creates the DialogFragment's UI
        return inflater.inflate(R.layout.dialog_fragment_bt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Assigns the adapter to the UI element for displaying lists of data
        btRecyclerView.adapter = adapter
        // Starts the Bluetooth Scan
        findBt()
    }

    override fun onStart() {
        // Handles the sizing of the DialogFragment that appears on screen
        // Sets the size to match the width of the parent fragment, and 70% the height of the parent fragment
        // In this case, the width of the phone screen and 70% the phone screen height
        super.onStart()
        val display = dialog?.window?.windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        val height: Int = size.y
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            (height * .7).toInt()
        )
    }

    fun setCommunicationInterface(listener: CommunicationInterface): BTDialogFragment {
        // Sets the listener to propagate the Bluetooth Device that was clicked to the parent fragment
        communicationInterface = listener
        return this
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregisters the BroadCast receiver that is responsible for collecting the information about the nearby Bluetooth devices
        try {
            requireActivity().unregisterReceiver(bluetoothReceiver)
            btAdapter.cancelDiscovery()
        } catch (e: Exception) {
            Log.e("BluetoothHandler", "Couldn't stop discovery or unregister receiver")
        }
    }

    private fun findBt() {
        // Only starts scanning if it already isn't
        if (!viewModel.scanningFlag) {
            viewModel.scanningFlag = true
            Log.i(TAG, "findBt: startingDiscovery")

            // Starts the search and creates an IntentFilter so that my application knows when the phone finds a device
            btAdapter.startDiscovery()
            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

            // Registers the receiver which notifies the code below when a device is found
            requireActivity().registerReceiver(bluetoothReceiver, filter)
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action?:return

            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // If a bluetooth device is found, get any information about it and save to the variable
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        // Make sure the device isn't null
                        if (it.name != null) {
                            // Adds to the list of currently found devices, and calls the code to update the UI
                            bluetoothList.add(it)
                            adapter.submitList(bluetoothList.map { el -> UiBluetoothDevice(el) })
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                // When the phone finishes the scan, notify the class that the scanning is finished and can be started again if needed.
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "Discovery Finished")
                    viewModel.scanningFlag = false

                }
            }
        }

    }


}