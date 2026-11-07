package com.example.playme.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.playme.data.db.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FolderScanner {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "3gp", "mov", "avi", "ts", "flv", "m4v"
    )

    data class ScanResult(
        val videos: List<VideoEntity>,
        val folderDisplayName: String,
        val scanTimestamp: String,
        val totalBytes: Long
    )

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val formatted = String.format(Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "--:--"
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hours = min / 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, min % 60, sec)
        } else {
            String.format(Locale.US, "%02d:%02d", min, sec)
        }
    }

    fun formatTimestamp(epochMs: Long): String {
        if (epochMs <= 0) return "Never scanned"
        val sdf = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.US)
        return sdf.format(Date(epochMs))
    }

    suspend fun scanFolderUri(context: Context, treeUri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        val folderDisplayName = rootDoc?.name ?: treeUri.lastPathSegment ?: "Selected Folder"
        val videos = mutableListOf<VideoEntity>()
        var totalBytes = 0L

        // Attempt direct ContentResolver query via childDocumentsUriUsingTree for high-performance indexing
        val queried = tryQueryChildDocuments(context, treeUri)
        if (queried.isNotEmpty()) {
            for (v in queried) {
                videos.add(v)
                totalBytes += v.fileSizeBytes
            }
        } else if (rootDoc != null && rootDoc.isDirectory) {
            // Fallback to DocumentFile tree traversal
            val files = rootDoc.listFiles()
            for (file in files) {
                if (!file.isDirectory && isVideoFile(file.name, file.type)) {
                    val duration = extractDuration(context, file.uri)
                    val size = file.length()
                    totalBytes += size
                    videos.add(
                        VideoEntity(
                            uriString = file.uri.toString(),
                            fileName = file.name ?: "Untitled Video",
                            fileSizeBytes = size,
                            durationMs = duration,
                            dateModified = file.lastModified(),
                            lastPositionMs = 0L
                        )
                    )
                }
            }
        }

        val scanTimeStr = formatTimestamp(System.currentTimeMillis())
        ScanResult(
            videos = videos,
            folderDisplayName = folderDisplayName,
            scanTimestamp = scanTimeStr,
            totalBytes = totalBytes
        )
    }

    private fun isVideoFile(name: String?, mimeType: String?): Boolean {
        if (mimeType?.startsWith("video/") == true) return true
        if (name == null) return false
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in VIDEO_EXTENSIONS
    }

    private fun tryQueryChildDocuments(context: Context, treeUri: Uri): List<VideoEntity> {
        val list = mutableListOf<VideoEntity>()
        try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx)
                    val mime = cursor.getString(mimeIdx)
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val mod = if (modIdx >= 0) cursor.getLong(modIdx) else System.currentTimeMillis()

                    if (isVideoFile(name, mime)) {
                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val duration = extractDuration(context, fileDocUri)
                        list.add(
                            VideoEntity(
                                uriString = fileDocUri.toString(),
                                fileName = name ?: "Video $docId",
                                fileSizeBytes = size,
                                durationMs = duration,
                                dateModified = mod,
                                lastPositionMs = 0L
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            list.clear()
        }
        return list
    }

    private fun extractDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
