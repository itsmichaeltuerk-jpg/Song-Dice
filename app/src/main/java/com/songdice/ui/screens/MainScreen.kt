package com.songdice.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.songdice.ui.viewmodel.SongDiceViewModel

/**
 * Modern 2-Tab Studio Architecture Main Screen.
 * Hardware console header with dynamic primary tab navigation:
 * - Tab 0: 🎲 Roll Studio (RollScreen)
 * - Tab 1: 🎧 Player & Stems (PlayerScreen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SongDiceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF launcher for direct local file export
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportToUri(context, it) }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Song Dice Logo",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                    text = "STUDIO CONSOLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "OFFLINE STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Material 3 Primary Tab Row
                PrimaryTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("primary_tab_row")
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🎲 Roll Studio",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_roll_studio")
                    )

                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🎧 Player & Stems",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_player_stems")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    RollScreen(
                        blueprint = uiState.blueprint,
                        rollingState = uiState.rollingState,
                        onRollAll = { viewModel.rollAllDice() },
                        onRollSingle = { viewModel.rollSingleDie(it) },
                        onToggleLock = { viewModel.toggleLock(it) },
                        onSetGenre = { viewModel.setGenre(it) },
                        onSetBpm = { viewModel.setBpm(it) },
                        onPreviewRoll = { viewModel.previewRoll() }
                    )
                }
                1 -> {
                    PlayerScreen(
                        blueprint = uiState.blueprint,
                        isPlaying = uiState.isPlaying,
                        progress = uiState.progress,
                        positionMs = uiState.positionMs,
                        durationMs = uiState.durationMs,
                        currentBar = uiState.currentBar,
                        currentBeat = uiState.currentBeat,
                        mutedChannels = uiState.mutedChannels,
                        soloedChannels = uiState.soloedChannels,
                        exportReady = uiState.exportReady,
                        bundleFileName = uiState.bundleFileName,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekTo(it) },
                        onToggleMute = { viewModel.toggleMute(it) },
                        onToggleSolo = { viewModel.toggleSolo(it) },
                        onSaveToDevice = { safLauncher.launch(uiState.bundleFileName) },
                        onShareBundle = { viewModel.shareBundle(context) }
                    )
                }
            }
        }
    }
}
