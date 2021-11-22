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

class NetworkManager: Thread() {

    var ipAddress: String = "10.0.41.79"
    var port: Int = 5679
    var socket: Socket?= null
    private val coroutineScope = MainScope()
    private var running = true

    companion object {
        private const val TAG = "NetworkManager"
    }

    init {
        try {
            socket = Socket(ipAddress, port)
        } catch (e: Exception) {
            Log.e(TAG, "SocketError: ${e.localizedMessage}")
        }
    }

    override fun run() {
        running = true
        while(running && socket != null) {
            try {
                if (socket?.getInputStream()?.available() != 0) {
                    val msg = InputStreamReader(socket?.getInputStream()).buffered().readLine()
                    Log.i(TAG, "message: $msg")
                }

            } catch (e: Exception) {
                Log.e(TAG, "run: ${e.localizedMessage}")
            }
        }
    }

    fun sendNetworkMessage(msg: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val stream = socket?.getOutputStream() ?: return@launch
                val printWriter = PrintWriter(stream)
                printWriter.write(msg)
                printWriter.flush()
            } catch (e: IOException) {
                Log.i(TAG, "sendNetworkMessage: ${e.localizedMessage} ")
                onDisconnect()
            }
        }
    }

    fun onDisconnect() {
        Log.i(TAG, "onDisconnect: disconnect called")
        running = false
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "onDisconnect: couldn't close socket", )
        }

    }
}