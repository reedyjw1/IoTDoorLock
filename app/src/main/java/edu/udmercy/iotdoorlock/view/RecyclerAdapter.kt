package edu.udmercy.iotdoorlock.view

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter

class RecyclerAdapter: ListAdapter<UiLock, LockVH>(UiLock.DIFFER) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockVH =
        LockVH.create(parent, viewType)

    override fun onBindViewHolder(holder: LockVH, position: Int) {
        holder.entity = getItem(position)
    }

}