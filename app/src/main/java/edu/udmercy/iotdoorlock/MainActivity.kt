package edu.udmercy.iotdoorlock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import edu.udmercy.iotdoorlock.cryptography.DHKeyExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch(Dispatchers.IO) {
            val client = DHKeyExchange()
            val server = DHKeyExchange(client.pubKey.encoded)

            client.setReceivedPublicKey(server.pubKey.encoded)
            server.setReceivedPublicKey(client.pubKey.encoded)

            val msg = "Hello World!"
            val encrypted = client.encrypt(msg)
            Log.i(TAG, "onCreate: encrypted=$encrypted")

            val recvMesg = server.decrypt(encrypted)
            Log.i(TAG, "onCreate: ServerDecrypt=${recvMesg.toString()}")
        }

    }
}