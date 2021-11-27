package edu.udmercy.iotdoorlock.view

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.udmercy.iotdoorlock.R
import kotlinx.android.synthetic.main.lock_cell.view.*

class LockVH(itemView: View): RecyclerView.ViewHolder(itemView) {
    var entity: UiLock? = null
        set(value) {
            field = value
            value?.let { information ->
                Log.i("ViewHolder", "updating List: $information")
                itemView.titleTextView.text = information.name
                itemView.descriptionChip.text = if (information.locked == 1) {
                    "Locked"
                } else {
                    "Unlocked"
                }
                itemView.descriptionChip.isChecked = information.locked == 1
                itemView.descriptionChip.setOnClickListener {
                    // TODO - Propagate on click to view model
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