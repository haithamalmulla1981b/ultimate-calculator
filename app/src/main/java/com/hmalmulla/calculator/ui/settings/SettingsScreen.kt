package com.hmalmulla.calculator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hmalmulla.calculator.CalculatorApp
import com.hmalmulla.calculator.data.Palette
import com.hmalmulla.calculator.data.Settings
import com.hmalmulla.calculator.data.SizeOption
import com.hmalmulla.calculator.data.ThemeMode
import com.hmalmulla.calculator.engine.AngleMode
import com.hmalmulla.calculator.ui.theme.AccentChoices
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as CalculatorApp
    val repo = app.settings
    val s by repo.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var confirmHistory by remember { mutableStateOf(false) }
    fun set(t: (Settings) -> Settings) { scope.launch { repo.update(t) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Section("Appearance")
        Label("Theme")
        Chips(ThemeMode.entries.map { it to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, s.themeMode) { m -> set { it.copy(themeMode = m) } }
        Toggle("AMOLED black (dark theme)", s.amoled) { v -> set { it.copy(amoled = v) } }
        Label("Color style")
        Chips(Palette.entries.map { it to it.label }, s.palette) { p -> set { it.copy(palette = p) } }
        if (s.palette == Palette.CUSTOM) {
            Label("Accent color")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentChoices.forEach { c ->
                    Box(
                        Modifier.size(40.dp).background(Color(c), CircleShape)
                            .then(if (c == s.customAccent) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { set { it.copy(customAccent = c) } }
                            .semantics { contentDescription = "Accent color" },
                    )
                }
            }
        }
        Label("Display font size")
        Chips(SizeOption.entries.map { it to it.label }, s.fontSize) { v -> set { it.copy(fontSize = v) } }
        Label("Button size")
        Chips(SizeOption.entries.map { it to it.label }, s.buttonSize) { v -> set { it.copy(buttonSize = v) } }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section("Calculation")
        Label("Decimal places: ${s.decimals}")
        Slider(
            value = s.decimals.toFloat(), onValueChange = { v -> set { it.copy(decimals = v.toInt()) } },
            valueRange = 0f..15f, steps = 14,
        )
        Toggle("Thousands separators", s.grouping) { v -> set { it.copy(grouping = v) } }
        Toggle("Show result while typing", s.livePreview) { v -> set { it.copy(livePreview = v) } }
        Label("Default angle unit")
        Chips(AngleMode.entries.map { it to if (it == AngleMode.DEG) "Degrees" else "Radians" }, s.defaultAngle) { a -> set { it.copy(defaultAngle = a) } }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section("Feedback")
        Toggle("Vibration on key press", s.haptics) { v -> set { it.copy(haptics = v) } }
        Toggle("Key click sound", s.sound) { v -> set { it.copy(sound = v) } }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section("History and privacy")
        Toggle("Save calculations to history", s.saveHistory) { v -> set { it.copy(saveHistory = v) } }
        Text(
            "Everything stays on this device. Nothing is sent anywhere, except the optional currency rate download.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { confirmHistory = true }) { Text("Delete all history") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        OutlinedButton(onClick = { confirmReset = true }) { Text("Reset all settings") }
        Text("Version 1.0.0\nDeveloped by : H.M.Al-Mulla", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Theme, sizes and calculation options return to their defaults. History is kept.") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.reset() }; confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
    if (confirmHistory) {
        AlertDialog(
            onDismissRequest = { confirmHistory = false },
            title = { Text("Delete all history?") },
            text = { Text("All saved calculations, including favorites, are permanently deleted.") },
            confirmButton = { TextButton(onClick = { scope.launch { app.database.history().deleteAll() }; confirmHistory = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmHistory = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(t: String) = Text(t, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun Label(t: String) = Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, l) -> FilterChip(selected = v == selected, onClick = { onPick(v) }, label = { Text(l) }) }
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = onChange)
    }
}
