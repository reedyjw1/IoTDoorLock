package edu.udmercy.iotdoorlock.utils

/**
 * Emits a single event to be observed.
 * (This prevents Toast messages (notification messages through the Android's API) from sending notifications when app first starts,
 * This is important when LiveData is being observed)
 * Code implemented using the help of this article: https://medium.com/android-news/sending-events-from-viewmodel-to-activities-fragments-the-right-way-26bb68502b24
 */
open class SingleEvent<out T>(private val content: T) {

    var handled = false
        private set

    /**
     * If the event has not been observed, the contents of the event will be returned.
     * Otherwise, null will be returned.
     *
     * @return null or T
     */
    fun getContentIfNotHandledOrNull(): T? {
        return if (handled)
            null
        else {
            handled = true
            content
        }
    }

    /**
     * See the contents of the event, even when the event has already been observed
     *
     * @return T
     */
    fun peekContent(): T = content
}