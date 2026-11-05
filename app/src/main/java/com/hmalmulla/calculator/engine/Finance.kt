package com.hmalmulla.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Financial calculations in BigDecimal (34 significant digits). Money values
 * are rounded to 2 decimals only when displayed or when a schedule row is
 * produced, so totals do not drift.
 */
object Finance {
    private val MC = MathContext(34, RoundingMode.HALF_EVEN)
    private val HUNDRED = BigDecimal(100)
    private val TWELVE = BigDecimal(12)

    fun money(v: BigDecimal): BigDecimal = v.setScale(2, RoundingMode.HALF_UP)

    /** Monthly payment for an amortising loan. Zero interest is handled. */
    fun monthlyPayment(principal: BigDecimal, annualRatePercent: BigDecimal, months: Int): BigDecimal {
        require(principal.signum() > 0) { "Enter a loan amount above 0" }
        require(months > 0) { "Enter a duration of at least 1 month" }
        require(annualRatePercent.signum() >= 0) { "Interest rate can't be negative" }
        if (annualRatePercent.signum() == 0) return principal.divide(BigDecimal(months), MC)
        val r = annualRatePercent.divide(HUNDRED, MC).divide(TWELVE, MC)
        val f = BigDecimal.ONE.add(r).pow(months, MC)
        return principal.multiply(r, MC).multiply(f, MC).divide(f.subtract(BigDecimal.ONE), MC)
    }

    data class AmortRow(
        val month: Int, val payment: BigDecimal, val principal: BigDecimal,
        val interest: BigDecimal, val balance: BigDecimal,
    )

    data class Loan(
        val monthly: BigDecimal, val totalPaid: BigDecimal, val totalInterest: BigDecimal,
        val schedule: List<AmortRow>,
    )

    fun loan(principal: BigDecimal, annualRatePercent: BigDecimal, months: Int): Loan {
        val pay = money(monthlyPayment(principal, annualRatePercent, months))
        val r = annualRatePercent.divide(HUNDRED, MC).divide(TWELVE, MC)
        var balance = principal
        val rows = ArrayList<AmortRow>(months)
        var totalPaid = BigDecimal.ZERO
        for (m in 1..months) {
            val interest = money(balance.multiply(r, MC))
            var p = pay.subtract(interest)
            var thisPay = pay
            if (m == months || p > balance) { // last payment clears the balance exactly
                p = balance
                thisPay = balance.add(interest)
            }
            balance = balance.subtract(p)
            totalPaid = totalPaid.add(thisPay)
            rows.add(AmortRow(m, money(thisPay), money(p), interest, money(balance)))
        }
        return Loan(pay, money(totalPaid), money(totalPaid.subtract(principal)), rows)
    }

    data class Mortgage(val loanAmount: BigDecimal, val monthly: BigDecimal, val totalInterest: BigDecimal, val totalCost: BigDecimal)

    fun mortgage(price: BigDecimal, downPayment: BigDecimal, annualRatePercent: BigDecimal, years: Int): Mortgage {
        require(downPayment.signum() >= 0 && downPayment < price) { "Down payment must be less than the price" }
        val amount = price.subtract(downPayment)
        val l = loan(amount, annualRatePercent, years * 12)
        return Mortgage(money(amount), l.monthly, l.totalInterest, money(downPayment.add(l.totalPaid)))
    }

    data class Savings(val finalBalance: BigDecimal, val totalDeposits: BigDecimal, val interestEarned: BigDecimal)

    /** Monthly compounding with a deposit at the end of each month. */
    fun savings(initial: BigDecimal, monthly: BigDecimal, annualRatePercent: BigDecimal, years: Int): Savings {
        require(years in 1..100) { "Enter 1 to 100 years" }
        val r = annualRatePercent.divide(HUNDRED, MC).divide(TWELVE, MC)
        var bal = initial
        for (m in 1..years * 12) bal = bal.add(bal.multiply(r, MC)).add(monthly)
        val deposits = initial.add(monthly.multiply(BigDecimal(years * 12)))
        return Savings(money(bal), money(deposits), money(bal.subtract(deposits)))
    }

    /** A = P(1 + r/n)^(n·t). */
    fun compound(principal: BigDecimal, annualRatePercent: BigDecimal, timesPerYear: Int, years: Int): BigDecimal {
        require(timesPerYear > 0 && years >= 0) { "Check compounding and years" }
        val r = annualRatePercent.divide(HUNDRED, MC).divide(BigDecimal(timesPerYear), MC)
        return money(principal.multiply(BigDecimal.ONE.add(r).pow(timesPerYear * years, MC), MC))
    }

    /** I = P·r·t. */
    fun simpleInterest(principal: BigDecimal, annualRatePercent: BigDecimal, years: BigDecimal): BigDecimal =
        money(principal.multiply(annualRatePercent, MC).multiply(years, MC).divide(HUNDRED, MC))

    data class Tax(val net: BigDecimal, val tax: BigDecimal, val gross: BigDecimal)

    /** Adds tax to a net price, or extracts it from a gross price. */
    fun tax(amount: BigDecimal, ratePercent: BigDecimal, amountIncludesTax: Boolean): Tax {
        val rate = ratePercent.divide(HUNDRED, MC)
        return if (amountIncludesTax) {
            val net = amount.divide(BigDecimal.ONE.add(rate), MC)
            Tax(money(net), money(amount.subtract(net)), money(amount))
        } else {
            val t = amount.multiply(rate, MC)
            Tax(money(amount), money(t), money(amount.add(t)))
        }
    }

    data class Discount(val saved: BigDecimal, val finalPrice: BigDecimal)

    fun discount(price: BigDecimal, percent: BigDecimal): Discount {
        val saved = price.multiply(percent, MC).divide(HUNDRED, MC)
        return Discount(money(saved), money(price.subtract(saved)))
    }

    data class ProfitLoss(val amount: BigDecimal, val percentOfCost: BigDecimal, val isProfit: Boolean)

    fun profitLoss(cost: BigDecimal, revenue: BigDecimal): ProfitLoss {
        require(cost.signum() > 0) { "Cost must be above 0" }
        val diff = revenue.subtract(cost)
        return ProfitLoss(money(diff.abs()), diff.divide(cost, MC).multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP).abs(), diff.signum() >= 0)
    }

    data class Margin(val price: BigDecimal, val profit: BigDecimal, val markupPercent: BigDecimal, val marginPercent: BigDecimal)

    fun fromMarkup(cost: BigDecimal, markupPercent: BigDecimal): Margin {
        val price = cost.multiply(BigDecimal.ONE.add(markupPercent.divide(HUNDRED, MC)), MC)
        return margin(cost, price)
    }

    fun fromMargin(cost: BigDecimal, marginPercent: BigDecimal): Margin {
        require(marginPercent < HUNDRED) { "Margin must be below 100%" }
        val price = cost.divide(BigDecimal.ONE.subtract(marginPercent.divide(HUNDRED, MC)), MC)
        return margin(cost, price)
    }

    private fun margin(cost: BigDecimal, price: BigDecimal): Margin {
        val profit = price.subtract(cost)
        val markup = if (cost.signum() == 0) BigDecimal.ZERO else profit.divide(cost, MC).multiply(HUNDRED)
        val margin = if (price.signum() == 0) BigDecimal.ZERO else profit.divide(price, MC).multiply(HUNDRED)
        return Margin(money(price), money(profit), markup.setScale(2, RoundingMode.HALF_UP), margin.setScale(2, RoundingMode.HALF_UP))
    }

    data class Bill(val tip: BigDecimal, val total: BigDecimal, val perPerson: BigDecimal)

    fun tipAndSplit(bill: BigDecimal, tipPercent: BigDecimal, people: Int): Bill {
        require(people >= 1) { "At least 1 person" }
        val tip = bill.multiply(tipPercent, MC).divide(HUNDRED, MC)
        val total = bill.add(tip)
        return Bill(money(tip), money(total), money(total.divide(BigDecimal(people), MC)))
    }

    data class Salary(val hourly: BigDecimal, val weekly: BigDecimal, val monthly: BigDecimal, val yearly: BigDecimal)

    /** From an hourly rate and hours per week (52 weeks a year). */
    fun salaryFromHourly(hourly: BigDecimal, hoursPerWeek: BigDecimal): Salary {
        val weekly = hourly.multiply(hoursPerWeek, MC)
        val yearly = weekly.multiply(BigDecimal(52), MC)
        return Salary(money(hourly), money(weekly), money(yearly.divide(TWELVE, MC)), money(yearly))
    }

    fun salaryFromMonthly(monthly: BigDecimal, hoursPerWeek: BigDecimal): Salary {
        require(hoursPerWeek.signum() > 0) { "Hours per week must be above 0" }
        val yearly = monthly.multiply(TWELVE, MC)
        val weekly = yearly.divide(BigDecimal(52), MC)
        return Salary(money(weekly.divide(hoursPerWeek, MC)), money(weekly), money(monthly), money(yearly))
    }

    data class Investment(val gain: BigDecimal, val roiPercent: BigDecimal, val annualisedPercent: BigDecimal?)

    fun investmentReturn(invested: BigDecimal, finalValue: BigDecimal, years: BigDecimal?): Investment {
        require(invested.signum() > 0) { "Amount invested must be above 0" }
        val gain = finalValue.subtract(invested)
        val roi = gain.divide(invested, MC).multiply(HUNDRED)
        val ann = if (years != null && years.signum() > 0 && finalValue.signum() > 0) {
            val g = Math.pow(finalValue.divide(invested, MC).toDouble(), 1.0 / years.toDouble()) - 1.0
            BigDecimal(g * 100).setScale(2, RoundingMode.HALF_UP)
        } else null
        return Investment(money(gain), roi.setScale(2, RoundingMode.HALF_UP), ann)
    }
}
