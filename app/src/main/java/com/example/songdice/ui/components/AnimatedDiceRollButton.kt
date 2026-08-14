package com.example.songdice.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch

@Composable
fun AnimatedDiceRollButton(
    isRolling: Boolean,
    lockedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Click tumble animation
    val clickRotation = remember { Animatable(0f) }
    val pressScale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            pressScale.animateTo(0.95f, spring(stiffness = Spring.StiffnessHigh))
        } else {
            pressScale.animateTo(1.0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // 2. Infinite transitions for background shimmer & continuous roll tumbling
    val infiniteTransition = rememberInfiniteTransition(label = "dice_roll_infinite")

    // Rotating gradient border angle
    val borderRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_gradient_rotation"
    )

    // Pulse scale during rolling
    val rollingPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rolling_pulse_scale"
    )

    // Continuous spin during active AI / roll generation
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
    val currentScale = if (isRolling) rollingPulseScale * pressScale.value else pressScale.value

    // Glowing multi-color gradient border
    val glowingGradient = Brush.sweepGradient(
        listOf(
            NeonViolet,
            NeonPink,
            NeonCyan,
            NeonAmber,
            NeonViolet
        )
    )

    Surface(
        modifier = modifier
            .testTag("roll_all_button")
            .fillMaxWidth()
            .height(56.dp)
            .scale(currentScale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isRolling) 2.5.dp else 1.5.dp,
                brush = glowingGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                coroutineScope.launch {
                    clickRotation.snapTo(0f)
                    clickRotation.animateTo(
                        targetValue = 720f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NeonViolet.copy(alpha = 0.9f),
                            NeonPink.copy(alpha = 0.85f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Tumbling Casino Dice Icon
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Roll All Dice",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(currentRotation)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Button Text
                Text(
                    text = when {
                        isRolling -> "ROLLING DICE & COMPOSING..."
                        lockedCount > 0 -> "REROLL UNLOCKED ($lockedCount LOCKED)"
                        else -> "ROLL ALL DICE"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.8.sp,
                    fontSize = 15.sp
                )
            }
        }
    }
}
