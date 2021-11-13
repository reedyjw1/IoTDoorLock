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

class BTDialogFragment: DialogFragment() {

    companion object {
        const val TAG = "BTDialog"
    }

    private val adapter by lazy {
        BluetoothRecyclerAdapter().apply {
            onDeviceClick = { device ->
                // Check if bonded, if so continue, if not pair
                communicationInterface.updateSelectedBluetoothDevice(device.device)
                Log.i(TAG, "clicked: ${device.device.bondState == BluetoothDevice.BOND_BONDED}")
                
                dismissAllowingStateLoss()
            }
        }
    }

    private val btAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothList = mutableListOf<BluetoothDevice>()
    private lateinit var communicationInterface: CommunicationInterface

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fragment_bt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btRecyclerView.adapter = adapter
        //btRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        findBt()
    }

    override fun onStart() {
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
        communicationInterface = listener
        return this
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            requireActivity().unregisterReceiver(bluetoothReceiver)
            btAdapter.cancelDiscovery()
        } catch (e: Exception) {
            Log.e("BluetoothHandler", "Couldn't stop discovery or unregister receiver")
        }
    }

    private fun findBt() {
        if (!viewModel.scanningFlag) {
            viewModel.scanningFlag = true
            Log.i(TAG, "findBt: startingDiscovery")
            btAdapter.startDiscovery()
            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            requireActivity().registerReceiver(bluetoothReceiver, filter)
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action?:return

            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it.name != null) {
                            bluetoothList.add(it)
                            adapter.submitList(bluetoothList.map { el -> UiBluetoothDevice(el) })
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "Discovery Finished")
                    viewModel.scanningFlag = false

                }
            }
        }

    }


}