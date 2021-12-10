package edu.udmercy.iotdoorlock.bluetooth.btWifiDialogFragment

import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import edu.udmercy.iotdoorlock.R
import kotlinx.android.synthetic.main.dialog_wifi_info.*

class BTWifiDialogFragment: DialogFragment() {

    // Interface to communicate between DialogFragment and the fragment that started it
    private var communicationInterface: WifiInformationInterface? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Sets up when the the button click listener, sends back the inputted wifi name and password
        submitBtn.setOnClickListener {
            communicationInterface?.wifiInformation(editTextTextPersonName2.text.toString(), editTextTextPassword.text.toString())
            // Closes the DialogFragment
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        // When the DialogFragment starts, set the size of the box to only take up as much space as the UI elements inside need.
        val display = dialog?.window?.windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        val height: Int = size.y
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Presents the DialogView
        return inflater.inflate(R.layout.dialog_wifi_info, container, false)
    }

    fun setCommunicationInterface(listener: WifiInformationInterface): BTWifiDialogFragment {
        // Sets the interface that is hooked up in the previous fragment
        communicationInterface = listener
        return this
    }
}