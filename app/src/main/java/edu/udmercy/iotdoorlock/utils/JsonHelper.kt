package edu.udmercy.iotdoorlock.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> String.fromJson(): T? {
    return Gson().fromJson(this, object : TypeToken<T>() {}.type)
}

inline fun <reified T> T.toJson(): String {
    return Gson().toJson(this)
}