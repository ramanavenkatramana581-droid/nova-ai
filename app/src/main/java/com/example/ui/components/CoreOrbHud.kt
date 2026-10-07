package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CoreOrbHud(
    status: AssistantStatus,
    assistantName: String,
    rmsVolume: Float,
    primaryColor: Color,
    secondaryColor: Color,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

    // Continuous rotation for outer HUD ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.PROCESSING -> 3000
                    AssistantStatus.SPEAKING -> 8000
                    AssistantStatus.LISTENING -> 6000
                    else -> 20000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Reverse rotation for middle HUD ring
    val middleRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.PROCESSING -> 2000
                    AssistantStatus.SPEAKING -> 5000
                    else -> 14000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "middle_rotation"
    )

    // Core breathing pulsation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.LISTENING -> 800
                    AssistantStatus.PROCESSING -> 600
                    AssistantStatus.SPEAKING -> 1000
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Scanner beam rotation (for processing state)
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_angle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic HUD Header Text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .then(
                        Modifier.padding(0.dp)
                    )
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val statusDotColor = when (status) {
                        AssistantStatus.LISTENING -> Color(0xFF00E5FF)
                        AssistantStatus.PROCESSING -> Color(0xFFFFD600)
                        AssistantStatus.SPEAKING -> Color(0xFF00FF88)
                        AssistantStatus.ACTION_EXECUTED -> Color(0xFFFF9100)
                        AssistantStatus.IDLE -> primaryColor
                    }
                    drawCircle(color = statusDotColor)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$assistantName // ${status.label}",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            )
        }

        // Animated Sci-Fi AI Core Orb Canvas
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOrbClick
                )
                .testTag("ai_core_orb"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f - 4.dp.toPx()

                // Audio reactive boost during listening
                val audioBoost = if (status == AssistantStatus.LISTENING) rmsVolume * 24f else 0f
                val activeRadius = (maxRadius * 0.72f * pulseScale) + audioBoost

                // Layer 1: Outermost Ambient Glow Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (status == AssistantStatus.LISTENING) 0.35f else 0.18f),
                            secondaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )

                // Layer 2: Outer Segmented HUD Ring (Rotates)
                rotate(outerRotation, pivot = center) {
                    val outerStroke = Stroke(
                        width = 1.8f.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f, 10f, 15f), 0f)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.55f),
                        radius = maxRadius * 0.94f,
                        center = center,
                        style = outerStroke
                    )

                    // Cardinal tick crosshairs on outer ring
                    val tickLength = 10.dp.toPx()
                    val tickRadius = maxRadius * 0.94f
                    for (i in 0 until 4) {
                        val angleRad = (i * 90f) * (PI / 180f).toFloat()
                        val start = Offset(
                            center.x + (tickRadius - tickLength) * cos(angleRad),
                            center.y + (tickRadius - tickLength) * sin(angleRad)
                        )
                        val end = Offset(
                            center.x + (tickRadius + 4f) * cos(angleRad),
                            center.y + (tickRadius + 4f) * sin(angleRad)
                        )
                        drawLine(
                            color = primaryColor,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Layer 3: Middle Counter-Rotating Angular Ring
                rotate(middleRotation, pivot = center) {
                    val middleStroke = Stroke(
                        width = 1.2f.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 24f), 0f)
                    )
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.6f),
                        radius = maxRadius * 0.82f,
                        center = center,
                        style = middleStroke
                    )

                    // Triangular orbital telemetry nodes
                    for (i in 0 until 3) {
                        val nodeAngle = (i * 120f) * (PI / 180f).toFloat()
                        val nodeOffset = Offset(
                            center.x + (maxRadius * 0.82f) * cos(nodeAngle),
                            center.y + (maxRadius * 0.82f) * sin(nodeAngle)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx(),
                            center = nodeOffset
                        )
                    }
                }

                // Layer 4: Processing State Laser Radar Sweep
                if (status == AssistantStatus.PROCESSING) {
                    rotate(scanAngle, pivot = center) {
                        val scanPath = Path().apply {
                            moveTo(center.x, center.y)
                            val rad = 45f * (PI / 180f).toFloat()
                            lineTo(
                                center.x + maxRadius * cos(rad),
                                center.y + maxRadius * sin(rad)
                            )
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    center.x - maxRadius,
                                    center.y - maxRadius,
                                    center.x + maxRadius,
                                    center.y + maxRadius
                                ),
                                startAngleDegrees = 45f,
                                sweepAngleDegrees = -45f,
                                forceMoveTo = false
                            )
                            close()
                        }
                        drawPath(
                            path = scanPath,
                            brush = Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                                center = center,
                                radius = maxRadius
                            )
                        )
                    }
                }

                // Layer 5: Speaking / Listening Waveform Rings
                if (status == AssistantStatus.SPEAKING || status == AssistantStatus.LISTENING) {
                    val ringAlpha = if (status == AssistantStatus.SPEAKING) 0.65f else 0.45f
                    for (wave in 1..3) {
                        val waveRadius = activeRadius * (0.6f + wave * 0.16f)
                        drawCircle(
                            color = primaryColor.copy(alpha = ringAlpha / wave),
                            radius = waveRadius,
                            center = center,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                // Layer 6: Inner Glowing Arc Reactor Singularity
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = activeRadius * 0.62f
                    ),
                    radius = activeRadius * 0.62f,
                    center = center
                )

                // Layer 7: Quantum Singularity Core Center
                val coreRadius = activeRadius * 0.28f
                drawCircle(
                    color = Color.White,
                    radius = coreRadius,
                    center = center
                )

                // Layer 8: Inner Hexagonal Shield Ring
                val hexRadius = activeRadius * 0.46f
                val hexPath = Path()
                for (i in 0 until 6) {
                    val angle = (i * 60f - 30f) * (PI / 180f).toFloat()
                    val x = center.x + hexRadius * cos(angle)
                    val y = center.y + hexRadius * sin(angle)
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()

                drawPath(
                    path = hexPath,
                    color = primaryColor.copy(alpha = 0.75f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }

        // Subtitle Tip
        Text(
            text = when (status) {
                AssistantStatus.LISTENING -> "LISTENING... SPEAK YOUR DIRECTIVE"
                AssistantStatus.PROCESSING -> "ANALYZING ACOUSTIC TELEMETRY..."
                AssistantStatus.SPEAKING -> "TRANSMITTING VOCAL FEEDBACK (TAP TO STOP)"
                AssistantStatus.ACTION_EXECUTED -> "OPERATION EXECUTED SUCCESSFULLY"
                AssistantStatus.IDLE -> "TAP MIC OR CORE TO TRANSMIT"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
