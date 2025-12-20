package com.github.andreyasadchy.xtra.ui.stats

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentStatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class StatsFragment : Fragment(R.layout.fragment_stats) {

    private val viewModel: StatsViewModel by viewModels()
    private var binding: FragmentStatsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentStatsBinding.bind(view)
        this.binding = binding

        val adapter = StreamStatsAdapter()
        binding.topStreamsRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.screenTime.collectLatest { screenTimes ->
                        val sb = StringBuilder()
                        val sorted = screenTimes.sortedByDescending { it.date }
                        
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                        val today = sdf.format(Date())
                        
                        sorted.take(7).forEach {
                            val hours = it.totalSeconds / 3600
                            val minutes = (it.totalSeconds % 3600) / 60
                            
                            val dateStr = if (it.date == today) "Today" else {
                                try {
                                    val d = sdf.parse(it.date)
                                    if (d != null) displayFormat.format(d) else it.date
                                } catch (e: Exception) { it.date }
                            }
                            
                            sb.append("$dateStr: ${hours}h ${minutes}m\n")
                        }
                        if (sorted.isEmpty()) {
                            sb.append("No screen time data recorded yet.")
                        }
                        binding.screenTimeText.text = sb.toString()
                    }
                }
                launch {
                    viewModel.topStreams.collectLatest { streams ->
                        adapter.submitList(streams)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
