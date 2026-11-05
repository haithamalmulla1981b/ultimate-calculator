package com.hmalmulla.calculator

import com.hmalmulla.calculator.engine.AdvancedMath
import com.hmalmulla.calculator.engine.Finance
import com.hmalmulla.calculator.engine.Fraction
import com.hmalmulla.calculator.engine.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinanceTest {
    // t20: known textbook values
    @Test fun loan() {
        val l = Finance.loan(BigDecimal(100000), BigDecimal(6), 360)
        assertEquals(BigDecimal("599.55"), l.monthly)
        assertEquals(0, l.schedule.last().balance.signum())
        assertTrue(l.totalInterest.subtract(BigDecimal("115838.19")).abs() < BigDecimal("1.00"))
    }
    @Test fun zeroRateLoan() = assertEquals(BigDecimal("100.00"), Finance.loan(BigDecimal(1200), BigDecimal.ZERO, 12).monthly)
    @Test fun compound() = assertEquals(BigDecimal("1647.01"), Finance.compound(BigDecimal(1000), BigDecimal(5), 12, 10))
    @Test fun simple() = assertEquals(BigDecimal("100.00"), Finance.simpleInterest(BigDecimal(1000), BigDecimal(5), BigDecimal(2)))
    @Test fun vat() {
        val t = Finance.tax(BigDecimal(110), BigDecimal(10), true)
        assertEquals(BigDecimal("100.00"), t.net); assertEquals(BigDecimal("10.00"), t.tax)
    }
    @Test fun discount() = assertEquals(BigDecimal("60.00"), Finance.discount(BigDecimal(80), BigDecimal(25)).finalPrice)
    @Test fun margin() {
        assertEquals(BigDecimal("100.00"), Finance.fromMargin(BigDecimal(60), BigDecimal(40)).price)
        assertEquals(BigDecimal("20.00"), Finance.fromMarkup(BigDecimal(100), BigDecimal(25)).marginPercent)
    }
    @Test fun tip() = assertEquals(BigDecimal("28.75"), Finance.tipAndSplit(BigDecimal(100), BigDecimal(15), 4).perPerson)
    @Test fun salary() = assertEquals(BigDecimal("41600.00"), Finance.salaryFromHourly(BigDecimal(20), BigDecimal(40)).yearly)
}

class AdvancedMathTest {
    @Test fun fractions() {
        assertEquals("7/4", Fraction.parse("1 3/4").toString())
        assertEquals("1 3/4", Fraction.parse("7/4").toMixedString())
        assertEquals("5/6", (Fraction.of(1, 2) + Fraction.of(1, 3)).toString())
        assertEquals("1/3", Fraction.approximate(BigDecimal("0.3333333333")).toString())
        assertEquals("3/4", Fraction.fromDecimal(BigDecimal("0.75")).toString())
    }
    @Test fun primes() {
        assertTrue(AdvancedMath.isPrime(97).value)
        assertEquals(listOf(2L, 2, 2, 3, 3, 5), AdvancedMath.primeFactors(360).value)
    }
    @Test fun stats() {
        val s = AdvancedMath.stats(AdvancedMath.parseList("2 4 4 4 5 5 7 9"))
        assertEquals(0, s.mean.compareTo(BigDecimal(5)))
        assertEquals(0, s.median.compareTo(BigDecimal("4.5")))
        assertEquals(0, s.populationSd.compareTo(BigDecimal(2)))
    }
    @Test fun equations() {
        assertEquals(listOf("2", "1"), AdvancedMath.quadratic(BigDecimal(1), BigDecimal(-3), BigDecimal(2)).value.roots)
        assertEquals(listOf("0 + 1i", "0 − 1i"), AdvancedMath.quadratic(BigDecimal(1), BigDecimal(0), BigDecimal(1)).value.roots)
        val s = AdvancedMath.linear2(BigDecimal(2), BigDecimal(1), BigDecimal(5), BigDecimal(1), BigDecimal(-1), BigDecimal(1)).value
        assertEquals(0, s.first.compareTo(BigDecimal(2))); assertEquals(0, s.second.compareTo(BigDecimal(1)))
    }
    @Test fun progressions() {
        assertEquals(0, AdvancedMath.arithmetic(BigDecimal(1), BigDecimal(1), 100).value.second.compareTo(BigDecimal(5050)))
        assertEquals(0, AdvancedMath.geometric(BigDecimal(1), BigDecimal(2), 10).value.second.compareTo(BigDecimal(1023)))
    }
}

class UnitsTest {
    private fun cv(cat: String, v: String, from: String, to: String): BigDecimal {
        val c = Units.categories.first { it.id == cat }
        return Units.convert(BigDecimal(v), c.units.first { it.symbol == from }, c.units.first { it.symbol == to })
    }
    private fun eq(expected: String, actual: BigDecimal) = assertEquals(0, actual.compareTo(BigDecimal(expected)))

    @Test fun conversions() {
        eq("2.54", cv("length", "1", "in", "cm")); eq("212", cv("temperature", "100", "°C", "°F"))
        eq("0", cv("temperature", "32", "°F", "°C")); eq("1000", cv("data", "1", "GB", "MB"))
        eq("1024", cv("data", "1", "GiB", "MiB")); eq("1", cv("transfer", "8", "Mbps", "MB/s"))
        eq("10", cv("speed", "36", "km/h", "m/s")); eq("20", cv("fuel", "5", "L/100km", "km/L"))
        eq("3", cv("cooking", "1", "tbsp", "tsp"))
    }
}
