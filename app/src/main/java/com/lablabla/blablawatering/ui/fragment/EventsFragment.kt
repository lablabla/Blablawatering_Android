package com.lablabla.blablawatering.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lablabla.blablawatering.R
import com.lablabla.blablawatering.databinding.FragmentEventsBinding
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.ui.adapter.EventAdapter
import com.lablabla.blablawatering.util.navigateSafe

val EVENT_REQUEST_KEY = "event_request"
val EVENT_BUNDLE_KEY = "event_bundle"

class EventsFragment : Fragment(R.layout.fragment_events) {

    private lateinit var binding: FragmentEventsBinding
    private lateinit var eventsAdapter: EventAdapter
    private var eventsList: MutableList<Event> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEventsBinding.bind(view)
        setupRecyclerView()

        val navController = findNavController()
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Event>("new_event")?.observe(
            viewLifecycleOwner) { event ->
            eventsList.add(event)
            eventsAdapter.differ.submitList(eventsList.toMutableList())
        }
        binding.addEventFAB.setOnClickListener {
            navController.navigateSafe(R.id.action_eventsFragment_to_newEventFragment)
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventAdapter()
        binding.eventsRecyclerView.apply {
            adapter = eventsAdapter
            val manager = LinearLayoutManager(activity)
            layoutManager = manager
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    manager.orientation
                )
            )
        }

//        eventsList.add(Event("Event 1", listOf(), 32, 1, 2, 3, 4))
//        eventsList.add(Event("Event 2", listOf(),1, 2,3, 4, 5))

        eventsAdapter.differ.submitList(eventsList)
    }
}