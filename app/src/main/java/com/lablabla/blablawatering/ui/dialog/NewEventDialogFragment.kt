package com.lablabla.blablawatering.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.lablabla.blablawatering.R
import com.lablabla.blablawatering.databinding.AddNewEventBinding
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.ui.fragment.EVENT_BUNDLE_KEY
import com.lablabla.blablawatering.ui.fragment.EVENT_REQUEST_KEY
import kotlin.experimental.or

class NewEventDialogFragment: DialogFragment() {

    private val SUNDAY_MASK: Byte =       0b00000001
    private val MONDAY_MASK: Byte =       0b00000010
    private val TUESDAY_MASK: Byte =      0b00000100
    private val WEDNSEDAY_MASK: Byte =    0b00001000
    private val THURSDAY_MASK: Byte =     0b00010000
    private val FRIDAY_MASK: Byte =       0b00100000
    private val SATURDAY_MASK: Byte =     0b01000000

    private lateinit var binding: AddNewEventBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val inflater = requireActivity().layoutInflater;

            val view = inflater.inflate(R.layout.add_new_event, null)
            binding = AddNewEventBinding.bind(view)
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.addNewEvent)
                .setView(view)
                .setPositiveButton(R.string.save
                ) { _, _ ->
                    val event = Event(
                        name = binding.nameEt.text.toString(),
                        stations = listOf(),
                        days = getDaysFromCheckboxes(),
                        startHours = 0,
                        startMinutes = 0,
                        endHours = 0,
                        endMinutes = 0
                    )
                    setFragmentResult(EVENT_REQUEST_KEY, bundleOf(EVENT_BUNDLE_KEY to event))
                }
                .setNegativeButton(R.string.cancel
                ) { _, _ ->
                    setFragmentResult(EVENT_REQUEST_KEY, bundleOf(EVENT_BUNDLE_KEY to null))
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
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