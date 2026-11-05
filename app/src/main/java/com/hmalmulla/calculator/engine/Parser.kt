package com.hmalmulla.calculator.engine

import com.hmalmulla.calculator.engine.MathEngine.MC
import com.hmalmulla.calculator.engine.MathEngine.stripZeros
import com.hmalmulla.calculator.engine.MathEngine.toIntegerOrNull
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal sealed class Token {
    data class Num(val value: BigDecimal) : Token()
    data class Op(val c: Char) : Token() // + − × ÷ ^
    data class Func(val name: String) : Token()
    data class Const(val name: String) : Token()
    object LParen : Token()
    object RParen : Token()
    object Comma : Token()
    object Percent : Token()
    object Factorial : Token()
}

internal object Tokenizer {
    val FUNCTIONS = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
        "log", "ln", "logb", "sqrt", "cbrt", "root", "abs", "floor", "ceil", "round",
        "exp", "npr", "ncr", "gcd", "lcm", "mod",
    )

    fun tokenize(input: String): List<Token> {
        val out = ArrayList<Token>()
        var i = 0
        val s = input
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    var dots = 0
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                        if (s[i] == '.') dots++
                        i++
                    }
                    if (dots > 1) throw CalcException(CalcError.Syntax("A number has more than one decimal point"))
                    // scientific notation: 1.5E-3
                    if (i < s.length && s[i] == 'E') {
                        var j = i + 1
                        if (j < s.length && (s[j] == '+' || s[j] == '-' || s[j] == '−')) j++
                        if (j < s.length && s[j].isDigit()) {
                            while (j < s.length && s[j].isDigit()) j++
                            i = j
                        }
                    }
                    val text = s.substring(start, i).replace('−', '-')
                    if (text == ".") throw CalcException(CalcError.Syntax("A decimal point needs digits"))
                    out.add(Token.Num(BigDecimal(text)))
                }
                c == '+' -> { out.add(Token.Op('+')); i++ }
                c == '-' || c == '−' || c == '–' -> { out.add(Token.Op('−')); i++ }
                c == '*' || c == '×' || c == '·' -> { out.add(Token.Op('×')); i++ }
                c == '/' || c == '÷' -> { out.add(Token.Op('÷')); i++ }
                c == '^' -> { out.add(Token.Op('^')); i++ }
                c == '²' -> { out.add(Token.Op('^')); out.add(Token.Num(BigDecimal(2))); i++ }
                c == '³' -> { out.add(Token.Op('^')); out.add(Token.Num(BigDecimal(3))); i++ }
                c == '%' -> { out.add(Token.Percent); i++ }
                c == '!' -> { out.add(Token.Factorial); i++ }
                c == '(' -> { out.add(Token.LParen); i++ }
                c == ')' -> { out.add(Token.RParen); i++ }
                c == ',' || c == ';' -> { out.add(Token.Comma); i++ }
                c == 'π' -> { out.add(Token.Const("π")); i++ }
                c == '√' -> { out.add(Token.Func("sqrt")); i++ }
                c == '∛' -> { out.add(Token.Func("cbrt")); i++ }
                c.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter() && s[i] != 'π') i++
                    val word = s.substring(start, i).lowercase()
                    when {
                        word == "e" -> out.add(Token.Const("e"))
                        word == "pi" -> out.add(Token.Const("π"))
                        word in FUNCTIONS -> out.add(Token.Func(word))
                        else -> throw CalcException(CalcError.Syntax("Unknown function \"$word\""))
                    }
                }
                else -> throw CalcException(CalcError.Syntax("Unexpected symbol \"$c\""))
            }
        }
        return out
    }
}

/** Parses and evaluates in one pass. */
internal class Parser(private val tokens: List<Token>, private val angle: AngleMode) {
    private var pos = 0

    private class Value(val v: BigDecimal, val percent: Boolean = false)

    private fun peek(): Token? = tokens.getOrNull(pos)
    private fun next(): Token? = tokens.getOrNull(pos++)

    fun parseAll(): BigDecimal {
        val v = expr()
        if (pos < tokens.size) {
            throw CalcException(
                if (tokens[pos] == Token.RParen) CalcError.Syntax("Too many closing brackets")
                else CalcError.Syntax("Unexpected input")
            )
        }
        return value(v)
    }

    private fun value(x: Value): BigDecimal =
        if (x.percent) x.v.divide(BigDecimal(100), MC) else x.v

    private fun expr(): Value {
        var left = term()
        while (true) {
            val t = peek()
            if (t is Token.Op && (t.c == '+' || t.c == '−')) {
                pos++
                if (peek() == null) throw CalcException(CalcError.Syntax("Finish the calculation after \"${t.c}\""))
                val right = term()
                val l = value(left)
                val r = if (right.percent) l.multiply(right.v, MC).divide(BigDecimal(100), MC) else right.v
                left = Value(MathEngine.binary(l, t.c, r))
            } else return left
        }
    }

    private fun startsPrimary(t: Token?) =
        t is Token.Num || t is Token.Const || t is Token.Func || t == Token.LParen

    private fun term(): Value {
        var left = unary()
        while (true) {
            val t = peek()
            if (t is Token.Op && (t.c == '×' || t.c == '÷')) {
                pos++
                if (peek() == null) throw CalcException(CalcError.Syntax("Finish the calculation after \"${t.c}\""))
                val right = unary()
                left = Value(MathEngine.binary(value(left), t.c, value(right)))
            } else if (startsPrimary(t)) {
                // implicit multiplication: 2π, 3(4), (2)(3), 2sin(30)
                val right = unary()
                left = Value(MathEngine.binary(value(left), '×', value(right)))
            } else return left
        }
    }

    private fun unary(): Value {
        val t = peek()
        if (t is Token.Op && t.c == '−') {
            pos++
            val v = unary()
            return Value(v.v.negate(), v.percent)
        }
        if (t is Token.Op && t.c == '+') {
            pos++
            return unary()
        }
        return power()
    }

    private fun power(): Value {
        val base = postfix()
        val t = peek()
        if (t is Token.Op && t.c == '^') {
            pos++
            if (peek() == null) throw CalcException(CalcError.Syntax("Add an exponent after \"^\""))
            val exp = unary()
            return Value(MathEngine.power(value(base), value(exp)))
        }
        return base
    }

    private fun postfix(): Value {
        var v = primary()
        while (true) {
            when (peek()) {
                Token.Factorial -> { pos++; v = Value(MathEngine.factorial(value(v))) }
                Token.Percent -> { pos++; v = Value(value(v), percent = true) }
                else -> return v
            }
        }
    }

    private fun primary(): Value {
        return when (val t = next()) {
            null -> throw CalcException(CalcError.Syntax("Expression is incomplete"))
            is Token.Num -> Value(t.value)
            is Token.Const -> Value(if (t.name == "π") MathEngine.PI else MathEngine.E)
            Token.LParen -> {
                if (peek() == Token.RParen) throw CalcException(CalcError.Syntax("Empty brackets"))
                val v = expr()
                if (peek() == Token.RParen) pos++ // missing ')' at the end closes automatically
                else if (peek() != null) throw CalcException(CalcError.Syntax("Missing closing bracket"))
                Value(value(v))
            }
            is Token.Func -> Value(function(t.name))
            is Token.Op -> throw CalcException(CalcError.Syntax("Operator \"${t.c}\" is in the wrong place"))
            Token.RParen -> throw CalcException(CalcError.Syntax("Closing bracket without opening"))
            Token.Comma -> throw CalcException(CalcError.Syntax("Comma in the wrong place"))
            Token.Percent -> throw CalcException(CalcError.Syntax("% needs a number before it"))
            Token.Factorial -> throw CalcException(CalcError.Syntax("! needs a number before it"))
        }
    }

    private fun args(name: String): List<BigDecimal> {
        if (peek() != Token.LParen) {
            // e.g. √144 or sin30: a single argument without brackets
            return listOf(value(power()))
        }
        pos++
        val list = ArrayList<BigDecimal>()
        if (peek() == Token.RParen) throw CalcException(CalcError.Syntax("$name() needs a value"))
        while (true) {
            list.add(value(expr()))
            when (peek()) {
                Token.Comma -> pos++
                Token.RParen -> { pos++; return list }
                null -> return list // auto-close
                else -> throw CalcException(CalcError.Syntax("Missing closing bracket"))
            }
        }
    }

    private fun need(name: String, a: List<BigDecimal>, n: Int) {
        if (a.size != n) throw CalcException(CalcError.Syntax("$name needs $n value${if (n > 1) "s" else ""}"))
    }

    private fun toRad(x: BigDecimal): Double =
        if (angle == AngleMode.DEG) Math.toRadians(x.toDouble()) else x.toDouble()

    private fun fromRad(r: Double): BigDecimal =
        MathEngine.fromDouble(if (angle == AngleMode.DEG) Math.toDegrees(r) else r).let { MathEngine.cleanRoot(it) }

    /** Trig with exact results for multiples of 90° in degree mode. */
    private fun trig(name: String, x: BigDecimal): BigDecimal {
        if (angle == AngleMode.DEG) {
            val q = x.remainder(BigDecimal(360)).let { if (it.signum() < 0) it.add(BigDecimal(360)) else it }
            val exact = q.remainder(BigDecimal(90)).signum() == 0
            if (exact) {
                val k = q.divide(BigDecimal(90)).toInt() // 0..3
                val sin = intArrayOf(0, 1, 0, -1)[k]
                val cos = intArrayOf(1, 0, -1, 0)[k]
                return when (name) {
                    "sin" -> BigDecimal(sin)
                    "cos" -> BigDecimal(cos)
                    else -> if (cos == 0) throw CalcException(CalcError.Domain("tan is undefined at ${x.stripZeros().toPlainString()}°"))
                    else BigDecimal(sin / cos)
                }
            }
        }
        val r = toRad(x)
        val d = when (name) {
            "sin" -> Math.sin(r)
            "cos" -> Math.cos(r)
            else -> {
                if (Math.abs(Math.cos(r)) < 1e-15) throw CalcException(CalcError.Domain("tan is undefined here"))
                Math.tan(r)
            }
        }
        return if (Math.abs(d) < 1e-14) BigDecimal.ZERO else MathEngine.cleanRoot(MathEngine.fromDouble(d))
    }

    private fun function(name: String): BigDecimal {
        val a = args(name)
        fun one(): BigDecimal { need(name, a, 1); return a[0] }
        return when (name) {
            "sin", "cos", "tan" -> trig(name, one())
            "asin", "acos" -> {
                val x = one()
                if (x.abs() > BigDecimal.ONE) throw CalcException(CalcError.Domain("$name needs a value between −1 and 1"))
                fromRad(if (name == "asin") Math.asin(x.toDouble()) else Math.acos(x.toDouble()))
            }
            "atan" -> fromRad(Math.atan(one().toDouble()))
            "sinh" -> MathEngine.fromDouble(Math.sinh(one().toDouble()))
            "cosh" -> MathEngine.fromDouble(Math.cosh(one().toDouble()))
            "tanh" -> MathEngine.fromDouble(Math.tanh(one().toDouble()))
            "log" -> {
                if (a.size == 2) logBase(a[1], a[0]) else logBase(BigDecimal.TEN, one())
            }
            "logb" -> { need(name, a, 2); logBase(a[0], a[1]) }
            "ln" -> {
                val x = one()
                if (x.signum() <= 0) throw CalcException(CalcError.Domain("ln needs a number greater than 0"))
                MathEngine.fromDouble(Math.log(x.toDouble()))
            }
            "sqrt" -> {
                val x = one()
                if (x.signum() < 0) throw CalcException(CalcError.Domain("Square root of a negative number"))
                MathEngine.cleanRoot(x.sqrt(MC))
            }
            "cbrt" -> {
                val x = one()
                MathEngine.cleanRoot(MathEngine.fromDouble(Math.cbrt(x.toDouble())))
            }
            "root" -> { // root(n, x) = n-th root of x
                need(name, a, 2)
                val n = a[0]
                if (n.signum() == 0) throw CalcException(CalcError.Domain("The 0th root is undefined"))
                MathEngine.power(a[1], BigDecimal.ONE.divide(n, MC))
            }
            "abs" -> one().abs()
            "floor" -> one().setScale(0, RoundingMode.FLOOR)
            "ceil" -> one().setScale(0, RoundingMode.CEILING)
            "round" -> {
                if (a.size == 2) {
                    val d = a[1].toIntegerOrNull()?.toInt() ?: throw CalcException(CalcError.Domain("round needs whole digits"))
                    a[0].setScale(d.coerceIn(0, 30), RoundingMode.HALF_UP)
                } else one().setScale(0, RoundingMode.HALF_UP)
            }
            "exp" -> {
                val x = one()
                if (x > BigDecimal(709)) throw CalcException(CalcError.Overflow)
                MathEngine.fromDouble(Math.exp(x.toDouble()))
            }
            "npr" -> { need("nPr", a, 2); MathEngine.permutations(a[0], a[1]) }
            "ncr" -> { need("nCr", a, 2); MathEngine.combinations(a[0], a[1]) }
            "gcd", "lcm" -> {
                if (a.size < 2) throw CalcException(CalcError.Syntax("$name needs at least 2 values"))
                val ints = a.map { MathEngine.requireInteger(it, name).abs() }
                BigDecimal(if (name == "gcd") AdvancedMath.gcd(ints) else AdvancedMath.lcm(ints))
            }
            "mod" -> {
                need(name, a, 2)
                if (a[1].signum() == 0) throw CalcException(CalcError.DivisionByZero)
                a[0].remainder(a[1], MC)
            }
            else -> throw CalcException(CalcError.Syntax("Unknown function \"$name\""))
        }
    }

    private fun logBase(base: BigDecimal, x: BigDecimal): BigDecimal {
        if (x.signum() <= 0) throw CalcException(CalcError.Domain("log needs a number greater than 0"))
        if (base.signum() <= 0 || base.compareTo(BigDecimal.ONE) == 0) {
            throw CalcException(CalcError.Domain("log base must be positive and not 1"))
        }
        // exact answer for whole powers of the base (log(1000) = 3)
        val guess = Math.round(Math.log(x.toDouble()) / Math.log(base.toDouble()))
        if (kotlin.math.abs(guess) <= 400) {
            val p = try { MathEngine.power(base, BigDecimal(guess)) } catch (e: CalcException) { null }
            if (p != null && p.compareTo(x) == 0) return BigDecimal(guess)
        }
        return MathEngine.cleanRoot(MathEngine.fromDouble(Math.log(x.toDouble()) / Math.log(base.toDouble())))
    }
}

/** Formats results for display. */
object NumberFormatter {
    fun format(
        v: BigDecimal,
        maxDecimals: Int = 10,
        grouping: Boolean = true,
    ): String {
        if (v.signum() == 0) return "0"
        val a = v.abs()
        if (a >= BigDecimal("1E16") || a < BigDecimal("1E-9")) return scientific(v, maxDecimals)
        var s = v.setScale(maxDecimals, RoundingMode.HALF_UP).let {
            if (it.signum() == 0) BigDecimal.ZERO else it.stripTrailingZeros()
        }.toPlainString()
        if (s == "-0") s = "0"
        return if (grouping) group(s) else s
    }

    fun scientific(v: BigDecimal, maxDecimals: Int = 10): String {
        val digits = (maxDecimals + 1).coerceIn(2, 16)
        val r = v.round(MathContext(digits, RoundingMode.HALF_UP))
        val exp = r.precision() - r.scale() - 1
        val mantissa = r.movePointLeft(exp).stripTrailingZeros().toPlainString()
        return "${mantissa}E$exp"
    }

    /** Adds thousands separators to the integer part of a plain number. */
    fun group(plain: String): String {
        val neg = plain.startsWith("-")
        val body = if (neg) plain.substring(1) else plain
        val dot = body.indexOf('.')
        val intPart = if (dot >= 0) body.substring(0, dot) else body
        val frac = if (dot >= 0) body.substring(dot) else ""
        val sb = StringBuilder()
        for ((i, ch) in intPart.withIndex()) {
            if (i > 0 && (intPart.length - i) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        return (if (neg) "−" else "") + sb + frac
    }

    /** Groups every number inside an expression for the input display. */
    fun groupExpression(expr: String): String {
        val re = Regex("\\d+(\\.\\d*)?")
        return re.replace(expr) { m ->
            // leave exponent digits of scientific notation alone
            if (m.range.first > 0 && expr[m.range.first - 1] == 'E') m.value else group(m.value)
        }
    }
}
