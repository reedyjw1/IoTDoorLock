package edu.udmercy.iotdoorlock

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import edu.udmercy.iotdoorlock.cryptography.DHKeyExchange
import edu.udmercy.iotdoorlock.view.LockState
import edu.udmercy.iotdoorlock.view.MainViewModel
import edu.udmercy.iotdoorlock.view.RecyclerAdapter
import edu.udmercy.iotdoorlock.view.UiLock
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel by viewModels<MainViewModel>()

    private val lockListObserver =
        Observer { list: List<UiLock> ->
            this.updateLockList(list)
        }

    private val adapter by lazy {
        RecyclerAdapter()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.adapter = adapter

        viewModel.lockList.postValue(mutableListOf(UiLock("test", "IoT Door Lock 1", LockState.LOCKED)))

        lifecycleScope.launch(Dispatchers.IO) {
            val client = DHKeyExchange()
            val server = DHKeyExchange(client.pubKey.encoded)

            client.setReceivedPublicKey(server.pubKey.encoded)
            server.setReceivedPublicKey(client.pubKey.encoded)

            val msg = "Hello World!"
            val encrypted = client.encrypt(msg)
            Log.i(TAG, "onCreate: encrypted=$encrypted")

            val recvMesg = server.decrypt(encrypted)
            Log.i(TAG, "onCreate: ServerDecrypt=${recvMesg}")
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateLockList(list: List<UiLock>) {
        adapter.submitList(list)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        viewModel.lockList.observe(this, lockListObserver)
    }

    override fun onPause() {
        super.onPause()
        viewModel.lockList.removeObserver(lockListObserver)
    }
}