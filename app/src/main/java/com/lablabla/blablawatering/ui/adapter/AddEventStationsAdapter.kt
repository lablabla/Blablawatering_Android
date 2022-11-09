package com.lablabla.blablawatering.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lablabla.blablawatering.databinding.AddEventStationViewBinding
import com.lablabla.blablawatering.model.Station
import javax.inject.Inject

class AddEventStationsAdapter @Inject constructor(private val stations: List<Station>) :
    RecyclerView.Adapter<AddEventStationsAdapter.SpinnerViewHolder>()
{

    inner class SpinnerViewHolder(private val binding: AddEventStationViewBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(spinner: Spinner) {
            ArrayAdapter(
                binding.addEventStationNameSP.context,
                android.R.layout.simple_spinner_item,
                stations.map { it.name }
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                binding.addEventStationNameSP.adapter = adapter
            }
        }
    }
    private val differCallback = object : DiffUtil.ItemCallback<Spinner>() {
        override fun areItemsTheSame(oldItem: Spinner, newItem: Spinner): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: Spinner, newItem: Spinner): Boolean {
            return false
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpinnerViewHolder {
        val binding = AddEventStationViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpinnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SpinnerViewHolder, position: Int) {
        val spinner = differ.currentList[position]
        holder.bind(spinner)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}