package com.example.songdice.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.songdice.data.model.MusicalGenre
import com.example.ui.theme.FlatAmber
import com.example.ui.theme.FlatCyan
import com.example.ui.theme.FlatViolet
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface

@Composable
fun GenreBpmHeader(
    selectedGenre: MusicalGenre,
    bpm: Int,
    onGenreSelected: (MusicalGenre) -> Unit,
    onBpmChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("genre_bpm_card")
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section 1: Genre Selector Label & Scrollable Chips
            Text(
                text = "MUSICAL GENRE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicalGenre.entries.forEach { genre ->
                    val isSelected = genre == selectedGenre
                    FilterChip(
                        selected = isSelected,
                        onClick = { onGenreSelected(genre) },
                        label = {
                            Text(
                                text = genre.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlatViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = StudioBorder,
                            selectedBorderColor = FlatViolet
                        ),
                        modifier = Modifier.testTag("genre_chip_${genre.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: BPM Control Slider & Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "BPM Tempo",
                        tint = FlatAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEMPO / BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onBpmChanged(bpm - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease BPM",
                            tint = FlatCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$bpm BPM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlatAmber,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { onBpmChanged(bpm + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase BPM",
                            tint = FlatCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Slider(
                value = bpm.toFloat(),
                onValueChange = { onBpmChanged(it.toInt()) },
                valueRange = 60f..200f,
                steps = 140,
                colors = SliderDefaults.colors(
                    thumbColor = FlatAmber,
                    activeTrackColor = FlatAmber,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .testTag("bpm_slider")
                    .fillMaxWidth()
            )
        }
    }
}
