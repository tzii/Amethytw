package com.github.andreyasadchy.xtra.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.databinding.ItemStreamStatBinding
import com.github.andreyasadchy.xtra.model.stats.StreamWatchStats

class StreamStatsAdapter : ListAdapter<StreamWatchStats, StreamStatsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStreamStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(private val binding: ItemStreamStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StreamWatchStats, rank: Int) {
            binding.rank.text = rank.toString()
            binding.channelName.text = item.channelName
            val hours = item.totalSecondsWatched / 3600
            val minutes = (item.totalSecondsWatched % 3600) / 60
            binding.watchTime.text = String.format("%dh %02dm", hours, minutes)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StreamWatchStats>() {
        override fun areItemsTheSame(oldItem: StreamWatchStats, newItem: StreamWatchStats): Boolean {
            return oldItem.channelId == newItem.channelId
        }

        override fun areContentsTheSame(oldItem: StreamWatchStats, newItem: StreamWatchStats): Boolean {
            return oldItem == newItem
        }
    }
}
