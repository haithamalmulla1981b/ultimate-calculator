package com.hmalmulla.calculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/** A result with optional human-readable working steps. */
data class Worked<T>(val value: T, val steps: List<String> = emptyList())

/** Exact fraction a/b in lowest terms, b > 0. */
data class Fraction(val num: BigInteger, val den: BigInteger) {
    init {
        require(den.signum() != 0) { "Denominator can't be zero" }
    }

    fun simplified(): Fraction {
        val g = num.gcd(den).let { if (it.signum() == 0) BigInteger.ONE else it }
        val sign = if (den.signum() < 0) BigInteger.ONE.negate() else BigInteger.ONE
        return Fraction(num.divide(g).multiply(sign), den.divide(g).multiply(sign))
    }

    operator fun plus(o: Fraction) = Fraction(num * o.den + o.num * den, den * o.den).simplified()
    operator fun minus(o: Fraction) = Fraction(num * o.den - o.num * den, den * o.den).simplified()
    operator fun times(o: Fraction) = Fraction(num * o.num, den * o.den).simplified()
    operator fun div(o: Fraction): Fraction {
        if (o.num.signum() == 0) throw ArithmeticException("Can't divide by zero")
        return Fraction(num * o.den, den * o.num).simplified()
    }

    fun toDecimal(scale: Int = 20): BigDecimal =
        BigDecimal(num).divide(BigDecimal(den), scale, RoundingMode.HALF_UP).stripTrailingZeros()

    /** "1 3/4" style; "−1 3/4" for negatives; whole numbers stay whole. */
    fun toMixedString(): String {
        val f = simplified()
        val neg = f.num.signum() < 0
        val n = f.num.abs()
        val whole = n.divide(f.den)
        val rest = n.mod(f.den)
        val sign = if (neg) "−" else ""
        return when {
            rest.signum() == 0 -> "$sign$whole"
            whole.signum() == 0 -> "$sign$rest/${f.den}"
            else -> "$sign$whole $rest/${f.den}"
        }
    }

    override fun toString(): String {
        val f = simplified()
        return if (f.den == BigInteger.ONE) f.num.toString() else "${f.num}/${f.den}"
    }

    companion object {
        fun of(n: Long, d: Long) = Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d)).simplified()

        /** Parses "3/4", "1 3/4", "-2", "0.75". */
        fun parse(text: String): Fraction {
            val t = text.trim().replace('−', '-')
            require(t.isNotEmpty()) { "Enter a fraction" }
            val neg = t.startsWith("-")
            val body = t.removePrefix("-").trim()
            val f = when {
                body.contains(' ') && body.contains('/') -> {
                    val (w, fr) = body.split(Regex("\\s+"), limit = 2)
                    Fraction(BigInteger(w), BigInteger.ONE) + parse(fr)
                }
                body.contains('/') -> {
                    val (a, b) = body.split('/', limit = 2).map { it.trim() }
                    require(BigInteger(b).signum() != 0) { "Denominator can't be zero" }
                    Fraction(BigInteger(a), BigInteger(b)).simplified()
                }
                else -> fromDecimal(BigDecimal(body))
            }
            return if (neg) Fraction(f.num.negate(), f.den) else f
        }

        /** Exact conversion of a terminating decimal: 0.75 -> 3/4. */
        fun fromDecimal(d: BigDecimal): Fraction {
            val s = d.stripTrailingZeros()
            return if (s.scale() <= 0) Fraction(s.toBigIntegerExact(), BigInteger.ONE)
            else Fraction(s.unscaledValue(), BigInteger.TEN.pow(s.scale())).simplified()
        }

        /**
         * Best fraction for a decimal with denominator ≤ maxDen (continued
         * fractions), so 0.3333333333 -> 1/3.
         */
        fun approximate(d: BigDecimal, maxDen: Long = 1_000_000): Fraction {
            val exact = fromDecimal(d)
            if (exact.den <= BigInteger.valueOf(maxDen)) return exact
            var h0 = BigInteger.ZERO; var h1 = BigInteger.ONE
            var k0 = BigInteger.ONE; var k1 = BigInteger.ZERO
            var x = d
            repeat(64) {
                val a = x.setScale(0, RoundingMode.FLOOR).toBigInteger()
                val h2 = a * h1 + h0; val k2 = a * k1 + k0
                if (k2 > BigInteger.valueOf(maxDen)) return Fraction(h1, k1).simplified()
                h0 = h1; h1 = h2; k0 = k1; k1 = k2
                val frac = x.subtract(BigDecimal(a))
                if (frac.abs() < BigDecimal("1E-30")) return Fraction(h1, k1).simplified()
                x = BigDecimal.ONE.divide(frac, MathContext(50))
            }
            return Fraction(h1, k1).simplified()
        }
    }
}

object AdvancedMath {
    private val MC = MathContext(34, RoundingMode.HALF_EVEN)
    private val HUNDRED = BigDecimal(100)

    fun gcd(values: List<BigInteger>): BigInteger = values.reduce { a, b -> a.gcd(b) }

    fun lcm(values: List<BigInteger>): BigInteger = values.reduce { a, b ->
        if (a.signum() == 0 || b.signum() == 0) BigInteger.ZERO else a.divide(a.gcd(b)).multiply(b)
    }

    // ---------------------------------------------------------------- primes

    fun isPrime(n: Long): Worked<Boolean> {
        if (n < 2) return Worked(false, listOf("$n is less than 2, so it is not prime"))
        if (n < 4) return Worked(true, listOf("$n is prime"))
        if (n % 2 == 0L) return Worked(false, listOf("$n is divisible by 2"))
        var i = 3L
        while (i * i <= n) {
            if (n % i == 0L) return Worked(false, listOf("$n = $i × ${n / i}"))
            i += 2
        }
        return Worked(true, listOf("No divisor up to √$n, so $n is prime"))
    }

    fun primeFactors(n0: Long): Worked<List<Long>> {
        require(n0 >= 2) { "Enter a whole number ≥ 2" }
        var n = n0
        val f = ArrayList<Long>()
        val steps = ArrayList<String>()
        var p = 2L
        while (p * p <= n) {
            while (n % p == 0L) {
                steps.add("$n ÷ $p = ${n / p}")
                f.add(p); n /= p
            }
            p += if (p == 2L) 1 else 2
        }
        if (n > 1) f.add(n)
        val grouped = f.groupingBy { it }.eachCount().entries.joinToString(" × ") { (k, c) -> if (c > 1) "$k^$c" else "$k" }
        steps.add("$n0 = $grouped")
        return Worked(f, steps)
    }

    // ---------------------------------------------------------------- percentages

    fun percentChange(from: BigDecimal, to: BigDecimal): BigDecimal {
        require(from.signum() != 0) { "The starting value can't be zero" }
        return to.subtract(from).divide(from.abs(), MC).multiply(HUNDRED)
    }

    fun percentDifference(a: BigDecimal, b: BigDecimal): BigDecimal {
        val avg = a.add(b).divide(BigDecimal(2), MC)
        require(avg.signum() != 0) { "The average of the values is zero" }
        return a.subtract(b).abs().divide(avg.abs(), MC).multiply(HUNDRED)
    }

    fun percentOf(percent: BigDecimal, of: BigDecimal): BigDecimal = of.multiply(percent).divide(HUNDRED, MC)

    fun applyPercent(value: BigDecimal, percent: BigDecimal, increase: Boolean): BigDecimal {
        val delta = percentOf(percent, value)
        return if (increase) value.add(delta) else value.subtract(delta)
    }

    /** Solves a/b = c/x for x. */
    fun proportion(a: BigDecimal, b: BigDecimal, c: BigDecimal): Worked<BigDecimal> {
        require(a.signum() != 0) { "The first value can't be zero" }
        val x = b.multiply(c).divide(a, MC)
        return Worked(x, listOf("x = ($b × $c) ÷ $a", "x = ${x.stripTrailingZeros().toPlainString()}"))
    }

    fun simplifyRatio(values: List<BigInteger>): List<BigInteger> {
        val g = gcd(values.map { it.abs() }).let { if (it.signum() == 0) BigInteger.ONE else it }
        return values.map { it.divide(g) }
    }

    // ---------------------------------------------------------------- statistics

    data class Stats(
        val count: Int, val sum: BigDecimal, val mean: BigDecimal, val median: BigDecimal,
        val modes: List<BigDecimal>, val populationVariance: BigDecimal, val sampleVariance: BigDecimal?,
        val populationSd: BigDecimal, val sampleSd: BigDecimal?, val min: BigDecimal, val max: BigDecimal,
    )

    fun parseList(text: String): List<BigDecimal> =
        text.replace('−', '-').split(Regex("[,;\\s]+")).filter { it.isNotBlank() }.map { BigDecimal(it) }

    fun stats(values: List<BigDecimal>): Stats {
        require(values.isNotEmpty()) { "Enter at least one number" }
        val n = values.size
        val sum = values.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val mean = sum.divide(BigDecimal(n), MC)
        val sorted = values.sorted()
        val median = if (n % 2 == 1) sorted[n / 2]
        else sorted[n / 2 - 1].add(sorted[n / 2]).divide(BigDecimal(2), MC)
        val counts = values.groupingBy { it.stripTrailingZeros() }.eachCount()
        val top = counts.values.max()
        val modes = if (top == 1) emptyList() else counts.filter { it.value == top }.keys.sorted()
        val ss = values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v.subtract(mean).pow(2, MC)) }
        val pv = ss.divide(BigDecimal(n), MC)
        val sv = if (n > 1) ss.divide(BigDecimal(n - 1), MC) else null
        return Stats(
            n, sum, mean, median, modes, pv, sv, pv.sqrt(MC), sv?.sqrt(MC), sorted.first(), sorted.last(),
        )
    }

    // ---------------------------------------------------------------- progressions

    /** n-th term and sum of the first n terms of an arithmetic progression. */
    fun arithmetic(first: BigDecimal, diff: BigDecimal, n: Int): Worked<Pair<BigDecimal, BigDecimal>> {
        require(n >= 1) { "n must be at least 1" }
        val nb = BigDecimal(n)
        val nth = first.add(diff.multiply(nb.subtract(BigDecimal.ONE)))
        val sum = nb.multiply(first.add(nth)).divide(BigDecimal(2), MC)
        return Worked(nth to sum, listOf(
            "aₙ = a₁ + (n − 1)d = ${plain(first)} + (${n} − 1) × ${plain(diff)} = ${plain(nth)}",
            "Sₙ = n(a₁ + aₙ) / 2 = $n × (${plain(first)} + ${plain(nth)}) / 2 = ${plain(sum)}",
        ))
    }

    fun geometric(first: BigDecimal, ratio: BigDecimal, n: Int): Worked<Pair<BigDecimal, BigDecimal>> {
        require(n in 1..10_000) { "n must be between 1 and 10000" }
        val nth = first.multiply(ratio.pow(n - 1, MC), MC)
        val sum = if (ratio.compareTo(BigDecimal.ONE) == 0) first.multiply(BigDecimal(n))
        else first.multiply(BigDecimal.ONE.subtract(ratio.pow(n, MC)), MC).divide(BigDecimal.ONE.subtract(ratio), MC)
        return Worked(nth to sum, listOf(
            "aₙ = a₁ × r^(n − 1) = ${plain(first)} × ${plain(ratio)}^${n - 1} = ${plain(nth)}",
            if (ratio.compareTo(BigDecimal.ONE) == 0) "r = 1, so Sₙ = n × a₁ = ${plain(sum)}"
            else "Sₙ = a₁(1 − rⁿ) / (1 − r) = ${plain(sum)}",
        ))
    }

    // ---------------------------------------------------------------- equations

    /** Solves a·x + b = c. */
    fun linear(a: BigDecimal, b: BigDecimal, c: BigDecimal): Worked<BigDecimal> {
        require(a.signum() != 0) { if (b.compareTo(c) == 0) "Every x is a solution" else "No solution (a = 0)" }
        val x = c.subtract(b).divide(a, MC)
        return Worked(x, listOf(
            "${plain(a)}x + ${plain(b)} = ${plain(c)}",
            "${plain(a)}x = ${plain(c)} − ${plain(b)} = ${plain(c.subtract(b))}",
            "x = ${plain(c.subtract(b))} ÷ ${plain(a)} = ${plain(x)}",
        ))
    }

    /** Solves a₁x + b₁y = c₁, a₂x + b₂y = c₂ by Cramer's rule. */
    fun linear2(
        a1: BigDecimal, b1: BigDecimal, c1: BigDecimal,
        a2: BigDecimal, b2: BigDecimal, c2: BigDecimal,
    ): Worked<Pair<BigDecimal, BigDecimal>> {
        val d = a1.multiply(b2).subtract(a2.multiply(b1))
        require(d.signum() != 0) { "The equations have no single solution (determinant is 0)" }
        val dx = c1.multiply(b2).subtract(c2.multiply(b1))
        val dy = a1.multiply(c2).subtract(a2.multiply(c1))
        val x = dx.divide(d, MC); val y = dy.divide(d, MC)
        return Worked(x to y, listOf(
            "D = a₁b₂ − a₂b₁ = ${plain(d)}",
            "Dx = c₁b₂ − c₂b₁ = ${plain(dx)},  Dy = a₁c₂ − a₂c₁ = ${plain(dy)}",
            "x = Dx / D = ${plain(x)},  y = Dy / D = ${plain(y)}",
        ))
    }

    data class Quadratic(val discriminant: BigDecimal, val roots: List<String>)

    /** Solves ax² + bx + c = 0, including complex roots. */
    fun quadratic(a: BigDecimal, b: BigDecimal, c: BigDecimal): Worked<Quadratic> {
        require(a.signum() != 0) { "a can't be 0 (that is a linear equation)" }
        val disc = b.multiply(b).subtract(BigDecimal(4).multiply(a).multiply(c))
        val twoA = BigDecimal(2).multiply(a)
        val steps = mutableListOf("Δ = b² − 4ac = ${plain(b)}² − 4 × ${plain(a)} × ${plain(c)} = ${plain(disc)}")
        val roots = when (disc.signum()) {
            1 -> {
                val s = disc.sqrt(MC)
                val x1 = b.negate().add(s).divide(twoA, MC)
                val x2 = b.negate().subtract(s).divide(twoA, MC)
                steps.add("x = (−b ± √Δ) / 2a")
                listOf(plain(x1), plain(x2))
            }
            0 -> {
                steps.add("Δ = 0, one repeated root: x = −b / 2a")
                listOf(plain(b.negate().divide(twoA, MC)))
            }
            else -> {
                val re = b.negate().divide(twoA, MC)
                val im = disc.negate().sqrt(MC).divide(twoA.abs(), MC)
                steps.add("Δ < 0, complex roots: x = −b/2a ± i√(−Δ)/2a")
                listOf("${plain(re)} + ${plain(im)}i", "${plain(re)} − ${plain(im)}i")
            }
        }
        return Worked(Quadratic(disc, roots), steps)
    }

    fun plain(v: BigDecimal): String = NumberFormatter.format(v, maxDecimals = 10, grouping = false)

    /** Scientific notation, e.g. 123400 -> "1.234 × 10^5". */
    fun toScientific(v: BigDecimal, digits: Int = 10): String {
        if (v.signum() == 0) return "0"
        val r = v.round(MathContext(digits, RoundingMode.HALF_UP))
        val exp = r.precision() - r.scale() - 1
        val m = r.movePointLeft(exp).stripTrailingZeros().toPlainString()
        return "$m × 10^$exp"
    }

    /** Parses "1.234e5", "1.234E5", "1.234 × 10^5" to a plain number. */
    fun fromScientific(text: String): BigDecimal {
        val t = text.replace(" ", "").replace('−', '-').replace("×10^", "E").replace("x10^", "E").replace('e', 'E')
        return BigDecimal(t)
    }
}
