package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.playme.ui.PlayMeViewModel
import com.example.playme.ui.Screen
import com.example.playme.ui.dashboard.DashboardScreen
import com.example.playme.ui.player.VerticalPlayerScreen
import com.example.ui.theme.PlayMeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PlayMeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PlayMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0F14)
                ) {
                    val context = LocalContext.current
                    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                    // SAF Document Tree Folder Picker Launcher
                    val folderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            try {
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                // Ignore if provider doesn't support persistable permissions
                            }
                            viewModel.onFolderSelected(context, uri)
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Dashboard -> {
                            val videoCount by viewModel.videoCount.collectAsStateWithLifecycle()
                            val totalSizeBytesStr by viewModel.totalSizeBytesStr.collectAsStateWithLifecycle()
                            val activeFolderDisplay by viewModel.activeFolderDisplay.collectAsStateWithLifecycle()
                            val lastScanTimeStr by viewModel.lastScanTimeStr.collectAsStateWithLifecycle()
                            val playlistCounts by viewModel.playlistCounts.collectAsStateWithLifecycle()
                            val lastPlayedIndex by viewModel.lastPlayedIndex.collectAsStateWithLifecycle()
                            val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

                            DashboardScreen(
                                videoCount = videoCount,
                                totalSizeBytesStr = totalSizeBytesStr,
                                activeFolderDisplay = activeFolderDisplay,
                                lastScanTimeStr = lastScanTimeStr,
                                playlistCounts = playlistCounts,
                                lastPlayedIndex = lastPlayedIndex,
                                isScanning = isScanning,
                                onResumeRandom = { viewModel.playResumeRandom() },
                                onNewRandom = { viewModel.playNewRandom() },
                                onRescan = { viewModel.rescanCurrentFolder(context) },
                                onChangeFolder = { folderLauncher.launch(null) },
                                onLoadDemo = { viewModel.loadDemoVideos(context) },
                                onSelectPlaylist = { playlistId -> viewModel.playPlaylist(playlistId) }
                            )
                        }

                        is Screen.Player -> {
                            val playbackVideos by viewModel.playbackVideos.collectAsStateWithLifecycle()
                            val playMode by viewModel.playOrderMode.collectAsStateWithLifecycle()
                            val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
                            val videoPlaylistMap by viewModel.videoPlaylistMap.collectAsStateWithLifecycle()

                            VerticalPlayerScreen(
                                videos = playbackVideos,
                                initialIndex = screen.initialIndex,
                                playMode = playMode,
                                playbackSpeed = playbackSpeed,
                                videoPlaylistMap = videoPlaylistMap,
                                onCyclePlayMode = { viewModel.cyclePlayOrderMode() },
                                onSpeedChanged = { spd -> viewModel.setPlaybackSpeed(spd) },
                                onTogglePlaylist = { videoId, playlistId ->
                                    viewModel.togglePlaylist(videoId, playlistId)
                                },
                                onDeleteVideo = { videoId ->
                                    viewModel.deleteVideo(videoId)
                                },
                                onVideoChanged = { video, idx ->
                                    viewModel.onVideoChanged(video, idx)
                                },
                                onSavePosition = { videoId, pos ->
                                    viewModel.saveVideoPosition(videoId, pos)
                                },
                                onBack = { viewModel.navigateToDashboard() }
                            )
                        }
                    }
                }
            }
        }
    }
}
