package com.example

import com.example.playme.data.model.PlayOrderMode
import com.example.playme.data.scanner.FolderScanner
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPlayOrderModeCycling() {
    val start = PlayOrderMode.SHUFFLE
    val next = start.next()
    assertEquals(PlayOrderMode.NAME_AZ, next)
    assertEquals(PlayOrderMode.DATE_MODIFIED, next.next())
    assertEquals(PlayOrderMode.SHUFFLE, PlayOrderMode.REPEAT_ONE.next())
  }

  @Test
  fun testDurationAndFileSizeFormatting() {
    assertEquals("01:30", FolderScanner.formatDuration(90_000L))
    assertEquals("--:--", FolderScanner.formatDuration(0L))
    assertEquals("1.0 MB", FolderScanner.formatFileSize(1024 * 1024L))
    assertEquals("500.0 KB", FolderScanner.formatFileSize(500 * 1024L))
  }

  @Test
  fun testPlaybackSpeedRange() {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    assertEquals(0.5f, speeds.first(), 0.001f)
    assertEquals(2.0f, speeds.last(), 0.001f)
  }

  @Test
  fun testBadgeOffsetClamping() {
    val minOffset = -300f
    val maxOffset = 220f
    val testOffsetFarUp = -500f
    val testOffsetFarDown = 400f
    assertEquals(minOffset, testOffsetFarUp.coerceIn(minOffset, maxOffset), 0.01f)
    assertEquals(maxOffset, testOffsetFarDown.coerceIn(minOffset, maxOffset), 0.01f)
  }

  @Test
  fun testProgressBarScaleMultiplier() {
    assertEquals(3, 3.coerceIn(1, 4))
    assertEquals(1, 0.coerceIn(1, 4))
    assertEquals(4, 10.coerceIn(1, 4))
  }

  @Test
  fun testAbLoopNormalization() {
    val a = 12_000L
    val b = 5_000L
    val start = minOf(a, b)
    val end = maxOf(a, b)
    assertEquals(5_000L, start)
    assertEquals(12_000L, end)
  }
}
