package com.hmalmulla.calculator.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Discount
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import com.hmalmulla.calculator.engine.Finance
import java.math.BigDecimal
import java.math.MathContext

private val PCT = "%"

val FinanceTools: List<Tool> = listOf(
    Tool(
        "loan", "Loan", "Monthly payment, total interest and schedule", Icons.Outlined.CreditCard,
        listOf(
            Field("amount", "Loan amount", "10000"),
            Field("rate", "Annual interest rate", "5", suffix = PCT),
            Field("years", "Duration", "5", suffix = "years"),
        ),
    ) { i ->
        val months = (i.num("years").multiply(BigDecimal(12))).setScale(0, java.math.RoundingMode.HALF_UP).toInt()
        require(months in 1..600) { "Duration must be between 1 month and 50 years" }
        val l = Finance.loan(i.num("amount"), i.num("rate"), months)
        ToolOutput(
            rows = listOf(
                "Monthly payment" to money(l.monthly),
                "Total interest" to money(l.totalInterest),
                "Total repayment" to money(l.totalPaid),
                "Number of payments" to months.toString(),
            ),
            table = Table(
                listOf("Month", "Payment", "Principal", "Interest", "Balance"),
                l.schedule.map { listOf(it.month.toString(), money(it.payment), money(it.principal), money(it.interest), money(it.balance)) },
            ),
        )
    },
    Tool(
        "mortgage", "Mortgage", "Payment and total cost of a home loan", Icons.Outlined.Home,
        listOf(
            Field("price", "Property price", "300000"),
            Field("down", "Down payment", "60000"),
            Field("rate", "Annual interest rate", "4.5", suffix = PCT),
            Field("years", "Duration", "25", suffix = "years"),
        ),
    ) { i ->
        val years = i.int("years")
        require(years in 1..50) { "Duration must be 1 to 50 years" }
        val m = Finance.mortgage(i.num("price"), i.num("down"), i.num("rate"), years)
        ToolOutput(listOf(
            "Monthly payment" to money(m.monthly),
            "Loan amount" to money(m.loanAmount),
            "Total interest" to money(m.totalInterest),
            "Total cost (incl. down payment)" to money(m.totalCost),
        ))
    },
    Tool(
        "savings", "Savings", "Growth with monthly contributions", Icons.Outlined.Savings,
        listOf(
            Field("initial", "Initial deposit", "1000"),
            Field("monthly", "Monthly contribution", "200"),
            Field("rate", "Annual interest rate", "4", suffix = PCT),
            Field("years", "Duration", "10", suffix = "years"),
        ),
    ) { i ->
        val s = Finance.savings(i.num("initial"), i.num("monthly"), i.num("rate"), i.int("years"))
        ToolOutput(
            listOf("Final balance" to money(s.finalBalance), "Total deposits" to money(s.totalDeposits), "Interest earned" to money(s.interestEarned)),
            note = "Interest compounds monthly; contributions are added at the end of each month.",
        )
    },
    Tool(
        "compound", "Compound interest", "A = P(1 + r/n)^(nt)", Icons.Outlined.TrendingUp,
        listOf(
            Field("p", "Principal", "1000"),
            Field("rate", "Annual interest rate", "5", suffix = PCT),
            Field("n", "Compounding", "Monthly", FieldKind.CHOICE, options = listOf("Yearly", "Quarterly", "Monthly", "Daily")),
            Field("years", "Years", "10"),
        ),
    ) { i ->
        val n = when (i.choice("n")) { "Yearly" -> 1; "Quarterly" -> 4; "Daily" -> 365; else -> 12 }
        val p = i.num("p")
        val a = Finance.compound(p, i.num("rate"), n, i.int("years"))
        ToolOutput(
            listOf("Final amount" to money(a), "Interest earned" to money(a.subtract(p))),
            steps = listOf("A = ${money(p)} × (1 + ${i.text("rate")}% / $n)^($n × ${i.text("years")})", "A = ${money(a)}"),
        )
    },
    Tool(
        "simple", "Simple interest", "I = P × r × t", Icons.Outlined.Percent,
        listOf(Field("p", "Principal", "1000"), Field("rate", "Annual interest rate", "5", suffix = PCT), Field("years", "Years", "3")),
    ) { i ->
        val interest = Finance.simpleInterest(i.num("p"), i.num("rate"), i.num("years"))
        ToolOutput(
            listOf("Interest" to money(interest), "Total" to money(i.num("p").add(interest))),
            steps = listOf("I = ${i.text("p")} × ${i.text("rate")}% × ${i.text("years")} = ${money(interest)}"),
        )
    },
    Tool(
        "vat", "VAT", "Add or remove value-added tax", Icons.Outlined.ReceiptLong,
        listOf(
            Field("amount", "Amount", "100"),
            Field("rate", "VAT rate", "10", suffix = PCT),
            Field("mode", "The amount is", "Without VAT", FieldKind.CHOICE, options = listOf("Without VAT", "Including VAT")),
        ),
    ) { i ->
        val t = Finance.tax(i.num("amount"), i.num("rate"), i.choice("mode") == "Including VAT")
        ToolOutput(listOf("Gross (incl. VAT)" to money(t.gross), "VAT" to money(t.tax), "Net (excl. VAT)" to money(t.net)))
    },
    Tool(
        "salestax", "Sales tax", "Price after sales tax", Icons.Outlined.Storefront,
        listOf(Field("amount", "Price before tax", "50"), Field("rate", "Sales tax rate", "7.5", suffix = PCT)),
    ) { i ->
        val t = Finance.tax(i.num("amount"), i.num("rate"), false)
        ToolOutput(listOf("Total price" to money(t.gross), "Sales tax" to money(t.tax)))
    },
    Tool(
        "discount", "Discount", "Sale price and savings", Icons.Outlined.Discount,
        listOf(Field("price", "Original price", "80"), Field("pct", "Discount", "25", suffix = PCT)),
    ) { i ->
        val d = Finance.discount(i.num("price"), i.num("pct"))
        ToolOutput(listOf("Final price" to money(d.finalPrice), "You save" to money(d.saved)))
    },
    Tool(
        "profit", "Profit and loss", "Gain or loss on cost", Icons.Outlined.ShowChart,
        listOf(Field("cost", "Cost price", "100"), Field("revenue", "Selling price", "125")),
    ) { i ->
        val p = Finance.profitLoss(i.num("cost"), i.num("revenue"))
        ToolOutput(listOf((if (p.isProfit) "Profit" else "Loss") to money(p.amount), (if (p.isProfit) "Profit %" else "Loss %") to pct(p.percentOfCost)))
    },
    Tool(
        "margin", "Markup and margin", "Price from markup or margin", Icons.Outlined.PieChart,
        listOf(
            Field("cost", "Cost", "60"),
            Field("mode", "Calculate from", "Margin", FieldKind.CHOICE, options = listOf("Margin", "Markup")),
            Field("pct", "Percentage", "40", suffix = PCT),
        ),
    ) { i ->
        val m = if (i.choice("mode") == "Margin") Finance.fromMargin(i.num("cost"), i.num("pct")) else Finance.fromMarkup(i.num("cost"), i.num("pct"))
        ToolOutput(listOf("Selling price" to money(m.price), "Profit" to money(m.profit), "Markup" to pct(m.markupPercent), "Margin" to pct(m.marginPercent)))
    },
    Tool(
        "tip", "Tip and split bill", "Tip, total and share per person", Icons.Outlined.Restaurant,
        listOf(Field("bill", "Bill amount", "100"), Field("tip", "Tip", "15", suffix = PCT), Field("people", "People", "4")),
    ) { i ->
        val b = Finance.tipAndSplit(i.num("bill"), i.num("tip"), i.int("people"))
        ToolOutput(listOf("Per person" to money(b.perPerson), "Tip" to money(b.tip), "Total" to money(b.total)))
    },
    Tool(
        "salary", "Salary", "Hourly, weekly, monthly and yearly", Icons.Outlined.Work,
        listOf(
            Field("mode", "I know my", "Hourly rate", FieldKind.CHOICE, options = listOf("Hourly rate", "Monthly salary")),
            Field("amount", "Amount", "20"),
            Field("hours", "Hours per week", "40"),
        ),
    ) { i ->
        val s = if (i.choice("mode") == "Hourly rate") Finance.salaryFromHourly(i.num("amount"), i.num("hours"))
        else Finance.salaryFromMonthly(i.num("amount"), i.num("hours"))
        ToolOutput(
            listOf("Monthly" to money(s.monthly), "Yearly" to money(s.yearly), "Weekly" to money(s.weekly), "Hourly" to money(s.hourly)),
            note = "Based on 52 weeks per year.",
        )
    },
    Tool(
        "salarypct", "Percentage of salary", "Share of income for rent, savings and more", Icons.Outlined.Payments,
        listOf(Field("salary", "Salary", "3000"), Field("pct", "Percentage", "30", suffix = PCT)),
    ) { i ->
        val part = i.num("salary").multiply(i.num("pct"), MathContext.DECIMAL128).divide(BigDecimal(100), MathContext.DECIMAL128)
        ToolOutput(listOf("Amount" to money(part), "Remaining" to money(i.num("salary").subtract(part))))
    },
    Tool(
        "roi", "Investment return", "ROI and annualised return", Icons.Outlined.AccountBalance,
        listOf(Field("invested", "Amount invested", "5000"), Field("final", "Final value", "7500"), Field("years", "Years held (optional)", "3")),
    ) { i ->
        val r = Finance.investmentReturn(i.num("invested"), i.num("final"), i.numOrNull("years"))
        ToolOutput(listOfNotNull(
            "Return (ROI)" to pct(r.roiPercent),
            "Gain" to money(r.gain),
            r.annualisedPercent?.let { "Annualised return" to pct(it) },
        ))
    },
)
