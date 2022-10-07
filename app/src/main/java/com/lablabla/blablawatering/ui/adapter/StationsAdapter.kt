package com.lablabla.blablawatering.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lablabla.blablawatering.databinding.StationsCardViewBinding
import com.lablabla.blablawatering.model.Station

class StationsAdapter : RecyclerView.Adapter<StationsAdapter.StationViewHolder>() {

    inner class StationViewHolder(val binding: StationsCardViewBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(station: Station) {
            binding.stationsCVName.text = station.name
            binding.stationsCVStatus.text = "Status: On"
            onItemClickListener?.let {
                it(station)
            }
        }
    }
    private val differCallback = object : DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.name == newItem.name &&
                    oldItem.gpio_pin == newItem.gpio_pin &&
                    oldItem.is_on == newItem.is_on
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = StationsCardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = differ.currentList[position]
        holder.binding.stationsCVName.text = "Station Name: ${station.name}"
        var isOn = "Off"
        if (station.is_on) {
            isOn = "On"
        }
        holder.binding.stationsCVStatus.text = "Status:  $isOn"
        holder.binding.root.setOnClickListener {
            onItemClickListener?.let {
                it(station)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Station) -> Unit)? = null

    fun setOnItemClickListener(listener: (Station) -> Unit) {
        onItemClickListener = listener
    }
}