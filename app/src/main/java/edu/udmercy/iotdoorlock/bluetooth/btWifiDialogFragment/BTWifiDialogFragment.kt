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

    private var communicationInterface: WifiInformationInterface? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        submitBtn.setOnClickListener {
            communicationInterface?.wifiInformation(editTextTextPersonName2.text.toString(), editTextTextPassword.text.toString())
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
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
        return inflater.inflate(R.layout.dialog_wifi_info, container, false)
    }

    fun setCommunicationInterface(listener: WifiInformationInterface): BTWifiDialogFragment {
        communicationInterface = listener
        return this
    }
}