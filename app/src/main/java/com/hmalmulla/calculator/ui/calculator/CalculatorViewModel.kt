package com.hmalmulla.calculator.ui.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hmalmulla.calculator.CalculatorApp
import com.hmalmulla.calculator.data.HistoryEntry
import com.hmalmulla.calculator.data.Settings
import com.hmalmulla.calculator.engine.AngleMode
import com.hmalmulla.calculator.engine.EvalResult
import com.hmalmulla.calculator.engine.MathEngine
import com.hmalmulla.calculator.engine.MemoryStore
import com.hmalmulla.calculator.engine.NumberFormatter
import com.hmalmulla.calculator.system.QuickCalcWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CalcUiState(
    val expression: String = "",
    val preview: String = "",
    val result: String? = null,
    val resultValue: BigDecimal? = null,
    val error: String? = null,
    val angle: AngleMode = AngleMode.DEG,
    val memory: List<BigDecimal> = emptyList(),
    val evaluated: Boolean = false,
)

/** Shared by the standard and scientific calculators and the history screen. */
class CalculatorViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val calcApp = app as CalculatorApp
    private val dao = calcApp.database.history()
    private val settingsRepo = calcApp.settings

    val settings: StateFlow<Settings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val recent: StateFlow<List<HistoryEntry>> = dao.recent(3).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val memory = MemoryStore()
    private val _state = MutableStateFlow(CalcUiState(expression = saved.get<String>(KEY_EXPR) ?: ""))
    val state: StateFlow<CalcUiState> = _state.asStateFlow()

    private var lastOp: Pair<Char, BigDecimal>? = null
    private var saveJob: Job? = null

    private var angleTouched = false

    init {
        viewModelScope.launch {
            val (expr, mem) = settingsRepo.loadState()
            mem.split(';').filter { it.isNotBlank() }.mapNotNull { it.toBigDecimalOrNull() }.reversed().forEach { memory.store(it) }
            _state.update {
                it.copy(
                    expression = if (it.expression.isEmpty()) expr else it.expression,
                    memory = memory.values,
                )
            }
            refreshPreview()
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                if (!angleTouched) _state.update { it.copy(angle = s.defaultAngle) }
                refreshPreview()
            }
        }
    }

    // ------------------------------------------------------------ input

    private val binaryOps = setOf('+', '−', '×', '÷', '^')

    /** Inserts text from a key (digits, operators, functions like "sin("). */
    fun input(token: String) {
        _state.update { s ->
            var expr = s.expression
            if (s.evaluated) {
                val startsOperator = token.length == 1 && (token[0] in binaryOps || token == "%" || token == "!") ||
                    token == "²" || token == "³" || token == "^(−1)"
                expr = if (startsOperator && s.resultValue != null) plain(s.resultValue) else ""
            }
            expr = append(expr, token)
            s.copy(expression = expr, evaluated = false, result = null, error = null)
        }
        afterChange()
    }

    private fun append(expr: String, token: String): String {
        if (token.length == 1) {
            val c = token[0]
            val last = expr.lastOrNull()
            if (c in binaryOps) {
                if (expr.isEmpty()) return if (c == '−') "−" else expr // only minus may start
                if (last != null && last in binaryOps) {
                    // allow a unary minus after × ÷ ^ (e.g. 5×−3); otherwise replace the operator
                    if (c == '−' && last != '−' && last != '+') return expr + c
                    return expr.dropLast(1) + c
                }
                if (last == '(' && c != '−') return expr
            }
            if (c == '.') {
                val tail = expr.takeLastWhile { it.isDigit() || it == '.' }
                if (tail.contains('.')) return expr
                if (tail.isEmpty()) return expr + "0."
            }
        }
        return expr + token
    }

    fun clear() {
        _state.update { it.copy(expression = "", preview = "", result = null, resultValue = null, error = null, evaluated = false) }
        lastOp = null
        afterChange()
    }

    fun backspace() {
        _state.update { s ->
            if (s.evaluated) return@update s.copy(evaluated = false, result = null, error = null)
            val e = s.expression
            val fn = Regex("(asin|acos|atan|sinh|cosh|tanh|sin|cos|tan|logb|log|ln|root|abs|floor|ceil|round|exp|nPr|nCr|gcd|lcm|mod)\\($").find(e)
            val newExpr = when {
                fn != null -> e.substring(0, fn.range.first)
                e.endsWith("^(−1)") -> e.dropLast(5)
                else -> e.dropLast(1)
            }
            s.copy(expression = newExpr, error = null)
        }
        afterChange()
    }

    /** Smart bracket: opens or closes depending on context. */
    fun bracket() {
        val e = _state.value.let { if (it.evaluated) "" else it.expression }
        val open = e.count { it == '(' } - e.count { it == ')' }
        val last = e.lastOrNull()
        val close = open > 0 && last != null && (last.isDigit() || last == ')' || last == '%' || last == '!' || last == 'π' || last == 'e' || last == '.')
        input(if (close) ")" else "(")
    }

    /** ± : negates the last number (or the result). */
    fun toggleSign() {
        _state.update { s ->
            if (s.evaluated && s.resultValue != null) {
                val v = s.resultValue.negate()
                return@update s.copy(expression = plain(v), evaluated = false, result = null)
            }
            val e = s.expression
            val m = Regex("(\\(−)?(\\d+\\.?\\d*|π|e)$").find(e) ?: return@update s.copy(expression = e + "(−")
            val newExpr = if (m.groupValues[1].isNotEmpty()) e.substring(0, m.range.first) + m.groupValues[2]
            else e.substring(0, m.range.first) + "(−" + m.groupValues[2]
            s.copy(expression = newExpr)
        }
        afterChange()
    }

    fun setExpression(expr: String) {
        _state.update { it.copy(expression = expr, evaluated = false, result = null, error = null) }
        afterChange()
    }

    fun toggleAngle() {
        angleTouched = true
        _state.update { it.copy(angle = if (it.angle == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG) }
        refreshPreview()
    }

    // ------------------------------------------------------------ evaluation

    fun equals() {
        val s = _state.value
        if (s.evaluated && s.resultValue != null) {
            val op = lastOp ?: return
            when (val r = MathEngine.apply(s.resultValue, op.first, op.second)) {
                is EvalResult.Success -> {
                    val expr = "${plain(s.resultValue)}${op.first}${plain(op.second)}"
                    showResult(expr, r.value)
                }
                is EvalResult.Failure -> _state.update { it.copy(error = r.error.message) }
            }
            return
        }
        if (s.expression.isBlank()) return
        when (val r = MathEngine.evaluate(s.expression, s.angle)) {
            is EvalResult.Success -> {
                lastOp = MathEngine.lastOperation(s.expression)
                showResult(closeBrackets(s.expression), r.value)
            }
            is EvalResult.Failure -> _state.update { it.copy(error = r.error.message, preview = "") }
        }
    }

    private fun showResult(expr: String, value: BigDecimal) {
        val text = format(value)
        _state.update { it.copy(expression = expr, result = text, resultValue = value, evaluated = true, error = null, preview = "") }
        if (settings.value.saveHistory) {
            viewModelScope.launch { dao.insert(HistoryEntry(expression = expr, result = text, timestamp = System.currentTimeMillis())) }
        }
        QuickCalcWidget.saveLastResult(getApplication(), text)
        persist()
    }

    private fun closeBrackets(e: String): String {
        val open = e.count { it == '(' } - e.count { it == ')' }
        return if (open > 0) e + ")".repeat(open) else e
    }

    private fun afterChange() {
        saved[KEY_EXPR] = _state.value.expression
        refreshPreview()
        persist()
    }

    private fun refreshPreview() {
        val s = _state.value
        if (s.evaluated || !settings.value.livePreview) {
            if (!s.evaluated) _state.update { it.copy(preview = "") }
            return
        }
        val e = s.expression
        val hasOperation = e.any { it in "+−×÷^%!(√∛" } || e.any { it.isLetter() && it != 'e' } || e.contains('π')
        val preview = if (e.isNotBlank() && hasOperation) {
            when (val r = MathEngine.evaluate(e, s.angle)) {
                is EvalResult.Success -> "= " + format(r.value)
                is EvalResult.Failure -> ""
            }
        } else ""
        _state.update { it.copy(preview = preview) }
    }

    fun format(v: BigDecimal): String = NumberFormatter.format(v, settings.value.decimals, settings.value.grouping)

    /** Plain number for re-use inside an expression (no grouping). */
    private fun plain(v: BigDecimal): String {
        val s = NumberFormatter.format(v, 15, grouping = false)
        return if (s.startsWith("-")) "(−${s.substring(1)})" else s
    }

    /** Current value: the result, or the live value of the expression. */
    private fun currentValue(): BigDecimal? {
        val s = _state.value
        if (s.evaluated) return s.resultValue
        return (MathEngine.evaluate(s.expression, s.angle) as? EvalResult.Success)?.value
    }

    // ------------------------------------------------------------ memory

    fun memoryClear() { memory.clear(); memChanged() }
    fun memoryRecall(index: Int = 0) {
        if (!memory.hasValue) return
        val v = memory.recall(index)
        val s = _state.value
        val needsOp = !s.evaluated && s.expression.lastOrNull()?.let { it.isDigit() || it == ')' } == true
        if (needsOp) input("×")
        input(plain(v))
    }
    fun memoryAdd() { currentValue()?.let { memory.add(it); memChanged() } }
    fun memorySubtract() { currentValue()?.let { memory.subtract(it); memChanged() } }
    fun memoryStore() { currentValue()?.let { memory.store(it); memChanged() } }
    fun memoryRemove(index: Int) { memory.remove(index); memChanged() }

    private fun memChanged() {
        _state.update { it.copy(memory = memory.values) }
        persist()
    }

    private fun persist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            settingsRepo.saveState(_state.value.expression, memory.values.joinToString(";") { it.toPlainString() })
        }
    }

    // ------------------------------------------------------------ history

    fun reuse(entry: HistoryEntry) = setExpression(entry.expression)

    fun useResult(entry: HistoryEntry) {
        val v = entry.result.replace(",", "").replace('−', '-').toBigDecimalOrNull() ?: return
        setExpression(plain(v))
    }

    fun shareText(): String? {
        val s = _state.value
        return if (s.evaluated && s.result != null) "${s.expression} = ${s.result}" else null
    }

    companion object {
        private const val KEY_EXPR = "expression"
    }
}
