package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothDevice
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

typealias OnDeviceClick = (UiBluetoothDevice) -> Unit

class BluetoothRecyclerAdapter: ListAdapter<UiBluetoothDevice, BluetoothVH>(UiBluetoothDevice.DIFFER) {

    var onDeviceClick: OnDeviceClick? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothVH =
        BluetoothVH.create(parent, viewType).apply {
            deviceClick= onDeviceClick
        }

    override fun onBindViewHolder(holder: BluetoothVH, position: Int) {
        holder.entity = getItem(position)
    }

}

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