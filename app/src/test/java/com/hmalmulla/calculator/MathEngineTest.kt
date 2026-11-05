package com.hmalmulla.calculator

import com.hmalmulla.calculator.engine.AngleMode
import com.hmalmulla.calculator.engine.CalcError
import com.hmalmulla.calculator.engine.EvalResult
import com.hmalmulla.calculator.engine.MathEngine
import com.hmalmulla.calculator.engine.MemoryStore
import com.hmalmulla.calculator.engine.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class MathEngineTest {
    private fun value(e: String, m: AngleMode = AngleMode.DEG): BigDecimal =
        when (val r = MathEngine.evaluate(e, m)) {
            is EvalResult.Success -> r.value
            is EvalResult.Failure -> { fail("$e failed: ${r.error.message}"); BigDecimal.ZERO }
        }

    private fun assertValue(e: String, expected: String, m: AngleMode = AngleMode.DEG) =
        assertEquals("$e", 0, value(e, m).compareTo(BigDecimal(expected)))

    private fun error(e: String): CalcError =
        (MathEngine.evaluate(e) as? EvalResult.Failure)?.error ?: run { fail("$e should fail"); CalcError.Empty }

    // ---- The 20 required cases ----------------------------------------
    @Test fun t01_addition() = assertValue("2+2", "4")
    @Test fun t02_division() = assertValue("100/4", "25")
    @Test fun t03_precedence() = assertValue("5×5+10", "35")
    @Test fun t04_precedence2() = assertValue("10+5×2", "20")
    @Test fun t05_brackets() = assertValue("(10+5)×2", "30")
    @Test fun t06_percentOf() { assertValue("10%×200", "20"); assertValue("200×10%", "20") }
    @Test fun t07_sqrt() = assertValue("√144", "12")
    @Test fun t08_cube() { assertValue("2³", "8"); assertValue("2^3", "8") }
    @Test fun t09_sin90() = assertValue("sin(90)", "1")
    @Test fun t10_log() = assertValue("log(100)", "2")
    @Test fun t11_factorial() = assertValue("5!", "120")
    @Test fun t12_oneThird() {
        assertEquals("0.3333333333", NumberFormatter.format(value("1/3"), 10))
        assertEquals("0.333", NumberFormatter.format(value("1/3"), 3))
    }
    @Test fun t13_divisionByZero() { assertTrue(error("5/0") is CalcError.DivisionByZero); assertTrue(error("1÷(3−3)") is CalcError.DivisionByZero) }
    @Test fun t14_negatives() { assertValue("-5+3", "-2"); assertValue("−5×−5", "25"); assertValue("-2^2", "-4"); assertValue("(-2)^2", "4") }
    @Test fun t15_veryLarge() {
        assertValue("99999999999×99999999999", "9999999999800000000001")
        assertEquals("1E20", NumberFormatter.format(value("10^20")))
        assertTrue(error("10^100000") is CalcError.Overflow)
    }
    @Test fun t16_verySmall() {
        assertValue("1E-300×1E-300", "1E-600")
        assertEquals("1E-12", NumberFormatter.format(value("1/10^12")))
    }
    @Test fun t17_nestedBrackets() { assertValue("((2+3)×(4−1))^2", "225"); assertValue("2×(3+(4×(5−1)))", "38") }
    @Test fun t18_memory() {
        val m = MemoryStore()
        m.add(BigDecimal(5)); m.add(BigDecimal(3)); m.subtract(BigDecimal(2))
        assertEquals(0, m.recall().compareTo(BigDecimal(6)))
        m.store(BigDecimal(9)); assertEquals(2, m.values.size)
        m.clear(); assertFalse(m.hasValue)
    }
    // t19 (history persistence) is an instrumented Room test in androidTest/HistoryDaoTest.
    // t20 (financial accuracy) is in FinanceTest.

    // ---- More coverage -------------------------------------------------
    @Test fun percentAddSubtract() { assertValue("200+10%", "220"); assertValue("200−10%", "180"); assertValue("50%", "0.5") }
    @Test fun floatingPoint() { assertValue("0.1+0.2", "0.3"); assertValue("sqrt(2)^2", "2") }
    @Test fun trig() {
        assertValue("cos(90)", "0"); assertValue("sin(30)", "0.5"); assertValue("tan(45)", "1")
        assertValue("asin(1)", "90"); assertValue("atan(1)", "45")
        assertValue("sin(π/2)", "1", AngleMode.RAD); assertValue("cos(π)", "-1", AngleMode.RAD); assertValue("sin(π)", "0", AngleMode.RAD)
        assertTrue(error("tan(90)") is CalcError.Domain)
    }
    @Test fun roots() { assertValue("cbrt(27)", "3"); assertValue("∛(-8)", "-2"); assertValue("root(4,16)", "2"); assertValue("8^(1/3)", "2") }
    @Test fun logs() { assertValue("log(1000)", "3"); assertValue("ln(e)", "1"); assertValue("logb(2,8)", "3"); assertTrue(error("log(0)") is CalcError.Domain) }
    @Test fun integerFunctions() {
        assertValue("nPr(5,2)", "20"); assertValue("nCr(52,5)", "2598960"); assertValue("gcd(12,18)", "6"); assertValue("lcm(4,6)", "12")
        assertValue("mod(10,3)", "1"); assertValue("floor(2.7)", "2"); assertValue("ceil(2.1)", "3"); assertValue("round(3.14159,2)", "3.14")
        assertValue("abs(−7)", "7"); assertValue("0!", "1"); assertValue("1000!/999!", "1000")
        assertTrue(error("2.5!") is CalcError.Domain)
    }
    @Test fun implicitMultiplication() { assertValue("2(3+4)", "14"); assertValue("(1+2)(3+4)", "21") }
    @Test fun autoCloseBrackets() = assertValue("(2+3", "5")
    @Test fun syntaxErrors() {
        assertTrue(error("2+") is CalcError.Syntax); assertTrue(error("×5") is CalcError.Syntax)
        assertTrue(error("2)") is CalcError.Syntax); assertTrue(error("abc(2)") is CalcError.Syntax)
        assertTrue(error("√(−4)") is CalcError.Domain)
    }
    @Test fun scientificInput() = assertValue("1.5E3+1", "1501")
    @Test fun grouping() {
        assertEquals("1,234,567.891", NumberFormatter.format(BigDecimal("1234567.891")))
        assertEquals("−1,234.5", NumberFormatter.format(BigDecimal("-1234.5")))
    }
    @Test fun repeatedEquals() {
        assertEquals('+' to BigDecimal(3), MathEngine.lastOperation("5+3"))
        assertNull(MathEngine.lastOperation("2×(3+4)"))
        val r = MathEngine.apply(BigDecimal(8), '+', BigDecimal(3)) as EvalResult.Success
        assertEquals(0, r.value.compareTo(BigDecimal(11)))
    }
}
