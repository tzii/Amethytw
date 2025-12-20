package com.github.andreyasadchy.xtra.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.stats.ScreenTime
import com.github.andreyasadchy.xtra.model.stats.StreamWatchStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: StatsRepository
) : ViewModel() {

    val screenTime: StateFlow<List<ScreenTime>> = repository.getAllScreenTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topStreams: StateFlow<List<StreamWatchStats>> = repository.getTopWatchedStreams(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
