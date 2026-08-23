package com.songdice.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.songdice.data.model.SongBlueprint

/**
 * Responsive 4-card Modular Dice Grid composable supporting individual parameter roll/lock,
 * 3D spring animations, and a master "Roll All Unlocked" trigger button.
 */
@Composable
fun ModularDiceGrid(
    blueprint: SongBlueprint,
    isRolling: Boolean,
    onRollSingle: (DiceParameter) -> Unit,
    onToggleLock: (DiceParameter) -> Unit,
    onRollAllUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diceStates = blueprint.diceStates

    val keyState = diceStates[DiceParameter.KEY] ?: DiceState(DiceParameter.KEY, blueprint.keySignature)
    val progressionState = diceStates[DiceParameter.PROGRESSION] ?: DiceState(DiceParameter.PROGRESSION, blueprint.progression)
    val genreState = diceStates[DiceParameter.MELODY_CONTOUR] ?: DiceState(DiceParameter.MELODY_CONTOUR, blueprint.genre.displayName)
    val bpmState = diceStates[DiceParameter.CHORD_RHYTHM] ?: DiceState(DiceParameter.CHORD_RHYTHM, "${blueprint.bpm} BPM")

    val lockedCount = diceStates.values.count { it.isLocked }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("modular_dice_grid"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "INTERACTIVE MODULAR DICE GRID",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Grid Row 1: Key & Scale Die + Tempo / BPM Die
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Die 1: Key & Scale
                SongDieCard(
                    parameter = DiceParameter.KEY,
                    title = "Key & Scale",
                    value = keyState.value,
                    subtitle = "Tonal center & scale mode",
                    isLocked = keyState.isLocked,
                    onTapRoll = { onRollSingle(DiceParameter.KEY) },
                    onToggleLock = { onToggleLock(DiceParameter.KEY) },
                    isRolling = isRolling,
                    modifier = Modifier.weight(1f)
                )

                // Die 2: Tempo / BPM
                SongDieCard(
                    parameter = DiceParameter.CHORD_RHYTHM,
                    title = "Tempo / BPM",
                    value = "${blueprint.bpm} BPM",
                    subtitle = bpmState.value,
                    isLocked = bpmState.isLocked,
                    onTapRoll = { onRollSingle(DiceParameter.CHORD_RHYTHM) },
                    onToggleLock = { onToggleLock(DiceParameter.CHORD_RHYTHM) },
                    isRolling = isRolling,
                    modifier = Modifier.weight(1f)
                )
            }

            // Grid Row 2: Genre / Style Die + Chord Progression Die
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Die 3: Genre / Style
                SongDieCard(
                    parameter = DiceParameter.MELODY_CONTOUR,
                    title = "Genre / Style",
                    value = blueprint.genre.displayName,
                    subtitle = genreState.value,
                    isLocked = genreState.isLocked,
                    onTapRoll = { onRollSingle(DiceParameter.MELODY_CONTOUR) },
                    onToggleLock = { onToggleLock(DiceParameter.MELODY_CONTOUR) },
                    isRolling = isRolling,
                    modifier = Modifier.weight(1f)
                )

                // Die 4: Chord Progression
                SongDieCard(
                    parameter = DiceParameter.PROGRESSION,
                    title = "Chord Progression",
                    value = progressionState.value,
                    subtitle = "Harmonic movement",
                    isLocked = progressionState.isLocked,
                    onTapRoll = { onRollSingle(DiceParameter.PROGRESSION) },
                    onToggleLock = { onToggleLock(DiceParameter.PROGRESSION) },
                    isRolling = isRolling,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Master "Roll All Unlocked" Action Trigger Button
            DiceRollButton(
                isRolling = isRolling,
                onClick = onRollAllUnlocked,
                lockedCount = lockedCount
            )
        }
    }
}
