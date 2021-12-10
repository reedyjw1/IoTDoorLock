package edu.udmercy.iotdoorlock.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import kotlin.Exception

class BluetoothHandler(private val device: BluetoothDevice, private val ssid: String, private val password: String) {

    companion object {
        private const val TAG = "BluetoothHandler"
    }

    // Class variables for the BluetoothSocket
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
                forwardingListener?.connected(true, ssid, password)

            } catch (e: Exception) {
                Log.e(TAG, "connect Error: ${e.localizedMessage}")
                forwardingListener?.connected(false, ssid, password)
            }
        }
    }

    fun setBluetoothReceiverListener(mListener: BluetoothReceiver) {
        // Lister that passes information received from the Bluetooth socket to  a different class (For updating the UI)
        forwardingListener = mListener
    }

    fun disconnect() {
        // Stops the thread, and clears it from memory
        connectedThread?.cancel()
        connectedThread = null
    }

    // Custom Thread Class for handling writing to the socket and receiving data (prevents blocking UI thread)
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket): Thread()  {

        var listener: BluetoothReceiver? = null
        private var alive = true

        override fun run() {
            // When function is called, starts a loop always listening for data sent through the socket
            alive = true
            while (alive) {
                try {
                    // If there is data to be read,
                    if (mmSocket.inputStream.available() > 0) {
                        val buffered = mmSocket.inputStream.bufferedReader()
                        // Read every byte until the new line character
                        val msg = buffered.readLine()
                        // Pass the infromation to the class that set up the listener
                        listener?.receivedBluetoothMessage(msg)
                    }
                } catch (e: Exception) {
                    // Notify ViewModel that an exception occured
                    val exception = e.localizedMessage ?: return
                    listener?.errorSending(exception)
                    Log.e(TAG, "run: $exception")
                }

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                // Writes the bytes to the output stream of the Bluetooth Socket
                mmSocket.outputStream.write(bytes)
                Log.i(TAG, "write: sendingMessage=${bytes.decodeToString()}")
            } catch (e: IOException) {
                listener?.errorSending("Output Stream disconnected")
                return
            }

        }

        fun cancel() {
            // Tries to clos the bluetooth socket
            try {
                alive = false
                mmSocket.close()
                listener?.connected(false, ssid, password)
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}