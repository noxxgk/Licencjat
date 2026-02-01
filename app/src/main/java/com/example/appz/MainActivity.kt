package com.example.appz


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlin.math.ceil
import kotlin.math.max
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import kotlin.math.roundToInt

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown


val DarkBrown = Color(0xFF40351E)
val LightBrown = Color(0xFF7A6C4F)
val HighlightRed = Color(0xFFFF0000)


fun formatToHoursAndMinutes(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}min"
}

fun generateBacTimeline(
    volumeMl: Double,
    percent: Double,
    weightKg: Double,
    rEff: Double,
    fAbs: Double,
    bodyTypeFactor: Double,
    betaPerHour: Double,
    durationHours: Double,
    stepMinutes: Int = 10,
    maxHours: Double = 72.0
): List<Map<String, Any>> {
    val A_g_total = volumeMl * (percent / 100.0) * 0.8
    val totalAbsorbable = A_g_total * fAbs * bodyTypeFactor

    val inputRate = if (durationHours > 0.0) totalAbsorbable / durationHours else totalAbsorbable

    val kAbsBase = 1.0
    val dt = stepMinutes.toDouble() / 60.0
    val metabolismGramsPerHour = betaPerHour * (weightKg * rEff)

    val points = mutableListOf<Map<String, Any>>()
    var t = 0.0
    var S = 0.0
    var absorbed = 0.0
    var metabolized = 0.0

    while (t <= maxHours) {
        if (t <= durationHours) {
            S += inputRate * dt
            if (S + absorbed > totalAbsorbable) {
                val overflow = (S + absorbed) - totalAbsorbable
                S -= overflow.coerceAtMost(S)
            }
        }
        val deltaAbs = (kAbsBase * S) * dt
        val actuallyAbsorbed = deltaAbs.coerceAtMost(S)
        S -= actuallyAbsorbed
        absorbed += actuallyAbsorbed

        var gramsInBody = (absorbed - metabolized).coerceAtLeast(0.0)
        val desiredMetabolize = metabolismGramsPerHour * dt
        val deltaMet = desiredMetabolize.coerceAtMost(gramsInBody)
        metabolized += deltaMet
        gramsInBody = (absorbed - metabolized).coerceAtLeast(0.0)

        val bac = if ((weightKg * rEff) > 0.0) gramsInBody / (weightKg * rEff) else 0.0

        points.add(
            mapOf(
                "t_h" to t,
                "bac" to bac,
                "grams" to gramsInBody
            )
        )
        if (t > durationHours && gramsInBody <= 1e-4) break

        t += dt
    }

    return points
}

fun sampleBacFromTimeline(timeline: List<Map<String, Any>>, tFromStart: Double): Double {
    if (timeline.isEmpty()) return 0.0
    val points = timeline.mapNotNull {
        val t = (it["t_from_start"] as? Double) ?: (it["t_h"] as? Double)
        val bac = (it["bac"] as? Double) ?: (it["bac"] as? Number)?.toDouble()
        if (t != null && bac != null) Pair(t, bac) else null
    }.sortedBy { it.first }
    if (points.isEmpty()) return 0.0
    if (tFromStart <= points.first().first) return points.first().second
    if (tFromStart >= points.last().first) return points.last().second
    for (i in 0 until points.size - 1) {
        val (t0, b0) = points[i]
        val (t1, b1) = points[i + 1]
        if (tFromStart >= t0 && tFromStart <= t1) {
            val frac = if (t1 - t0 > 1e-9) (tFromStart - t0) / (t1 - t0) else 0.0
            return b0 + frac * (b1 - b0)
        }
    }
    return points.last().second
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, Notify::class.java)
        startService(intent)
        setContent {
            AppzTheme {
                AppContent(modifier = Modifier)
            }
        }
    }

    @Composable
    fun AppContent(modifier: Modifier) {
        var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

        if (isLoggedIn) {
            BACCalculatorUI(onLogout = {
                FirebaseAuth.getInstance().signOut()
                isLoggedIn = false
            })
        } else {
            navigateToLoginScreen()
        }
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun openMap() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("geo:?q=komisariat")
        }
        startActivity(intent)
    }

    @Composable
    fun BACCalculatorUI(onLogout: () -> Unit) {
        var permissionGranted by remember { mutableStateOf(checkNotificationPermission()) }
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionGranted = isGranted
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Brak zezwolenia ! Nie można wyświetlić powiadomienia.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        var age by remember { mutableStateOf(30) }
        var weight by remember { mutableStateOf(70f) }
        var gender by remember { mutableStateOf("Mężczyzna") }
        var bodyType by remember { mutableStateOf("Normalna") }
        var bodyFatEnabled by remember { mutableStateOf(false) }
        var bodyFatPercent by remember { mutableStateOf(20f) }
        var alcoholVolume by remember { mutableStateOf("500") }
        var alcoholPercentage by remember { mutableStateOf("40") }
        var drinkingDuration by remember { mutableStateOf(1.0f) }
        var timeSinceLastDrink by remember { mutableStateOf(2f) }
        var mealBeforeDrinking by remember { mutableStateOf("Brak posiłku") }
        var liverHealth by remember { mutableStateOf(10f) }
        var drinkingPace by remember { mutableStateOf(1f) }
        var activityLevel by remember { mutableStateOf("umiarkowana") }
        var toleranceWeeks by remember { mutableStateOf(0f) }
        var cyclePhase by remember { mutableStateOf("faza folikularna") }
        var carbonationYes by remember { mutableStateOf(false) }
        var bacResult by remember { mutableStateOf(0.0) }
        var soberTime by remember { mutableStateOf(0) }
        var timeline by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var errorMessage by remember { mutableStateOf("") }
        var peakBac by remember { mutableStateOf(0.0) }
        var peakTime by remember { mutableStateOf(0.0) }
        var sicknesYes by remember { mutableStateOf(false)}
        var sicknessWarning by remember { mutableStateOf(false) }
        val firestore = Firebase.firestore
        LaunchedEffect(Unit) {
            if (!permissionGranted) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val first_pass_factor = 0.9
        val (rActivityMap, betaActivityMap) = Pair(
            mapOf("niska" to 1.00, "umiarkowana" to 1.02, "wysoka" to 1.04, "bardzo wysoka" to 1.06),
            mapOf("niska" to 0.95, "umiarkowana" to 1.00, "wysoka" to 1.10, "bardzo wysoka" to 1.20)
        )
        val (rPhase, fAbsPhase, betaPhase) = if (gender == "Kobieta") {
            when (cyclePhase) {
                "faza folikularna" -> Triple(1.00, 1.00, 1.00)
                "faza owulacyjna " -> Triple(1.00, 1.05, 1.02)
                "faza lutealna" -> Triple(1.02, 0.90, 0.98)
                "miesiączka" -> Triple(0.99, 1.00, 1.00)
                else -> Triple(1.00, 1.00, 1.00)
            }
        } else {
            Triple(1.00, 1.00, 1.00)
        }
        Box(
            modifier = Modifier.fillMaxSize().background(AppGradientBrush)
        ) {
            val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp + navigationBarHeight
                )
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFFFFF),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {



                            StrokedText(
                                text = "Dane osobowe",
                                modifier = Modifier.padding(top = 8.dp),
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleLarge,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )
                            StrokedText(
                                text = "Płeć:",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleMedium,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                listOf("Mężczyzna", "Kobieta").forEach { option ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 16.dp)
                                    ) {
                                        RadioButton(
                                            selected = gender == option,
                                            onClick = { gender = option },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFFf1c523),
                                                unselectedColor = Color(0xFF7A6C4F),
                                                disabledSelectedColor = Color(0xFF3A3960),
                                                disabledUnselectedColor = Color(0xFF3A3960)
                                            )
                                        )
                                        val optionColor = if (gender == option) LightBrown else DarkBrown

                                        StrokedText(
                                            text = option,
                                            fillColor = optionColor,
                                            strokeColor = optionColor,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                    }
                                }
                            }


                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StrokedText(
                                    text = "Wiek: ",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                StrokedText(
                                    text = "$age lat",
                                    fillColor = LightBrown,
                                    strokeColor = LightBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            CustomSlider(
                                value = age.toFloat(),
                                onValueChange = { age = it.toInt() },
                                valueRange = 18f..100f,
                                stepSize = 1f
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StrokedText(
                                    text = "Waga: ",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                StrokedText(
                                    text = "${weight.toInt()} kg",
                                    fillColor = LightBrown,
                                    strokeColor = LightBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            CustomSlider(
                                value = weight,
                                onValueChange = { weight = it },
                                valueRange = 40f..150f,
                                stepSize = 1f
                            )


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = bodyFatEnabled,
                                    onCheckedChange = { bodyFatEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFf1c523))
                                )
                                StrokedText(
                                    text = "Wprowadź %tkanki tłuszczowej (bodyfat):",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f)
                            }
                            AnimatedVisibility(visible = bodyFatEnabled) {
                                Column{

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StrokedText(
                                            text = "BodyFat: ",
                                            fillColor = DarkBrown,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                        StrokedText(
                                            text = "${bodyFatPercent.toInt()} %",
                                            fillColor = LightBrown,
                                            strokeColor = LightBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                    }
                                    CustomSlider(
                                        value = bodyFatPercent,
                                        onValueChange = { bodyFatPercent = it },
                                        valueRange = 5f..45f,
                                        stepSize = 1f
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFFFFF),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StrokedText(
                                text = "Parametry alkoholu",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleLarge,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )


                            OutlinedTextField(
                                value = alcoholVolume,
                                onValueChange = { alcoholVolume = it },
                                textStyle = TextStyle(
                                    color = LightBrown,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = DarkBrown,
                                    unfocusedTextColor = DarkBrown,
                                    focusedBorderColor = DarkBrown,
                                    unfocusedBorderColor = DarkBrown,
                                    cursorColor = DarkBrown
                                ),
                                label = {
                                    StrokedText(
                                        text = "Ilość alkoholu (ml)",
                                        fillColor = DarkBrown,
                                        strokeColor = DarkBrown,
                                        strokeWidth = 1f,
                                        style = MaterialTheme.typography.titleSmall,
                                        shadowColor = Color.Black,
                                        shadowOffset = Offset(1f, 1f),
                                        shadowBlurRadius = 1f
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = alcoholPercentage,
                                onValueChange = { alcoholPercentage = it },
                                textStyle = TextStyle(
                                    color = LightBrown,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = DarkBrown,
                                    unfocusedTextColor = DarkBrown,
                                    focusedBorderColor = DarkBrown,
                                    unfocusedBorderColor = DarkBrown,
                                    cursorColor = DarkBrown
                                ),
                                label = {  StrokedText(
                                    text = "Procent alkoholu (%)",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleSmall,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                ) },
                                modifier = Modifier.fillMaxWidth()
                            )


                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StrokedText(
                                    text = "Długość picia: ",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                StrokedText(
                                    text = formatToHoursAndMinutes(drinkingDuration.toDouble()),
                                    fillColor = LightBrown,
                                    strokeColor = LightBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            CustomSlider(
                                value = drinkingDuration,
                                onValueChange = { drinkingDuration = it },
                                valueRange = 0.1f..12f,
                                stepSize = 0.1f
                            )


                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StrokedText(
                                    text = "Czas od ostatniego wypicia: ",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                StrokedText(
                                    text = formatToHoursAndMinutes(timeSinceLastDrink.toDouble()),
                                    fillColor = LightBrown,
                                    strokeColor = LightBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            CustomSlider(
                                value = timeSinceLastDrink,
                                onValueChange = { timeSinceLastDrink = it },
                                valueRange = 0.5f..24f,
                                stepSize = 0.5f
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFFFFF),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StrokedText(
                                text = "Dodatkowe czynniki",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleLarge,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )

                            StrokedText(
                                text = "Sylwetka:",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleMedium,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )
                            DropdownMenuField(
                                value = bodyType,
                                options = listOf("Chuda", "Normalna", "Grubsza"),
                                onValueChange = { bodyType = it }
                            )

                            StrokedText(
                                text = "Posiłek przed piciem:",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleMedium,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )
                            DropdownMenuField(
                                value = mealBeforeDrinking,
                                options = listOf("Brak posiłku", "Lekki posiłek", "Obfity posiłek"),
                                onValueChange = { mealBeforeDrinking = it },


                                )


                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StrokedText(
                                    text = "Zdrowie wątroby: ",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                StrokedText(
                                    text = "${liverHealth.toInt()} / 10",
                                    fillColor = LightBrown,
                                    strokeColor = LightBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            CustomSlider(
                                value = liverHealth,
                                onValueChange = { liverHealth = it },
                                valueRange = 1f..10f,
                                stepSize = 1f
                            )


                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StrokedText(
                                    text = "Dynamika picia: ",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                StrokedText(
                                    text = "${drinkingPace.toInt()} / 10",
                                    fillColor = LightBrown,
                                    strokeColor = LightBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            CustomSlider(
                                value = drinkingPace,
                                onValueChange = { drinkingPace = it },
                                valueRange = 1f..10f,
                                stepSize = 1f
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StrokedText(
                                text = "Napoje gazowane:",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleMedium,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )
                            Row {
                                RadioButton(selected = carbonationYes, onClick = { carbonationYes = true },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor =Color(0xFFf1c523),
                                        unselectedColor = Color(0xFF7A6C4F),
                                        disabledSelectedColor = Color(0xFF3A3960),
                                        disabledUnselectedColor = Color(0xFF3A3960)
                                    )
                                )
                                StrokedText(
                                    text = "Tak",
                                    fillColor = if(carbonationYes) LightBrown else DarkBrown,
                                    strokeColor = if(carbonationYes) LightBrown else DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RadioButton(selected = !carbonationYes, onClick = { carbonationYes = false },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor =Color(0xFFf1c523),
                                        unselectedColor = Color(0xFF7A6C4F),
                                        disabledSelectedColor = Color(0xFF3A3960),
                                        disabledUnselectedColor = Color(0xFF3A3960)
                                    )
                                )
                                StrokedText(
                                    text = "Nie",
                                    fillColor = if(!carbonationYes) LightBrown else DarkBrown,
                                    strokeColor = if(!carbonationYes) LightBrown else DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }

                            StrokedText(
                                text = "Aktywność fizyczna:",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleMedium,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )
                            DropdownMenuField(
                                value = activityLevel,
                                options = listOf("niska", "umiarkowana", "wysoka", "bardzo wysoka"),
                                onValueChange = { activityLevel = it }
                            )


                            StrokedText(
                                text = "Przewlekła choroba lub stale przyjmowane leki: ",
                                fillColor = DarkBrown,
                                strokeColor = DarkBrown,
                                strokeWidth = 1f,
                                style = MaterialTheme.typography.titleMedium,
                                shadowColor = Color.Black,
                                shadowOffset = Offset(1f, 1f),
                                shadowBlurRadius = 1f
                            )

                            Row {
                                RadioButton(selected = sicknesYes, onClick = { sicknesYes = true },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor =Color(0xFFf1c523),
                                        unselectedColor = Color(0xFF7A6C4F),
                                        disabledSelectedColor = Color(0xFF3A3960),
                                        disabledUnselectedColor = Color(0xFF3A3960)
                                    )
                                )
                                StrokedText(
                                    text = "Tak",
                                    fillColor = if(sicknesYes) LightBrown else DarkBrown,
                                    strokeColor = if(sicknesYes) LightBrown else DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RadioButton(selected = !sicknesYes, onClick = { sicknesYes = false },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor =Color(0xFFf1c523),
                                        unselectedColor = Color(0xFF7A6C4F),
                                        disabledSelectedColor = Color(0xFF3A3960),
                                        disabledUnselectedColor = Color(0xFF3A3960)
                                    )
                                )
                                StrokedText(
                                    text = "Nie",
                                    fillColor = if(!sicknesYes) LightBrown else DarkBrown,
                                    strokeColor = if(!sicknesYes) LightBrown else DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }
                            if (gender == "Kobieta") {
                                StrokedText(
                                    text = "Faza cyklu menstruacyjnego:",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                                DropdownMenuField(
                                    value = cyclePhase,
                                    options = listOf("faza folikularna", "faza owulacyjna ", "faza lutealna", "miesiączka"),
                                    onValueChange = { cyclePhase = it }
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFFFFF),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Button(
                                onClick = {
                                    if (sicknesYes) {
                                        sicknessWarning = true
                                        errorMessage = ""
                                        timeline = emptyList()
                                        bacResult = 0.0
                                        peakBac = 0.0
                                        peakTime = 0.0
                                        soberTime = 0
                                        return@Button
                                    }
                                    sicknessWarning = false

                                    try {
                                        val alcoholVolumeValue = alcoholVolume.toDoubleOrNull()
                                        val alcoholPercentageValue = alcoholPercentage.toDoubleOrNull()
                                        if (alcoholVolumeValue == null || alcoholPercentageValue == null) {
                                            errorMessage = "Wprowadź poprawne wartości liczbowe"
                                            return@Button
                                        }

                                        val firstPassFactor = 0.9
                                        val alcoholGrams = alcoholVolumeValue * (alcoholPercentageValue / 100.0) * 0.8
                                        val bodyTypeFactor = when (bodyType) {
                                            "Chuda" -> 1.15
                                            "Normalna" -> 1.0
                                            "Grubsza" -> 0.85
                                            else -> 1.0
                                        }
                                        val carbonationFactor = if (carbonationYes) 1.15 else 1.0
                                        val mealFactor = when (mealBeforeDrinking) {
                                            "Brak posiłku" -> 1.2
                                            "Lekki posiłek" -> 1.0
                                            "Obfity posiłek" -> 0.8
                                            else -> 1.0
                                        }

                                        val ageYears = age.toDouble()
                                        val decades = max(0.0, (ageYears - 40.0) / 10.0)
                                        val ageFactorBeta = max(0.6, 1.0 - 0.02 * decades)
                                        val ageFactorR = max(0.9, 1.0 - 0.01 * decades)

                                        val rBase = if (gender == "Mężczyzna") 0.68 else 0.55
                                        val betaBase = if (gender == "Mężczyzna") 0.15 else 0.12
                                        val rActivity = rActivityMap[activityLevel] ?: 1.0
                                        val betaActivity = betaActivityMap[activityLevel] ?: 1.0

                                        val (liverToleranceFactor) = when (toleranceWeeks.toInt()) {
                                            in 0..1 -> Pair(0.9, 1.0)
                                            in 2..4 -> Pair(1.0, 1.0)
                                            in 5..12 -> Pair(1.05, 1.0)
                                            in 13..52 -> Pair(1.10, 0.95)
                                            else -> Pair(1.0, 1.0)
                                        }

                                        val bodyFatCorrection = if (bodyFatEnabled) {
                                            (1.0 - 0.01 * (bodyFatPercent - 20.0))
                                        } else 1.0


                                        val rEff = rBase * bodyFatCorrection * rActivity * ageFactorR * rPhase
                                        val fAbs = mealFactor * carbonationFactor * firstPassFactor * fAbsPhase


                                        val aEff = alcoholGrams * fAbs * bodyTypeFactor
                                        val bac0 = if (weight > 0f) aEff / (weight.toDouble() * rEff) else 0.0

                                        val toleranceFactorForFormula = 1.0 + 0.05 * (toleranceWeeks.toDouble() / 4.0)
                                        val activityFactorForFormula = 1.0 + 0.02 * (when (activityLevel) {
                                            "niska" -> 1.0
                                            "umiarkowana" -> 2.0
                                            "wysoka" -> 3.0
                                            "bardzo wysoka" -> 4.0
                                            else -> 1.0
                                        } - 1.0)

                                        var beta = betaBase
                                        beta *= (liverHealth.toDouble() / 10.0)
                                        beta *= toleranceFactorForFormula
                                        beta *= activityFactorForFormula
                                        beta *= ageFactorBeta
                                        beta *= betaPhase
                                        beta *= betaActivity
                                        beta *= liverToleranceFactor

                                        val estimatedHoursToSoberInitial = if (beta > 1e-6) kotlin.math.ceil(bac0 / beta).toDouble() else 48.0
                                        val totalSimHours = max(24.0, estimatedHoursToSoberInitial + drinkingDuration.toDouble() + 4.0)
                                        val timelineGenerated = generateBacTimeline(
                                            volumeMl = alcoholVolumeValue,
                                            percent = alcoholPercentageValue,
                                            weightKg = weight.toDouble(),
                                            rEff = rEff,
                                            fAbs = fAbs,
                                            bodyTypeFactor = bodyTypeFactor,
                                            betaPerHour = beta,
                                            durationHours = drinkingDuration.toDouble(),
                                            stepMinutes = 10,
                                            maxHours = totalSimHours
                                        )

                                        timeline = timelineGenerated
                                        peakBac = timeline.maxOfOrNull {
                                            (it["bac"] as? Number)?.toDouble() ?: 0.0
                                        } ?: 0.0

                                        peakTime = timeline.firstOrNull {
                                            ((it["bac"] as? Number)?.toDouble() ?: 0.0) >= peakBac - 0.001
                                        }?.let { (it["t_h"] as? Number)?.toDouble() } ?: 0.0

                                        val tMeasure = drinkingDuration.toDouble() + timeSinceLastDrink.toDouble()
                                        bacResult = sampleBacFromTimeline(timeline, tMeasure)

                                        val threshold = 0.00005
                                        val timeWhenZero = timeline.firstOrNull {
                                            val t = (it["t_h"] as? Number)?.toDouble() ?: Double.MAX_VALUE
                                            val b = (it["bac"] as? Number)?.toDouble() ?: 0.0
                                            t >= tMeasure && b <= threshold
                                        }?.let { (it["t_h"] as? Number)?.toDouble() }

                                        val soberHoursFromStart = timeWhenZero ?: (timeline.lastOrNull()?.get("t_h") as? Double ?: tMeasure)
                                        soberTime = max(0.0, soberHoursFromStart - tMeasure).let { ceil(it).toInt() }

                                        errorMessage = ""
                                    } catch (e: Exception) {
                                        errorMessage = "Wystąpił błąd podczas obliczeń: ${e.message}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf1c523))
                            ) {
                                StrokedText(
                                    text = "Oblicz BAC",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }
                            if (errorMessage.isNotEmpty() && !sicknessWarning) {
                                StrokedText(
                                    text = errorMessage,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fillColor = HighlightRed,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleSmall,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }


                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StrokedText(
                                        text = "Wyniki",
                                        fillColor = DarkBrown,
                                        strokeColor = DarkBrown,
                                        strokeWidth = 1f,
                                        style = MaterialTheme.typography.titleSmall,
                                        shadowColor = Color.Black,
                                        shadowOffset = Offset(1f, 1f),
                                        shadowBlurRadius = 1f
                                    )

                                    val tMeasure = drinkingDuration.toDouble() + timeSinceLastDrink.toDouble()
                                    val context = LocalContext.current

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (sicknessWarning) {
                                        StrokedText(
                                            text = "OSTRZEŻENIE: Picie alkoholu podczas choroby lub przyjmowania leków jest skrajnie niebezpieczne. Obliczenia w takiej sytuacji są niemiarodajne.",
                                            fillColor = HighlightRed,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleLarge,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                    } else if (timeline.isNotEmpty()) {

                                        StrokedText(
                                            text = "🔴 Peak BAC: ${String.format("%.3f", peakBac)} ‰",
                                            fillColor = DarkBrown,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                        StrokedText(
                                            text = "    (osiągnięty po ${formatToHoursAndMinutes(peakTime)})",
                                            fillColor = LightBrown,
                                            strokeColor = LightBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleSmall,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        StrokedText(

                                            text = "🟢 Obecny BAC: ${String.format("%.3f", bacResult)} ‰",
                                            fillColor = DarkBrown,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                        StrokedText(
                                            text = "    (po ${formatToHoursAndMinutes(tMeasure)} od początku)",
                                            fillColor = LightBrown,
                                            strokeColor = LightBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleSmall,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        StrokedText(
                                            text = "⏱️ Czas do wytrzeźwienia: ${soberTime} godz.",
                                            fillColor = DarkBrown,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                        StrokedText(
                                            text = "    (od teraz = od ${formatToHoursAndMinutes(tMeasure)})",
                                            fillColor = LightBrown,
                                            strokeColor = LightBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleSmall,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                    } else {
                                        StrokedText(
                                            text = "Kliknij 'Oblicz BAC', aby zobaczyć wyniki.",
                                            fillColor = DarkBrown,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleSmall,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            val gson = Gson()
                                            val timelineJson = gson.toJson(timeline)
                                            val intent = Intent(context, ChartActivity::class.java).apply {
                                                putExtra("timeline_json", timelineJson)
                                                putExtra("current_bac", bacResult)
                                                putExtra("peak_bac", peakBac)
                                                putExtra("peak_time", peakTime)
                                                putExtra("measurement_time", tMeasure)
                                                putExtra("sober_time", soberTime)
                                            }
                                            context.startActivity(intent)
                                        },
                                        enabled = timeline.isNotEmpty() && bacResult >= 0.0,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFf1c523),
                                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        StrokedText(
                                            text = "Pokaż wykres",
                                            fillColor = DarkBrown,
                                            strokeColor = DarkBrown,
                                            strokeWidth = 1f,
                                            style = MaterialTheme.typography.titleMedium,
                                            shadowColor = Color.Black,
                                            shadowOffset = Offset(1f, 1f),
                                            shadowBlurRadius = 1f
                                        )
                                    }
                                }
                            }

                            val auth = FirebaseAuth.getInstance()
                            Button(
                                onClick = {
                                    val auth = FirebaseAuth.getInstance()
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val inputs = mutableMapOf<String, Any>(
                                            "wiek" to age,
                                            "waga" to weight,
                                            "płeć" to gender,
                                            "wpływ tkanki tłuszczowej" to bodyFatEnabled,
                                            "procent tkanki tłuszczowej" to bodyFatPercent,
                                            "ilość alkoholu (ml)" to alcoholVolume,
                                            "procent alkoholu" to alcoholPercentage,
                                            "długość (h) picia" to drinkingDuration,
                                            "czas od ostatniego wypicia" to timeSinceLastDrink,
                                            "sylwetka" to bodyType,
                                            "posiłek przed piciem" to mealBeforeDrinking,
                                            "zdrowie wątroby" to liverHealth,
                                            "dynamika picia" to drinkingPace,
                                            "picie napoi gazowanych" to carbonationYes,
                                            "first pass (const)" to first_pass_factor,
                                            "aktywność fizyczna" to activityLevel,
                                            "tolerancja na alkohol (tyg.)" to toleranceWeeks,
                                            "bodyTypeFactor" to bodyType,
                                        )
                                        if (gender == "Kobieta") inputs["faza cyklu menstruacyjnego"] = cyclePhase

                                        val results = mapOf(
                                            "aktualny BAC" to bacResult,
                                            "peak BAC" to peakBac,
                                            "peak time (h)" to peakTime,
                                            "czas pomiaru (h)" to (drinkingDuration.toDouble() + timeSinceLastDrink.toDouble()),
                                            "czas do wytrzeźwienia (h)" to soberTime
                                        )

                                        val doc = hashMapOf<String, Any>(
                                            "userId" to user.uid,
                                            "timestamp" to com.google.firebase.Timestamp.now(),
                                            "inputs" to inputs,
                                            "results" to results,
                                        )

                                        firestore.collection("users")
                                            .document(user.uid)
                                            .collection("BAC")
                                            .add(doc)
                                            .addOnSuccessListener {
                                                Toast.makeText(this@MainActivity, "Sesja zapisana", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this@MainActivity, "Błąd zapisu: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Musisz być zalogowany aby zapisać", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf1c523))
                            ) {
                                StrokedText(
                                    text = "Zapisz w bazie danych",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }


                            Button(
                                onClick = { openMap() }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf1c523))
                            ) {

                                StrokedText(
                                    text = "Mapa",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }
                            Button(
                                onClick = onLogout, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf1c523))
                            ) {
                                StrokedText(
                                    text = "Wyloguj",
                                    fillColor = DarkBrown,
                                    strokeColor = DarkBrown,
                                    strokeWidth = 1f,
                                    style = MaterialTheme.typography.titleMedium,
                                    shadowColor = Color.Black,
                                    shadowOffset = Offset(1f, 1f),
                                    shadowBlurRadius = 1f
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun DropdownMenuField(value: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var parentSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { parentSize = it }
    ) {

        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DarkBrown,
                containerColor = Color.Transparent
            ),

            border = BorderStroke(2.dp, Color(0xFFf1c523).copy(alpha = 0.9f))
        ) {

            StrokedText(
                text = value,
                modifier = Modifier.weight(1f),
                fillColor = LightBrown,
                strokeColor = LightBrown,
                strokeWidth = 4f,
                style = MaterialTheme.typography.titleSmall,
                shadowColor = Color.Black,
                shadowOffset = Offset(1f, 1f),
                shadowBlurRadius = 1f
            )


            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Rozwiń",
                tint = DarkBrown
            )
        }


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },

            modifier = Modifier
                .width(with(LocalDensity.current) { parentSize.width.toDp() })
                .background(Color(0xFFFEF3E2)),
            offset = DpOffset(0.dp, 0.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {

                            StrokedText(
                                text = option,
                            fillColor = DarkBrown,
                            strokeColor = DarkBrown,
                            strokeWidth = 1f,
                            style = MaterialTheme.typography.titleMedium,
                            shadowColor = Color.Black,
                            shadowOffset = Offset(1f, 1f),
                            shadowBlurRadius = 1f
                            )
                        }
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },

                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(48.dp)
                )

                if (index < options.size - 1) {
                    HorizontalDivider(
                        color = Color(0xFF40351E),
                        thickness = 2.dp,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}