package com.example.songdice.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.ui.components.AnimatedDiceRollButton
import com.example.songdice.ui.components.DiceCard
import com.example.songdice.ui.components.GenreBpmHeader
import com.example.songdice.ui.components.PianoRollView
import com.example.songdice.ui.components.StylePromptInput
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDiceScreen(
    viewModel: SongDiceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val lockedCount = uiState.diceStates.values.count { it.isLocked }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonViolet.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonViolet)
                        ) {
                            Box(
                                modifier = Modifier.padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Song Dice Logo",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "SONG DICE",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "AI MULTI-TRACK ARRANGEMENT GENERATOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = StudioBackground
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Genre & BPM Header
                item {
                    GenreBpmHeader(
                        selectedGenre = uiState.selectedGenre,
                        bpm = uiState.bpm,
                        onGenreSelected = { viewModel.setGenre(it) },
                        onBpmChanged = { viewModel.setBpm(it) }
                    )
                }

                // Section 1.5: Gemini AI Style Prompt & Vibe Input
                item {
                    StylePromptInput(
                        prompt = uiState.userStylePrompt,
                        onPromptChange = { viewModel.setUserStylePrompt(it) },
                        onApplyStyleRoll = { viewModel.rollDiceWithStyle(it) }
                    )
                }

                // Section 2: Master Controls & Roll Actions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioSurface),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Animated Master Roll Button
                            AnimatedDiceRollButton(
                                isRolling = uiState.isLoading,
                                lockedCount = lockedCount,
                                onClick = { viewModel.rollAllDice() }
                            )

                            // Play Preview & Export MIDI Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Play / Pause Audio Synthesizer Preview
                                Button(
                                    onClick = { viewModel.togglePlayPreview() },
                                    enabled = uiState.currentArrangement != null && !uiState.isLoading,
                                    modifier = Modifier
                                        .testTag("play_preview_button")
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.isPlaying) NeonPink else NeonCyan,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play preview",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.isPlaying) "PAUSE" else "PREVIEW",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Export MIDI File Button
                                OutlinedButton(
                                    onClick = { viewModel.exportMidi(context) },
                                    enabled = uiState.currentArrangement != null && !uiState.isLoading,
                                    modifier = Modifier
                                        .testTag("export_midi_button")
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, NeonAmber),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NeonAmber
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Export MIDI",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "EXPORT MIDI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Interactive Dice Grid (2 Columns, 3 Rows)
                item {
                    Text(
                        text = "THE DICE PARAMETERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val diceList = DiceParameter.entries
                        for (i in diceList.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val param1 = diceList[i]
                                val state1 = uiState.diceStates[param1] ?: return@Row
                                DiceCard(
                                    diceState = state1,
                                    onToggleLock = { viewModel.toggleLock(param1) },
                                    onRerollSingle = { viewModel.rerollSingle(param1) },
                                    modifier = Modifier.weight(1f)
                                )

                                if (i + 1 < diceList.size) {
                                    val param2 = diceList[i + 1]
                                    val state2 = uiState.diceStates[param2] ?: return@Row
                                    DiceCard(
                                        diceState = state2,
                                        onToggleLock = { viewModel.toggleLock(param2) },
                                        onRerollSingle = { viewModel.rerollSingle(param2) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Multi-Track Piano Roll Visualizer & Studio Mixer
                item {
                    PianoRollView(
                        arrangement = uiState.currentArrangement,
                        playbackProgressBeats = uiState.playbackProgressBeats,
                        isPlaying = uiState.isPlaying,
                        trackMutes = uiState.trackMutes,
                        trackSolos = uiState.trackSolos,
                        trackVolumes = uiState.trackVolumes,
                        masterReverb = uiState.masterReverb,
                        masterDelay = uiState.masterDelay,
                        onToggleMute = { viewModel.toggleTrackMute(it) },
                        onToggleSolo = { viewModel.toggleTrackSolo(it) },
                        onVolumeChange = { name, vol -> viewModel.setTrackVolume(name, vol) },
                        onMasterReverbChange = { viewModel.setMasterReverb(it) },
                        onMasterDelayChange = { viewModel.setMasterDelay(it) }
                    )
                }

                // Arrangement Details Info Footer
                if (uiState.currentArrangement != null) {
                    val arrangement = uiState.currentArrangement!!
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CURRENT ARRANGEMENT: ${arrangement.title.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Key: ${arrangement.key} • ${arrangement.genre} • ${arrangement.bpm} BPM • Progression: ${arrangement.progression}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Loading Overlay with Circular Progress
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioSurface),
                        border = BorderStroke(1.dp, NeonViolet)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI COMPOSING ARRANGEMENT...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Synthesizing multi-track MIDI matrix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
