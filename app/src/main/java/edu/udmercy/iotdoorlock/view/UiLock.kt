package edu.udmercy.iotdoorlock.view

import androidx.recyclerview.widget.DiffUtil

// Data class that is used for presenting the Lock information the the UI
data class UiLock (
    val uid: String,
    val name: String,
    val ipAddress: String,
    var locked: Int,
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