package com.songdice.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songdice.data.model.RollSettings

/**
 * Primary rolling interface button with physics-based spring animations,
 * Material Design 3 container styling, and interactive lock/roll status.
 */
@Composable
fun DiceRollButton(
    isRolling: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rollSettings: RollSettings? = null,
    lockedCount: Int = rollSettings?.lockedParameters?.size ?: 0,
    enabled: Boolean = !isRolling
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Physics spring animation for press scale feedback
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dice_roll_button_scale"
    )

    // Tumble rotation animation on click trigger
    val clickRotation = remember { Animatable(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            clickRotation.animateTo(
                targetValue = 360f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            clickRotation.snapTo(0f)
        }
    }

    // Infinite continuous spin transition during generation / rolling state
    val infiniteTransition = rememberInfiniteTransition(label = "dice_rolling_transition")
    val continuousSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_dice_spin"
    )

    val currentRotation = if (isRolling) continuousSpin else clickRotation.value

    val buttonText = when {
        isRolling -> "ROLLING DICE & COMPOSING..."
        lockedCount > 0 -> "REROLL UNLOCKED ($lockedCount LOCKED)"
        else -> "ROLL ALL DICE"
    }

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .testTag("dice_roll_button")
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = "Roll Dice Icon",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(currentRotation)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}
