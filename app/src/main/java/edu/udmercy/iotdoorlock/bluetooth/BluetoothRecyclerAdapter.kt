package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothDevice
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

typealias OnDeviceClick = (UiBluetoothDevice) -> Unit

// This class is responsible for setting up the ViewHolder for every device in the list on the main UI screen
class BluetoothRecyclerAdapter: ListAdapter<UiBluetoothDevice, BluetoothVH>(UiBluetoothDevice.DIFFER) {

    var onDeviceClick: OnDeviceClick? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothVH =
        BluetoothVH.create(parent, viewType).apply {
            // Propagates a click listener from the button displayed on screen for each device to the main controlling class
            deviceClick= onDeviceClick
        }

    override fun onBindViewHolder(holder: BluetoothVH, position: Int) {
        holder.entity = getItem(position)
    }

}

// Data class that allows the adapter to know when the data in UIBluetoothDevice changes (used for updating the the information in the list)
data class UiBluetoothDevice (
    val device: BluetoothDevice
){
    companion object{
        val DIFFER = object: DiffUtil.ItemCallback<UiBluetoothDevice>(){
            override fun areItemsTheSame(oldItem: UiBluetoothDevice, newItem: UiBluetoothDevice): Boolean {
                return oldItem.device.address == newItem.device.address
            }

            override fun areContentsTheSame(oldItem: UiBluetoothDevice, newItem: UiBluetoothDevice): Boolean {
                return oldItem == newItem
            }

        }
    }
}