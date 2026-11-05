package com.hmalmulla.calculator.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hmalmulla.calculator.CalculatorApp
import com.hmalmulla.calculator.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

/**
 * Currency conversion with rates downloaded from ExchangeRate-API's free open
 * endpoint. Nothing is invented: without a successful download there are no
 * rates, and the last update time is always shown.
 */
private const val RATES_URL = "https://open.er-api.com/v6/latest/USD"

private data class Rates(val base: String, val rates: Map<String, BigDecimal>, val time: Long)

private fun parseRates(json: String, time: Long): Rates {
    val o = JSONObject(json)
    require(o.optString("result") == "success") { "The rate service returned an error" }
    val r = o.getJSONObject("rates")
    val map = HashMap<String, BigDecimal>()
    r.keys().forEach { k -> map[k] = BigDecimal(r.get(k).toString()) }
    return Rates(o.optString("base_code", "USD"), map, time)
}

private suspend fun download(): String = withContext(Dispatchers.IO) {
    val c = URL(RATES_URL).openConnection() as HttpURLConnection
    c.connectTimeout = 10_000
    c.readTimeout = 10_000
    try {
        require(c.responseCode == 200) { "Server answered ${c.responseCode}" }
        c.inputStream.bufferedReader().use { it.readText() }
    } finally {
        c.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = (context.applicationContext as CalculatorApp).settings
    val scope = rememberCoroutineScope()
    var rates by remember { mutableStateOf<Rates?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var amount by rememberSaveable { mutableStateOf("100") }
    var from by rememberSaveable { mutableStateOf("USD") }
    var to by rememberSaveable { mutableStateOf("EUR") }

    LaunchedEffect(Unit) {
        repo.loadRates()?.let { c -> runCatching { parseRates(c.json, c.time) }.onSuccess { rates = it } }
    }

    fun update() {
        loading = true; message = null
        scope.launch {
            try {
                val json = download()
                val now = System.currentTimeMillis()
                rates = parseRates(json, now)
                repo.saveRates(SettingsRepository.CachedRates(json, now, "USD"))
            } catch (e: Exception) {
                message = "Couldn't update rates. Check your internet connection." + (e.message?.let { " ($it)" } ?: "")
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currency converter") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Internet is needed to update exchange rates.", style = MaterialTheme.typography.titleMedium)
                    val r = rates
                    Text(
                        if (r == null) "No rates downloaded yet. Tap Update rates."
                        else "Last update: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.time)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Rates by ExchangeRate-API (open.er-api.com). Rates are for reference, not for trading.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { update() }, enabled = !loading) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Update rates")
                        }
                        if (loading) {
                            Spacer(Modifier.width(12.dp))
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            val r = rates
            if (r != null) {
                val codes = r.rates.keys.sorted()
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it }, label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyPicker(from, codes, { from = it }, Modifier.weight(1f))
                    IconButton(onClick = { val t = from; from = to; to = t }) { Icon(Icons.Outlined.SwapVert, contentDescription = "Swap currencies") }
                    CurrencyPicker(to, codes, { to = it }, Modifier.weight(1f))
                }
                val a = amount.replace(",", "").toBigDecimalOrNull()
                val rf = r.rates[from]; val rt = r.rates[to]
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        if (a == null || rf == null || rt == null || rf.signum() == 0) {
                            Text("Enter an amount", style = MaterialTheme.typography.titleMedium)
                        } else {
                            val mc = MathContext.DECIMAL128
                            val out = a.multiply(rt, mc).divide(rf, mc)
                            val one = rt.divide(rf, mc)
                            Text("${money(a)} $from =", style = MaterialTheme.typography.titleMedium)
                            Text("${money(out)} $to", style = MaterialTheme.typography.displaySmall)
                            Text("1 $from = ${num(one, 6)} $to", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyPicker(selected: String, codes: List<String>, onPick: (String) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(selected) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            codes.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onPick(c); open = false }) }
        }
    }
}
