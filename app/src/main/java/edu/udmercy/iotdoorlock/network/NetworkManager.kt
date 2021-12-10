package edu.udmercy.iotdoorlock.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

// Class for handling the TCP connection and communication
class NetworkManager(private val ipAddress: String, private val listener: IoTDeviceStateInterface): Thread() {

    var port: Int = 5679
    var socket: Socket?= null

    // Used for executing code on a new thread ( a must for internet communication)
    private val coroutineScope = MainScope()
    private var running = true

    companion object {
        private const val TAG = "NetworkManager"
    }

    // Creates the socket when the class is initialized and starts the connection
    init {
        try {
            socket = Socket(ipAddress, port)
        } catch (e: Exception) {
            Log.e(TAG, "SocketError: ${e.localizedMessage}")
        }
    }

    override fun run() {
        // First sends a command requesting the initial stat of the ESP32
        running = true
        try {
            sendNetworkMessage("john123", "adminpassword", 2)
        } catch (e: Exception) {
            Log.e(TAG, "run: ${e.localizedMessage}")
        }
        // Code that always listens for informationc coming through the input stream (ESP32 socket)
        while(running && socket != null) {
            try {
                // IF there is data to be read
                if (socket?.getInputStream()?.available() != 0) {
                    // Reads the bytes comming in until there is a new line character (important so that individual bytes are not read one by one)
                    // Also this code is blocking, therefore needs to be on a new thread to prevent the UI from freezing (since UI thread is default thread Android code is run on)
                    val msg = InputStreamReader(socket?.getInputStream()).buffered().readLine()
                    Log.i(TAG, "message: $msg")
                    // Updates the class that initialized the class on the information)
                    listener.onDeviceStateUpdated(msg.toInt(), ipAddress)
                }

            } catch (e: Exception) {
                Log.e(TAG, "run: ${e.localizedMessage}")
            }
        }
    }

    fun sendNetworkMessage(username: String, password: String, msg: Int) {
        // Formates the message in JSON (adds the username, password, and message command)
        val formattedMsg = "{\"username\": \"$username\", \"password\": \"$password\", \"message\": $msg}"
        // Creates a new thread for sending the bytes out of the socket
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Writes the message to the output stream of the socket.
                val stream = socket?.getOutputStream() ?: return@launch
                val printWriter = PrintWriter(stream)
                printWriter.write(formattedMsg)
                printWriter.flush()
                Log.i(TAG, "sendNetworkMessage: $formattedMsg")
            } catch (e: IOException) {
                Log.i(TAG, "sendNetworkMessage: ${e.localizedMessage} ")
                //onDisconnect()
            }
        }
    }

    fun onDisconnect() {
        // Function for closing the socket (useful for when the app closes)
        Log.i(TAG, "onDisconnect: disconnect called: $ipAddress")
        running = false
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "onDisconnect: couldn't close socket", )
        }

    }
}