package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.udmercy.iotdoorlock.R
import kotlinx.android.synthetic.main.bluetooth_cell.view.*
import kotlinx.android.synthetic.main.lock_cell.view.*

class BluetoothVH(itemView: View): RecyclerView.ViewHolder(itemView) {

    var deviceClick: OnDeviceClick? = null

    var entity: UiBluetoothDevice? = null
        set(value) {
            field = value
            value?.let { information ->
                // Responsible for showing the individual devices
                // Sets the text view in the UI cell and the button
                itemView.title.text = information.device.name
                itemView.setOnClickListener { deviceClick?.invoke(information) }
            }
        }

    companion object {
        fun create(parent: ViewGroup, viewType: Int): BluetoothVH {
            //Inflates the UI Cell (UI cell is the peice of UI information that is repeated everytime a new device is added to the list on screen)
            return BluetoothVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.bluetooth_cell, parent, false)
            )
        }
    }


}