package com.example.playme.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.playme.data.db.PlayMeDatabase
import com.example.playme.data.db.VideoEntity
import com.example.playme.data.model.PlayOrderMode
import com.example.playme.data.repository.PlayMeRepository
import com.example.playme.data.scanner.FolderScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Dashboard : Screen()
    data class Player(val initialIndex: Int = 0, val playlistFilter: Int? = null) : Screen()
}

class PlayMeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlayMeRepository

    init {
        val database = PlayMeDatabase.getDatabase(application)
        repository = PlayMeRepository(database.playMeDao())
    }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _playOrderMode = MutableStateFlow(PlayOrderMode.SHUFFLE)
    val playOrderMode: StateFlow<PlayOrderMode> = _playOrderMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    val availableSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun cyclePlaybackSpeed() {
        val current = _playbackSpeed.value
        val idx = availableSpeeds.indexOf(current)
        val nextIdx = if (idx >= 0) (idx + 1) % availableSpeeds.size else 2
        _playbackSpeed.value = availableSpeeds[nextIdx]
    }

    private val _activePlaylistFilter = MutableStateFlow<Int?>(null)
    val activePlaylistFilter: StateFlow<Int?> = _activePlaylistFilter.asStateFlow()

    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoCount: StateFlow<Int> = repository.videoCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSizeBytes: StateFlow<Long?> = repository.totalSizeBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalSizeBytesStr: StateFlow<String> = totalSizeBytes.map { bytes ->
        FolderScanner.formatFileSize(bytes ?: 0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0 B")

    val activeFolderDisplay: StateFlow<String> = repository.getSettingFlow("active_folder_display")
        .map { it ?: "No folder selected" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No folder selected")

    val lastScanTimeStr: StateFlow<String> = repository.getSettingFlow("last_scan_time")
        .map { it ?: "Never scanned" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Never scanned")

    val playlistCounts: StateFlow<Map<Int, Int>> = repository.playlistCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _lastPlayedIndex = MutableStateFlow(0)
    val lastPlayedIndex: StateFlow<Int> = _lastPlayedIndex.asStateFlow()

    private val _activeVideoId = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activePlaylistIdsForCurrentVideo: StateFlow<Set<Int>> = _activeVideoId.flatMapLatest { id ->
        if (id != null) {
            repository.getPlaylistIdsForVideo(id).map { it.toSet() }
        } else {
            flowOf(emptySet())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Filtered and sorted videos for playback
    val playbackVideos: StateFlow<List<VideoEntity>> = combine(
        allVideos,
        _activePlaylistFilter,
        _playOrderMode,
        playlistCounts // triggers re-eval when playlist changes
    ) { videos, filter, mode, _ ->
        val candidateVideos = if (filter != null) {
            // Need videos in this playlist
            // Filter by cross-ref
            videos.filter { video ->
                repository.isVideoInPlaylist(video.id, filter)
            }
        } else {
            videos
        }

        when (mode) {
            PlayOrderMode.SHUFFLE -> candidateVideos.shuffled()
            PlayOrderMode.NAME_AZ -> candidateVideos.sortedBy { it.fileName.lowercase() }
            PlayOrderMode.DATE_MODIFIED -> candidateVideos.sortedByDescending { it.dateModified }
            PlayOrderMode.DURATION -> candidateVideos.sortedByDescending { it.durationMs }
            PlayOrderMode.FILE_SIZE -> candidateVideos.sortedByDescending { it.fileSizeBytes }
            PlayOrderMode.REPEAT_ONE -> candidateVideos
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load initial index and play order mode from settings
        viewModelScope.launch {
            val savedIdx = repository.getSetting("last_played_index")?.toIntOrNull() ?: 0
            _lastPlayedIndex.value = savedIdx
            val savedMode = repository.getSetting("play_order_mode")
            if (savedMode != null) {
                PlayOrderMode.entries.find { it.name == savedMode }?.let {
                    _playOrderMode.value = it
                }
            }
        }
    }

    fun onFolderSelected(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanFolder(context, treeUri)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun rescanCurrentFolder(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val uriStr = repository.getSetting("active_folder_uri")
                if (!uriStr.isNullOrEmpty() && uriStr != "demo://local_samples") {
                    val uri = Uri.parse(uriStr)
                    repository.scanFolder(context, uri)
                } else if (uriStr == "demo://local_samples") {
                    repository.seedDemoVideos(context)
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun loadDemoVideos(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.seedDemoVideos(context)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun playAllVideos() {
        val targetIndex = _lastPlayedIndex.value.coerceIn(0, (allVideos.value.size - 1).coerceAtLeast(0))
        _activePlaylistFilter.value = null
        _currentScreen.value = Screen.Player(initialIndex = targetIndex, playlistFilter = null)
    }

    fun playPlaylist(playlistId: Int) {
        _activePlaylistFilter.value = playlistId
        _currentScreen.value = Screen.Player(initialIndex = 0, playlistFilter = playlistId)
    }

    fun cyclePlayOrderMode() {
        val next = _playOrderMode.value.next()
        _playOrderMode.value = next
        viewModelScope.launch {
            repository.saveSetting("play_order_mode", next.name)
        }
    }

    fun togglePlaylist(videoId: Long, playlistId: Int) {
        viewModelScope.launch {
            repository.toggleVideoInPlaylist(videoId, playlistId)
        }
    }

    fun onVideoChanged(video: VideoEntity, index: Int) {
        _activeVideoId.value = video.id
        _lastPlayedIndex.value = index
        viewModelScope.launch {
            repository.saveSetting("last_played_index", index.toString())
            repository.saveSetting("last_played_video_id", video.id.toString())
        }
    }

    fun saveVideoPosition(videoId: Long, positionMs: Long) {
        viewModelScope.launch {
            repository.updateLastPosition(videoId, positionMs)
        }
    }

    fun navigateToDashboard() {
        _currentScreen.value = Screen.Dashboard
    }
}
