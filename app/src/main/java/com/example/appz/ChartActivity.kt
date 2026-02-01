package com.example.appz

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt


class ChartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppzTheme {
                ChartScreen { finish() }
            }
        }
    }
}

fun formatToHoursAndMinutesChart(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "%d:%02d".format(h, m)
}

fun formatToHoursAndMinutesText(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val gson = remember { Gson() }

    val componentActivity = (context as? ComponentActivity)
    val json = remember { componentActivity?.intent?.getStringExtra("timeline_json") ?: "[]" }
    val currentBac = remember { componentActivity?.intent?.getDoubleExtra("current_bac", 0.0) ?: 0.0 }
    val peakBac = remember { componentActivity?.intent?.getDoubleExtra("peak_bac", 0.0) ?: 0.0 }
    val peakTime = remember { componentActivity?.intent?.getDoubleExtra("peak_time", 0.0) ?: 0.0 }
    val measurementTime = remember { componentActivity?.intent?.getDoubleExtra("measurement_time", 0.0) ?: 0.0 }
    val soberTime = remember { componentActivity?.intent?.getIntExtra("sober_time", 0) ?: 0 }
    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
    val rawList: List<Map<String, Any>> = try {
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    val points = rawList.mapNotNull { map ->
        val tNum = map["t_h"] as? Number
        val bacNum = map["bac"] as? Number
        if (tNum != null && bacNum != null) {
            Pair(tNum.toDouble(), bacNum.toDouble())
        } else null
    }.sortedBy { it.first }

    val brownColor = Color(0xFF40351E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradientBrush)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {  StrokedText(
                        text = "Wykres BAC",
                        fillColor = brownColor,
                        strokeColor = brownColor,
                        strokeWidth = 1f,
                        style = MaterialTheme.typography.titleLarge,
                        shadowColor = Color.Black,
                        shadowOffset = Offset(1f, 1f),
                        shadowBlurRadius = 1f
                    ) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Wróć",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    },
                    colors = AppTopBarColors
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))

                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StrokedText(
                            text = "Legenda wykresu:",
                            fillColor = brownColor,
                            strokeColor = brownColor,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleSmall,
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StrokedText(
                            text = "🔴 Peak BAC: ${String.format("%.3f", peakBac)} ‰ (po ${formatToHoursAndMinutesText(peakTime)})",
                            fillColor = brownColor,
                            strokeColor = brownColor,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleMedium,
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f
                        )
                        StrokedText(
                            text = "\uD83D\uDFE2 Obecny BAC: ${String.format("%.3f", currentBac)} ‰ (po ${formatToHoursAndMinutesText(measurementTime)})",
                            fillColor = brownColor,
                            strokeColor = brownColor,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleMedium,
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f
                        )
                        StrokedText(
                            text = "⏱️ Czas do wytrzeźwienia: $soberTime godz. (od teraz)",
                            fillColor = brownColor,
                            strokeColor = brownColor,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleMedium,
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        StrokedText(
                            text = "Przebieg BAC w czasie",
                            fillColor = brownColor,
                            strokeColor = brownColor,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleSmall.copy(
                                textAlign = TextAlign.Center
                            ),
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .height(210.dp)
                                .fillMaxWidth()
                        ) {
                            if (points.isNotEmpty()) {
                                EnhancedLineChart(
                                    points = points,
                                    peakTime = peakTime,
                                    peakBac = peakBac,
                                    measurementTime = measurementTime,
                                    currentBac = currentBac
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    StrokedText(
                                        text = "Brak danych do wykresu",
                                        fillColor = brownColor,
                                        strokeColor = brownColor,
                                        strokeWidth = 1f,
                                        style = MaterialTheme.typography.titleSmall,
                                        shadowColor = Color.Black,
                                        shadowOffset = Offset(1f, 1f),
                                        shadowBlurRadius = 1f
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        StrokedText(
                            text = "Szczegóły (czas [h] / BAC [‰]):",
                            fillColor = brownColor,
                            strokeColor = brownColor,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleSmall.copy(
                                textAlign = TextAlign.Center
                            ),
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            items(points) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val label = when {
                                        kotlin.math.abs(p.first - peakTime) < 0.1 -> "🔴 ${formatToHoursAndMinutesText(p.first)} (PEAK)"
                                        kotlin.math.abs(p.first - measurementTime) < 0.1 -> "📍 ${formatToHoursAndMinutesText(p.first)} (TERAZ)"
                                        else -> formatToHoursAndMinutesText(p.first)
                                    }
                                    Text(text = label, color = brownColor)
                                    Text(text = "${"%.3f".format(p.second)} ‰", color = brownColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedLineChart(
    points: List<Pair<Double, Double>>,
    peakTime: Double,
    peakBac: Double,
    measurementTime: Double,
    currentBac: Double
) {
    val maxX = (points.maxOfOrNull { it.first } ?: 1.0)
    val minX = (points.minOfOrNull { it.first } ?: 0.0)
    val maxY = max((points.maxOfOrNull { it.second } ?: 0.1), 0.1)
    val minY = min((points.minOfOrNull { it.second } ?: 0.0), 0.0)

    val axisPaint = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        color = Color(0xFF40351E).toArgb()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val paddingLeft = 50f
        val paddingRight = 12f
        val paddingTop = 16f
        val paddingBottom = 32f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val ySteps = 5
        for (i in 0..ySteps) {
            val frac = i.toFloat() / ySteps
            val y = paddingTop + chartHeight * (1f - frac)
            drawLine(
                color = Color.Black.copy(alpha = 0.1f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
            val yValue = minY + (maxY - minY) * frac
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.2f", yValue),
                6f,
                y + 8f,
                axisPaint
            )
        }

        val xSteps = 6
        for (i in 0..xSteps) {
            val frac = i.toFloat() / xSteps
            val x = paddingLeft + chartWidth * frac
            drawLine(
                color = Color.Black.copy(alpha = 0.1f),
                start = Offset(x, paddingTop),
                end = Offset(x, paddingTop + chartHeight),
                strokeWidth = 1f
            )
            val xValue = minX + (maxX - minX) * frac
            drawContext.canvas.nativeCanvas.drawText(
                formatToHoursAndMinutesChart(xValue),
                x - 20f,
                size.height - 6f,
                axisPaint
            )
        }

        val path = Path()
        points.forEachIndexed { idx, p ->
            val px = ((p.first - minX) / max(1e-6, (maxX - minX))).toFloat()
            val py = ((p.second - minY) / max(1e-6, (maxY - minY))).toFloat()
            val x = paddingLeft + px * chartWidth
            val y = paddingTop + chartHeight * (1f - py)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF3B82F6),
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val peakPx = ((peakTime - minX) / max(1e-6, (maxX - minX))).toFloat()
        val peakPy = ((peakBac - minY) / max(1e-6, (maxY - minY))).toFloat()
        val peakX = paddingLeft + peakPx * chartWidth
        val peakY = paddingTop + chartHeight * (1f - peakPy)

        drawCircle(
            color = Color.Red,
            radius = 8f,
            center = Offset(peakX, peakY)
        )
        val currentPx = ((measurementTime - minX) / max(1e-6, (maxX - minX))).toFloat()
        val currentPy = ((currentBac - minY) / max(1e-6, (maxY - minY))).toFloat()
        val currentX = paddingLeft + currentPx * chartWidth
        val currentY = paddingTop + chartHeight * (1f - currentPy)

        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 8f,
            center = Offset(currentX, currentY)
        )
    }
}