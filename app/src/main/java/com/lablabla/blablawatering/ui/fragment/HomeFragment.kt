package com.lablabla.blablawatering.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lablabla.blablawatering.R
import com.lablabla.blablawatering.data.repository.Callbacks
import com.lablabla.blablawatering.databinding.FragmentHomeBinding
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station
import com.lablabla.blablawatering.ui.adapter.StationsAdapter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home), Callbacks {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var stationsAdapter: StationsAdapter

    @Inject
    lateinit var btManager: com.lablabla.blablawatering.bluetooth.BluetoothManager

    @Inject
    lateinit var stations: List<Station>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btManager.callbacks = this
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
            showProgressIndicator()
            btManager.sync()
            hideProgressIndicator()
        }

        if (btManager.connected) {
            updateUIDevice(true, btManager.getDeviceName(), btManager.getDeviceAddress())
            btManager.sync()
        }
        else {
            btManager.startScan()
        }

        onUpdateStations(stations)
    }

    private fun setupRecyclerView() {
        stationsAdapter = StationsAdapter()
        stationsAdapter.setOnItemClickListener {
            btManager.setStationState(it, !it.is_on)
        }
        val defaultItemAnimator: DefaultItemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        binding.stationsRecyclerView.apply {
            adapter = stationsAdapter
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = defaultItemAnimator
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

    override fun onStationStateNotification(stations: List<Station>) {
        val list = stationsAdapter.differ.currentList.map { it.copy() }
        list.forEach { current ->
            stations.forEach StationsForEach@ { new ->
                if (current.id == new.id) {
                    current.is_on = new.is_on
                    return@StationsForEach
                }
            }
        }
        onUpdateStations(list)
    }

    override fun onUpdateStations(stations: List<Station>) {
        hideProgressIndicator()
        Timber.d("Updating to ${stations.size} stations")
        activity?.runOnUiThread {
            stationsAdapter.differ.submitList(stations)
        }
    }

    override fun onUpdateEvents(events: List<Event>) {
        hideProgressIndicator()
        Timber.d("Updating to ${events.size} events")
    }

    override fun onDeviceConnected(name: String, address: String) {
        Timber.d("Connected to device $name at $address")
        updateUIDevice(true, name, address)
    }

    override fun onDeviceDisconnected(name: String, address: String) {
        Timber.d("Disconnected from device $name at $address")
        updateUIDevice(false)
        btManager.startScan()
    }
}