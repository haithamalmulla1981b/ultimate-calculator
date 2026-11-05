package com.hmalmulla.calculator.ui.calculator

import android.content.res.Configuration
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmalmulla.calculator.engine.AngleMode

/** Physical keyboard support for both calculators. */
private fun Modifier.calcKeyboard(vm: CalculatorViewModel, focus: FocusRequester): Modifier =
    this.focusRequester(focus).focusable().onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (e.key) {
            Key.Enter, Key.NumPadEnter, Key.NumPadEquals -> { vm.equals(); return@onPreviewKeyEvent true }
            Key.Backspace -> { vm.backspace(); return@onPreviewKeyEvent true }
            Key.Escape, Key.Delete -> { vm.clear(); return@onPreviewKeyEvent true }
            else -> {}
        }
        val ch = e.utf16CodePoint.toChar()
        val token = when (ch) {
            in '0'..'9', '.', '(', ')', '%', '!', '^' -> ch.toString()
            '+' -> "+"
            '-' -> "−"
            '*', 'x', 'X' -> "×"
            '/' -> "÷"
            '=' -> { vm.equals(); return@onPreviewKeyEvent true }
            ',' -> "."
            else -> null
        }
        if (token != null) { vm.input(token); true } else false
    }

@Composable
private fun rememberKeyboardFocus(): FocusRequester {
    val f = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { f.requestFocus() } }
    return f
}

@Composable
private fun isLandscape() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

@Composable
fun StandardCalculatorScreen(vm: CalculatorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = rememberKeyboardFocus()
    val landscape = isLandscape()
    Box(Modifier.fillMaxSize().calcKeyboard(vm, focus)) {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxSize()) {
                    CalcDisplay(state, recent, { vm.useResult(it) }, { shareText(context, it) }, Modifier.weight(1f))
                    DisplayActions(vm)
                    MemoryRow(vm, 44.dp)
                }
                Column(Modifier.weight(1.2f).fillMaxSize().padding(6.dp)) { StandardPad(vm) }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                CalcDisplay(state, recent, { vm.useResult(it) }, { shareText(context, it) }, Modifier.weight(0.42f))
                DisplayActions(vm)
                MemoryRow(vm, 40.dp)
                Column(Modifier.weight(0.58f).padding(horizontal = 8.dp, vertical = 4.dp)) { StandardPad(vm) }
            }
        }
    }
}

@Composable
private fun DisplayActions(vm: CalculatorViewModel, onTools: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onTools != null) {
            IconButton(onClick = onTools) { Icon(Icons.Outlined.Functions, contentDescription = "Math tools") }
        }
        Box(Modifier.weight(1f))
        IconButton(onClick = { vm.backspace() }) {
            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete last", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 4 × 5 everyday keypad. Long-press hints are shown in the key corner. */
@Composable
fun ColumnScope.StandardPad(vm: CalculatorViewModel) {
    Row(Modifier.weight(1f)) {
        CalcKey("AC", KeyKind.CLEAR, { vm.clear() }, description = "Clear all")
        CalcKey("( )", KeyKind.FUNCTION, { vm.bracket() }, description = "Brackets", onLongClick = { vm.input("(") }, longLabel = "(")
        CalcKey("%", KeyKind.FUNCTION, { vm.input("%") }, description = "Percent", onLongClick = { vm.input("!") }, longLabel = "!")
        CalcKey("÷", KeyKind.OPERATOR, { vm.input("÷") }, description = "Divide", onLongClick = { vm.input("√(") }, longLabel = "√")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("7", KeyKind.NUMBER, { vm.input("7") })
        CalcKey("8", KeyKind.NUMBER, { vm.input("8") })
        CalcKey("9", KeyKind.NUMBER, { vm.input("9") })
        CalcKey("×", KeyKind.OPERATOR, { vm.input("×") }, description = "Multiply", onLongClick = { vm.input("^") }, longLabel = "xʸ")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("4", KeyKind.NUMBER, { vm.input("4") })
        CalcKey("5", KeyKind.NUMBER, { vm.input("5") })
        CalcKey("6", KeyKind.NUMBER, { vm.input("6") })
        CalcKey("−", KeyKind.OPERATOR, { vm.input("−") }, description = "Minus", onLongClick = { vm.toggleSign() }, longLabel = "±")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("1", KeyKind.NUMBER, { vm.input("1") })
        CalcKey("2", KeyKind.NUMBER, { vm.input("2") })
        CalcKey("3", KeyKind.NUMBER, { vm.input("3") })
        CalcKey("+", KeyKind.OPERATOR, { vm.input("+") }, description = "Plus")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("±", KeyKind.NUMBER, { vm.toggleSign() }, description = "Change sign")
        CalcKey("0", KeyKind.NUMBER, { vm.input("0") }, onLongClick = { vm.input("00") }, longLabel = "00")
        CalcKey(".", KeyKind.NUMBER, { vm.input(".") }, description = "Decimal point")
        CalcKey("=", KeyKind.EQUALS, { vm.equals() }, description = "Equals")
    }
}

@Composable
fun ScientificCalculatorScreen(vm: CalculatorViewModel, onTools: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = rememberKeyboardFocus()
    var second by rememberSaveable { mutableStateOf(false) }
    val landscape = isLandscape()
    Box(Modifier.fillMaxSize().calcKeyboard(vm, focus)) {
        if (landscape) {
            Column(Modifier.fillMaxSize()) {
                CalcDisplay(state, recent, { vm.useResult(it) }, { shareText(context, it) }, Modifier.weight(0.3f),
                    showAngle = true, onAngle = { vm.toggleAngle() })
                Row(Modifier.weight(0.7f).padding(4.dp)) {
                    Column(Modifier.weight(1.5f).fillMaxSize()) { ScientificGrid(vm, state.angle, second, { second = !second }, onTools = onTools) }
                    Column(Modifier.weight(1f).fillMaxSize()) { StandardPad(vm) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                CalcDisplay(state, recent, { vm.useResult(it) }, { shareText(context, it) }, Modifier.weight(0.26f),
                    showAngle = true, onAngle = { vm.toggleAngle() })
                DisplayActions(vm, onTools)
                Column(Modifier.weight(0.34f).padding(horizontal = 6.dp)) {
                    ScientificGrid(vm, state.angle, second, { second = !second }, onTools = null)
                }
                Column(Modifier.weight(0.40f).padding(horizontal = 6.dp, vertical = 2.dp)) { StandardPad(vm) }
            }
        }
    }
}

@Composable
private fun ColumnScope.ScientificGrid(vm: CalculatorViewModel, angle: AngleMode, second: Boolean, toggleSecond: () -> Unit, onTools: (() -> Unit)?) {
    val f = KeyKind.FUNCTION
    Row(Modifier.weight(1f)) {
        CalcKey(if (second) "2nd ✓" else "2nd", if (second) KeyKind.OPERATOR else f, toggleSecond, description = "Second functions")
        CalcKey(if (second) "sin⁻¹" else "sin", f, { vm.input(if (second) "asin(" else "sin(") }, description = if (second) "arc sine" else "sine")
        CalcKey(if (second) "cos⁻¹" else "cos", f, { vm.input(if (second) "acos(" else "cos(") }, description = if (second) "arc cosine" else "cosine")
        CalcKey(if (second) "tan⁻¹" else "tan", f, { vm.input(if (second) "atan(" else "tan(") }, description = if (second) "arc tangent" else "tangent")
        CalcKey(if (angle == AngleMode.DEG) "DEG" else "RAD", KeyKind.OPERATOR, { vm.toggleAngle() }, description = "Switch degrees or radians")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("sinh", f, { vm.input("sinh(") }, description = "hyperbolic sine")
        CalcKey("cosh", f, { vm.input("cosh(") }, description = "hyperbolic cosine")
        CalcKey("tanh", f, { vm.input("tanh(") }, description = "hyperbolic tangent")
        CalcKey("π", f, { vm.input("π") }, description = "pi")
        CalcKey("e", f, { vm.input("e") }, description = "Euler's number")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("x²", f, { vm.input("²") }, description = "square")
        CalcKey("x³", f, { vm.input("³") }, description = "cube")
        CalcKey("xʸ", f, { vm.input("^") }, description = "power")
        CalcKey("x⁻¹", f, { vm.input("^(−1)") }, description = "reciprocal")
        CalcKey("n!", f, { vm.input("!") }, description = "factorial")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("√", f, { vm.input("√(") }, description = "square root")
        CalcKey("∛", f, { vm.input("∛(") }, description = "cube root")
        CalcKey("ʸ√x", f, { vm.input("root(") }, description = "n-th root, root of n comma x")
        CalcKey(if (second) "10ˣ" else "log", f, { vm.input(if (second) "10^" else "log(") }, description = if (second) "ten to the power" else "log base 10")
        CalcKey(if (second) "eˣ" else "ln", f, { vm.input(if (second) "exp(" else "ln(") }, description = if (second) "e to the power" else "natural log")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("logₓ", f, { vm.input("logb(") }, description = "log with base, base comma value")
        CalcKey("|x|", f, { vm.input("abs(") }, description = "absolute value")
        CalcKey("mod", f, { vm.input("mod(") }, description = "modulo")
        CalcKey("nPr", f, { vm.input("nPr(") }, description = "permutations")
        CalcKey("nCr", f, { vm.input("nCr(") }, description = "combinations")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("⌊x⌋", f, { vm.input("floor(") }, description = "floor")
        CalcKey("⌈x⌉", f, { vm.input("ceil(") }, description = "ceiling")
        CalcKey("rnd", f, { vm.input("round(") }, description = "round")
        CalcKey("gcd", f, { vm.input("gcd(") }, description = "greatest common divisor")
        CalcKey("lcm", f, { vm.input("lcm(") }, description = "least common multiple")
    }
    Row(Modifier.weight(1f)) {
        CalcKey("(", f, { vm.input("(") }, description = "open bracket")
        CalcKey(")", f, { vm.input(")") }, description = "close bracket")
        CalcKey(",", f, { vm.input(",") }, description = "comma, separates values")
        CalcKey("EE", f, { vm.input("E") }, description = "times ten to the power")
        if (onTools != null) CalcKey("Tools", KeyKind.OPERATOR, onTools, description = "Math tools")
        else CalcKey("⌫", f, { vm.backspace() }, description = "Delete")
    }
}
