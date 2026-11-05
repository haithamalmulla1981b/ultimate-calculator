package com.hmalmulla.calculator.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hmalmulla.calculator.engine.NumberFormatter
import com.hmalmulla.calculator.ui.calculator.copyToClipboard
import com.hmalmulla.calculator.ui.calculator.shareText
import java.math.BigDecimal
import java.math.RoundingMode

enum class FieldKind { NUMBER, TEXT, CHOICE }

data class Field(
    val key: String,
    val label: String,
    val default: String = "",
    val kind: FieldKind = FieldKind.NUMBER,
    val suffix: String? = null,
    val options: List<String> = emptyList(),
    val hint: String? = null,
)

data class Table(val headers: List<String>, val rows: List<List<String>>)

data class ToolOutput(
    val rows: List<Pair<String, String>>,
    val steps: List<String> = emptyList(),
    val table: Table? = null,
    val note: String? = null,
)

class Tool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val fields: List<Field>,
    val compute: (Inputs) -> ToolOutput,
)

/** Typed access to the form values; problems become friendly messages. */
class Inputs(private val values: Map<String, String>, private val fields: List<Field>) {
    private fun label(key: String) = fields.firstOrNull { it.key == key }?.label ?: key

    fun text(key: String): String = values[key].orEmpty().trim()

    fun num(key: String): BigDecimal {
        val t = text(key).replace(",", "").replace('−', '-')
        require(t.isNotEmpty()) { "Enter ${label(key).lowercase()}" }
        return t.toBigDecimalOrNull() ?: throw IllegalArgumentException("${label(key)} is not a valid number")
    }

    fun numOrNull(key: String): BigDecimal? = text(key).replace(",", "").toBigDecimalOrNull()

    fun int(key: String): Int {
        val v = num(key)
        require(v.stripTrailingZeros().scale() <= 0) { "${label(key)} must be a whole number" }
        require(v.abs() <= BigDecimal(1_000_000)) { "${label(key)} is too large" }
        return v.toInt()
    }

    fun long(key: String): Long {
        val v = num(key)
        require(v.stripTrailingZeros().scale() <= 0) { "${label(key)} must be a whole number" }
        require(v.abs() <= BigDecimal(Long.MAX_VALUE)) { "${label(key)} is too large" }
        return v.toLong()
    }

    fun choice(key: String): String = values[key] ?: fields.first { it.key == key }.options.first()
}

/** Money with thousands separators and 2 decimals. */
fun money(v: BigDecimal): String = NumberFormatter.group(v.setScale(2, RoundingMode.HALF_UP).toPlainString())

fun num(v: BigDecimal, decimals: Int = 6): String = NumberFormatter.format(v, decimals, grouping = true)

fun pct(v: BigDecimal): String = NumberFormatter.format(v, 2, grouping = true) + "%"

@Composable
fun ToolIcon(icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(10.dp).size(24.dp))
    }
}

@Composable
fun ToolList(tools: List<Tool>, onOpen: (Tool) -> Unit, header: (@Composable () -> Unit)? = null) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (header != null) item { header() }
        items(tools, key = { it.id }) { t ->
            Card(
                onClick = { onOpen(t) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ToolIcon(t.icon)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                        Text(t.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolScreen(tool: Tool, onBack: () -> Unit) {
    val context = LocalContext.current
    val values = remember(tool.id) {
        mutableStateMapOf<String, String>().apply { tool.fields.forEach { put(it.key, it.default.ifEmpty { it.options.firstOrNull().orEmpty() }) } }
    }
    val result: Result<ToolOutput> = runCatching { tool.compute(Inputs(values.toMap(), tool.fields)) }
    val output = result.getOrNull()
    val shareBody = output?.let { o -> tool.title + "\n" + o.rows.joinToString("\n") { "${it.first}: ${it.second}" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (shareBody != null) {
                        IconButton(onClick = { copyToClipboard(context, shareBody) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy results") }
                        IconButton(onClick = { shareText(context, shareBody) }) { Icon(Icons.Outlined.Share, contentDescription = "Share results") }
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tool.fields, key = { it.key }) { f ->
                when (f.kind) {
                    FieldKind.CHOICE -> Column {
                        Text(f.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            f.options.forEach { o ->
                                FilterChip(selected = values[f.key] == o, onClick = { values[f.key] = o }, label = { Text(o) })
                            }
                        }
                    }
                    else -> OutlinedTextField(
                        value = values[f.key].orEmpty(),
                        onValueChange = { values[f.key] = it },
                        label = { Text(f.label) },
                        placeholder = if (f.hint != null) { { Text(f.hint) } } else null,
                        suffix = if (f.suffix != null) { { Text(f.suffix) } } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (f.kind == FieldKind.NUMBER) KeyboardType.Decimal else KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                val err = result.exceptionOrNull()
                if (output == null) {
                    Text(
                        err?.message ?: "Enter the values above",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            output.rows.forEachIndexed { i, (k, v) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(k, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        v,
                                        style = if (i == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                    output.note?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            if (output != null && output.steps.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Steps", style = MaterialTheme.typography.titleMedium)
                            output.steps.forEach { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
            output?.table?.let { table ->
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        table.headers.forEach {
                            Text(it, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                        }
                    }
                    HorizontalDivider()
                }
                items(table.rows) { r ->
                    Row(Modifier.fillMaxWidth()) {
                        r.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End) }
                    }
                }
            }
        }
    }
}
