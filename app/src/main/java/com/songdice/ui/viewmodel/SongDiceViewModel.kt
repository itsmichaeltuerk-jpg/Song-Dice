package com.songdice.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.songdice.data.midi.MidiEncoder
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
import core.midi.builder.MidiBuilder
import core.midi.model.toMidiSong
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
 * UI State for Song Dice 2-Tab Studio Console architecture & MIDI audio engine interaction.
 */
data class SongDiceUiState(
    val selectedTab: Int = 0, // 0: Roll Studio, 1: Player & Stems
    val blueprint: SongBlueprint = SongBlueprint(),
    val rollingState: RollingState = RollingState.IDLE,
    val statusMessage: String? = null,
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val progress: Float = 0.0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentBar: Int = 1,
    val currentBeat: Int = 1,
    val mutedChannels: Set<Int> = emptySet(),
    val soloedChannels: Set<Int> = emptySet(),
    val zipBytes: ByteArray? = null,
    val midiBytes: ByteArray? = null,
    val bundleFileName: String = "Song_Dice_Export.zip",
    val exportReady: Boolean = false,
    val isDarkTheme: Boolean = true
)

/**
 * Primary ViewModel for Song Dice application coordinating offline roll generation,
 * 2-Tab Studio navigation, native MIDI playback state via AudioPlayer, and SAF / Share Sheet export.
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
            audioPlayer.currentBar.collect { bar ->
                _uiState.update { it.copy(currentBar = bar) }
            }
        }
        viewModelScope.launch {
            audioPlayer.currentBeat.collect { beat ->
                _uiState.update { it.copy(currentBeat = beat) }
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
     * Tab selection control: 0 = Roll Studio, 1 = Player & Stems
     */
    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex.coerceIn(0, 1)) }
    }

    /**
     * Executes complete 4-bar song idea roll generation flow:
     * - Rolls unlocked dice parameters
     * - Generates multi-track procedural arrangement
     * - Encodes Type 1 MIDI bytes and loads directly into native AudioPlayer
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

                val generatedMidiBytes = MidiBuilder.buildMidiFile(arrangement.toMidiSong(MidiEncoder.TICKS_PER_QUARTER_NOTE))

                // Load MIDI file directly into native AudioPlayer
                audioPlayer.loadMidiBytes(generatedMidiBytes)

                // Synthesize 4-asset .zip bundle
                val zipBytes = zipExporter.createZipBundle(newBlueprint)
                val fileName = zipExporter.getBundleFileName(newBlueprint)

                _uiState.update {
                    it.copy(
                        blueprint = newBlueprint,
                        rollingState = RollingState.READY,
                        zipBytes = zipBytes,
                        midiBytes = generatedMidiBytes,
                        bundleFileName = fileName,
                        exportReady = true,
                        statusMessage = "Song idea generated! Preview ready."
                    )
                }

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

    /**
     * Preview Roll action triggered from Roll Studio (Tab 1):
     * Generates roll, switches tab to Player & Stems (Tab 2), and starts playback immediately.
     */
    fun previewRoll() {
        rollAllDice()
        selectTab(1)
        audioPlayer.play()
    }

    /**
     * Re-rolls a single specified unlocked die parameter while leaving all other parameters intact.
     */
    fun rollSingleDie(parameter: DiceParameter) {
        val currentBlueprint = _uiState.value.blueprint
        val currentDice = currentBlueprint.diceStates
        val currentState = currentDice[parameter] ?: return

        if (currentState.isLocked) return // Do not re-roll locked die

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    rollingState = RollingState.GENERATING,
                    statusMessage = "Rolling ${parameter.displayName}..."
                )
            }

            try {
                val presets = repository.dicePresets[parameter] ?: listOf("Default")
                val newValue = presets.filter { it != currentState.value }.randomOrNull() ?: presets.random()

                val updatedDice = currentDice.toMutableMap()
                updatedDice[parameter] = currentState.copy(value = newValue)

                val genre = currentBlueprint.genre
                val bpm = currentBlueprint.bpm

                val arrangement = repository.generateProceduralArrangement(genre, bpm, updatedDice)

                val keyStr = updatedDice[DiceParameter.KEY]?.value ?: "C Major"
                val isMinor = keyStr.contains("Minor", ignoreCase = true) || keyStr.contains("m", ignoreCase = true)

                val newBlueprint = currentBlueprint.copy(
                    id = System.currentTimeMillis().toString(),
                    title = arrangement.title,
                    keySignature = keyStr,
                    scaleMode = if (isMinor) "Minor" else "Major",
                    progression = arrangement.progression,
                    arrangement = arrangement,
                    diceStates = updatedDice
                )

                val generatedMidiBytes = MidiBuilder.buildMidiFile(arrangement.toMidiSong(MidiEncoder.TICKS_PER_QUARTER_NOTE))

                audioPlayer.loadMidiBytes(generatedMidiBytes)

                val zipBytes = zipExporter.createZipBundle(newBlueprint)
                val fileName = zipExporter.getBundleFileName(newBlueprint)

                _uiState.update {
                    it.copy(
                        blueprint = newBlueprint,
                        rollingState = RollingState.READY,
                        zipBytes = zipBytes,
                        midiBytes = generatedMidiBytes,
                        bundleFileName = fileName,
                        exportReady = true,
                        statusMessage = "Rolled ${parameter.displayName}: $newValue"
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        rollingState = RollingState.ERROR,
                        statusMessage = "Error rolling ${parameter.displayName}: ${e.message}"
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
                val generatedMidiBytes = MidiBuilder.buildMidiFile(updatedArrangement.toMidiSong(MidiEncoder.TICKS_PER_QUARTER_NOTE))
                audioPlayer.loadMidiBytes(generatedMidiBytes)
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
