package com.lablabla.blablawatering.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.lablabla.blablawatering.R
import com.lablabla.blablawatering.databinding.FragmentStationBinding

class StationFragment : Fragment(R.layout.fragment_station) {

//    val args: StationFragmentArgs by navArgs()
    private lateinit var binding: FragmentStationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStationBinding.bind(view)

//        val station = args.station
//        binding.sfTextView.text = station.name
    }
}