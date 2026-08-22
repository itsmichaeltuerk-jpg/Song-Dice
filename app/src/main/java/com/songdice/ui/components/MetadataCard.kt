package com.songdice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songdice.data.model.SongBlueprint
import com.songdice.data.model.SongSection

/**
 * Metadata card displaying generated song blueprint attributes:
 * Title, Genre, Tempo (BPM), Key Signature, Scale Mode, Progression, and Section/Chord Badges.
 * Formatted with Material 3 ElevatedCard, 20dp rounded corners, and M3 typography tokens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataCard(
    blueprint: SongBlueprint,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        modifier = modifier
            .testTag("metadata_card")
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Song Title & Genre Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = blueprint.title.ifEmpty { "Untitled Song" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SONG BLUEPRINT & METADATA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("genre_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Genre Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = blueprint.genre.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Musical Attribute Badges Row (Key, Scale, BPM)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttributeChip(
                    icon = Icons.Default.Key,
                    label = "Key",
                    value = blueprint.keySignature,
                    testTag = "key_chip"
                )
                AttributeChip(
                    icon = Icons.Default.MusicNote,
                    label = "Scale",
                    value = blueprint.scaleMode,
                    testTag = "scale_chip"
                )
                AttributeChip(
                    icon = Icons.Default.Speed,
                    label = "Tempo",
                    value = "${blueprint.bpm} BPM",
                    testTag = "bpm_chip"
                )
            }

            if (blueprint.progression.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                // Chord Progression Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progression_box")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Progression Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CHORD PROGRESSION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = blueprint.progression,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Song Structure Sections Badges
            if (blueprint.sections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "STRUCTURE SECTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sections_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    blueprint.sections.forEach { section ->
                        SectionBadge(section = section)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeChip(
    icon: ImageVector,
    label: String,
    value: String,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionBadge(
    section: SongSection
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.testTag("section_badge_${section.sectionType}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.sectionType,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            if (section.chordProgression.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${section.chordProgression.joinToString("-")})",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
