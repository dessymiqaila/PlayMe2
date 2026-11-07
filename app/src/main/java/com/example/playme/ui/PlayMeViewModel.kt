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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import kotlin.random.Random

sealed class Screen {
    object Dashboard : Screen()
    data class Player(val initialIndex: Int = 0, val playlistFilter: Int? = null) : Screen()
}

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val _shuffleSeed = MutableStateFlow(System.currentTimeMillis())

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

    val videoPlaylistMap: StateFlow<Map<Long, Set<Int>>> = repository.videoPlaylistMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _lastPlayedIndex = MutableStateFlow(0)
    val lastPlayedIndex: StateFlow<Int> = _lastPlayedIndex.asStateFlow()

    private val _activeVideoId = MutableStateFlow<Long?>(null)

    val activePlaylistIdsForCurrentVideo: StateFlow<Set<Int>> = combine(
        _activeVideoId,
        videoPlaylistMap
    ) { activeId, map ->
        if (activeId != null) {
            map[activeId] ?: emptySet()
        } else {
            emptySet()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Base source videos cleanly switches between Playlist and All Videos
    private val baseSourceVideos: Flow<List<VideoEntity>> = _activePlaylistFilter.flatMapLatest { filter ->
        if (filter != null) {
            repository.getVideosForPlaylist(filter)
        } else {
            repository.allVideos
        }
    }

    // Filtered and sorted videos for playback
    val playbackVideos: StateFlow<List<VideoEntity>> = combine(
        baseSourceVideos,
        _playOrderMode,
        _shuffleSeed
    ) { source, mode, seed ->
        when (mode) {
            PlayOrderMode.SHUFFLE -> source.shuffled(Random(seed))
            PlayOrderMode.NAME_AZ -> source.sortedBy { it.fileName.lowercase() }
            PlayOrderMode.DATE_MODIFIED -> source.sortedByDescending { it.dateModified }
            PlayOrderMode.DURATION -> source.sortedByDescending { it.durationMs }
            PlayOrderMode.FILE_SIZE -> source.sortedByDescending { it.fileSizeBytes }
            PlayOrderMode.REPEAT_ONE -> source
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

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun cyclePlaybackSpeed() {
        val current = _playbackSpeed.value
        val idx = availableSpeeds.indexOf(current)
        val nextIdx = if (idx >= 0) (idx + 1) % availableSpeeds.size else 2
        _playbackSpeed.value = availableSpeeds[nextIdx]
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

    fun playResumeRandom() {
        val count = allVideos.value.size
        val targetIndex = if (count > 0) _lastPlayedIndex.value.coerceIn(0, count - 1) else 0
        _activePlaylistFilter.value = null
        _playOrderMode.value = PlayOrderMode.SHUFFLE
        _currentScreen.value = Screen.Player(initialIndex = targetIndex, playlistFilter = null)
    }

    fun playNewRandom() {
        _shuffleSeed.value = System.currentTimeMillis()
        _activePlaylistFilter.value = null
        _playOrderMode.value = PlayOrderMode.SHUFFLE
        _lastPlayedIndex.value = 0
        viewModelScope.launch {
            repository.saveSetting("last_played_index", "0")
        }
        _currentScreen.value = Screen.Player(initialIndex = 0, playlistFilter = null)
    }

    fun playPlaylist(playlistId: Int) {
        _activePlaylistFilter.value = playlistId
        _currentScreen.value = Screen.Player(initialIndex = 0, playlistFilter = playlistId)
    }

    fun cyclePlayOrderMode() {
        val next = _playOrderMode.value.next()
        _playOrderMode.value = next
        if (next == PlayOrderMode.SHUFFLE) {
            _shuffleSeed.value = System.currentTimeMillis()
        }
        viewModelScope.launch {
            repository.saveSetting("play_order_mode", next.name)
        }
    }

    fun togglePlaylist(videoId: Long, playlistId: Int) {
        viewModelScope.launch {
            repository.toggleVideoInPlaylist(videoId, playlistId)
        }
    }

    fun deleteVideo(videoId: Long) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
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
