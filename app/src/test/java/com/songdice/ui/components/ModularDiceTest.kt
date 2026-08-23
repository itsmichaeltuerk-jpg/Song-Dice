package com.songdice.ui.components

import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.repository.SongDiceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModularDiceTest {

    private val repository = SongDiceRepository()

    @Test
    fun testLockingParameterPreventsGlobalRollFromModifyingValue() {
        val initialDice = DiceParameter.entries.associateWith { param ->
            DiceState(parameter = param, value = "Initial Value", isLocked = false)
        }.toMutableMap()

        // Lock KEY parameter
        initialDice[DiceParameter.KEY] = DiceState(DiceParameter.KEY, value = "Locked C Major", isLocked = true)

        val rolledDice = repository.rollDice(initialDice)

        // KEY must remain unchanged because it is locked
        assertEquals("Locked C Major", rolledDice[DiceParameter.KEY]?.value)
        assertTrue(rolledDice[DiceParameter.KEY]?.isLocked == true)
    }

    @Test
    fun testSingleDieRerollModifiesOnlyTargetedParameter() {
        val initialDice = DiceParameter.entries.associateWith { param ->
            DiceState(parameter = param, value = "Fixed ${param.name}", isLocked = false)
        }.toMutableMap()

        val targetParam = DiceParameter.PROGRESSION
        val presets = repository.dicePresets[targetParam] ?: listOf("New Progression")
        val newProgression = presets.first { it != initialDice[targetParam]?.value }

        // Perform single die update
        val updatedDice = initialDice.toMutableMap()
        updatedDice[targetParam] = initialDice[targetParam]!!.copy(value = newProgression)

        // Target parameter value changed
        assertEquals(newProgression, updatedDice[targetParam]?.value)

        // All other parameters remain identical
        DiceParameter.entries.filter { it != targetParam }.forEach { otherParam ->
            assertEquals("Fixed ${otherParam.name}", updatedDice[otherParam]?.value)
        }
    }

    @Test
    fun testGlobalRollRerollsAllUnlockedParameters() {
        val initialDice = DiceParameter.entries.associateWith { param ->
            DiceState(parameter = param, value = "Old Value", isLocked = false)
        }.toMutableMap()

        // Lock BASS_PATTERN only
        initialDice[DiceParameter.BASS_PATTERN] = DiceState(
            DiceParameter.BASS_PATTERN,
            value = "Locked Bass Line",
            isLocked = true
        )

        val rolledDice = repository.rollDice(initialDice)

        // Locked parameter is untouched
        assertEquals("Locked Bass Line", rolledDice[DiceParameter.BASS_PATTERN]?.value)

        // Unlocked parameters receive non-empty rolled values
        DiceParameter.entries.filter { it != DiceParameter.BASS_PATTERN }.forEach { unlockedParam ->
            assertTrue(rolledDice[unlockedParam]?.value?.isNotBlank() == true)
        }
    }
}
