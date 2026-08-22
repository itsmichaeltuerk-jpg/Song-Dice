package com.songdice.ui.viewmodel

import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.MusicalGenre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SongDiceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: SongDiceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SongDiceViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndAutoRollOnStartup() {
        val state = viewModel.uiState.value

        assertEquals("Rolling state should be READY after startup roll", RollingState.READY, state.rollingState)
        assertNotNull("SongBlueprint should not be null", state.blueprint)
        assertTrue("Dice states map should contain entries for all parameters", state.blueprint.diceStates.isNotEmpty())
        assertNotNull("Arrangement should be present in blueprint", state.blueprint.arrangement)
        assertTrue("Zip bytes should be generated", state.zipBytes != null && state.zipBytes!!.isNotEmpty())
        assertTrue("Export ready flag should be true", state.exportReady)
        assertTrue("Bundle filename should end with .zip", state.bundleFileName.endsWith(".zip"))
    }

    @Test
    fun testToggleLockParameter() {
        val initialLocked = viewModel.uiState.value.blueprint.diceStates[DiceParameter.KEY]?.isLocked ?: false

        viewModel.toggleLock(DiceParameter.KEY)

        val updatedState = viewModel.uiState.value
        val isLockedAfterToggle = updatedState.blueprint.diceStates[DiceParameter.KEY]?.isLocked ?: false
        assertEquals(!initialLocked, isLockedAfterToggle)
        assertTrue(
            "Locked parameters set in RollSettings should contain KEY",
            updatedState.blueprint.rollSettings.lockedParameters.contains(DiceParameter.KEY) == isLockedAfterToggle
        )
    }

    @Test
    fun testSetGenreAndBpmUpdatesState() {
        viewModel.setGenre(MusicalGenre.EDM)
        var state = viewModel.uiState.value
        assertEquals(MusicalGenre.EDM, state.blueprint.genre)

        viewModel.setBpm(128)
        state = viewModel.uiState.value
        assertEquals(128, state.blueprint.bpm)
        assertTrue("Bundle file name should reflect 128BPM", state.bundleFileName.contains("128BPM"))
    }

    @Test
    fun testPlaybackControlsAndSeek() {
        viewModel.seekTo(0.5f)
        var state = viewModel.uiState.value
        assertEquals(0.5f, state.progress, 0.01f)

        viewModel.pause()
        state = viewModel.uiState.value
        assertFalse("Playback state should not be playing when paused", state.isPlaying)
    }

    @Test
    fun testMuteAndSoloChannelToggles() {
        viewModel.toggleMute(0)
        var state = viewModel.uiState.value
        assertTrue("Muted channels set should contain channel 0", state.mutedChannels.contains(0))

        viewModel.toggleMute(0)
        state = viewModel.uiState.value
        assertFalse("Muted channels set should no longer contain channel 0", state.mutedChannels.contains(0))

        viewModel.toggleSolo(1)
        state = viewModel.uiState.value
        assertTrue("Soloed channels set should contain channel 1", state.soloedChannels.contains(1))
    }

    @Test
    fun testGeneratedZipBundleContainsFourCoreAssets() {
        val zipBytes = viewModel.uiState.value.zipBytes
        assertNotNull("Zip bytes must not be null", zipBytes)
        assertTrue("Zip bytes must not be empty", zipBytes!!.isNotEmpty())

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertEquals(4, entryNames.size)
        assertTrue(entryNames.contains("song_idea.mid"))
        assertTrue(entryNames.contains("preview.wav"))
        assertTrue(entryNames.contains("LeadSheet.txt"))
        assertTrue(entryNames.contains("AIPrompt.txt"))
    }
}
