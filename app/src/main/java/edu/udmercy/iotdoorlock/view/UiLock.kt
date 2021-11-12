package edu.udmercy.iotdoorlock.view

import androidx.recyclerview.widget.DiffUtil

data class UiLock (
    val uid: String,
    val name: String,
    val locked: LockState,
){
    companion object{
        val DIFFER = object: DiffUtil.ItemCallback<UiLock>(){
            override fun areItemsTheSame(oldItem: UiLock, newItem: UiLock): Boolean {
                return oldItem.uid == newItem.uid && oldItem.locked == newItem.locked
            }

            override fun areContentsTheSame(oldItem: UiLock, newItem: UiLock): Boolean {
                return oldItem == newItem
            }

        }
    }
}

enum class LockState(val int: Int) {
    LOCKED(1),
    UNLOCKED(0)
}