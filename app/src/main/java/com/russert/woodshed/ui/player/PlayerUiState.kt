package com.russert.woodshed.ui.player

import com.russert.woodshed.data.db.RecordingEntity
import com.russert.woodshed.data.db.VideoTimestampEntity

private val SECTION_LABELS = listOf("A", "B", "C", "D", "E", "F", "G", "H")

data class PlayerUiState(
    val recording: RecordingEntity? = null,
    val currentTime: Double = 0.0,
    val duration: Double = 1.0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isScrubbing: Boolean = false,

    // Section loop
    val loopEnabled: Boolean = false,
    val sectionsEnabled: Boolean = false,
    val sectionStart: Double = 0.0,
    val splitPoints: List<Double> = emptyList(),
    val activeSection: Int? = null,

    // Quick loop (device-local, independent of sections)
    val isQuickLoopActive: Boolean = false,
    val quickLoopIn: Double = 0.0,
    val quickLoopOut: Double = 0.0,

    val timestamps: List<VideoTimestampEntity> = emptyList(),
) {
    val sectionCount: Int get() = if (sectionsEnabled) splitPoints.size + 1 else 0

    private val sectionBoundaries: List<Double>
        get() = listOf(sectionStart) + splitPoints + listOf(duration)

    val loopStart: Double
        get() {
            val s = activeSection ?: return 0.0
            return sectionBoundaries.getOrElse(s.coerceAtMost(sectionCount - 1)) { 0.0 }
        }

    val loopEnd: Double
        get() {
            val s = activeSection ?: return 0.0
            val idx = s.coerceAtMost(sectionCount - 1)
            return sectionBoundaries.getOrElse(idx + 1) { duration }
        }

    fun sectionRange(index: Int): Pair<Double, Double> {
        val clamped = index.coerceIn(0, sectionBoundaries.size - 2)
        return sectionBoundaries[clamped] to sectionBoundaries[clamped + 1]
    }

    companion object {
        val SECTION_LABELS = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    }
}
