package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ConversationFeed
import com.example.ui.components.CoreOrbHud
import com.example.ui.components.FuturisticInputBar
import com.example.ui.components.QuickCommandChips
import com.example.ui.dialogs.HistoryDialog
import com.example.ui.dialogs.PermissionDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.theme.getAccentColors

@Composable
fun NovaMainScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val (primaryColor, secondaryColor, _) = getAccentColors(state.themeAccent)

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        } else {
            viewModel.openPermissionRationale(true)
        }
    }

    val onMicAction = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleVoiceListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("nova_main_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FuturisticInputBar(
                currentInput = state.currentInput,
                status = state.status,
                isTelugu = state.language == "te",
                primaryColor = primaryColor,
                onInputChange = viewModel::onInputChange,
                onSendCommand = viewModel::onSendTextCommand,
                onMicClick = { onMicAction() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Futuristic Top HUD App Bar
            HudTopBar(
                assistantName = state.assistantName,
                language = state.language,
                primaryColor = primaryColor,
                onToggleLanguage = {
                    val nextLang = if (state.language == "en") "te" else "en"
                    viewModel.updateLanguage(nextLang)
                },
                onOpenHistory = { viewModel.openHistory(true) },
                onOpenSettings = { viewModel.openSettings(true) }
            )

            // System Notice Flare (if any)
            AnimatedVisibility(
                visible = !state.lastSystemNotice.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = primaryColor.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.lastSystemNotice ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = primaryColor
                            )
                        )
                    }
                }
            }

            // Central Animated AI Core Orb HUD
            CoreOrbHud(
                status = state.status,
                assistantName = state.assistantName,
                rmsVolume = state.rmsVolume,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                onOrbClick = { onMicAction() }
            )

            // Quick Sci-Fi Directive Chips
            QuickCommandChips(
                isTelugu = state.language == "te",
                primaryColor = primaryColor,
                onCommandClick = viewModel::onQuickCommandSelected
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Conversation Log Area
            ConversationFeed(
                messages = state.messages,
                assistantName = state.assistantName,
                primaryColor = primaryColor,
                onReplaySpeech = viewModel::replayMessageSpeech,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    // Settings Dialog
    if (state.isSettingsOpen) {
        SettingsDialog(
            assistantName = state.assistantName,
            language = state.language,
            isVoiceEnabled = state.isVoiceEnabled,
            speechRate = state.speechRate,
            speechPitch = state.speechPitch,
            themeAccent = state.themeAccent,
            customApiKey = state.customApiKey,
            primaryColor = primaryColor,
            onSaveName = viewModel::updateAssistantName,
            onSaveLanguage = viewModel::updateLanguage,
            onSaveVoiceEnabled = viewModel::updateVoiceEnabled,
            onSaveSpeechRate = viewModel::updateSpeechRate,
            onSaveSpeechPitch = viewModel::updateSpeechPitch,
            onSaveThemeAccent = viewModel::updateThemeAccent,
            onSaveApiKey = viewModel::updateCustomApiKey,
            onDismiss = { viewModel.openSettings(false) }
        )
    }

    // History Dialog
    if (state.isHistoryOpen) {
        HistoryDialog(
            sessions = state.distinctSessions,
            activeSessionId = state.activeSessionId,
            primaryColor = primaryColor,
            onSelectSession = viewModel::switchSession,
            onNewSession = viewModel::startNewSession,
            onDeleteSession = viewModel::deleteSession,
            onClearAll = viewModel::clearAllHistory,
            onDismiss = { viewModel.openHistory(false) }
        )
    }

    // Permission Rationale Dialog
    if (state.isPermissionRationaleOpen) {
        PermissionDialog(
            primaryColor = primaryColor,
            assistantName = state.assistantName,
            onRequestPermission = {
                viewModel.openPermissionRationale(false)
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { viewModel.openPermissionRationale(false) }
        )
    }
}

@Composable
private fun HudTopBar(
    assistantName: String,
    language: String,
    primaryColor: Color,
    onToggleLanguage: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Brand & Designation
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = assistantName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = primaryColor
                        )
                    )
                    Text(
                        text = "TACTICAL AI MATRIX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Switch Chip
                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(6.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("language_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Switch language",
                            tint = primaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "te") "TEL" else "ENG",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = primaryColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // History Logs
                IconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Mission logs history",
                        tint = primaryColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = primaryColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
