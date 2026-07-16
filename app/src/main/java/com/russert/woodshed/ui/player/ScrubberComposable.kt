package com.russert.woodshed.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russert.woodshed.data.db.VideoTimestampEntity
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.MutedGreen
import com.russert.woodshed.ui.theme.WarmBrown
import com.russert.woodshed.util.TimeFormatter
import kotlin.math.abs

private const val HIT_RADIUS_PX = 44f  // touch target radius in px

@Composable
fun ScrubberComposable(
    currentTime: Double,
    duration: Double,
    timestamps: List<VideoTimestampEntity>,
    loopEnabled: Boolean,
    loopStart: Double,
    loopEnd: Double,
    splitPoints: List<Double>,
    sectionStart: Double,
    sectionsEnabled: Boolean,
    isQuickLoopActive: Boolean,
    quickLoopIn: Double,
    quickLoopOut: Double,
    isEditMode: Boolean,
    editMarkers: List<Double>,
    onSeek: (Double) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit,
    onQuickLoopInMoved: (Double) -> Unit,
    onQuickLoopOutMoved: (Double) -> Unit,
    onDeactivateQuickLoop: () -> Unit,
    onMoveMarker: (Int, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    var scrubPosition by remember { mutableStateOf<Double?>(null) }

    val progress = if (duration > 0) ((scrubPosition ?: currentTime) / duration).coerceIn(0.0, 1.0) else 0.0

    Column(modifier = modifier.fillMaxWidth()) {
        // Time labels
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = TimeFormatter.formatDetailed(scrubPosition ?: currentTime),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Cream,
            )
            if (isQuickLoopActive) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = TimeFormatter.format(maxOf(0.0, quickLoopOut - quickLoopIn)),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MutedGreen,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = TimeFormatter.format(duration),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Cream.copy(alpha = 0.5f),
            )
        }

        // Track canvas
        val trackHeight = if (isEditMode) 40.dp else 32.dp

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .pointerInput(duration, timestamps, isQuickLoopActive, quickLoopIn, quickLoopOut, isEditMode, editMarkers) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent().changes.firstOrNull() ?: continue
                            if (!down.pressed) continue

                            val startX = down.position.x
                            val width = size.width.toFloat()

                            // Determine drag target
                            var dragTarget: DragTarget = DragTarget.Scrub
                            if (isQuickLoopActive && duration > 0) {
                                val inX = (quickLoopIn / duration * width).toFloat()
                                val outX = (quickLoopOut / duration * width).toFloat()
                                dragTarget = when {
                                    abs(startX - inX) < HIT_RADIUS_PX  -> DragTarget.QuickLoopIn
                                    abs(startX - outX) < HIT_RADIUS_PX -> DragTarget.QuickLoopOut
                                    else -> DragTarget.Scrub
                                }
                            }
                            if (isEditMode && dragTarget == DragTarget.Scrub) {
                                for ((i, markerTime) in editMarkers.withIndex()) {
                                    val markerX = if (duration > 0) (markerTime / duration * width).toFloat() else 0f
                                    if (abs(startX - markerX) < HIT_RADIUS_PX) {
                                        dragTarget = DragTarget.Marker(i)
                                        break
                                    }
                                }
                            }

                            var scrubStarted = false

                            // Process drag
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                val x = change.position.x.coerceIn(0f, width)
                                val pct = if (width > 0) x / width else 0f
                                val time = pct * duration

                                when (dragTarget) {
                                    DragTarget.QuickLoopIn  -> onQuickLoopInMoved(time)
                                    DragTarget.QuickLoopOut -> onQuickLoopOutMoved(time)
                                    is DragTarget.Marker    -> onMoveMarker(dragTarget.index, time)
                                    DragTarget.Scrub -> {
                                        if (!scrubStarted) {
                                            onScrubbingChanged(true)
                                            scrubStarted = true
                                        }
                                        scrubPosition = time
                                    }
                                }

                                if (!change.pressed) {
                                    // Gesture ended
                                    if (dragTarget == DragTarget.Scrub) {
                                        onScrubbingChanged(false)
                                        scrubPosition?.let { onSeek(it) }
                                        scrubPosition = null
                                    }
                                    break
                                }
                                change.consume()
                            }
                        }
                    }
                },
        ) {
            val width = size.width
            val trackY = size.height * 0.75f
            val trackH = 4.dp.toPx()

            // Background track
            drawRoundRect(
                color = WarmBrown.copy(alpha = 0.4f),
                topLeft = Offset(0f, trackY - trackH / 2),
                size = Size(width, trackH),
                cornerRadius = CornerRadius(trackH / 2),
            )

            // Section loop highlight
            if (loopEnabled && duration > 0 && loopEnd > loopStart) {
                val startX = (loopStart / duration * width).toFloat()
                val endX = (loopEnd / duration * width).toFloat()
                drawRoundRect(
                    color = MutedGreen.copy(alpha = 0.5f),
                    topLeft = Offset(startX, trackY - trackH / 2),
                    size = Size(maxOf(0f, endX - startX), trackH),
                    cornerRadius = CornerRadius(trackH / 2),
                )
            }

            // Quick loop fill
            if (isQuickLoopActive && duration > 0 && quickLoopOut > quickLoopIn) {
                val inX = (quickLoopIn / duration * width).toFloat()
                val outX = (quickLoopOut / duration * width).toFloat()
                drawRoundRect(
                    color = MutedGreen.copy(alpha = 0.35f),
                    topLeft = Offset(inX, trackY - 4.dp.toPx()),
                    size = Size(maxOf(0f, outX - inX), 8.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }

            // Played portion
            if (width > 0) {
                drawRoundRect(
                    color = Amber,
                    topLeft = Offset(0f, trackY - trackH / 2),
                    size = Size((progress * width).toFloat(), trackH),
                    cornerRadius = CornerRadius(trackH / 2),
                )
            }

            // Timestamp ticks
            if (duration > 0) {
                for (ts in timestamps) {
                    val x = (ts.timeOffset / duration * width).toFloat()
                    drawRoundRect(
                        color = Cream.copy(alpha = 0.8f),
                        topLeft = Offset(x - 1.dp.toPx(), trackY - 5.dp.toPx()),
                        size = Size(2.dp.toPx(), 10.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx()),
                    )
                }
            }

            // Section markers (non-edit mode)
            if (!isEditMode && sectionsEnabled && duration > 0) {
                val aX = (sectionStart / duration * width).toFloat()
                drawSectionMarker(aX, "A", textMeasurer, trackY)
                for ((i, sp) in splitPoints.withIndex()) {
                    val letter = PlayerUiState.SECTION_LABELS.getOrElse(i + 1) { "?" }
                    val x = (sp / duration * width).toFloat()
                    drawSectionMarker(x, letter, textMeasurer, trackY)
                }
            }

            // Edit mode drag handles
            if (isEditMode && duration > 0) {
                for ((i, markerTime) in editMarkers.withIndex()) {
                    val letter = PlayerUiState.SECTION_LABELS.getOrElse(i) { "?" }
                    val x = (markerTime / duration * width).toFloat()
                    drawEditHandle(x, letter, textMeasurer, trackY)
                }
            }

            // Quick loop handles
            if (isQuickLoopActive && duration > 0) {
                drawQuickLoopHandle((quickLoopIn / duration * width).toFloat(), trackY)
                drawQuickLoopHandle((quickLoopOut / duration * width).toFloat(), trackY)
            }

            // Thumb
            val thumbX = (progress * width).toFloat()
            if (isQuickLoopActive) {
                // Thin playhead when quick loop active
                drawRoundRect(
                    color = Cream.copy(alpha = 0.7f),
                    topLeft = Offset(thumbX - 1.dp.toPx(), trackY - 8.dp.toPx()),
                    size = Size(2.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            } else {
                drawCircle(color = Cream, radius = 8.dp.toPx(), center = Offset(thumbX, trackY))
            }
        }

        if (isQuickLoopActive) {
            Text(
                text = "Quick loop active",
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                color = MutedGreen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// MARK: - Canvas helpers

private fun DrawScope.drawSectionMarker(
    x: Float,
    letter: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    trackY: Float,
) {
    // Amber stem
    drawRect(
        color = Amber,
        topLeft = Offset(x - 1.dp.toPx(), trackY - 7.dp.toPx()),
        size = Size(2.dp.toPx(), 14.dp.toPx()),
    )
    // Floating letter above stem
    val layout = textMeasurer.measure(
        text = letter,
        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Amber, fontFamily = FontFamily.Monospace),
    )
    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, trackY - 7.dp.toPx() - layout.size.height - 2.dp.toPx()))
}

private fun DrawScope.drawEditHandle(
    x: Float,
    letter: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    trackY: Float,
) {
    val capR = 11.dp.toPx()
    val stemH = 20.dp.toPx()
    // Stem
    drawRoundRect(
        color = Amber,
        topLeft = Offset(x - 1.5f.dp.toPx(), trackY - stemH),
        size = Size(3.dp.toPx(), stemH),
        cornerRadius = CornerRadius(1.5f.dp.toPx()),
    )
    // Circle cap
    val capCenter = Offset(x, trackY - stemH - capR)
    drawCircle(color = Amber, radius = capR, center = capCenter)
    // Letter inside cap
    val layout = textMeasurer.measure(
        text = letter,
        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF382114), fontFamily = FontFamily.Monospace),
    )
    drawText(
        layout,
        topLeft = Offset(capCenter.x - layout.size.width / 2f, capCenter.y - layout.size.height / 2f),
    )
}

private fun DrawScope.drawQuickLoopHandle(x: Float, trackY: Float) {
    val stemH = 14.dp.toPx()
    val capR = 5.5f.dp.toPx()
    drawRoundRect(
        color = MutedGreen,
        topLeft = Offset(x - 1.dp.toPx(), trackY - stemH),
        size = Size(2.dp.toPx(), stemH),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )
    drawCircle(color = MutedGreen, radius = capR, center = Offset(x, trackY - stemH - capR))
}

private sealed class DragTarget {
    data object Scrub : DragTarget()
    data object QuickLoopIn : DragTarget()
    data object QuickLoopOut : DragTarget()
    data class Marker(val index: Int) : DragTarget()
}
