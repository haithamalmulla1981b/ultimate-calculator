package com.hmalmulla.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** A unit converts to and from its category's base unit. */
class UnitDef(
    val name: String,
    val symbol: String,
    private val toBaseFn: (BigDecimal) -> BigDecimal,
    private val fromBaseFn: (BigDecimal) -> BigDecimal,
) {
    fun toBase(v: BigDecimal) = toBaseFn(v)
    fun fromBase(v: BigDecimal) = fromBaseFn(v)
}

class UnitCategory(val id: String, val name: String, val units: List<UnitDef>)

object Units {
    private val MC = MathContext(34, RoundingMode.HALF_EVEN)

    /** Linear unit: value × factor = base. Factor may be "a/b". */
    private fun lin(name: String, symbol: String, factor: String): UnitDef {
        val f = if (factor.contains('/')) {
            val (a, b) = factor.split('/')
            BigDecimal(a).divide(BigDecimal(b), MC)
        } else BigDecimal(factor)
        return UnitDef(name, symbol, { it.multiply(f, MC) }, { it.divide(f, MC) })
    }

    /** Fuel units relative to L/100 km: value = k / (L/100 km). */
    private fun inverseFuel(name: String, symbol: String, k: String): UnitDef {
        val kk = BigDecimal(k)
        fun inv(v: BigDecimal): BigDecimal {
            if (v.signum() == 0) throw ArithmeticException("Can't convert zero fuel consumption")
            return kk.divide(v, MC)
        }
        return UnitDef(name, symbol, ::inv, ::inv)
    }

    private val two = BigDecimal("273.15")
    private val nine5 = BigDecimal(9).divide(BigDecimal(5), MC)

    val categories: List<UnitCategory> = listOf(
        UnitCategory("length", "Length", listOf(
            lin("Millimeter", "mm", "0.001"), lin("Centimeter", "cm", "0.01"), lin("Meter", "m", "1"),
            lin("Kilometer", "km", "1000"), lin("Micrometer", "µm", "0.000001"),
            lin("Inch", "in", "0.0254"), lin("Foot", "ft", "0.3048"), lin("Yard", "yd", "0.9144"),
            lin("Mile", "mi", "1609.344"), lin("Nautical mile", "nmi", "1852"),
        )),
        UnitCategory("distance", "Distance", listOf(
            lin("Meter", "m", "1"), lin("Kilometer", "km", "1000"), lin("Mile", "mi", "1609.344"),
            lin("Nautical mile", "nmi", "1852"), lin("Yard", "yd", "0.9144"), lin("Foot", "ft", "0.3048"),
            lin("Astronomical unit", "au", "149597870700"), lin("Light-year", "ly", "9460730472580800"),
        )),
        UnitCategory("weight", "Weight", listOf(
            lin("Gram", "g", "0.001"), lin("Kilogram", "kg", "1"), lin("Pound", "lb", "0.45359237"),
            lin("Ounce", "oz", "0.028349523125"), lin("Stone", "st", "6.35029318"), lin("Tonne", "t", "1000"),
        )),
        UnitCategory("mass", "Mass", listOf(
            lin("Microgram", "µg", "0.000000001"), lin("Milligram", "mg", "0.000001"), lin("Gram", "g", "0.001"),
            lin("Kilogram", "kg", "1"), lin("Tonne", "t", "1000"), lin("Carat", "ct", "0.0002"),
            lin("Grain", "gr", "0.00006479891"), lin("Troy ounce", "ozt", "0.0311034768"),
            lin("Pound", "lb", "0.45359237"), lin("Ounce", "oz", "0.028349523125"),
        )),
        UnitCategory("temperature", "Temperature", listOf(
            UnitDef("Celsius", "°C", { it }, { it }),
            UnitDef("Fahrenheit", "°F",
                { it.subtract(BigDecimal(32)).divide(nine5, MC) },
                { it.multiply(nine5, MC).add(BigDecimal(32)) }),
            UnitDef("Kelvin", "K", { it.subtract(two) }, { it.add(two) }),
            UnitDef("Rankine", "°R",
                { it.divide(nine5, MC).subtract(two) },
                { it.add(two).multiply(nine5, MC) }),
        )),
        UnitCategory("area", "Area", listOf(
            lin("Square millimeter", "mm²", "0.000001"), lin("Square centimeter", "cm²", "0.0001"),
            lin("Square meter", "m²", "1"), lin("Hectare", "ha", "10000"), lin("Square kilometer", "km²", "1000000"),
            lin("Square inch", "in²", "0.00064516"), lin("Square foot", "ft²", "0.09290304"),
            lin("Square yard", "yd²", "0.83612736"), lin("Acre", "ac", "4046.8564224"),
            lin("Square mile", "mi²", "2589988.110336"),
        )),
        UnitCategory("volume", "Volume", listOf(
            lin("Milliliter", "mL", "0.001"), lin("Liter", "L", "1"), lin("Cubic meter", "m³", "1000"),
            lin("Cubic centimeter", "cm³", "0.001"), lin("US gallon", "gal (US)", "3.785411784"),
            lin("UK gallon", "gal (UK)", "4.54609"), lin("US quart", "qt", "0.946352946"),
            lin("US pint", "pt", "0.473176473"), lin("US fluid ounce", "fl oz", "0.0295735295625"),
            lin("Cubic foot", "ft³", "28.316846592"), lin("Cubic inch", "in³", "0.016387064"),
        )),
        UnitCategory("speed", "Speed", listOf(
            lin("Meter per second", "m/s", "1"), lin("Kilometer per hour", "km/h", "1000/3600"),
            lin("Mile per hour", "mph", "0.44704"), lin("Knot", "kn", "1852/3600"), lin("Foot per second", "ft/s", "0.3048"),
        )),
        UnitCategory("time", "Time", listOf(
            lin("Millisecond", "ms", "0.001"), lin("Second", "s", "1"), lin("Minute", "min", "60"),
            lin("Hour", "h", "3600"), lin("Day", "d", "86400"), lin("Week", "wk", "604800"),
            lin("Month (average)", "mo", "2629746"), lin("Year (average)", "yr", "31556952"),
        )),
        UnitCategory("data", "Data storage", listOf(
            lin("Bit", "bit", "0.125"), lin("Byte", "B", "1"), lin("Kilobyte", "KB", "1000"),
            lin("Megabyte", "MB", "1000000"), lin("Gigabyte", "GB", "1000000000"),
            lin("Terabyte", "TB", "1000000000000"), lin("Petabyte", "PB", "1000000000000000"),
            lin("Kibibyte", "KiB", "1024"), lin("Mebibyte", "MiB", "1048576"),
            lin("Gibibyte", "GiB", "1073741824"), lin("Tebibyte", "TiB", "1099511627776"),
        )),
        UnitCategory("transfer", "Transfer speed", listOf(
            lin("Bit per second", "bps", "1"), lin("Kilobit per second", "Kbps", "1000"),
            lin("Megabit per second", "Mbps", "1000000"), lin("Gigabit per second", "Gbps", "1000000000"),
            lin("Terabit per second", "Tbps", "1000000000000"), lin("Byte per second", "B/s", "8"),
            lin("Kilobyte per second", "KB/s", "8000"), lin("Megabyte per second", "MB/s", "8000000"),
            lin("Gigabyte per second", "GB/s", "8000000000"),
        )),
        UnitCategory("pressure", "Pressure", listOf(
            lin("Pascal", "Pa", "1"), lin("Kilopascal", "kPa", "1000"), lin("Megapascal", "MPa", "1000000"),
            lin("Bar", "bar", "100000"), lin("Millibar", "mbar", "100"), lin("Atmosphere", "atm", "101325"),
            lin("Pound per square inch", "psi", "6894.757293168"), lin("Millimeter of mercury", "mmHg", "133.322387415"),
            lin("Inch of mercury", "inHg", "3386.389"),
        )),
        UnitCategory("energy", "Energy", listOf(
            lin("Joule", "J", "1"), lin("Kilojoule", "kJ", "1000"), lin("Calorie", "cal", "4.184"),
            lin("Kilocalorie", "kcal", "4184"), lin("Watt-hour", "Wh", "3600"), lin("Kilowatt-hour", "kWh", "3600000"),
            lin("Electronvolt", "eV", "1.602176634E-19"), lin("British thermal unit", "BTU", "1055.05585262"),
            lin("Foot-pound", "ft·lbf", "1.3558179483314004"),
        )),
        UnitCategory("power", "Power", listOf(
            lin("Watt", "W", "1"), lin("Kilowatt", "kW", "1000"), lin("Megawatt", "MW", "1000000"),
            lin("Horsepower (mechanical)", "hp", "745.69987158227022"), lin("Horsepower (metric)", "PS", "735.49875"),
            lin("BTU per hour", "BTU/h", "0.29307107017"),
        )),
        UnitCategory("force", "Force", listOf(
            lin("Newton", "N", "1"), lin("Kilonewton", "kN", "1000"), lin("Dyne", "dyn", "0.00001"),
            lin("Pound-force", "lbf", "4.4482216152605"), lin("Kilogram-force", "kgf", "9.80665"),
        )),
        UnitCategory("frequency", "Frequency", listOf(
            lin("Hertz", "Hz", "1"), lin("Kilohertz", "kHz", "1000"), lin("Megahertz", "MHz", "1000000"),
            lin("Gigahertz", "GHz", "1000000000"), lin("Revolutions per minute", "rpm", "1/60"),
        )),
        UnitCategory("angle", "Angle", listOf(
            lin("Degree", "°", "1"),
            UnitDef("Radian", "rad",
                { it.multiply(BigDecimal(180), MC).divide(MathEngine.PI, MC) },
                { it.multiply(MathEngine.PI, MC).divide(BigDecimal(180), MC) }),
            lin("Gradian", "gon", "0.9"), lin("Arcminute", "′", "1/60"), lin("Arcsecond", "″", "1/3600"),
            lin("Turn", "turn", "360"),
        )),
        UnitCategory("fuel", "Fuel consumption", listOf(
            UnitDef("Liters per 100 km", "L/100km", { it }, { it }),
            inverseFuel("Kilometers per liter", "km/L", "100"),
            inverseFuel("Miles per gallon (US)", "mpg US", "235.214583"),
            inverseFuel("Miles per gallon (UK)", "mpg UK", "282.480936"),
        )),
        UnitCategory("cooking", "Cooking", listOf(
            lin("Milliliter", "mL", "1"), lin("Liter", "L", "1000"), lin("Teaspoon (US)", "tsp", "4.92892159375"),
            lin("Tablespoon (US)", "tbsp", "14.78676478125"), lin("Cup (US)", "cup", "236.5882365"),
            lin("Cup (metric)", "cup (metric)", "250"), lin("Fluid ounce (US)", "fl oz", "29.5735295625"),
            lin("Pint (US)", "pt", "473.176473"),
        )),
    )

    fun convert(value: BigDecimal, from: UnitDef, to: UnitDef): BigDecimal =
        to.fromBase(from.toBase(value)).round(MathContext(15, RoundingMode.HALF_EVEN)).let {
            if (it.signum() == 0) BigDecimal.ZERO else it.stripTrailingZeros()
        }
}
