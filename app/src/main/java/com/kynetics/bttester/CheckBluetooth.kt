/*
 * Copyright © 2021-2022  Kynetics  LLC
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * @author Andrea Zoleo
 */

package com.kynetics.bttester

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat.checkSelfPermission
import java.util.*


class CheckBluetooth(private val activity: MainActivity, private val onSuccess: ()-> Unit) {

    private val requireBtEnabling =
        activity.registerForActivityResult(StartActivityForResult().apply {
            createIntent(activity.baseContext, Intent(ACTION_REQUEST_ENABLE))
        }) {
            if(it.resultCode == RESULT_OK){
                btEnabled = true
                go()
            } else {
                activity.alert("Info", "This application can not work with Bluetooth disabled")
            }
        }

    private val requireBtPermissions =
        activity.registerForActivityResult(RequestMultiplePermissions()) {
            if (it[ACCESS_FINE_LOCATION]!! && it[ACCESS_BACKGROUND_LOCATION]!!) {
                btPermitted = true
                go()
            } else {
                activity.alert("Note", "The application can't run without the required permissions")
            }
        }

    init {
        go()
    }

    private fun go() {
        if(
            (btPresent      || checkBTisPresent())              .also { btPresent   = it } &&
            (btEnabled      || checkEnabledOrTryToEnableBT())   .also { btEnabled   = it } &&
            (btPermitted    || checkBtPermissions())            .also { btPermitted = it }
        ) onSuccess()
    }

    private fun checkBTisPresent():Boolean {
        val ba =  BluetoothAdapter.getDefaultAdapter()
        return if(ba != null) {
            this.bluetoothAdapter = ba
            true
        } else {
            activity.alert("System Error", "Missing BluetoothAdapter in this device!")
            false
        }
    }

    private fun checkEnabledOrTryToEnableBT(): Boolean {
        return if (bluetoothAdapter.isEnabled) {
            true
        } else {
            requireBtEnabling.launch(Intent(ACTION_REQUEST_ENABLE))
            false
        }
    }

    private fun checkBtPermissions(): Boolean {
        fun checkPrm(p: String) =
            checkSelfPermission(activity.baseContext, p) == PERMISSION_GRANTED
        return if(checkPrm(ACCESS_FINE_LOCATION) && checkPrm(ACCESS_BACKGROUND_LOCATION)) {
            true
        } else {
            requireBtPermissions.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION))
            false
        }
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var btPresent = false
    private var btEnabled = false
    private var btPermitted = false
}