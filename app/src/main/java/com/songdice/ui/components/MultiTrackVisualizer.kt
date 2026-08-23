package com.songdice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.example.songdice.data.model.InstrumentTrack

data class VisualizerLaneInfo(
    val channel: Int,
    val name: String,
    val subtitle: String,
    val color: Color
)

val DEFAULT_VISUALIZER_LANES = listOf(
    VisualizerLaneInfo(0, "Chords", "Ch 0 • Keys", Color(0xFF00E5FF)),
    VisualizerLaneInfo(1, "Bassline", "Ch 1 • Low-End", Color(0xFF7C4DFF)),
    VisualizerLaneInfo(2, "Lead", "Ch 2 • Melody", Color(0xFFFF4081)),
    VisualizerLaneInfo(9, "Drums", "Ch 9 • Percussion", Color(0xFFFFAB00))
)

/**
 * 4-Lane Interactive Multi-Track Waveform & Sequencer Visualizer.
 * Renders individual stem track lanes (Ch 0, Ch 1, Ch 2, Ch 9),
 * synchronized playhead cursor, tap-to-seek, and inline Solo/Mute toggles.
 */
@Composable
fun MultiTrackVisualizer(
    tracks: List<InstrumentTrack>?,
    progress: Float,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean,
    mutedChannels: Set<Int>,
    soloedChannels: Set<Int>,
    onMuteToggle: (channel: Int) -> Unit,
    onSoloToggle: (channel: Int) -> Unit,
    modifier: Modifier = Modifier,
    lanes: List<VisualizerLaneInfo> = DEFAULT_VISUALIZER_LANES,
    totalBeats: Double = 16.0
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_track_visualizer"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visualizer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MULTI-TRACK WAVEFORM VISUALIZER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "4-Lane Interactive Stem Sequences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4 Track Lanes Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                lanes.forEach { lane ->
                    val isMuted = mutedChannels.contains(lane.channel)
                    val isSoloed = soloedChannels.contains(lane.channel)
                    val trackData = tracks?.firstOrNull { it.channel == lane.channel }

                    val trackColor = when {
                        isMuted -> lane.color.copy(alpha = 0.25f)
                        isSoloed -> lane.color
                        else -> lane.color.copy(alpha = 0.85f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_lane_${lane.channel}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Lane Top Row: Channel Label + Inline Mute & Solo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(trackColor, shape = RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lane.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${lane.subtitle})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Inline Mute FilterChip
                                    FilterChip(
                                        selected = isMuted,
                                        onClick = { onMuteToggle(lane.channel) },
                                        label = {
                                            Text(
                                                text = if (isMuted) "MUTED" else "MUTE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .testTag("mute_chip_${lane.channel}")
                                            .height(28.dp)
                                            .semantics {
                                                contentDescription = "Mute ${lane.name}"
                                            },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )

                                    // Inline Solo FilledIconToggleButton
                                    FilledIconToggleButton(
                                        checked = isSoloed,
                                        onCheckedChange = { onSoloToggle(lane.channel) },
                                        modifier = Modifier
                                            .testTag("solo_button_${lane.channel}")
                                            .size(28.dp)
                                            .semantics {
                                                contentDescription = "Solo ${lane.name}"
                                            },
                                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                                            checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                                            checkedContentColor = MaterialTheme.colorScheme.onTertiary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Headphones,
                                            contentDescription = "Solo ${lane.name}",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Lane Canvas: Note Step Blocks & Moving Playhead Line
                            val playheadColor = MaterialTheme.colorScheme.primary
                            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("lane_canvas_${lane.channel}")
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val seekFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                            onSeek(seekFraction)
                                        }
                                    }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Draw bar grid background lines (4 bars = 16 beats)
                                val barWidth = canvasWidth / 4f
                                for (b in 1 until 4) {
                                    val x = b * barWidth
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1f
                                    )
                                }

                                // Render Note Events as Step Rectangles
                                val notes = trackData?.notes ?: emptyList()
                                if (notes.isNotEmpty()) {
                                    // Calculate pitch bounds for vertical scaling
                                    val minPitch = notes.minOfOrNull { it.pitch } ?: 36
                                    val maxPitch = (notes.maxOfOrNull { it.pitch } ?: 72).coerceAtLeast(minPitch + 1)
                                    val pitchRange = (maxPitch - minPitch).toDouble().coerceAtLeast(12.0)

                                    for (note in notes) {
                                        val startFrac = (note.startBeat / totalBeats).toFloat().coerceIn(0f, 1f)
                                        val durFrac = (note.durationBeats / totalBeats).toFloat().coerceIn(0.01f, 1f)

                                        val rectX = startFrac * canvasWidth
                                        val rectW = (durFrac * canvasWidth).coerceAtLeast(4f)

                                        val pitchNorm = ((note.pitch - minPitch) / pitchRange).toFloat().coerceIn(0f, 1f)
                                        val rectH = (canvasHeight * 0.4f).coerceAtLeast(6f)
                                        val rectY = (1f - pitchNorm) * (canvasHeight - rectH)

                                        val noteAlpha = if (isMuted) 0.2f else (note.velocity / 127f).coerceIn(0.4f, 1f)

                                        drawRoundRect(
                                            color = trackColor.copy(alpha = noteAlpha),
                                            topLeft = Offset(rectX, rectY),
                                            size = Size(rectW, rectH),
                                            cornerRadius = CornerRadius(3f, 3f)
                                        )
                                    }
                                }

                                // Render Synchronized Real-Time Playhead Cursor Line
                                val cursorX = (progress.coerceIn(0f, 1f)) * canvasWidth
                                drawLine(
                                    color = playheadColor,
                                    start = Offset(cursorX, 0f),
                                    end = Offset(cursorX, canvasHeight),
                                    strokeWidth = 3f
                                )

                                drawCircle(
                                    color = playheadColor,
                                    radius = 4f,
                                    center = Offset(cursorX, 0f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
