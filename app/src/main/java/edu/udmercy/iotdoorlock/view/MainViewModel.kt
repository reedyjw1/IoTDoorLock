package edu.udmercy.iotdoorlock.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val lockList: MutableLiveData<List<UiLock>> = MutableLiveData(emptyList())
}