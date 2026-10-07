package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickCommand(
    val labelEn: String,
    val labelTe: String,
    val commandEn: String,
    val commandTe: String,
    val icon: ImageVector
)

private val QUICK_COMMANDS = listOf(
    QuickCommand("Diagnostics", "స్టేటస్", "System status report", "సిస్టమ్ ఎలా ఉంది?", Icons.Default.Bolt),
    QuickCommand("YouTube", "యూట్యూబ్", "Open YouTube", "యూట్యూబ్ ఓపెన్ చేయి", Icons.Default.PlayArrow),
    QuickCommand("Camera", "కెమెరా", "Take a photo", "ఫోటో తీయి", Icons.Default.CameraAlt),
    QuickCommand("5m Timer", "5నిమి టైమర్", "Set timer for 5 minutes", "5 నిమిషాల టైమర్ పెట్టు", Icons.Default.Alarm),
    QuickCommand("Flashlight", "ఫ్లాష్ లైట్", "Turn on flashlight", "ఫ్లాష్ లైట్ ఆన్ చేయి", Icons.Default.FlashlightOn),
    QuickCommand("Battery", "బ్యాటరీ", "Battery status", "బ్యాటరీ ఎంత ఉంది?", Icons.Default.BatteryFull),
    QuickCommand("Chrono", "సమయం", "What time is it?", "సమయం ఎంత?", Icons.Default.Schedule)
)

@Composable
fun QuickCommandChips(
    isTelugu: Boolean,
    primaryColor: Color,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QUICK_COMMANDS.forEach { cmd ->
            val label = if (isTelugu) cmd.labelTe else cmd.labelEn
            val command = if (isTelugu) cmd.commandTe else cmd.commandEn

            AssistChip(
                onClick = { onCommandClick(command) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = cmd.icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}
