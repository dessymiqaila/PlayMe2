package com.example.playme.data.model

enum class PlayOrderMode(val label: String, val shortLabel: String) {
    SHUFFLE("Random / Shuffle", "Shuffle"),
    NAME_AZ("File Name (A-Z)", "A-Z"),
    DATE_MODIFIED("Date (Newest)", "Date"),
    DURATION("Duration", "Duration"),
    FILE_SIZE("File Size", "Size"),
    REPEAT_ONE("Repeat One", "Repeat 1");

    fun next(): PlayOrderMode {
        val values = entries
        val nextIdx = (ordinal + 1) % values.size
        return values[nextIdx]
    }
}
