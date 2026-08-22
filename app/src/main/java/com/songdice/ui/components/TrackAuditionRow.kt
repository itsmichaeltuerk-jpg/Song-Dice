package com.songdice.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AuditionChannelInfo(
    val channel: Int,
    val title: String,
    val subtitle: String
)

val DEFAULT_AUDITION_CHANNELS = listOf(
    AuditionChannelInfo(0, "Chords", "Chords / Keys"),
    AuditionChannelInfo(1, "Bass", "Bassline"),
    AuditionChannelInfo(2, "Lead", "Lead / Vocal Guide"),
    AuditionChannelInfo(9, "Drums", "GM Drums")
)

/**
 * UI Component displaying channel solo and mute toggles for live track auditioning.
 * Uses Material 3 FilterChip and FilledIconToggleButton components.
 */
@Composable
fun TrackAuditionRow(
    mutedChannels: Set<Int>,
    soloedChannels: Set<Int>,
    onMuteToggle: (channel: Int) -> Unit,
    onSoloToggle: (channel: Int) -> Unit,
    modifier: Modifier = Modifier,
    channels: List<AuditionChannelInfo> = DEFAULT_AUDITION_CHANNELS
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("track_audition_row"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Track Auditioning Controls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("audition_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                channels.forEach { channelInfo ->
                    val isMuted = mutedChannels.contains(channelInfo.channel)
                    val isSoloed = soloedChannels.contains(channelInfo.channel)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_card_${channelInfo.channel}"),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isSoloed -> MaterialTheme.colorScheme.primaryContainer
                                isMuted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = channelInfo.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSoloed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = channelInfo.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSoloed) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Mute FilterChip
                                FilterChip(
                                    selected = isMuted,
                                    onClick = { onMuteToggle(channelInfo.channel) },
                                    label = { Text(if (isMuted) "MUTED" else "MUTE") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    },
                                    modifier = Modifier
                                        .testTag("mute_chip_${channelInfo.channel}")
                                        .semantics {
                                            contentDescription = "Mute ${channelInfo.subtitle}"
                                        },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )

                                // Solo FilledIconToggleButton
                                FilledIconToggleButton(
                                    checked = isSoloed,
                                    onCheckedChange = { onSoloToggle(channelInfo.channel) },
                                    modifier = Modifier
                                        .testTag("solo_button_${channelInfo.channel}")
                                        .semantics {
                                            contentDescription = "Solo ${channelInfo.subtitle}"
                                        },
                                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                                        checkedContentColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = "Solo ${channelInfo.subtitle}",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
