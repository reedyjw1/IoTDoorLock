package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import kotlin.Exception

class BluetoothHandler(private val device: BluetoothDevice) {

    companion object {
        private const val TAG = "BluetoothHandler"
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var connectedThread: ConnectedThread? = null
    private val coroutineScope = MainScope()
    private var forwardingListener: BluetoothReceiver? = null

    fun sendMessage(msg: String) {
        val formatted = msg + "\n"
        connectedThread?.write(formatted.toByteArray())
    }

    fun connect() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val connectionDevice = adapter.getRemoteDevice(device.address)

        if (bluetoothSocket == null) {
            bluetoothSocket = connectionDevice.createInsecureRfcommSocketToServiceRecord(uuid)
            try {
                val socket = bluetoothSocket ?: return
                socket.connect()
                connectedThread = ConnectedThread(socket).apply {
                    listener = forwardingListener
                    coroutineScope.launch(Dispatchers.IO) {
                        run()
                    }
                }
                forwardingListener?.connected(true)

            } catch (e: Exception) {
                Log.e(TAG, "connect Error: ${e.localizedMessage}")
                forwardingListener?.connected(false)
            }
        }
    }

    fun setBluetoothReceiverListener(mListener: BluetoothReceiver) {
        forwardingListener = mListener
    }

    fun disconnect() {
        connectedThread?.cancel()
        connectedThread = null
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket): Thread()  {

        var listener: BluetoothReceiver? = null
        private var alive = true

        override fun run() {
            alive = true
            while (alive) {
                try {
                    if (mmSocket.inputStream.available() > 0) {
                        val buffered = mmSocket.inputStream.bufferedReader()
                        val msg = buffered.readLine()
                        listener?.receivedBluetoothMessage(msg)
                    }
                } catch (e: Exception) {
                    val exception = e.localizedMessage ?: return
                    listener?.errorSending(exception)
                    Log.e(TAG, "run: $exception")
                }

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmSocket.outputStream.write(bytes)
                Log.i(TAG, "write: sendingMessage=${bytes.decodeToString()}")
            } catch (e: IOException) {
                listener?.errorSending("Output Stream disconnected")
                return
            }

        }

        fun cancel() {
            try {
                alive = false
                mmSocket.close()
                listener?.connected(false)
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}