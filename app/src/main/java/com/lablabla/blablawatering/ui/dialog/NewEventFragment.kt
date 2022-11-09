package com.lablabla.blablawatering.ui.dialog

import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.view.View
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.lablabla.blablawatering.R
import com.lablabla.blablawatering.databinding.AddNewEventBinding
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station
import com.lablabla.blablawatering.ui.adapter.AddEventStationsAdapter
import com.lablabla.blablawatering.ui.fragment.EVENT_BUNDLE_KEY
import com.lablabla.blablawatering.ui.fragment.EVENT_REQUEST_KEY
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.experimental.or

@AndroidEntryPoint
class NewEventFragment: Fragment(R.layout.add_new_event) {

    private val SUNDAY_MASK: Byte =       0b00000001
    private val MONDAY_MASK: Byte =       0b00000010
    private val TUESDAY_MASK: Byte =      0b00000100
    private val WEDNSEDAY_MASK: Byte =    0b00001000
    private val THURSDAY_MASK: Byte =     0b00010000
    private val FRIDAY_MASK: Byte =       0b00100000
    private val SATURDAY_MASK: Byte =     0b01000000

    @Inject
    lateinit var stations: List<Station>

    private lateinit var binding: AddNewEventBinding
    private lateinit var stationsAdapter: AddEventStationsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = AddNewEventBinding.bind(view)
        setupRecyclerView()

        binding.newEventSaveBtn.setOnClickListener {
            setFragmentResult(EVENT_REQUEST_KEY, bundleOf(
                EVENT_BUNDLE_KEY to Event(
                    binding.nameEt.text.toString(),
                    listOf(),
                    getDaysFromCheckboxes(),
                    1,
                    2,
                    3,
                    4
                )))
            findNavController().navigateUp()
        }

        binding.newEventCancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.startTimeButton.setOnClickListener {
            showPickerUpdateET("Set Start Time", binding.startTimeET)
        }

        binding.endTimeButton.setOnClickListener {
            showPickerUpdateET("Set End Time", binding.endTimeET)
        }

        binding.addStationFAB.setOnClickListener {
            val list = stationsAdapter.differ.currentList.toMutableList()
            list.add(Spinner(binding.addStationFAB.context))
            stationsAdapter.differ.submitList(list)
        }
    }

    private fun setupRecyclerView() {
        stationsAdapter = AddEventStationsAdapter(stations)
        binding.stationsRecyclerView.apply {
            adapter = stationsAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun showPickerUpdateET(title: String, et: TextInputEditText) {
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val now = LocalDateTime.now()
        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(now.hour)
                .setMinute(now.minute)
                .setTitleText(title)
                .build()
        picker.show(childFragmentManager, picker.toString())
        picker.addOnPositiveButtonClickListener {
            val newHour: Int = picker.hour
            val newMinute: Int = picker.minute
            et.setText("$newHour:$newMinute")
        }
    }

    private fun getDaysFromCheckboxes(): Byte {
        var byte: Byte = 0
        byte = byte or if (binding.sundayCb.isChecked)  SUNDAY_MASK else 0
        byte = byte or if (binding.mondayCb.isChecked) MONDAY_MASK else 0
        byte = byte or if (binding.tuesdayCb.isChecked) TUESDAY_MASK else 0
        byte = byte or if (binding.wednesdayCb.isChecked) WEDNSEDAY_MASK else 0
        byte = byte or if (binding.thursdayCb.isChecked) THURSDAY_MASK else 0
        byte = byte or if (binding.fridayCb.isChecked) FRIDAY_MASK else 0
        byte = byte or if (binding.saturdayCb.isChecked) SATURDAY_MASK else 0
        return byte
    }
}