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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.kynetics.bttester.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var btModel: BtModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btModel = ViewModelProvider(requireActivity()).get(BtModel::class.java)

        binding.buttonSecond.setOnClickListener {
            btModel.unbindPhone()
            btModel.unpairSelectedDevice()
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        val connect = binding.buttonConnect
        val disconnect = binding.buttonDisconnect

        val connectedObserver = Observer<Boolean> { isConnected ->
            Log.i("MY_BT", "$isConnected")
            connect.isEnabled = !isConnected
            disconnect.isEnabled = isConnected
        }

        connect.setOnClickListener { btModel.connect() }

        disconnect.setOnClickListener { btModel.disconnect() }

        btModel.isConnected().observe(viewLifecycleOwner, connectedObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}