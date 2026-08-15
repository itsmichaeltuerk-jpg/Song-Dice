package com.example.songdice.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.songdice.audio.AudioSynthPlayer
import com.example.songdice.data.midi.MidiEncoder
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.model.MusicalGenre
import com.example.songdice.data.model.SongArrangement
import com.example.songdice.data.repository.SongDiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SongDiceUiState(
    val diceStates: Map<DiceParameter, DiceState> = emptyMap(),
    val selectedGenre: MusicalGenre = MusicalGenre.SYNTHWAVE,
    val bpm: Int = MusicalGenre.SYNTHWAVE.defaultBpm,
    val userStylePrompt: String = "",
    val currentArrangement: SongArrangement? = null,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isPlaying: Boolean = false,
    val playbackProgressBeats: Double = 0.0,
    val exportedFile: File? = null,
    val trackMutes: Map<String, Boolean> = mapOf("Drums" to false, "Bass" to false, "Chords" to false, "Melody" to false),
    val trackSolos: Map<String, Boolean> = mapOf("Drums" to false, "Bass" to false, "Chords" to false, "Melody" to false),
    val trackVolumes: Map<String, Float> = mapOf("Drums" to 0.95f, "Bass" to 0.90f, "Chords" to 0.85f, "Melody" to 0.90f),
    val masterReverb: Float = 0.35f,
    val masterDelay: Float = 0.25f
)

class SongDiceViewModel(
    private val repository: SongDiceRepository = SongDiceRepository(),
    private val audioPlayer: AudioSynthPlayer = AudioSynthPlayer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongDiceUiState())
    val uiState: StateFlow<SongDiceUiState> = _uiState.asStateFlow()

    init {
        // Initialize default dice states
        val initialDice = DiceParameter.entries.associateWith { param ->
            val presetValue = repository.dicePresets[param]?.firstOrNull() ?: "Default"
            DiceState(parameter = param, value = presetValue, isLocked = false)
        }
        _uiState.update { it.copy(diceStates = initialDice) }

        // Observe audio player state
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }
        viewModelScope.launch {
            audioPlayer.playbackProgressBeats.collect { beats ->
                _uiState.update { it.copy(playbackProgressBeats = beats) }
            }
        }

        // Perform initial master roll on startup
        rollAllDice()
    }

    fun setUserStylePrompt(prompt: String) {
        _uiState.update { it.copy(userStylePrompt = prompt) }
    }

    fun toggleLock(parameter: DiceParameter) {
        _uiState.update { state ->
            val current = state.diceStates[parameter] ?: return@update state
            val updated = state.diceStates.toMutableMap()
            updated[parameter] = current.copy(isLocked = !current.isLocked)
            state.copy(diceStates = updated)
        }
    }

    fun setGenre(genre: MusicalGenre) {
        _uiState.update {
            it.copy(
                selectedGenre = genre,
                bpm = genre.defaultBpm
            )
        }
    }

    fun setBpm(bpm: Int) {
        _uiState.update { it.copy(bpm = bpm.coerceIn(40, 280)) }
    }

    fun toggleTrackMute(trackName: String) {
        _uiState.update { state ->
            val updated = state.trackMutes.toMutableMap()
            val newMute = !(updated[trackName] ?: false)
            updated[trackName] = newMute
            audioPlayer.setTrackMute(trackName, newMute)
            state.copy(trackMutes = updated)
        }
    }

    fun toggleTrackSolo(trackName: String) {
        _uiState.update { state ->
            val updated = state.trackSolos.toMutableMap()
            val newSolo = !(updated[trackName] ?: false)
            updated[trackName] = newSolo
            audioPlayer.setTrackSolo(trackName, newSolo)
            state.copy(trackSolos = updated)
        }
    }

    fun setTrackVolume(trackName: String, volume: Float) {
        _uiState.update { state ->
            val updated = state.trackVolumes.toMutableMap()
            updated[trackName] = volume
            audioPlayer.setTrackVolume(trackName, volume)
            state.copy(trackVolumes = updated)
        }
    }

    fun setMasterReverb(volume: Float) {
        _uiState.update { state ->
            audioPlayer.setMasterReverb(volume)
            state.copy(masterReverb = volume)
        }
    }

    fun setMasterDelay(volume: Float) {
        _uiState.update { state ->
            audioPlayer.setMasterDelay(volume)
            state.copy(masterDelay = volume)
        }
    }

    fun rollAllDice(stylePromptOverride: String? = null) {
        viewModelScope.launch {
            audioPlayer.stop()

            val effectivePrompt = stylePromptOverride ?: _uiState.value.userStylePrompt
            if (stylePromptOverride != null) {
                _uiState.update { it.copy(userStylePrompt = stylePromptOverride) }
            }

            val currentDice = _uiState.value.diceStates
            val rolledDice = repository.rollDice(currentDice, effectivePrompt)

            val status = if (effectivePrompt.isNotBlank()) {
                "Rolling dice with AI style influence: \"$effectivePrompt\"..."
            } else {
                "Rolling dice & generating AI multi-track arrangement..."
            }

            _uiState.update {
                it.copy(
                    diceStates = rolledDice,
                    isLoading = true,
                    statusMessage = status
                )
            }

            fetchArrangement(rolledDice, effectivePrompt)
        }
    }

    fun rollDiceWithStyle(prompt: String) {
        setUserStylePrompt(prompt)
        rollAllDice(stylePromptOverride = prompt)
    }

    fun rerollUnlocked() {
        rollAllDice()
    }

    fun rerollSingle(parameter: DiceParameter) {
        viewModelScope.launch {
            audioPlayer.stop()

            val currentDice = _uiState.value.diceStates.toMutableMap()
            val existingState = currentDice[parameter] ?: return@launch
            val options = repository.dicePresets[parameter] ?: listOf("Default")
            val newValue = options.filter { it != existingState.value }.randomOrNull() ?: options.first()

            currentDice[parameter] = existingState.copy(value = newValue, isLocked = false)

            _uiState.update {
                it.copy(
                    diceStates = currentDice,
                    isLoading = true,
                    statusMessage = "Rerolling ${parameter.displayName} & adapting arrangement..."
                )
            }

            fetchArrangement(currentDice, _uiState.value.userStylePrompt)
        }
    }

    private suspend fun fetchArrangement(
        dice: Map<DiceParameter, DiceState>,
        stylePrompt: String = ""
    ) {
        try {
            val arrangement = repository.generateArrangement(
                genre = _uiState.value.selectedGenre,
                bpm = _uiState.value.bpm,
                diceStates = dice,
                userStylePrompt = stylePrompt,
                previousArrangement = _uiState.value.currentArrangement
            )

            _uiState.update {
                it.copy(
                    currentArrangement = arrangement,
                    isLoading = false,
                    statusMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = "Error generating arrangement: ${e.message}"
                )
            }
        }
    }

    fun togglePlayPreview() {
        val arrangement = _uiState.value.currentArrangement ?: return
        if (_uiState.value.isPlaying) {
            audioPlayer.stop()
        } else {
            audioPlayer.play(arrangement, viewModelScope)
        }
    }

    fun exportMidi(context: Context) {
        val arrangement = _uiState.value.currentArrangement ?: return

        try {
            // Sanitize file name to guarantee explicit .mid extension and clean alphanumeric characters
            val safeTitle = arrangement.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
            val fileName = "${safeTitle}_${arrangement.bpm}bpm.mid"

            // Save to internal app cache for FileProvider share
            val midiFile = File(context.cacheDir, fileName)
            MidiEncoder.createMidiFile(arrangement, midiFile)

            // Save directly into device Public Downloads folder for FL Studio Mobile / DAW local file browser discovery
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "audio/midi")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            midiFile.inputStream().copyTo(os)
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val publicCopy = File(downloadsDir, fileName)
                    midiFile.copyTo(publicCopy, overwrite = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _uiState.update {
                it.copy(
                    exportedFile = midiFile,
                    statusMessage = "MIDI File ($fileName) saved to Downloads. Ready for FL Studio!"
                )
            }

            // Trigger system share / open chooser with FileProvider URI
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                midiFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/midi"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "MIDI Export: $fileName")
                putExtra(Intent.EXTRA_TEXT, "Song Dice Standard MIDI File (.mid) arrangement: ${arrangement.title} (${arrangement.genre}, ${arrangement.bpm} BPM)")
                clipData = android.content.ClipData.newRawUri(fileName, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Open or Import $fileName in FL Studio"))

        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "MIDI Export Failed: ${e.message}") }
        }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
