package com.lablabla.blablawatering.ui.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lablabla.blablawatering.databinding.EventListItemViewBinding
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.util.fadeVisibility


class EventAdapter: RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: EventListItemViewBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Event) {
            binding.nameTV.text = event.name
            val visibility = if (event.isExpanded) View.VISIBLE else View.GONE
            binding.expandableSection.fadeVisibility(visibility)

            val anim: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.expandIcon, "rotation", event.rotationAngle,
                    (event.rotationAngle + 180)
                )
            anim.duration = 500
            anim.start()
            event.rotationAngle += 180
            event.rotationAngle %= 360

        }

    }
    
    private val differCallback = object : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.id == newItem.id &&
                    oldItem.cron_expr == newItem.cron_expr &&
                    oldItem.duration == newItem.duration &&
                    oldItem.station_ids == newItem.station_ids
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = EventListItemViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = differ.currentList[position]
        holder.bind(event)

        holder.itemView.setOnClickListener {
            event.isExpanded = !event.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}