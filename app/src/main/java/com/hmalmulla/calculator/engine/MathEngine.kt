package com.hmalmulla.calculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

enum class AngleMode { DEG, RAD }

/** A calculation problem explained in plain words. Never a crash. */
sealed class CalcError(val message: String) {
    object DivisionByZero : CalcError("Can't divide by zero")
    object Overflow : CalcError("Result is too large")
    object Empty : CalcError("Enter a calculation")
    class Syntax(detail: String) : CalcError(detail)
    class Domain(detail: String) : CalcError(detail)
}

sealed class EvalResult {
    data class Success(val value: BigDecimal) : EvalResult()
    data class Failure(val error: CalcError) : EvalResult()
}

internal class CalcException(val error: CalcError) : RuntimeException(error.message)

/**
 * Expression engine: tokenizer + recursive-descent parser that evaluates with
 * BigDecimal (40 significant digits) for + − × ÷, integer powers, roots of
 * perfect squares, factorials and combinatorics. Transcendental functions use
 * double precision and are rounded to 15 significant digits.
 *
 * Grammar (highest binding last):
 *   expr    := term (('+' | '−') term)*
 *   term    := unary (('×' | '÷') unary | <implicit ×> unary)*
 *   unary   := ('−' | '+') unary | power
 *   power   := postfix ('^' unary)?            (right-associative)
 *   postfix := primary ('!' | '%')*
 *   primary := number | constant | function | '(' expr ')'
 *
 * Percent rules (as on everyday calculators): a + b% = a + a·b/100,
 * a − b% = a − a·b/100, and elsewhere b% = b/100 (so 200 × 10% = 20).
 * Missing closing brackets are closed automatically at the end.
 */
object MathEngine {
    val MC: MathContext = MathContext(40, RoundingMode.HALF_EVEN)
    private val MAX = BigDecimal("1E10000")
    private val MIN = BigDecimal("1E-10000")
    val PI = BigDecimal("3.141592653589793238462643383279502884197")
    val E = BigDecimal("2.718281828459045235360287471352662497757")

    fun evaluate(expression: String, angle: AngleMode = AngleMode.DEG): EvalResult {
        if (expression.isBlank()) return EvalResult.Failure(CalcError.Empty)
        return try {
            val tokens = Tokenizer.tokenize(expression)
            if (tokens.isEmpty()) return EvalResult.Failure(CalcError.Empty)
            val v = Parser(tokens, angle).parseAll()
            // 32 significant digits hides tiny binary/rounding residue (√2² = 2)
            EvalResult.Success(checkRange(v.round(MathContext(32, RoundingMode.HALF_EVEN)).stripZeros()))
        } catch (e: CalcException) {
            EvalResult.Failure(e.error)
        } catch (e: ArithmeticException) {
            EvalResult.Failure(CalcError.Overflow)
        } catch (e: NumberFormatException) {
            EvalResult.Failure(CalcError.Syntax("Invalid number"))
        }
    }

    internal fun checkRange(v: BigDecimal): BigDecimal {
        val a = v.abs()
        if (a >= MAX) throw CalcException(CalcError.Overflow)
        if (a.signum() != 0 && a < MIN) return BigDecimal.ZERO // underflow
        return v
    }

    internal fun fromDouble(d: Double): BigDecimal {
        if (d.isNaN()) throw CalcException(CalcError.Domain("Result is undefined"))
        if (d.isInfinite()) throw CalcException(CalcError.Overflow)
        return BigDecimal(d).round(MathContext(15, RoundingMode.HALF_EVEN)).stripZeros()
    }

    internal fun BigDecimal.stripZeros(): BigDecimal =
        if (signum() == 0) BigDecimal.ZERO else stripTrailingZeros()

    /**
     * For "repeated equals": the last top-level operator and its plain-number
     * right operand, e.g. "5+3" -> ('+', 3). Null when not applicable.
     */
    fun lastOperation(expression: String): Pair<Char, BigDecimal>? {
        val tokens = try { Tokenizer.tokenize(expression) } catch (e: CalcException) { return null }
        if (tokens.size < 3) return null
        val last = tokens.last() as? Token.Num ?: return null
        val op = tokens[tokens.size - 2] as? Token.Op ?: return null
        if (op.c == '^') return null
        // the operator must be binary (preceded by a value)
        when (tokens[tokens.size - 3]) {
            is Token.Num, is Token.Const, Token.RParen, Token.Percent, Token.Factorial -> {}
            else -> return null
        }
        var depth = 0
        for (t in tokens) {
            if (t == Token.LParen) depth++
            if (t == Token.RParen) depth--
        }
        if (depth != 0) return null
        return op.c to last.value
    }

    /** Applies a remembered operation to a value (repeated equals). */
    fun apply(value: BigDecimal, op: Char, operand: BigDecimal): EvalResult = try {
        EvalResult.Success(checkRange(binary(value, op, operand)))
    } catch (e: CalcException) {
        EvalResult.Failure(e.error)
    }

    internal fun binary(a: BigDecimal, op: Char, b: BigDecimal): BigDecimal = when (op) {
        '+' -> a.add(b, MC)
        '−' -> a.subtract(b, MC)
        '×' -> a.multiply(b, MC)
        '÷' -> {
            if (b.signum() == 0) throw CalcException(CalcError.DivisionByZero)
            a.divide(b, MC)
        }
        else -> throw CalcException(CalcError.Syntax("Unknown operator $op"))
    }

    // ---------------------------------------------------------------- powers

    internal fun power(a: BigDecimal, b: BigDecimal): BigDecimal {
        val bInt = b.toIntegerOrNull()
        if (bInt != null && bInt.abs() <= BigInteger.valueOf(99_999)) {
            val n = bInt.toInt()
            if (a.signum() == 0) {
                if (n < 0) throw CalcException(CalcError.DivisionByZero)
                return if (n == 0) BigDecimal.ONE else BigDecimal.ZERO
            }
            // magnitude guard before computing
            val digits = a.abs().precision() - a.abs().scale()
            if (digits.toLong() * kotlin.math.abs(n) > 10_050) {
                if (n > 0 && a.abs() > BigDecimal.ONE) throw CalcException(CalcError.Overflow)
                if (n < 0 && a.abs() > BigDecimal.ONE) return BigDecimal.ZERO
            }
            val p = a.pow(kotlin.math.abs(n), MC)
            return if (n < 0) BigDecimal.ONE.divide(p, MC) else p
        }
        if (a.signum() < 0) {
            // allow odd roots of negatives: (-8)^(1/3) = -2
            val inv = BigDecimal.ONE.divide(b, MathContext(20)).round(MathContext(12))
            val invInt = inv.toIntegerOrNull()
            if (invInt != null && invInt.testBit(0)) {
                return fromDouble(-Math.pow(-a.toDouble(), b.toDouble())).let { cleanRoot(it) }
            }
            throw CalcException(CalcError.Domain("Negative number to a fractional power"))
        }
        if (a.signum() == 0) {
            if (b.signum() < 0) throw CalcException(CalcError.DivisionByZero)
            return BigDecimal.ZERO
        }
        if (b.compareTo(BigDecimal("0.5")) == 0) return cleanRoot(a.sqrt(MC)) // exact square root
        return cleanRoot(fromDouble(Math.pow(a.toDouble(), b.toDouble())))
    }

    /** Snaps values like 2.9999999999999996 to 3 when they are that close. */
    internal fun cleanRoot(v: BigDecimal): BigDecimal {
        val r = v.setScale(0, RoundingMode.HALF_EVEN)
        return if (v.subtract(r).abs() < BigDecimal("1E-12")) r else v.stripZeros()
    }

    internal fun BigDecimal.toIntegerOrNull(): BigInteger? = try {
        stripZeros().toBigIntegerExact()
    } catch (e: ArithmeticException) {
        null
    }

    internal fun requireInteger(v: BigDecimal, what: String): BigInteger =
        v.toIntegerOrNull() ?: throw CalcException(CalcError.Domain("$what needs a whole number"))

    internal fun factorial(v: BigDecimal): BigDecimal {
        val n = requireInteger(v, "Factorial")
        if (n.signum() < 0) throw CalcException(CalcError.Domain("Factorial needs a number ≥ 0"))
        if (n > BigInteger.valueOf(3000)) throw CalcException(CalcError.Overflow)
        var r = BigInteger.ONE
        for (i in 2..n.toInt()) r = r.multiply(BigInteger.valueOf(i.toLong()))
        return BigDecimal(r)
    }

    internal fun permutations(nv: BigDecimal, rv: BigDecimal): BigDecimal {
        val n = requireInteger(nv, "nPr"); val r = requireInteger(rv, "nPr")
        if (n.signum() < 0 || r.signum() < 0 || r > n) throw CalcException(CalcError.Domain("nPr needs n ≥ r ≥ 0"))
        if (n > BigInteger.valueOf(3000)) throw CalcException(CalcError.Overflow)
        var res = BigInteger.ONE
        var i = n.subtract(r).add(BigInteger.ONE)
        while (i <= n) { res = res.multiply(i); i = i.add(BigInteger.ONE) }
        return BigDecimal(res)
    }

    internal fun combinations(nv: BigDecimal, rv: BigDecimal): BigDecimal {
        val n = requireInteger(nv, "nCr"); val r0 = requireInteger(rv, "nCr")
        if (n.signum() < 0 || r0.signum() < 0 || r0 > n) throw CalcException(CalcError.Domain("nCr needs n ≥ r ≥ 0"))
        if (n > BigInteger.valueOf(3000)) throw CalcException(CalcError.Overflow)
        val r = r0.min(n.subtract(r0)).toInt()
        var res = BigInteger.ONE
        for (k in 1..r) {
            res = res.multiply(n.subtract(BigInteger.valueOf((r - k).toLong()))).divide(BigInteger.valueOf(k.toLong()))
        }
        return BigDecimal(res)
    }
}
