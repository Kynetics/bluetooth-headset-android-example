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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.kynetics.bttester.databinding.FragmentFirstBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var btModel: BtModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btModel = ViewModelProvider(requireActivity()).get(BtModel::class.java)
        binding.buttonFirst.setOnClickListener() {
            btModel.bindPhone()
        }
        binding.deviceList.setOnItemClickListener { parent, _, pos, _ ->
            val btAddress = (parent.getItemAtPosition(pos) as String).lines()[1]
            btModel.selectDevice(btAddress)
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        val devicesObserver = Observer<List<BtModel.DeviceInfo>> { newDevices ->
            if(newDevices != null){
                val deviceLabels = newDevices.map {
                 """|${it.device.name?:"Unknown Name"}
                    |${it.device.address}
                    |BOUND: ${it.bound}
                """.trimMargin() }
                binding.deviceList.adapter = ArrayAdapter(requireContext(),R.layout.device_list_item,R.id.textview_item,deviceLabels)
            }
        }
        btModel.devices.observe(viewLifecycleOwner, devicesObserver)
    }

    override fun onStart() {
        super.onStart()
        btModel.startDiscovering()
    }

    override fun onStop() {
        btModel.stopDiscovering()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}