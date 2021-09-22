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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.util.*

class BtModel(): ViewModel() {

    data class DeviceInfo(val device: BluetoothDevice, val bound: Boolean)

    var btAvailable = false
        set(v:Boolean){
            field = v
            start()
        }


    private var discovering: Boolean = false

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val devices: MutableLiveData<List<DeviceInfo>> by lazy { MutableLiveData(emptyList()) }

    private var selectedDevice: BluetoothDevice? = null

    private val profileProxy: MutableSet<BluetoothProfile> = emptySet<BluetoothProfile>().toMutableSet()

    private val connected: MutableLiveData<Boolean> = MutableLiveData(true)

    val protocolTesterService: ProtocolTesterService by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.197.245:9000")
            .build()
            .create(ProtocolTesterService::class.java)
    }

    fun selectDevice(address: String) {
        selectedDevice = devices.value!!.first {
            it.device.address == address
        }.device
        selectedDevice?.apply {
            if(createBond())
                Log.i("MY_BT", "start binding with ${this.address}")
            else
                Log.i("MY_BT", "unable to start binding with ${this.address}")
        }
    }

    fun start() {
        if(btAvailable && adapter != null && discovering) {
            this.devices.value = adapter.bondedDevices.map { DeviceInfo(it, true) }
            adapter.startDiscovery()
        }
    }

    fun startDiscovering() {
        if(!discovering){
            discovering = true
            start()
        }
        if(btAvailable && adapter != null && !discovering) {
            this.discovering = true
            this.devices.value = adapter.bondedDevices.map { DeviceInfo(it, true) }
            adapter.startDiscovery()
        }
    }

    fun stopDiscovering() {
        if(btAvailable && adapter != null && discovering) {
            adapter.cancelDiscovery()
            this.discovering = false
        }
    }

    fun startUsingBy(context: Context){
        context.registerReceiver(br, intentFilter)
    }

    fun stopUsingBy(context: Context){
        context.unregisterReceiver(br)
    }

    private val intentFilter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }

    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED ->
                    onBondStateChanged(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE), context)
                BluetoothDevice.ACTION_ACL_CONNECTED->
                    onAclConnected()
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED->
                    onAclDisconnectRequested()
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    onAclDisconnected()
                BluetoothDevice.ACTION_FOUND ->
                    onFound(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED ->
                    onDiscoveryFinished()
            }
        }
    }

    private val pl = object : BluetoothProfile.ServiceListener {

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                profileProxy.add(proxy)
                Log.i("MY_BT", "profile connected: HEADSET [${profileProxy.size}]")
            } else {
                Log.i("MY_BT", "profile connected: $profile")
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.i("MY_BT", "profile disconnected: HEADSET")
            } else {
                Log.i("MY_BT", "profile disconnected: $profile")
            }
            profileProxy.clear()
        }
    }

    //private val phoneAddress = "3482C5176355"
    private val phoneAddress = "DCDCE2AA4B49"

    fun bindPhone() {
        viewModelScope.launch(Dispatchers.IO){
            protocolTesterService.bindPhone(phoneAddress)
        }
    }

    fun unbindPhone() {
        viewModelScope.launch(Dispatchers.IO){
            protocolTesterService.unbindPhone(phoneAddress)
        }
    }

    fun connect() {
        if (profileProxy.isNotEmpty()) {
            val proxy = profileProxy.elementAt(0) as BluetoothHeadset
            Log.i("MY_BT", "Connecting to $selectedDevice...")
            viewModelScope.launch(Dispatchers.IO) {
                launch {
                    proxy.myConnect(selectedDevice!!)
                }
                delay(15000)
                withContext(Dispatchers.Main) {
                    connected.value = proxy.getConnectionState(selectedDevice!!) == BluetoothHeadset.STATE_CONNECTED
                }
            }
        } else {
            Log.w("MY_BT", "No active BT proxy")
        }
    }

    fun disconnect() {
        if (profileProxy.isNotEmpty()) {
            val proxy = profileProxy.elementAt(0) as BluetoothHeadset
            Log.i("MY_BT", "Disconnecting from $selectedDevice...")
            viewModelScope.launch(Dispatchers.IO) {
                proxy.myDisconnect(selectedDevice!!)
                delay(3000)
                withContext(Dispatchers.Main) {
                    connected.value = proxy.getConnectionState(selectedDevice!!) == BluetoothHeadset.STATE_CONNECTED
                }
            }
        } else {
            Log.w("MY_BT", "No active BT proxy")
        }
    }

    fun isConnected(): LiveData<Boolean> = connected

    fun unpairSelectedDevice() {
        fun BluetoothDevice.removeBond() {
            try {
                javaClass.getMethod("removeBond").invoke(this)
            } catch (e: Exception) {
                Log.e("MY_BT", "Removing bond has been failed. ${e.message}")
            }
        }
        selectedDevice?.removeBond()
    }

    private fun onBondStateChanged(device: BluetoothDevice?, context: Context){
        fun bs(s:Int) = when(s){
            10 -> "BOND NONE"
            11 -> "BONDING"
            12 -> "BONDED"
            else -> "UNKNOWN???"
        }
        if(device != null){
            Log.i("MY_BT", "bound changed: ${bs(device.bondState)}")
            if(device.bondState == 12) openProxy(context)
            if(device.bondState == 10){
                val devs = devices.value
                if(devs != null){
                    devices.value = devs.filterNot { it.device.address == device.address }
                }
            }
        }

    }

    private fun openProxy(context: Context){
        if(!adapter!!.getProfileProxy(context,pl, BluetoothProfile.HEADSET)){
            Log.e("MY_BT", "Error opening bt profile")
        }
    }

    private fun onAclConnected(){
        Log.i("MY_BT", "ACL connected")
    }

    private fun onAclDisconnectRequested(){
        Log.i("MY_BT", "ACL disconnect requested")

    }

    private fun onAclDisconnected(){
        Log.i("MY_BT", "ACL disconnected")
    }

    private fun onFound(device: BluetoothDevice?){
        val l = devices.value
        if(device != null && l != null && l.none { it.device.address == device.address }) {
            devices.value = l + DeviceInfo(device, false)
        }
    }

    private fun onDiscoveryFinished(){
        if(discovering && adapter != null) adapter.startDiscovery()
    }

    private fun serviceUuid(id: Short): UUID =
        UUID(
            id.toLong() shl 32,
            (0x80000080L shl 32) or 0x05f9b34fb
        )

    private fun BluetoothHeadset.myConnect(d: BluetoothDevice) {
        try {
            javaClass.getMethod("connect", BluetoothDevice::class.java).invoke(this, d)
        } catch (e: Exception) {
            Log.e("MY_BT", "Creating bond has been failed. ${e.message}")
        }
    }

    private fun BluetoothHeadset.myDisconnect(d: BluetoothDevice) {
        try {
            javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(this, d)
        } catch (e: Exception) {
            Log.e("MY_BT", "Removing bond has been failed. ${e.message}")
        }
    }

}