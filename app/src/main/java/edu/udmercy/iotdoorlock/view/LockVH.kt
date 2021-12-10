package edu.udmercy.iotdoorlock.view

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.udmercy.iotdoorlock.R
import kotlinx.android.synthetic.main.lock_cell.view.*

// ViewHolder that is responsible for showing every Lock's information in the list to the screen
class LockVH(itemView: View): RecyclerView.ViewHolder(itemView) {
    var deviceClick: OnDeviceClick? = null

    var entity: UiLock? = null
        set(value) {
            field = value
            value?.let { information ->
                // Displays the Devices name to the list
                Log.i("ViewHolder", "updating List: $information")
                itemView.titleTextView.text = information.name
                // If the state is 3, means that TCP connection hasn't been initialized (will wait for 0 or 1)
                if (information.locked == 3) {
                    itemView.descriptionChip.visibility = View.INVISIBLE
                    return@let
                } else {
                    itemView.descriptionChip.visibility = View.VISIBLE
                }
                // Updates text based on the current lock state
                itemView.descriptionChip.text = if (information.locked == 1) {
                    "Locked"
                } else {
                    "Unlocked"
                }
                // Click listener for the Chip that locks and unlocks the device
                itemView.descriptionChip.isChecked = information.locked == 1
                itemView.descriptionChip.setOnClickListener {
                    deviceClick?.invoke(information)
                }
            }
        }

    companion object {
        fun create(parent: ViewGroup, viewType: Int): LockVH {
            return LockVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.lock_cell, parent, false)
            )
        }
    }


}