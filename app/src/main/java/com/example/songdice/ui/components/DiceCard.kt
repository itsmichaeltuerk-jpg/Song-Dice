package com.example.songdice.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.ui.theme.FlatAmber
import com.example.ui.theme.FlatCyan
import com.example.ui.theme.FlatPink
import com.example.ui.theme.FlatViolet
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun DiceCard(
    diceState: DiceState,
    onToggleLock: () -> Unit,
    onRerollSingle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = diceState.isLocked
    val parameter = diceState.parameter

    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(diceState.value) {
        if (!isLocked) {
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 400)
            )
        }
    }

    val cardBorder = if (isLocked) {
        BorderStroke(1.dp, FlatAmber.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, StudioBorder.copy(alpha = 0.2f))
    }

    val iconColor = when (parameter) {
        DiceParameter.KEY -> FlatCyan
        DiceParameter.PROGRESSION -> FlatViolet
        DiceParameter.CHORD_RHYTHM -> FlatAmber
        DiceParameter.BASS_PATTERN -> FlatViolet
        DiceParameter.DRUM_RHYTHM -> FlatCyan
        DiceParameter.MELODY_CONTOUR -> FlatPink
    }

    Card(
        modifier = modifier
            .testTag("dice_card_${parameter.name}")
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) StudioSurfaceVariant.copy(alpha = 0.7f) else StudioSurface
        ),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Parameter Icon + Name + Lock Toggle
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
                            .size(36.dp)
                            .background(
                                color = iconColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getParameterIcon(parameter),
                            contentDescription = parameter.displayName,
                            tint = iconColor,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation.value)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = parameter.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isLocked) "LOCKED" else "READY TO ROLL",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLocked) FlatAmber else Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Lock Toggle Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLocked) FlatAmber.copy(alpha = 0.1f) else StudioSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .testTag("lock_button_${parameter.name}")
                        .clickable { onToggleLock() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) "Unlock parameter" else "Lock parameter",
                            tint = if (isLocked) FlatAmber else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLocked) "LOCKED" else "LOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLocked) FlatAmber else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = StudioBackground.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = diceState.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) FlatAmber else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onRerollSingle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reroll ${parameter.displayName}",
                            tint = if (isLocked) Color.Gray else FlatCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getParameterIcon(parameter: DiceParameter): ImageVector {
    return when (parameter) {
        DiceParameter.KEY -> Icons.Default.Key
        DiceParameter.PROGRESSION -> Icons.Default.FormatListNumbered
        DiceParameter.CHORD_RHYTHM -> Icons.Default.Speed
        DiceParameter.BASS_PATTERN -> Icons.Default.GraphicEq
        DiceParameter.DRUM_RHYTHM -> Icons.Default.GraphicEq
        DiceParameter.MELODY_CONTOUR -> Icons.Default.MusicNote
    }
}
