package dev.rideguard.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.rideguard.core.model.DriverGoals
import dev.rideguard.core.model.DurationDisplay
import dev.rideguard.core.model.PickupWaitEstimate
import dev.rideguard.core.model.WeeklySchedule
import dev.rideguard.core.settings.RideGuardSettings
import dev.rideguard.core.settings.RideGuardSettingsStore
import dev.rideguard.detection.accessibility.DetectorSettingsBroadcast
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.Locale
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
                    onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onOpenAppInfo = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:$packageName".toUri()))
                    },
                    onSave = { settings ->
                        val saved = settingsStore.save(settings)
                        if (saved) sendBroadcast(DetectorSettingsBroadcast.createIntent(this, settings))
                        saved
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
    onOpenAppInfo: () -> Unit,
    onSave: (RideGuardSettings) -> Boolean,
) {
    var regularHourly by remember { mutableStateOf(initial.regularGoals.targetArsPerHour.plain()) }
    var regularKm by remember { mutableStateOf(initial.regularGoals.minimumArsPerKm.plain()) }
    var busyHourly by remember { mutableStateOf(initial.busyDaysGoals.targetArsPerHour.plain()) }
    var busyKm by remember { mutableStateOf(initial.busyDaysGoals.minimumArsPerKm.plain()) }
    var usualHours by remember { mutableStateOf(initial.usualWorkHours.plain()) }
    var schedule by remember { mutableStateOf(initial.schedule) }
    var savedSchedule by remember { mutableStateOf(initial.schedule) }
    var message by remember { mutableStateOf("") }

    val hours = usualHours.numberOrNull()?.takeIf { it > 0.0 && it <= 24.0 }
    val regularEstimate = regularHourly.numberOrNull()?.let { hourly -> hours?.let { hourly * it } }
    val busyEstimate = busyHourly.numberOrNull()?.let { hourly -> hours?.let { hourly * it } }
    val today = LocalDateTime.now()
    val activeNow = savedSchedule.activeDay(today) != null
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val waitSeconds = DurationDisplay.roundedSeconds(PickupWaitEstimate.MINUTES)
    val estimatedWait = "${waitSeconds / 60} min ${waitSeconds % 60} s"

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("RideGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Ofertas claras mientras trabajas.")
            ServiceCard(accessibilityEnabled, activeNow, onOpenAccessibility, onOpenAppInfo)

            Text("Objetivos por día", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Una oferta cumple solo si alcanza el mínimo por hora y por kilómetro. La recogida y la espera estimada están incluidas.")
            Text("Verde: cumple ambos. Ámbar: queda hasta 5 % por debajo. Rojo: alguno queda más lejos.",
                style = MaterialTheme.typography.bodySmall)
            ProfileCard("Lunes a miércoles", regularHourly, regularKm,
                onHourlyChange = { regularHourly = it }, onKmChange = { regularKm = it })
            ProfileCard("Jueves a domingo", busyHourly, busyKm,
                onHourlyChange = { busyHourly = it }, onKmChange = { busyKm = it })

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Espera al recoger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Sumamos $estimatedWait estimados desde que llegas hasta que el pasajero sube. El tiempo del panel se muestra como min:seg; los ARS/h y el color incluyen esa espera, pero los ARS/km no cambian.")
                    Text("Es una media histórica de 416 viajes Uber/Lyft en Denver, no una medición argentina ni de tus viajes. No incluye esperas entre ofertas; un posible pago por espera puede cambiar el ingreso final.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { uriHandler.openUri(PickupWaitEstimate.STUDY_URL) }) {
                        Text("Ver estudio")
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jornada habitual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    NumberField("Normalmente, ¿cuántas horas trabajas?", usualHours) { usualHours = it }
                    Text("Ganancias estimadas en jornada de ${hours?.plain() ?: "—"} horas")
                    Text("Lun–Mié: ${regularEstimate?.money() ?: "—"}")
                    Text("Jue–Dom: ${busyEstimate?.money() ?: "—"}")
                    Text("Referencia bruta: objetivo por hora × horas habituales. La espera de $estimatedWait se aplica a cada oferta, no a esta proyección. Esta tarjeta supone actividad continua y no descuenta gastos del auto.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Horarios de trabajo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Activa el lunes y toca las horas para elegirlas. Puedes copiar ese horario a toda la semana y luego editar o desactivar cualquier día. Un horario como 22:00 a 02:00 cruza medianoche; fuera del horario RideGuard no analiza ofertas.")
            schedule.days.forEachIndexed { index, day ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(day.day.label(), style = MaterialTheme.typography.titleMedium)
                            Switch(checked = day.enabled, onCheckedChange = { checked ->
                                schedule = WeeklySchedule(schedule.days.toMutableList().apply {
                                    this[index] = day.copy(enabled = checked)
                                })
                            })
                        }
                        if (day.enabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimeField("Desde", day.startMinute, Modifier.weight(1f)) {
                                    showTimePicker(context, day.startMinute) { selected ->
                                        val current = schedule.days[index]
                                        val end = if (selected == 0 && current.endMinute == 0) 1440 else current.endMinute
                                        if (selected == end) {
                                            message = "El inicio y el fin deben ser distintos."
                                        } else {
                                            schedule = WeeklySchedule(schedule.days.toMutableList().apply {
                                                this[index] = current.copy(startMinute = selected, endMinute = end)
                                            })
                                            message = ""
                                        }
                                    }
                                }
                                TimeField("Hasta", day.endMinute, Modifier.weight(1f)) {
                                    showTimePicker(context, day.endMinute) { selected ->
                                        val current = schedule.days[index]
                                        val end = if (selected == 0 && current.startMinute == 0) 1440 else selected
                                        if (current.startMinute == end) {
                                            message = "El inicio y el fin deben ser distintos."
                                        } else {
                                            schedule = WeeklySchedule(schedule.days.toMutableList().apply {
                                                this[index] = current.copy(endMinute = end)
                                            })
                                            message = ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (day.day == DayOfWeek.MONDAY) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = day.enabled,
                        onClick = {
                            schedule = schedule.copyMondayToAllDays()
                            message = "Horario del lunes aplicado a todos los días. Puedes editarlos o desactivarlos antes de guardar."
                        },
                    ) { Text("Copiar lunes a toda la semana") }
                }
            }
            if (schedule.days.none { it.enabled }) {
                Text("Activa al menos un día para que aparezca el análisis de ofertas.", color = MaterialTheme.colorScheme.secondary)
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                val regularHour = regularHourly.numberOrNull()
                val regularDistance = regularKm.numberOrNull()
                val busyHour = busyHourly.numberOrNull()
                val busyDistance = busyKm.numberOrNull()
                val workHours = usualHours.numberOrNull()
                if (listOf(regularHour, regularDistance, busyHour, busyDistance).any { it == null || it <= 0.0 } ||
                    workHours == null || workHours <= 0.0 || workHours > 24.0
                ) {
                    message = "Completa metas positivas y una jornada de hasta 24 horas."
                    return@Button
                }
                val settings = RideGuardSettings(
                    regularGoals = DriverGoals(regularHour ?: return@Button, regularDistance ?: return@Button),
                    busyDaysGoals = DriverGoals(busyHour ?: return@Button, busyDistance ?: return@Button),
                    usualWorkHours = workHours,
                    schedule = schedule,
                )
                if (onSave(settings)) {
                    schedule = settings.schedule
                    savedSchedule = settings.schedule
                    message = "Configuración guardada"
                } else message = "No se pudo guardar. Inténtalo de nuevo."
            }) { Text("Guardar configuración") }
            if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
            Text("Con otra app abierta, RideGuard puede analizar una oferta si Uber o Cabify muestran pago, minutos y kilómetros en una ventana visible o notificación completa. Un aviso sin esos datos no basta.",
                style = MaterialTheme.typography.bodySmall)
            Text("Privacidad: el análisis ocurre en el teléfono. Nunca se aceptan ni rechazan viajes automáticamente.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProfileCard(title: String, hourly: String, perKm: String,
    onHourlyChange: (String) -> Unit, onKmChange: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NumberField("Mínimo ARS/h", hourly, onHourlyChange)
            NumberField("Mínimo ARS/km", perKm, onKmChange)
        }
    }
}

@Composable
private fun ServiceCard(enabled: Boolean, activeNow: Boolean,
    onOpenAccessibility: () -> Unit, onOpenAppInfo: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val status = when {
                !enabled -> "Permiso de Accesibilidad desactivado"
                activeNow -> "Permiso activo · dentro del horario"
                else -> "Permiso activo · fuera de horario"
            }
            Text(status, fontWeight = FontWeight.Bold,
                color = if (enabled) Color(0xFF76D17C) else Color(0xFFFF8A80))
            Text("Android mantiene el permiso de Accesibilidad. Si el teléfono lo desactiva al reiniciar, vuelve a habilitarlo aquí y revisa las restricciones de batería de RideGuard.")
            OutlinedButton(onClick = onOpenAccessibility) { Text("Abrir Accesibilidad") }
            OutlinedButton(onClick = onOpenAppInfo) { Text("Abrir ajustes de RideGuard") }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(), value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun TimeField(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(modifier = modifier, onClick = onClick) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(formatTime(minutes), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun showTimePicker(context: Context, currentMinutes: Int, onSelected: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(hour * 60 + minute) },
        (currentMinutes / 60) % 24,
        currentMinutes % 60,
        true,
    ).show()
}

private fun formatTime(minutes: Int): String = "%02d:%02d".format(Locale.US, minutes / 60, minutes % 60)
private fun String.numberOrNull(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf(Double::isFinite)
private fun Double.plain(): String {
    val rounded = round(this * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
private fun Double.money(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"))
    .apply { maximumFractionDigits = 0 }.format(this)

private fun DayOfWeek.label(): String = when (this) {
    DayOfWeek.MONDAY -> "Lunes"
    DayOfWeek.TUESDAY -> "Martes"
    DayOfWeek.WEDNESDAY -> "Miércoles"
    DayOfWeek.THURSDAY -> "Jueves"
    DayOfWeek.FRIDAY -> "Viernes"
    DayOfWeek.SATURDAY -> "Sábado"
    DayOfWeek.SUNDAY -> "Domingo"
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name.endsWith("OfferAccessibilityService")
        }
}

private val RideGuardColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF76D17C), secondary = Color(0xFFFFC857),
    background = Color(0xFF101418), surface = Color(0xFF1A2026),
)

@Composable
private fun RideGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RideGuardColors, content = content)
}
