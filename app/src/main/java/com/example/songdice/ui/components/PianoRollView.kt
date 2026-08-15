package com.example.songdice.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.songdice.data.model.SongArrangement
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TrackBassColor
import com.example.ui.theme.TrackChordsColor
import com.example.ui.theme.TrackDrumsColor
import com.example.ui.theme.TrackMelodyColor

@Composable
fun PianoRollView(
    arrangement: SongArrangement?,
    playbackProgressBeats: Double,
    isPlaying: Boolean,
    trackMutes: Map<String, Boolean> = emptyMap(),
    trackSolos: Map<String, Boolean> = emptyMap(),
    trackVolumes: Map<String, Float> = emptyMap(),
    masterReverb: Float = 0.35f,
    masterDelay: Float = 0.25f,
    onToggleMute: (String) -> Unit = {},
    onToggleSolo: (String) -> Unit = {},
    onVolumeChange: (String, Float) -> Unit = { _, _ -> },
    onMasterReverbChange: (Float) -> Unit = {},
    onMasterDelayChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasAnySolo = trackSolos.values.any { it }

    fun isTrackAudible(trackName: String): Boolean {
        return if (hasAnySolo) {
            trackSolos[trackName] == true
        } else {
            trackMutes[trackName] != true
        }
    }

    Card(
        modifier = modifier
            .testTag("piano_roll_card")
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title + Time Signature / Resolution
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(StudioSurfaceVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Studio Sequencer",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STUDIO SEQUENCER & MIXER",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Lush 4-Track Sound Engine • Stereo Matrix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                ) {
                    Text(
                        text = if (arrangement != null) "4 BARS • 16 BEATS" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Piano Roll Visual Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        color = Color(0xFF07050C),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .testTag("piano_roll_canvas")
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val totalBeats = 16.0

                    // 1. Bar & Beat Grid
                    val beatWidth = width / totalBeats.toFloat()
                    for (b in 0..16) {
                        val x = b * beatWidth
                        val isBar = b % 4 == 0
                        val strokeWidth = if (isBar) 2f else 0.75f
                        val color = if (isBar) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
                        drawLine(
                            color = color,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = strokeWidth
                        )
                    }

                    // 2. 4 Horizontal Lane Dividers
                    val laneHeight = height / 4f
                    for (l in 1..3) {
                        val y = l * laneHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    if (arrangement != null) {
                        // Render Note Blocks for Each Track
                        arrangement.tracks.forEach { track ->
                            val trackKey = when {
                                track.trackName.equals("Drums", ignoreCase = true) || track.channel == 9 -> "Drums"
                                track.trackName.equals("Bass", ignoreCase = true) || track.channel == 0 -> "Bass"
                                track.trackName.equals("Chords", ignoreCase = true) || track.channel == 1 -> "Chords"
                                else -> "Melody"
                            }

                            val (laneIdx, baseColor) = when (trackKey) {
                                "Drums" -> 0 to TrackDrumsColor
                                "Bass" -> 1 to TrackBassColor
                                "Chords" -> 2 to TrackChordsColor
                                else -> 3 to TrackMelodyColor
                            }

                            val isAudible = isTrackAudible(trackKey)
                            val noteAlpha = if (isAudible) 0.95f else 0.18f
                            val laneStartY = laneIdx * laneHeight

                            track.notes.forEach { note ->
                                val noteStartX = (note.startBeat / totalBeats).toFloat() * width
                                val noteWidth = (note.durationBeats / totalBeats).toFloat() * width

                                val normalizedPitchOffset = ((note.pitch % 12) / 12f) * (laneHeight * 0.55f)
                                val noteY = laneStartY + (laneHeight * 0.70f) - normalizedPitchOffset

                                // Glowing note box
                                drawRoundRect(
                                    color = baseColor.copy(alpha = noteAlpha),
                                    topLeft = Offset(noteStartX + 1f, noteY),
                                    size = Size((noteWidth - 2f).coerceAtLeast(3f), (laneHeight * 0.28f).coerceAtLeast(4f)),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }

                        // 3. Live Playhead
                        if (isPlaying && playbackProgressBeats > 0) {
                            val playheadX = (playbackProgressBeats / totalBeats).toFloat() * width
                            // Glow shadow line
                            drawLine(
                                color = NeonPink.copy(alpha = 0.5f),
                                start = Offset(playheadX, 0f),
                                end = Offset(playheadX, height),
                                strokeWidth = 5f
                            )
                            // Core sharp line
                            drawLine(
                                color = Color.White,
                                start = Offset(playheadX, 0f),
                                end = Offset(playheadX, height),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }

            // Interactive Studio Mixer Channel Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CHANNEL STRIP & ISOLATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    letterSpacing = 0.5.sp
                )

                val channels = listOf(
                    Triple("Drums", TrackDrumsColor, "Punchy Transients & Metallic Ring"),
                    Triple("Bass", TrackBassColor, "Moog Sub-Bass & Tube Overtones"),
                    Triple("Chords", TrackChordsColor, "Lush Rhodes Tines & Stereo Chorus"),
                    Triple("Melody", TrackMelodyColor, "Dual Osc Lead & Ping-Pong Delay")
                )

                channels.forEach { (name, color, soundBadge) ->
                    val isMuted = trackMutes[name] == true
                    val isSoloed = trackSolos[name] == true
                    val isAudible = isTrackAudible(name)
                    val volume = trackVolumes[name] ?: 0.9f

                    ChannelStripRow(
                        name = name,
                        soundBadge = soundBadge,
                        color = color,
                        isAudible = isAudible,
                        isMuted = isMuted,
                        isSoloed = isSoloed,
                        volume = volume,
                        onToggleMute = { onToggleMute(name) },
                        onToggleSolo = { onToggleSolo(name) },
                        onVolumeChange = { onVolumeChange(name, it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Master FX Control Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Reverb Control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MASTER REVERB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                        Slider(
                            value = masterReverb,
                            onValueChange = onMasterReverbChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan.copy(alpha = 0.85f),
                                inactiveTrackColor = StudioBorder
                            )
                        )
                    }

                    // Delay Control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MASTER DELAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                        Slider(
                            value = masterDelay,
                            onValueChange = onMasterDelayChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonAmber,
                                activeTrackColor = NeonAmber.copy(alpha = 0.85f),
                                inactiveTrackColor = StudioBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelStripRow(
    name: String,
    soundBadge: String,
    color: Color,
    isAudible: Boolean,
    isMuted: Boolean,
    isSoloed: Boolean,
    volume: Float,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val rowAlpha by animateFloatAsState(
        targetValue = if (isAudible) 1.0f else 0.45f,
        label = "row_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSoloed) color.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Track Color Dot & Name + Sound Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.3f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAudible) Color.White else Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = soundBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }

        // Mute / Solo Buttons & Volume Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1.2f)
        ) {
            // Mute Button (M)
            MixerToggleButton(
                label = "M",
                isActive = isMuted,
                activeColor = NeonPink,
                defaultColor = Color.DarkGray,
                onClick = onToggleMute,
                testTag = "mute_${name.lowercase()}"
            )

            // Solo Button (S)
            MixerToggleButton(
                label = "S",
                isActive = isSoloed,
                activeColor = NeonAmber,
                defaultColor = Color.DarkGray,
                onClick = onToggleSolo,
                testTag = "solo_${name.lowercase()}"
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Compact Volume Slider
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color.copy(alpha = 0.85f),
                    inactiveTrackColor = StudioBorder
                )
            )
        }
    }
}

@Composable
private fun MixerToggleButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    defaultColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) activeColor else defaultColor.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) activeColor else StudioBorder
        ),
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) Color.Black else Color.White
            )
        }
    }
}
