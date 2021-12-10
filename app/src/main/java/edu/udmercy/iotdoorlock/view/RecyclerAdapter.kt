package edu.udmercy.iotdoorlock.view

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import edu.udmercy.iotdoorlock.bluetooth.UiBluetoothDevice

typealias OnDeviceClick = (UiLock) -> Unit

// Links the ViewHolder class with the RecyclerView element
class RecyclerAdapter: ListAdapter<UiLock, LockVH>(UiLock.DIFFER) {

    var onDeviceClick: OnDeviceClick? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockVH =
        LockVH.create(parent, viewType).apply {
            deviceClick= onDeviceClick
        }

    override fun onBindViewHolder(holder: LockVH, position: Int) {
        holder.entity = getItem(position)
    }

}