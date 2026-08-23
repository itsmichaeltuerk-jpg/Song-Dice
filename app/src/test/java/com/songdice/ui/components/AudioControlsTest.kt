package com.songdice.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.SongArrangement
import com.songdice.audio.AudioPlayer
import com.songdice.audio.PlaybackState
import com.songdice.ui.theme.SongDiceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var audioPlayer: AudioPlayer
    private lateinit var sampleArrangement: SongArrangement

    @Before
    fun setup() {
        audioPlayer = AudioPlayer()

        sampleArrangement = SongArrangement(
            title = "Test Track",
            genre = "Synthwave",
            key = "C Major",
            bpm = 120,
            progression = "I - IV - V",
            tracks = listOf(
                InstrumentTrack(
                    trackName = "Chords",
                    channel = 0,
                    notes = listOf(MidiNote(pitch = 60, startBeat = 0.0, durationBeats = 1.0))
                ),
                InstrumentTrack(
                    trackName = "Bass",
                    channel = 1,
                    notes = listOf(MidiNote(pitch = 36, startBeat = 0.0, durationBeats = 1.0))
                ),
                InstrumentTrack(
                    trackName = "Lead",
                    channel = 2,
                    notes = listOf(MidiNote(pitch = 72, startBeat = 0.0, durationBeats = 1.0))
                ),
                InstrumentTrack(
                    trackName = "Drums",
                    channel = 9,
                    notes = listOf(MidiNote(pitch = 36, startBeat = 0.0, durationBeats = 0.5))
                )
            )
        )
    }

    // --- AudioPlayer Unit Tests ---

    @Test
    fun `AudioPlayer starts in IDLE state with zero progress`() {
        assertEquals(PlaybackState.IDLE, audioPlayer.playbackState.value)
        assertEquals(0.0f, audioPlayer.progress.value, 0.001f)
        assertEquals(0L, audioPlayer.positionMs.value)
    }

    @Test
    fun `AudioPlayer transitions to IDLE and calculates duration when arrangement is loaded`() {
        audioPlayer.loadArrangement(sampleArrangement)

        assertEquals(PlaybackState.IDLE, audioPlayer.playbackState.value)
        assertTrue(audioPlayer.durationMs.value > 0L)
        assertEquals(0.0f, audioPlayer.progress.value, 0.001f)
    }

    @Test
    fun `AudioPlayer manages play, pause, and stop state transitions`() {
        audioPlayer.loadArrangement(sampleArrangement)

        audioPlayer.play()
        assertEquals(PlaybackState.PLAYING, audioPlayer.playbackState.value)

        audioPlayer.pause()
        assertEquals(PlaybackState.PAUSED, audioPlayer.playbackState.value)

        audioPlayer.stop()
        assertEquals(PlaybackState.STOPPED, audioPlayer.playbackState.value)
        assertEquals(0.0f, audioPlayer.progress.value, 0.001f)
    }

    @Test
    fun `AudioPlayer scrub boundary math clamps target progress between 0_0 and 1_0`() {
        audioPlayer.loadArrangement(sampleArrangement)

        // Negative progress clamps to 0.0f
        audioPlayer.seekTo(-0.5f)
        assertEquals(0.0f, audioPlayer.progress.value, 0.001f)
        assertEquals(0L, audioPlayer.positionMs.value)

        // Overflow progress clamps to 1.0f
        audioPlayer.seekTo(1.5f)
        assertEquals(1.0f, audioPlayer.progress.value, 0.001f)
        assertEquals(audioPlayer.durationMs.value, audioPlayer.positionMs.value)

        // Mid-point progress
        audioPlayer.seekTo(0.5f)
        assertEquals(0.5f, audioPlayer.progress.value, 0.001f)
        assertEquals((0.5f * audioPlayer.durationMs.value).toLong(), audioPlayer.positionMs.value)
    }

    @Test
    fun `AudioPlayer default channel auditioning state has all channels active and zero bitmasks`() {
        assertEquals(0, audioPlayer.getMuteBitmask())
        assertEquals(0, audioPlayer.getSoloBitmask())

        assertTrue(audioPlayer.isChannelActive(0))
        assertTrue(audioPlayer.isChannelActive(1))
        assertTrue(audioPlayer.isChannelActive(2))
        assertTrue(audioPlayer.isChannelActive(9))
    }

    @Test
    fun `AudioPlayer channel muting updates bitmasks and active states correctly`() {
        audioPlayer.setChannelMute(0, true)

        assertEquals(1 shl 0, audioPlayer.getMuteBitmask())
        assertFalse(audioPlayer.isChannelActive(0))
        assertTrue(audioPlayer.isChannelActive(1))

        audioPlayer.setChannelMute(9, true)
        assertEquals((1 shl 0) or (1 shl 9), audioPlayer.getMuteBitmask())
        assertFalse(audioPlayer.isChannelActive(9))

        audioPlayer.setChannelMute(0, false)
        assertEquals(1 shl 9, audioPlayer.getMuteBitmask())
        assertTrue(audioPlayer.isChannelActive(0))
    }

    @Test
    fun `AudioPlayer channel soloing overrides un-soloed channels`() {
        audioPlayer.setChannelSolo(1, true)

        assertEquals(1 shl 1, audioPlayer.getSoloBitmask())
        assertFalse(audioPlayer.isChannelActive(0)) // Unsoloed
        assertTrue(audioPlayer.isChannelActive(1))  // Soloed
        assertFalse(audioPlayer.isChannelActive(2)) // Unsoloed
        assertFalse(audioPlayer.isChannelActive(9)) // Unsoloed

        // Soloing second channel
        audioPlayer.setChannelSolo(2, true)
        assertEquals((1 shl 1) or (1 shl 2), audioPlayer.getSoloBitmask())
        assertTrue(audioPlayer.isChannelActive(1))
        assertTrue(audioPlayer.isChannelActive(2))
        assertFalse(audioPlayer.isChannelActive(0))
    }

    @Test
    fun `AudioPlayer muted channel remains inactive even if soloed`() {
        audioPlayer.setChannelSolo(1, true)
        audioPlayer.setChannelMute(1, true)

        assertFalse(audioPlayer.isChannelActive(1))
    }

    // --- TrackAuditionRow Compose Tests ---

    @Test
    fun `TrackAuditionRow renders channel controls and triggers callbacks`() {
        val muted = mutableSetOf<Int>()
        val soloed = mutableSetOf<Int>()

        composeTestRule.setContent {
            SongDiceTheme {
                TrackAuditionRow(
                    mutedChannels = muted,
                    soloedChannels = soloed,
                    onMuteToggle = { ch -> if (muted.contains(ch)) muted.remove(ch) else muted.add(ch) },
                    onSoloToggle = { ch -> if (soloed.contains(ch)) soloed.remove(ch) else soloed.add(ch) }
                )
            }
        }

        composeTestRule.onNodeWithTag("track_audition_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("Track Auditioning Controls").assertIsDisplayed()

        // Check channel labels
        composeTestRule.onNodeWithText("Chords").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bass").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lead").assertIsDisplayed()
        composeTestRule.onNodeWithText("Drums").assertIsDisplayed()

        // Click Mute on Chords (channel 0)
        composeTestRule.onNodeWithTag("mute_chip_0").performClick()
        assertTrue(muted.contains(0))

        // Click Solo on Bass (channel 1)
        composeTestRule.onNodeWithTag("solo_button_1").performClick()
        assertTrue(soloed.contains(1))
    }

    // --- WaveformVisualizer Compose Tests ---

    @Test
    fun `WaveformVisualizer renders progress and responds to seek gestures`() {
        var soughtProgress = -1f

        composeTestRule.setContent {
            SongDiceTheme {
                WaveformVisualizer(
                    progress = 0.45f,
                    onSeek = { fraction -> soughtProgress = fraction },
                    isPlaying = true
                )
            }
        }

        composeTestRule.onNodeWithTag("waveform_visualizer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("progress_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("45%").assertIsDisplayed()

        // Perform click / tap seek on scrubber
        composeTestRule.onNodeWithTag("waveform_scrubber").performClick()
        assertTrue(soughtProgress >= 0.0f && soughtProgress <= 1.0f)
    }
}
