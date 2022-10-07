package com.lablabla.blablawatering.ui.fragment

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lablabla.blablawatering.R
import com.lablabla.blablawatering.bluetooth.BlablaBTCallback
import com.lablabla.blablawatering.databinding.FragmentHomeBinding
import com.lablabla.blablawatering.model.Station
import com.lablabla.blablawatering.ui.adapter.StationsAdapter
import com.lablabla.blablawatering.util.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home), BlablaBTCallback {

    private val TAG = "HomeFragment"
    private lateinit var binding: FragmentHomeBinding
    private lateinit var stationsAdapter: StationsAdapter

    @Inject
    lateinit var btManager: com.lablabla.blablawatering.bluetooth.BluetoothManager

    val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(
            requireActivity(),
            BluetoothManager::class.java
        ) as BluetoothManager
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
//            shouldShowRequestPermissionRationale(...) -> {
//            // In an educational UI, explain to the user why your app requires this
//            // permission for a specific feature to behave as expected. In this UI,
//            // include a "cancel" or "no thanks" button that allows the user to
//            // continue using your app without granting the permission.
//            showInContextUI(...)
//        }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        bluetoothManager.adapter
    }

    private var activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.e(TAG, "On Activity Result OK")
            // TODO: Check if it's ACTION_REQUEST_ENABLE and if so, update own BT manager
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                btManager.startScan()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btManager.btCallback = this
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(enableBtIntent);
        }
        else
        {
            btManager.bluetoothAdapter = bluetoothAdapter
        }
    }

    private fun showProgressIndicator() {
        if(this::binding.isInitialized) {
            activity?.runOnUiThread {
                binding.progressIndicator.visibility = View.VISIBLE
            }
        }
    }

    private fun hideProgressIndicator() {
        if(this::binding.isInitialized) {
            activity?.runOnUiThread {
                binding.progressIndicator.visibility = View.INVISIBLE
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        showProgressIndicator()
        setupRecyclerView()

        updateUIDevice(false)
        binding.syncButton.setOnClickListener {
            btManager.sync()
        }

        if (btManager.connected) {
            updateUIDevice(true, btManager.getDeviceName(), btManager.getDeviceAddress())
            btManager.sync()
        }
        else {
            btManager.startScan()
        }

        // Debug: TOOO: Remove
        val stations = listOf(
            Station(0, 0, false, "Test Station 1"),
            Station(1, 1, true, "Test Station 2"),
        )
        onUpdateStations(stations)
    }

    private fun setupRecyclerView() {
        stationsAdapter = StationsAdapter()
        binding.stationsRecyclerView.apply {
            adapter = stationsAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun updateUIDevice(connected: Boolean, name: String? = null, address: String? = null) {
        activity?.runOnUiThread {
            binding.syncButton.isEnabled = connected
            binding.deviceNameTV.isEnabled = connected
            name?.let {
                binding.deviceNameTV.text = it
            }
            binding.deviceAddrTV.isEnabled = connected
            address?.let {
                binding.deviceAddrTV.text = it
            }
        }
    }

    override fun onUpdateStations(stations: List<Station>) {
        hideProgressIndicator()
        Log.d(TAG, "Updating to ${stations.size} stations")
        activity?.runOnUiThread {
            stationsAdapter.differ.submitList(stations)
        }
    }

    override fun onDeviceConnected(name: String, address: String) {
        Log.d(TAG, "Connected to device $name at $address")
        updateUIDevice(true, name, address)
    }

    override fun onDeviceDisconnected(name: String, address: String) {
        Log.d(TAG, "Disconnected from device $name at $address")
        updateUIDevice(false)
    }
}