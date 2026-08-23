package com.songdice.audio

import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.SongArrangement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var audioPlayer: AudioPlayer

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        audioPlayer = AudioPlayer()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertEquals(PlaybackState.IDLE, audioPlayer.playbackState.value)
        assertEquals(0.0f, audioPlayer.progress.value, 0.001f)
        assertEquals(0L, audioPlayer.positionMs.value)
        assertEquals(1, audioPlayer.currentBar.value)
        assertEquals(1, audioPlayer.currentBeat.value)
    }

    @Test
    fun testLoadArrangementAndPlaybackControls() {
        val tracks = listOf(
            InstrumentTrack("Chords", 0, listOf(MidiNote(60, 0.0, 1.0, 100))),
            InstrumentTrack("Bass", 1, listOf(MidiNote(36, 0.0, 1.0, 100)))
        )
        val arrangement = SongArrangement(
            title = "Test Arrangement",
            genre = "Pop",
            bpm = 120,
            key = "C Major",
            progression = "I - V - vi - IV",
            tracks = tracks
        )

        audioPlayer.loadArrangement(arrangement)
        assertEquals(PlaybackState.IDLE, audioPlayer.playbackState.value)
        assertTrue("Duration should be calculated (> 0ms)", audioPlayer.durationMs.value > 0L)

        audioPlayer.play()
        assertEquals(PlaybackState.PLAYING, audioPlayer.playbackState.value)

        audioPlayer.pause()
        assertEquals(PlaybackState.PAUSED, audioPlayer.playbackState.value)

        audioPlayer.stop()
        assertEquals(PlaybackState.STOPPED, audioPlayer.playbackState.value)
        assertEquals(0.0f, audioPlayer.progress.value, 0.001f)
    }

    @Test
    fun testSeekToProgressAndPosition() {
        val dummyBytes = ByteArray(100)
        audioPlayer.loadMidiBytes(dummyBytes)

        audioPlayer.seekTo(0.5f)
        assertEquals(0.5f, audioPlayer.progress.value, 0.01f)
        val duration = audioPlayer.durationMs.value
        assertEquals(duration / 2, audioPlayer.positionMs.value)

        audioPlayer.seekTo(1.5f) // Clamped to 1.0f
        assertEquals(1.0f, audioPlayer.progress.value, 0.01f)
    }

    @Test
    fun testChannelMuteAndSoloToggles() {
        assertFalse(audioPlayer.isChannelMuted(0))
        assertFalse(audioPlayer.isChannelSoloed(1))
        assertTrue(audioPlayer.isChannelActive(0))

        audioPlayer.toggleMute(0)
        assertTrue(audioPlayer.isChannelMuted(0))
        assertFalse(audioPlayer.isChannelActive(0))

        audioPlayer.toggleSolo(1)
        assertTrue(audioPlayer.isChannelSoloed(1))
        assertTrue(audioPlayer.isChannelActive(1))
        assertFalse(audioPlayer.isChannelActive(2)) // Only soloed channel active

        audioPlayer.toggleMute(0)
        assertFalse(audioPlayer.isChannelMuted(0))
        audioPlayer.toggleSolo(1)
        assertFalse(audioPlayer.isChannelSoloed(1))
        assertTrue(audioPlayer.isChannelActive(2)) // Back to normal
    }
}
