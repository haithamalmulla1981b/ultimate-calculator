package com.hmalmulla.calculator.ui.converter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hmalmulla.calculator.engine.NumberFormatter
import com.hmalmulla.calculator.engine.UnitDef
import com.hmalmulla.calculator.engine.Units
import com.hmalmulla.calculator.ui.calculator.copyToClipboard
import java.math.BigDecimal

private val categoryIcons: Map<String, ImageVector> = mapOf(
    "length" to Icons.Outlined.Straighten, "distance" to Icons.Outlined.Map, "weight" to Icons.Outlined.FitnessCenter,
    "mass" to Icons.Outlined.Scale, "temperature" to Icons.Outlined.DeviceThermostat, "area" to Icons.Outlined.SquareFoot,
    "volume" to Icons.Outlined.LocalDrink, "speed" to Icons.Outlined.Speed, "time" to Icons.Outlined.Timer,
    "data" to Icons.Outlined.DataUsage, "transfer" to Icons.Outlined.NetworkCheck, "pressure" to Icons.Outlined.Compress,
    "energy" to Icons.Outlined.Bolt, "power" to Icons.Outlined.Power, "force" to Icons.Outlined.PanTool,
    "frequency" to Icons.Outlined.GraphicEq, "angle" to Icons.Outlined.Rotate90DegreesCcw,
    "fuel" to Icons.Outlined.LocalGasStation, "cooking" to Icons.Outlined.OutdoorGrill,
)

@Composable
fun ConverterScreen() {
    val context = LocalContext.current
    var catIndex by rememberSaveable { mutableIntStateOf(0) }
    val cat = Units.categories[catIndex]
    var fromIdx by rememberSaveable(catIndex) { mutableIntStateOf(0) }
    var toIdx by rememberSaveable(catIndex) { mutableIntStateOf(if (cat.units.size > 1) 1 else 0) }
    var input by rememberSaveable { mutableStateOf("1") }
    val from = cat.units[fromIdx.coerceIn(cat.units.indices)]
    val to = cat.units[toIdx.coerceIn(cat.units.indices)]
    val value = input.replace(",", "").replace('−', '-').toBigDecimalOrNull()

    fun conv(u: UnitDef): String = try {
        if (value == null) "—" else NumberFormatter.format(Units.convert(value, from, u), 10, grouping = true)
    } catch (e: ArithmeticException) {
        "—"
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Units.categories.size) { i ->
                val c = Units.categories[i]
                FilterChip(
                    selected = i == catIndex,
                    onClick = { catIndex = i },
                    label = { Text(c.name) },
                    leadingIcon = { Icon(categoryIcons[c.id] ?: Icons.Outlined.Straighten, contentDescription = null) },
                )
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    label = { Text("Value in ${from.name.lowercase()}") },
                    suffix = { Text(from.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = input.isNotBlank() && value == null,
                    supportingText = { if (input.isNotBlank() && value == null) Text("Enter a valid number") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UnitPicker(cat.units, fromIdx, { fromIdx = it }, Modifier.weight(1f))
                    IconButton(onClick = { val t = fromIdx; fromIdx = toIdx; toIdx = t }) {
                        Icon(Icons.Outlined.SwapVert, contentDescription = "Swap units")
                    }
                    UnitPicker(cat.units, toIdx, { toIdx = it }, Modifier.weight(1f))
                }
            }
            item {
                val text = conv(to)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = value != null) { copyToClipboard(context, text.replace(",", "")) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${input.ifBlank { "0" }} ${from.symbol} =", style = MaterialTheme.typography.titleMedium)
                        Text("$text ${to.symbol}", style = MaterialTheme.typography.displaySmall)
                        Text("Tap to copy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text("All ${cat.name.lowercase()} units", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(cat.units.size) { i ->
                val u = cat.units[i]
                Row(
                    Modifier.fillMaxWidth().clickable { toIdx = i }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(u.name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${conv(u)} ${u.symbol}", textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun UnitPicker(units: List<UnitDef>, selected: Int, onPick: (Int) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(units[selected.coerceIn(units.indices)].let { "${it.name} (${it.symbol})" }, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 400.dp)) {
            units.forEachIndexed { i, u ->
                DropdownMenuItem(text = { Text("${u.name} (${u.symbol})") }, onClick = { onPick(i); open = false })
            }
        }
    }
}
