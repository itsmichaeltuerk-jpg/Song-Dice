package com.songdice.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.MusicalGenre
import com.songdice.data.model.RollSettings
import com.songdice.data.model.SongBlueprint
import com.songdice.data.model.SongSection
import com.songdice.ui.theme.SongDiceTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComponentStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `DiceRollButton displays default state when not rolling and 0 parameters locked`() {
        var clicked = false
        val rollSettings = RollSettings(lockedParameters = emptySet())

        composeTestRule.setContent {
            SongDiceTheme {
                DiceRollButton(
                    isRolling = false,
                    rollSettings = rollSettings,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("dice_roll_button")
            .assertIsDisplayed()
            .assertIsEnabled()

        composeTestRule.onNodeWithText("ROLL ALL DICE")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("dice_roll_button").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `DiceRollButton displays locked count when parameters are locked`() {
        val rollSettings = RollSettings(
            lockedParameters = setOf(DiceParameter.KEY, DiceParameter.PROGRESSION)
        )

        composeTestRule.setContent {
            SongDiceTheme {
                DiceRollButton(
                    isRolling = false,
                    rollSettings = rollSettings,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("REROLL UNLOCKED (2 LOCKED)")
            .assertIsDisplayed()
    }

    @Test
    fun `DiceRollButton is disabled and displays rolling status when isRolling is true`() {
        var clicked = false

        composeTestRule.setContent {
            SongDiceTheme {
                DiceRollButton(
                    isRolling = true,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("dice_roll_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("ROLLING DICE & COMPOSING...")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("dice_roll_button").performClick()
        assertFalse(clicked)
    }

    @Test
    fun `MetadataCard renders song blueprint attributes correctly`() {
        val blueprint = SongBlueprint(
            id = "test_1",
            title = "Midnight Synth",
            genre = MusicalGenre.SYNTHWAVE,
            bpm = 115,
            keySignature = "F# Minor",
            scaleMode = "Dorian",
            progression = "i - VI - III - VII",
            sections = listOf(
                SongSection(sectionType = "Intro", lengthInBeats = 16.0, energyLevel = 0.4f, chordProgression = listOf("F#m", "D")),
                SongSection(sectionType = "Chorus", lengthInBeats = 32.0, energyLevel = 0.9f, chordProgression = listOf("F#m", "D", "A", "E"))
            )
        )

        composeTestRule.setContent {
            SongDiceTheme {
                MetadataCard(blueprint = blueprint)
            }
        }

        composeTestRule.onNodeWithTag("metadata_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Midnight Synth").assertIsDisplayed()
        composeTestRule.onNodeWithText("Synthwave / Cyberpunk").assertIsDisplayed()
        composeTestRule.onNodeWithText("F# Minor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dorian").assertIsDisplayed()
        composeTestRule.onNodeWithText("115 BPM").assertIsDisplayed()
        composeTestRule.onNodeWithText("i - VI - III - VII").assertIsDisplayed()

        composeTestRule.onNodeWithTag("section_badge_Intro").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_badge_Chorus").assertIsDisplayed()
    }
}
