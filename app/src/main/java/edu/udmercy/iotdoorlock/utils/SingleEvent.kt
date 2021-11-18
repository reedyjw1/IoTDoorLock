package edu.udmercy.iotdoorlock.utils

/**
 * Emits a single event to be observed.
 * https://medium.com/android-news/sending-events-from-viewmodel-to-activities-fragments-the-right-way-26bb68502b24
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