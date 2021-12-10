package edu.udmercy.iotdoorlock.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Extension functions that use a 3rd party library for easy conversion of any object
// Type to and from JSON
// Example: val message: String = TestClass("Test", 1).toJson()
inline fun <reified T> String.fromJson(): T? {
    return Gson().fromJson(this, object : TypeToken<T>() {}.type)
}

inline fun <reified T> T.toJson(): String {
    return Gson().toJson(this)
}