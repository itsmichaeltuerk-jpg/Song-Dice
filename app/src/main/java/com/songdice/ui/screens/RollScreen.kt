package com.songdice.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.MusicalGenre
import com.songdice.data.model.SongBlueprint
import com.songdice.ui.components.DiceRollButton
import com.songdice.ui.components.ModularDiceGrid
import com.songdice.ui.viewmodel.RollingState

/**
 * Roll Studio Tab (Tab 1): Dedicated dice rolling, parameter locking & blueprint display screen.
 */
@Composable
fun RollScreen(
    blueprint: SongBlueprint,
    rollingState: RollingState,
    onRollAll: () -> Unit,
    onRollSingle: (DiceParameter) -> Unit,
    onToggleLock: (DiceParameter) -> Unit,
    onSetGenre: (MusicalGenre) -> Unit,
    onSetBpm: (Int) -> Unit,
    onPreviewRoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRolling = (rollingState == RollingState.GENERATING)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("roll_screen_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Blueprint Card with Pill Badges
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blueprint_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = blueprint.title.ifEmpty { "Song Blueprint" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${blueprint.bpm} BPM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Metadata Pill Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BlueprintPill(label = "GENRE", value = blueprint.genre.displayName, modifier = Modifier.weight(1f))
                        BlueprintPill(label = "KEY", value = blueprint.keySignature, modifier = Modifier.weight(1f))
                        BlueprintPill(label = "SCALE", value = blueprint.scaleMode, modifier = Modifier.weight(1f))
                        BlueprintPill(label = "HARMONY", value = blueprint.progression, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 2. Interactive Parameter Locks Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parameter_locks_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PARAMETER LOCKS & CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    // Parameter Lock Chips Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val diceStates = blueprint.diceStates
                        val parameters = DiceParameter.entries

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            parameters.take(2).forEach { param ->
                                val isLocked = diceStates[param]?.isLocked ?: false
                                LockChip(
                                    parameter = param,
                                    value = diceStates[param]?.value ?: "",
                                    isLocked = isLocked,
                                    onToggleLock = { onToggleLock(param) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            parameters.drop(2).forEach { param ->
                                val isLocked = diceStates[param]?.isLocked ?: false
                                LockChip(
                                    parameter = param,
                                    value = diceStates[param]?.value ?: "",
                                    isLocked = isLocked,
                                    onToggleLock = { onToggleLock(param) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Tempo BPM Slider Controls (70 - 170 BPM)
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tempo (BPM)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${blueprint.bpm} BPM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = blueprint.bpm.toFloat(),
                            onValueChange = { onSetBpm(it.toInt()) },
                            valueRange = 70f..170f,
                            steps = 100,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("tempo_bpm_slider")
                        )
                    }
                }
            }
        }

        // 3. Central Rolling Surface Grid
        item {
            ModularDiceGrid(
                blueprint = blueprint,
                isRolling = isRolling,
                onRollSingle = onRollSingle,
                onToggleLock = onToggleLock,
                onRollAllUnlocked = onRollAll
            )
        }

        // 4. Quick Action: Prominent "Preview Roll" Button
        item {
            Button(
                onClick = onPreviewRoll,
                enabled = !isRolling,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("preview_roll_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PREVIEW ROLL (TAB 2 🎧)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BlueprintPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LockChip(
    parameter: DiceParameter,
    value: String,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isLocked,
        onClick = onToggleLock,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = parameter.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (isLocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Locked" else "Unlocked",
                    tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier.testTag("lock_chip_${parameter.name}")
    )
}
