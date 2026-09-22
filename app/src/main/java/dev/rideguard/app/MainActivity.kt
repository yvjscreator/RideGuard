package dev.rideguard.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.TargetMode
import dev.rideguard.core.model.VisualThresholds
import dev.rideguard.core.settings.RideGuardSettings
import dev.rideguard.core.settings.RideGuardSettingsStore
import dev.rideguard.detection.accessibility.DetectorSettingsBroadcast
import kotlin.math.round

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: RideGuardSettingsStore
    private var accessibilityEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = RideGuardSettingsStore(this)
        setContent {
            RideGuardTheme {
                SettingsScreen(
                    initial = settingsStore.load(),
                    accessibilityEnabled = accessibilityEnabled,
                    onOpenAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onSave = { settings ->
                        settingsStore.save(settings)
                        sendBroadcast(DetectorSettingsBroadcast.createIntent(this, settings))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityEnabled = isAccessibilityServiceEnabled(this)
    }
}

@Composable
private fun SettingsScreen(
    initial: RideGuardSettings,
    accessibilityEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onSave: (RideGuardSettings) -> Unit,
) {
    var mode by remember { mutableStateOf(initial.goals.targetMode) }
    var hourlyTarget by remember { mutableStateOf(initial.goals.targetArsPerHour.plain()) }
    var minimumPerKm by remember { mutableStateOf(initial.goals.minimumArsPerKm.plain()) }
    var shiftHours by remember { mutableStateOf((initial.goals.shiftDurationMinutes / 60.0).plain()) }
    var dailyTarget by remember { mutableStateOf(initial.goals.dailyTargetArs.plain()) }
    var elapsedHours by remember { mutableStateOf((initial.goals.elapsedShiftMinutes / 60.0).plain()) }
    var earningsSoFar by remember { mutableStateOf(initial.goals.earningsSoFarArs.plain()) }
    var goodPercent by remember { mutableStateOf((initial.thresholds.goodMinimumRatio * 100).plain()) }
    var warningPercent by remember { mutableStateOf((initial.thresholds.warningMinimumRatio * 100).plain()) }
    var savedMessage by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("RideGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Analiza ofertas visibles. Nunca acepta ni rechaza viajes.")

            ServiceCard(accessibilityEnabled, onOpenAccessibility)

            Text("Objetivo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton("Por hora", mode == TargetMode.HOURLY) { mode = TargetMode.HOURLY }
                ModeButton("Por jornada", mode == TargetMode.SHIFT) { mode = TargetMode.SHIFT }
            }

            NumberField("Objetivo ARS/h", hourlyTarget) { hourlyTarget = it }
            NumberField("Mínimo ARS/km", minimumPerKm) { minimumPerKm = it }

            if (mode == TargetMode.SHIFT) {
                NumberField("Duración de jornada (horas)", shiftHours) { shiftHours = it }
                NumberField("Meta diaria (ARS)", dailyTarget) { dailyTarget = it }
                NumberField("Horas transcurridas", elapsedHours) { elapsedHours = it }
                NumberField("Ganancias acumuladas (ARS)", earningsSoFar) { earningsSoFar = it }
            }

            Text("Clasificación visual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            NumberField("Buena desde (% del objetivo)", goodPercent) { goodPercent = it }
            NumberField("Revisar desde (% del objetivo)", warningPercent) { warningPercent = it }
            Text("Verde: cumple ambos objetivos. Amarillo: zona configurable. Rojo: queda por debajo.")

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val settings = RideGuardSettings(
                        goals = DriverGoals(
                            targetMode = mode,
                            targetArsPerHour = hourlyTarget.numberOr(20_000.0),
                            minimumArsPerKm = minimumPerKm.numberOr(700.0),
                            shiftDurationMinutes = (shiftHours.numberOr(6.0) * 60).toInt(),
                            dailyTargetArs = dailyTarget.numberOr(120_000.0),
                            elapsedShiftMinutes = (elapsedHours.numberOr(0.0) * 60).toInt(),
                            earningsSoFarArs = earningsSoFar.numberOr(0.0),
                        ),
                        thresholds = VisualThresholds(
                            goodMinimumRatio = goodPercent.numberOr(100.0) / 100.0,
                            warningMinimumRatio = warningPercent.numberOr(80.0) / 100.0,
                            goodColorArgb = initial.thresholds.goodColorArgb,
                            warningColorArgb = initial.thresholds.warningColorArgb,
                            badColorArgb = initial.thresholds.badColorArgb,
                        ),
                    )
                    onSave(settings)
                    savedMessage = "Configuración guardada"
                },
            ) {
                Text("Guardar")
            }
            if (savedMessage.isNotEmpty()) Text(savedMessage, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))
            Text(
                "Privacidad: el análisis ocurre en el teléfono. El servicio solo observa Uber Driver, Cabify Driver y DiDi Driver.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ServiceCard(enabled: Boolean, onOpenAccessibility: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (enabled) "Servicio activo" else "Servicio inactivo",
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color(0xFF76D17C) else Color(0xFFFF8A80),
            )
            Text("Activa RideGuard en Accesibilidad para detectar ofertas y mostrar el overlay.")
            OutlinedButton(onClick = onOpenAccessibility) {
                Text("Abrir Accesibilidad")
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.filter { it.isDigit() || it == ',' || it == '.' })
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

private fun Double.plain(): String {
    val rounded = round(this * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

private fun String.numberOr(default: Double): Double = replace(',', '.').toDoubleOrNull() ?: default

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name.endsWith("OfferAccessibilityService")
        }
}

private val RideGuardColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF76D17C),
    secondary = Color(0xFFFFC857),
    background = Color(0xFF101418),
    surface = Color(0xFF1A2026),
)

@Composable
private fun RideGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RideGuardColors, content = content)
}
