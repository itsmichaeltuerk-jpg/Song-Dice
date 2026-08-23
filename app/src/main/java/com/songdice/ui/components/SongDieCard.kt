package com.songdice.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.songdice.data.model.DiceParameter

/**
 * Individual Die Composable card with 3D physics spring roll animations,
 * parameter value display, lock/unlock toggle, and tap-to-roll action.
 */
@Composable
fun SongDieCard(
    parameter: DiceParameter,
    title: String,
    value: String,
    subtitle: String = "",
    isLocked: Boolean,
    onTapRoll: () -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
    isRolling: Boolean = false
) {
    var triggerAnim by remember { mutableStateOf(false) }

    LaunchedEffect(value, isRolling) {
        if (isRolling || !isLocked) {
            triggerAnim = true
            kotlinx.coroutines.delay(350)
            triggerAnim = false
        }
    }

    val rotX by animateFloatAsState(
        targetValue = if (triggerAnim) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "die_rotX"
    )

    val rotY by animateFloatAsState(
        targetValue = if (triggerAnim) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "die_rotY"
    )

    val scale by animateFloatAsState(
        targetValue = if (triggerAnim) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "die_scale"
    )

    val cardBorder = if (isLocked) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val iconVector = getDieIcon(parameter)
    val sanitizeTag = parameter.name.lowercase()

    Card(
        modifier = modifier
            .testTag("song_die_card_$sanitizeTag")
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = rotX
                rotationY = rotY
                scaleX = scale
                scaleY = scale
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Parameter Icon + Name + Lock Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLocked) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = title,
                                tint = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isLocked) "LOCKED 🔒" else "UNLOCKED 🔓",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lock / Unlock Toggle Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLocked) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .testTag("lock_button_$sanitizeTag")
                        .semantics { contentDescription = if (isLocked) "Unlock $title" else "Lock $title" }
                        .clickable { onToggleLock() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isLocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLocked) "LOCKED" else "LOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value Display Box & Tap-to-Roll trigger
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLocked) { onTapRoll() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onTapRoll,
                        enabled = !isLocked,
                        modifier = Modifier
                            .testTag("roll_button_$sanitizeTag")
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reroll $title",
                            tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getDieIcon(parameter: DiceParameter): ImageVector {
    return when (parameter) {
        DiceParameter.KEY -> Icons.Default.Key
        DiceParameter.PROGRESSION -> Icons.Default.FormatListNumbered
        DiceParameter.CHORD_RHYTHM -> Icons.Default.Speed
        DiceParameter.BASS_PATTERN -> Icons.Default.Casino
        DiceParameter.DRUM_RHYTHM -> Icons.Default.Casino
        DiceParameter.MELODY_CONTOUR -> Icons.Default.MusicNote
    }
}
