package com.hmalmulla.calculator.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TrendingUp
import com.hmalmulla.calculator.engine.AdvancedMath
import com.hmalmulla.calculator.engine.Fraction
import java.math.BigDecimal
import java.math.BigInteger

private fun fractionCalc(text: String): Pair<Fraction, List<String>> {
    // "a op b" where a and b are fractions or mixed numbers, op is + − × ÷
    val t = text.replace('*', '×').replace('/', '/').trim()
    val m = Regex("^(.+?)\\s+([+−\\-×÷])\\s+(.+)$").find(t)
        ?: return Fraction.parse(t).let { it to listOf("$t = $it") }
    val a = Fraction.parse(m.groupValues[1])
    val b = Fraction.parse(m.groupValues[3])
    val r = when (m.groupValues[2]) {
        "+" -> a + b
        "−", "-" -> a - b
        "×" -> a * b
        else -> {
            require(b.num.signum() != 0) { "Can't divide by zero" }
            a / b
        }
    }
    return r to listOf("$a ${m.groupValues[2]} $b = $r")
}

val MathTools: List<Tool> = listOf(
    Tool(
        "fractions", "Fractions", "Add, subtract, multiply and divide fractions", Icons.Outlined.PieChart,
        listOf(Field("expr", "Calculation", "1 1/2 + 3/4", FieldKind.TEXT, hint = "e.g. 2/3 × 3/8 (spaces around the operator)")),
    ) { i ->
        val (r, steps) = fractionCalc(i.text("expr"))
        ToolOutput(listOf("Result" to r.toString(), "Mixed number" to r.toMixedString(), "Decimal" to num(r.toDecimal(), 10)), steps)
    },
    Tool(
        "frac2dec", "Fraction ↔ decimal", "Convert and simplify", Icons.Outlined.CompareArrows,
        listOf(Field("value", "Fraction or decimal", "0.375", FieldKind.TEXT, hint = "e.g. 6/8, 1 1/4 or 0.375")),
    ) { i ->
        val t = i.text("value")
        require(t.isNotEmpty()) { "Enter a fraction or a decimal" }
        val f = if (t.contains('/')) Fraction.parse(t) else Fraction.approximate(BigDecimal(t.replace(",", "")))
        ToolOutput(listOf("Simplified fraction" to f.toString(), "Mixed number" to f.toMixedString(), "Decimal" to num(f.toDecimal(), 12)))
    },
    Tool(
        "ratio", "Ratios and proportions", "Simplify a:b or solve a/b = c/x", Icons.Outlined.LinearScale,
        listOf(Field("a", "a", "4"), Field("b", "b", "6"), Field("c", "c (optional, to solve a/b = c/x)", "")),
    ) { i ->
        val a = i.num("a"); val b = i.num("b")
        val rows = mutableListOf<Pair<String, String>>()
        val steps = mutableListOf<String>()
        if (a.stripTrailingZeros().scale() <= 0 && b.stripTrailingZeros().scale() <= 0) {
            val s = AdvancedMath.simplifyRatio(listOf(a.toBigInteger(), b.toBigInteger()))
            rows.add("Simplified ratio" to "${s[0]} : ${s[1]}")
        }
        i.numOrNull("c")?.let { c ->
            val x = AdvancedMath.proportion(a, b, c)
            rows.add("x" to num(x.value, 10)); steps.addAll(x.steps)
        }
        require(rows.isNotEmpty()) { "Use whole numbers to simplify, or enter c to solve" }
        ToolOutput(rows, steps)
    },
    Tool(
        "pctchange", "Percent increase / decrease", "Change from one value to another", Icons.Outlined.TrendingUp,
        listOf(Field("from", "From", "80"), Field("to", "To", "100")),
    ) { i ->
        val c = AdvancedMath.percentChange(i.num("from"), i.num("to"))
        ToolOutput(
            listOf((if (c.signum() >= 0) "Increase" else "Decrease") to pct(c.abs())),
            steps = listOf("(${i.text("to")} − ${i.text("from")}) ÷ |${i.text("from")}| × 100 = ${pct(c)}"),
        )
    },
    Tool(
        "pctapply", "Add or subtract a percentage", "Value ± p%", Icons.Outlined.Percent,
        listOf(
            Field("value", "Value", "250"), Field("pct", "Percentage", "12", suffix = "%"),
            Field("mode", "Operation", "Increase", FieldKind.CHOICE, options = listOf("Increase", "Decrease")),
        ),
    ) { i ->
        val inc = i.choice("mode") == "Increase"
        val r = AdvancedMath.applyPercent(i.num("value"), i.num("pct"), inc)
        ToolOutput(listOf("Result" to num(r, 10), "Change" to num(AdvancedMath.percentOf(i.num("pct"), i.num("value")), 10)))
    },
    Tool(
        "pctdiff", "Percentage difference", "|a − b| ÷ average", Icons.Outlined.CompareArrows,
        listOf(Field("a", "Value A", "40"), Field("b", "Value B", "50")),
    ) { i -> ToolOutput(listOf("Difference" to pct(AdvancedMath.percentDifference(i.num("a"), i.num("b"))))) },
    Tool(
        "prime", "Prime checker and factorization", "Is it prime? Its prime factors", Icons.Outlined.Tag,
        listOf(Field("n", "Whole number", "360")),
    ) { i ->
        val n = i.long("n")
        val p = AdvancedMath.isPrime(n)
        val rows = mutableListOf("Prime?" to if (p.value) "Yes" else "No")
        val steps = p.steps.toMutableList()
        if (n >= 2 && !p.value) {
            val f = AdvancedMath.primeFactors(n)
            rows.add("Prime factors" to f.steps.last().substringAfter("= "))
            steps.addAll(f.steps)
        }
        ToolOutput(rows, steps)
    },
    Tool(
        "gcdlcm", "GCD and LCM", "Greatest common divisor, least common multiple", Icons.Outlined.GridOn,
        listOf(Field("list", "Whole numbers", "12, 18, 30", FieldKind.TEXT, hint = "separate with commas")),
    ) { i ->
        val nums = AdvancedMath.parseList(i.text("list")).map {
            require(it.stripTrailingZeros().scale() <= 0) { "Use whole numbers only" }
            it.toBigInteger().abs()
        }
        require(nums.size >= 2) { "Enter at least two numbers" }
        ToolOutput(listOf("GCD" to AdvancedMath.gcd(nums).toString(), "LCM" to AdvancedMath.lcm(nums).toString()))
    },
    Tool(
        "stats", "Statistics", "Mean, median, mode, deviation, variance, sum", Icons.Outlined.BarChart,
        listOf(Field("list", "Numbers", "2, 4, 4, 4, 5, 5, 7, 9", FieldKind.TEXT, hint = "separate with commas or spaces")),
    ) { i ->
        val s = AdvancedMath.stats(AdvancedMath.parseList(i.text("list")))
        ToolOutput(listOfNotNull(
            "Mean" to num(s.mean, 8),
            "Median" to num(s.median, 8),
            "Mode" to (if (s.modes.isEmpty()) "No mode" else s.modes.joinToString(", ") { num(it, 8) }),
            "Sum" to num(s.sum, 8),
            "Count" to s.count.toString(),
            "Std. deviation (population)" to num(s.populationSd, 8),
            s.sampleSd?.let { "Std. deviation (sample)" to num(it, 8) },
            "Variance (population)" to num(s.populationVariance, 8),
            s.sampleVariance?.let { "Variance (sample)" to num(it, 8) },
            "Minimum" to num(s.min, 8),
            "Maximum" to num(s.max, 8),
        ))
    },
    Tool(
        "arith", "Arithmetic progression", "n-th term and sum", Icons.Outlined.ShowChart,
        listOf(Field("a", "First term a₁", "1"), Field("d", "Common difference d", "1"), Field("n", "Number of terms n", "100")),
    ) { i ->
        val r = AdvancedMath.arithmetic(i.num("a"), i.num("d"), i.int("n"))
        ToolOutput(listOf("Sum Sₙ" to num(r.value.second, 10), "n-th term aₙ" to num(r.value.first, 10)), r.steps)
    },
    Tool(
        "geo", "Geometric progression", "n-th term and sum", Icons.Outlined.TrendingUp,
        listOf(Field("a", "First term a₁", "1"), Field("r", "Common ratio r", "2"), Field("n", "Number of terms n", "10")),
    ) { i ->
        val r = AdvancedMath.geometric(i.num("a"), i.num("r"), i.int("n"))
        ToolOutput(listOf("Sum Sₙ" to num(r.value.second, 10), "n-th term aₙ" to num(r.value.first, 10)), r.steps)
    },
    Tool(
        "linear", "Linear equation", "Solve a·x + b = c", Icons.Outlined.Functions,
        listOf(Field("a", "a", "3"), Field("b", "b", "4"), Field("c", "c", "19")),
    ) { i ->
        val r = AdvancedMath.linear(i.num("a"), i.num("b"), i.num("c"))
        ToolOutput(listOf("x" to num(r.value, 10)), r.steps)
    },
    Tool(
        "linear2", "Two-variable linear system", "a₁x + b₁y = c₁ and a₂x + b₂y = c₂", Icons.Outlined.DataArray,
        listOf(
            Field("a1", "a₁", "2"), Field("b1", "b₁", "1"), Field("c1", "c₁", "5"),
            Field("a2", "a₂", "1"), Field("b2", "b₂", "−1"), Field("c2", "c₂", "1"),
        ),
    ) { i ->
        val r = AdvancedMath.linear2(i.num("a1"), i.num("b1"), i.num("c1"), i.num("a2"), i.num("b2"), i.num("c2"))
        ToolOutput(listOf("x" to num(r.value.first, 10), "y" to num(r.value.second, 10)), r.steps)
    },
    Tool(
        "quadratic", "Quadratic equation", "Solve ax² + bx + c = 0", Icons.Outlined.Calculate,
        listOf(Field("a", "a", "1"), Field("b", "b", "−3"), Field("c", "c", "2")),
    ) { i ->
        val r = AdvancedMath.quadratic(i.num("a"), i.num("b"), i.num("c"))
        val rows = r.value.roots.mapIndexed { k, s -> (if (r.value.roots.size == 1) "x" else "x${if (k == 0) "₁" else "₂"}") to s }
        ToolOutput(rows + ("Discriminant Δ" to num(r.value.discriminant, 10)), r.steps)
    },
    Tool(
        "sci", "Scientific notation", "Convert to and from a × 10ⁿ", Icons.Outlined.Science,
        listOf(Field("value", "Number or scientific form", "123400", FieldKind.TEXT, hint = "e.g. 0.00052 or 5.2 × 10^-4 or 5.2e-4")),
    ) { i ->
        val t = i.text("value")
        require(t.isNotEmpty()) { "Enter a number" }
        val v = try { AdvancedMath.fromScientific(t) } catch (e: NumberFormatException) { throw IllegalArgumentException("Not a valid number") }
        ToolOutput(listOf("Scientific" to AdvancedMath.toScientific(v), "Decimal" to v.stripTrailingZeros().toPlainString(), "E notation" to v.toString().replace("E+", "E")))
    },
)
