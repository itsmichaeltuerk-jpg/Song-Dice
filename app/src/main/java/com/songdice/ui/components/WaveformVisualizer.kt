package com.songdice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Interactive waveform visualizer and playback scrubber component.
 * Renders audio amplitude bars and responds to tap/drag seek gestures.
 */
@Composable
fun WaveformVisualizer(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    waveformData: List<Float> = emptyList(),
    isPlaying: Boolean = false,
    barCount: Int = 40
) {
    val clampedProgress = progress.coerceIn(0.0f, 1.0f)

    // Generate default waveform profile if data is not supplied
    val displayData = remember(waveformData, barCount) {
        if (waveformData.isNotEmpty()) {
            waveformData
        } else {
            List(barCount) { idx ->
                val norm = idx.toFloat() / barCount.toFloat()
                (0.3f + 0.6f * abs(sin(norm * Math.PI.toFloat() * 3.5f)) + ((idx % 3) * 0.1f)).coerceIn(0.15f, 1.0f)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeBarColor = if (isPlaying) MaterialTheme.colorScheme.tertiary else primaryColor
    val inactiveBarColor = MaterialTheme.colorScheme.outlineVariant
    val playheadColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("waveform_visualizer"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waveform Preview",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${(clampedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("progress_text")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("waveform_scrubber")
                    .semantics {
                        contentDescription = "Waveform playback scrubber, progress ${(clampedProgress * 100).toInt()}%"
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val seekFraction = (offset.x / size.width.toFloat()).coerceIn(0.0f, 1.0f)
                            onSeek(seekFraction)
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val seekFraction = (change.position.x / size.width.toFloat()).coerceIn(0.0f, 1.0f)
                            onSeek(seekFraction)
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val numBars = displayData.size
                    val spacing = 4.dp.toPx()
                    val totalSpacing = spacing * (numBars - 1)
                    val barWidth = ((width - totalSpacing) / numBars).coerceAtLeast(2.dp.toPx())

                    val currentX = clampedProgress * width

                    displayData.forEachIndexed { idx, amp ->
                        val barHeight = (amp.coerceIn(0.1f, 1.0f) * height * 0.85f).coerceAtLeast(4.dp.toPx())
                        val barLeft = idx * (barWidth + spacing)
                        val barTop = (height - barHeight) / 2.0f

                        val isPlayed = (barLeft + barWidth / 2.0f) <= currentX
                        val color = if (isPlayed) activeBarColor else inactiveBarColor

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }

                    // Draw playback head
                    drawRect(
                        color = playheadColor,
                        topLeft = Offset((currentX - 1.5.dp.toPx()).coerceAtLeast(0f), 0f),
                        size = Size(3.dp.toPx(), height)
                    )
                }
            }
        }
    }
}
