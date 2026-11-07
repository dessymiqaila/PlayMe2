package com.example.playme.data.scanner

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.playme.data.db.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleVideoGenerator {
    private const val TAG = "SampleVideoGenerator"

    data class DemoPreset(
        val title: String,
        val rawResId: Int,
        val fileName: String,
        val durationSec: Int
    )

    private val PRESETS = listOf(
        DemoPreset("Neon Waves 🌊", R.raw.demo_neon_waves, "demo_neon_waves.mp4", 6),
        DemoPreset("Sunset Chill 🌅", R.raw.demo_sunset_chill, "demo_sunset_chill.mp4", 6),
        DemoPreset("Carrot Groove 🥕", R.raw.demo_carrot_groove, "demo_carrot_groove.mp4", 7),
        DemoPreset("Emerald Flow 🌿", R.raw.demo_emerald_flow, "demo_emerald_flow.mp4", 6),
        DemoPreset("Cyber Midnight ⚡", R.raw.demo_cyber_midnight, "demo_cyber_midnight.mp4", 8)
    )

    suspend fun generateOrGetDemoVideos(context: Context): List<VideoEntity> = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "demo_videos")
        if (!demoDir.exists()) {
            demoDir.mkdirs()
        }

        val result = mutableListOf<VideoEntity>()

        for (preset in PRESETS) {
            val file = File(demoDir, preset.fileName)
            if (!file.exists() || file.length() < 100) {
                try {
                    context.resources.openRawResource(preset.rawResId).use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying raw demo video: ${preset.fileName}", e)
                }
            }

            if (file.exists() && file.length() > 0) {
                result.add(
                    VideoEntity(
                        uriString = Uri.fromFile(file).toString(),
                        fileName = preset.title,
                        fileSizeBytes = file.length(),
                        durationMs = preset.durationSec * 1000L,
                        dateModified = file.lastModified(),
                        lastPositionMs = 0L
                    )
                )
            }
        }
        result
    }
}
