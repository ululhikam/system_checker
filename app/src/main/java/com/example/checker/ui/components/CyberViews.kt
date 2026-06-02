package com.example.checker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checker.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas-based Sonar Radar Sweep animation.
 * Features rotating sweep, fading trailing sweep, expanding waves, and glowing threat nodes.
 */
@Composable
fun RadarSweepScanner(
    modifier: Modifier = Modifier,
    scannerColor: Color = NeonGreen,
    label: String = "PEMINDAIAN AKTIF..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    
    // Rotation angle
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRotation"
    )

    // Pulse diameter for sonar waves
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave"
    )

    // Bypassing coordinates for mock cyber-threat nodes
    val targetNodes = remember {
        listOf(
            Offset(0.35f, 0.25f),
            Offset(0.75f, 0.45f),
            Offset(0.20f, 0.65f),
            Offset(0.60f, 0.75f)
        )
    }

    // Glowing/Blinking values for nodes
    val nodeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NodeBlink"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(120.dp))
                .border(1.dp, CardBorder.copy(alpha = 0.3f), shape = RoundedCornerShape(120.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // 1. Draw static radar sonar grid circles
                drawCircle(
                    color = scannerColor.copy(alpha = 0.08f),
                    radius = radius * 0.33f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = scannerColor.copy(alpha = 0.08f),
                    radius = radius * 0.66f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = scannerColor.copy(alpha = 0.15f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 2. Draw static crosshair lines
                drawLine(
                    color = scannerColor.copy(alpha = 0.08f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = scannerColor.copy(alpha = 0.08f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // 3. Draw expanding sonar pulse waves
                drawCircle(
                    color = scannerColor.copy(alpha = (1f - wavePulse) * 0.25f),
                    radius = radius * wavePulse,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 4. Draw blinking simulated target nodes
                targetNodes.forEach { node ->
                    val nodePx = Offset(size.width * node.x, size.height * node.y)
                    drawCircle(
                        color = scannerColor.copy(alpha = nodeAlpha),
                        radius = 5.dp.toPx(),
                        center = nodePx
                    )
                    drawCircle(
                        color = scannerColor.copy(alpha = nodeAlpha * 0.3f),
                        radius = 12.dp.toPx(),
                        center = nodePx,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 5. Draw rotating sweep line with trailing gradient
                val rad = Math.toRadians(angle.toDouble())
                val sweepX = center.x + radius * cos(rad).toFloat()
                val sweepY = center.y + radius * sin(rad).toFloat()

                // Draw rotating radius sweep line
                drawLine(
                    color = scannerColor,
                    start = center,
                    end = Offset(sweepX, sweepY),
                    strokeWidth = 2.5.dp.toPx()
                )

                // Draw gradient sweep pie
                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        scannerColor.copy(alpha = 0.35f),
                        scannerColor.copy(alpha = 0.0f)
                    ),
                    center = center
                )
                
                // Draw arc gradient representing trailing radar beam
                drawArc(
                    brush = sweepBrush,
                    startAngle = angle - 45f,
                    sweepAngle = 45f,
                    useCenter = true,
                    size = Size(size.width, size.height),
                    topLeft = Offset.Zero
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = label,
            color = scannerColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Semi-circular Gauge Gauge chart to display percentages with glow effects.
 */
@Composable
fun GaugeChart(
    percentage: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    gaugeColor: Color = NeonGreen,
    label: String = "Trust Score"
) {
    val animatedPercentage = remember { Animatable(0f) }
    
    LaunchedEffect(percentage) {
        animatedPercentage.animateTo(
            targetValue = percentage,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(200.dp, 120.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val strokeWidthPx = 14.dp.toPx()
            
            // Core bounding box for the arc
            val arcSize = Size(canvasWidth - strokeWidthPx * 2, (canvasHeight - strokeWidthPx) * 2)
            val topLeft = Offset(strokeWidthPx, strokeWidthPx)

            // 1. Draw static background gray arc
            drawArc(
                color = CardBorder.copy(alpha = 0.6f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // 2. Draw glowing active color arc
            val sweepGradient = Brush.horizontalGradient(
                colors = listOf(
                    gaugeColor.copy(alpha = 0.4f),
                    gaugeColor
                )
            )

            drawArc(
                brush = sweepGradient,
                startAngle = 180f,
                sweepAngle = 180f * animatedPercentage.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // Draw a subtle shadow overlay underneath the tip to give glow depth
            val tipAngleRad = Math.toRadians((180f + 180f * animatedPercentage.value).toDouble())
            val rx = (canvasWidth - strokeWidthPx * 2) / 2f
            val ry = (canvasHeight - strokeWidthPx)
            val tipX = (canvasWidth / 2f) + rx * cos(tipAngleRad).toFloat()
            val tipY = canvasHeight + ry * sin(tipAngleRad).toFloat() - strokeWidthPx / 2
            
            drawCircle(
                color = gaugeColor,
                radius = 8.dp.toPx(),
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(tipX, tipY)
            )
        }

        // Percentage text overlay at the center base
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "${(animatedPercentage.value * 100).toInt()}%",
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = label.uppercase(),
                color = TextSteel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Real-time scrollable Cyber Terminal Security Logs view.
 */
@Composable
fun CyberpunkLogs(
    modifier: Modifier = Modifier,
    logs: List<String>,
    terminalColor: Color = NeonBlue
) {
    val listState = rememberLazyListState()

    // Auto-scroll terminal when new logs are added
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                Text(
                    text = "> $log",
                    color = terminalColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Stream cyber logs dynamically with typing simulation
 */
@Composable
fun rememberSimulatedLogs(
    triggerKey: Any?,
    logPool: List<String>
): State<List<String>> {
    val activeLogs = remember { mutableStateOf(listOf<String>()) }
    
    LaunchedEffect(triggerKey) {
        if (triggerKey == null) {
            activeLogs.value = emptyList()
            return@LaunchedEffect
        }
        activeLogs.value = listOf("Initializing connection...")
        logPool.forEachIndexed { index, log ->
            delay((200 + (100..400).random()).toLong())
            val current = activeLogs.value.toMutableList()
            current.add(log)
            activeLogs.value = current
        }
    }
    
    return activeLogs
}
