package com.songdice.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.model.MusicalGenre
import com.example.songdice.data.repository.SongDiceRepository
import com.songdice.audio.AudioPlayer
import com.songdice.audio.PlaybackState
import com.songdice.data.model.RollSettings
import com.songdice.data.model.SongBlueprint
import com.songdice.data.model.SongSection
import com.songdice.export.ShareManager
import com.songdice.export.ZipExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State enumeration for offline song idea roll generation flow.
 */
enum class RollingState {
    IDLE,
    GENERATING,
    READY,
    ERROR
}

/**
 * UI State for Song Dice MainScreen & audio engine interaction.
 */
data class SongDiceUiState(
    val blueprint: SongBlueprint = SongBlueprint(),
    val rollingState: RollingState = RollingState.IDLE,
    val statusMessage: String? = null,
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val progress: Float = 0.0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val mutedChannels: Set<Int> = emptySet(),
    val soloedChannels: Set<Int> = emptySet(),
    val zipBytes: ByteArray? = null,
    val bundleFileName: String = "Song_Dice_Export.zip",
    val exportReady: Boolean = false,
    val isDarkTheme: Boolean = true
)

/**
 * Primary ViewModel for Song Dice application coordinating offline roll generation,
 * audio preview playback state, and SAF / Share Sheet export.
 */
class SongDiceViewModel(
    private val repository: SongDiceRepository = SongDiceRepository(),
    private val audioPlayer: AudioPlayer = AudioPlayer(),
    private val zipExporter: ZipExporter = ZipExporter(),
    private val shareManager: ShareManager = ShareManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongDiceUiState())
    val uiState: StateFlow<SongDiceUiState> = _uiState.asStateFlow()

    init {
        // Initialize default dice states map
        val initialDice = DiceParameter.entries.associateWith { param ->
            val preset = repository.dicePresets[param]?.firstOrNull() ?: "Default"
            DiceState(parameter = param, value = preset, isLocked = false)
        }

        val initialBlueprint = SongBlueprint(
            title = "New Song Idea",
            genre = MusicalGenre.POP,
            bpm = MusicalGenre.POP.defaultBpm,
            keySignature = initialDice[DiceParameter.KEY]?.value ?: "C Major",
            scaleMode = "Major",
            progression = initialDice[DiceParameter.PROGRESSION]?.value ?: "I - V - vi - IV",
            diceStates = initialDice
        )

        _uiState.update { it.copy(blueprint = initialBlueprint) }

        // Observe AudioPlayer playback states
        viewModelScope.launch {
            audioPlayer.playbackState.collect { state ->
                _uiState.update {
                    it.copy(
                        playbackState = state,
                        isPlaying = (state == PlaybackState.PLAYING)
                    )
                }
            }
        }
        viewModelScope.launch {
            audioPlayer.progress.collect { prog ->
                _uiState.update { it.copy(progress = prog) }
            }
        }
        viewModelScope.launch {
            audioPlayer.positionMs.collect { pos ->
                _uiState.update { it.copy(positionMs = pos) }
            }
        }
        viewModelScope.launch {
            audioPlayer.durationMs.collect { dur ->
                _uiState.update { it.copy(durationMs = dur) }
            }
        }
        viewModelScope.launch {
            audioPlayer.mutedChannels.collect { mutes ->
                _uiState.update { it.copy(mutedChannels = mutes) }
            }
        }
        viewModelScope.launch {
            audioPlayer.soloedChannels.collect { solos ->
                _uiState.update { it.copy(soloedChannels = solos) }
            }
        }

        // Perform initial song idea generation on launch
        rollAllDice()
    }

    /**
     * Executes complete 4-bar song idea roll generation flow:
     * - Rolls unlocked dice parameters
     * - Generates multi-track procedural arrangement
     * - Loads audio preview into AudioPlayer
     * - Synthesizes .zip export bundle
     */
    fun rollAllDice() {
        viewModelScope.launch {
            audioPlayer.stop()

            _uiState.update {
                it.copy(
                    rollingState = RollingState.GENERATING,
                    statusMessage = "Rolling dice & composing arrangement..."
                )
            }

            try {
                val currentBlueprint = _uiState.value.blueprint
                val currentDice = currentBlueprint.diceStates.ifEmpty {
                    DiceParameter.entries.associateWith { param ->
                        val preset = repository.dicePresets[param]?.firstOrNull() ?: "Default"
                        DiceState(parameter = param, value = preset, isLocked = false)
                    }
                }

                val rolledDice = repository.rollDice(currentDice)
                val genre = currentBlueprint.genre
                val bpm = currentBlueprint.bpm

                val arrangement = repository.generateProceduralArrangement(genre, bpm, rolledDice)

                val keyStr = rolledDice[DiceParameter.KEY]?.value ?: "C Major"
                val isMinor = keyStr.contains("Minor", ignoreCase = true) || keyStr.contains("m", ignoreCase = true)

                val newBlueprint = SongBlueprint(
                    id = System.currentTimeMillis().toString(),
                    title = arrangement.title,
                    genre = genre,
                    bpm = arrangement.bpm,
                    keySignature = keyStr,
                    scaleMode = if (isMinor) "Minor" else "Major",
                    sections = listOf(
                        SongSection("Verse", 8.0, 0.6f, listOf("I", "V")),
                        SongSection("Chorus", 8.0, 0.9f, listOf("vi", "IV"))
                    ),
                    rollSettings = RollSettings(
                        lockedParameters = rolledDice.filterValues { it.isLocked }.keys,
                        activeParameters = rolledDice.keys
                    ),
                    progression = arrangement.progression,
                    arrangement = arrangement,
                    diceStates = rolledDice
                )

                // Load arrangement into AudioPlayer for instant audio preview
                audioPlayer.loadArrangement(arrangement)

                // Synthesize 4-asset .zip bundle
                val zipBytes = zipExporter.createZipBundle(newBlueprint)
                val fileName = zipExporter.getBundleFileName(newBlueprint)

                _uiState.update {
                    it.copy(
                        blueprint = newBlueprint,
                        rollingState = RollingState.READY,
                        zipBytes = zipBytes,
                        bundleFileName = fileName,
                        exportReady = true,
                        statusMessage = "Song idea generated! Preview ready."
                    )
                }

                // Automatically start preview playback upon generation completion
                audioPlayer.play()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        rollingState = RollingState.ERROR,
                        statusMessage = "Error rolling song idea: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleLock(parameter: DiceParameter) {
        _uiState.update { state ->
            val diceMap = state.blueprint.diceStates.toMutableMap()
            val current = diceMap[parameter] ?: return@update state
            val updatedState = current.copy(isLocked = !current.isLocked)
            diceMap[parameter] = updatedState

            val lockedKeys = diceMap.filterValues { it.isLocked }.keys
            val updatedSettings = state.blueprint.rollSettings.copy(lockedParameters = lockedKeys)
            val updatedBlueprint = state.blueprint.copy(diceStates = diceMap, rollSettings = updatedSettings)

            state.copy(blueprint = updatedBlueprint)
        }
    }

    fun setGenre(genre: MusicalGenre) {
        _uiState.update { state ->
            val updatedBlueprint = state.blueprint.copy(genre = genre, bpm = genre.defaultBpm)
            state.copy(blueprint = updatedBlueprint)
        }
        rollAllDice()
    }

    fun setBpm(bpm: Int) {
        val clampedBpm = bpm.coerceIn(40, 280)
        _uiState.update { state ->
            val updatedBlueprint = state.blueprint.copy(bpm = clampedBpm)
            val updatedArrangement = state.blueprint.arrangement?.copy(bpm = clampedBpm)
            val finalBlueprint = updatedBlueprint.copy(arrangement = updatedArrangement)

            if (updatedArrangement != null) {
                audioPlayer.loadArrangement(updatedArrangement)
            }

            val zipBytes = zipExporter.createZipBundle(finalBlueprint)
            val fileName = zipExporter.getBundleFileName(finalBlueprint)

            state.copy(
                blueprint = finalBlueprint,
                zipBytes = zipBytes,
                bundleFileName = fileName
            )
        }
    }

    // Audio Playback Controls
    fun play() {
        audioPlayer.play()
    }

    fun pause() {
        audioPlayer.pause()
    }

    fun stop() {
        audioPlayer.stop()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(progress: Float) {
        audioPlayer.seekTo(progress)
    }

    fun toggleMute(channel: Int) {
        audioPlayer.toggleMute(channel)
    }

    fun toggleSolo(channel: Int) {
        audioPlayer.toggleSolo(channel)
    }

    // Export Controls
    fun exportToUri(context: Context, uri: Uri) {
        val bytes = _uiState.value.zipBytes ?: return
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(bytes)
                os.flush()
            }
            _uiState.update {
                it.copy(statusMessage = "Exported ${_uiState.value.bundleFileName} to device storage!")
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(statusMessage = "SAF Export failed: ${e.message}")
            }
        }
    }

    fun shareBundle(context: Context) {
        val bytes = _uiState.value.zipBytes ?: return
        val fileName = _uiState.value.bundleFileName
        try {
            shareManager.shareZipBundle(context, fileName, bytes)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(statusMessage = "Share failed: ${e.message}")
            }
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
