package one.wabbit.quantities

import one.wabbit.math.Rational
import kotlin.math.*

enum class ErrorPropagation {
    WORST_CASE,     // e.g., Δ(total) = Δ(a) + Δ(b)
    QUADRATURE      // e.g., Δ(total) = sqrt(Δ(a)^2 + Δ(b)^2)
}

// ------------------------------------------------------------------------------------
// Helper function for plus/minus with different error-propagation rules.
// If you need a different approach for addition vs subtraction, adjust accordingly.
// ------------------------------------------------------------------------------------
private fun combineErrorsForAdd(err1: Double, err2: Double, model: ErrorPropagation): Double {
    return when (model) {
        ErrorPropagation.WORST_CASE -> err1 + err2
        ErrorPropagation.QUADRATURE -> sqrt(err1 * err1 + err2 * err2)
    }
}

private fun Double.sq(): Double = this * this

data class Quantity(val value: Double, val error: Double, val units: Units) {
    init {
        require(error >= 0) { "error must be >= 0" }
        require(value.isFinite()) { "value must be finite" }
        require(error.isFinite() || error == Double.POSITIVE_INFINITY) { "error must be finite or infinity" }
    }

    operator fun plus(other: Quantity): Quantity = plus(other, ErrorPropagation.WORST_CASE)
    operator fun minus(other: Quantity): Quantity = minus(other, ErrorPropagation.WORST_CASE)
    operator fun times(other: Quantity): Quantity = times(other, ErrorPropagation.WORST_CASE)
    operator fun div(other: Quantity): Quantity = div(other, ErrorPropagation.WORST_CASE)
    operator fun unaryMinus(): Quantity = Quantity(-value, error, units)

    fun plus(other: Quantity, model: ErrorPropagation): Quantity {
        require(units == other.units) { "Cannot add quantities with different units" }
        return Quantity(
            value + other.value,
            combineErrorsForAdd(error, other.error, model),
            units
        )
    }

    fun minus(other: Quantity, model: ErrorPropagation): Quantity {
        require(units == other.units) { "Cannot subtract quantities with different units" }
        return Quantity(
            value - other.value,
            combineErrorsForAdd(error, other.error, model),
            units
        )
    }

    fun times(other: Quantity, model: ErrorPropagation): Quantity {
        val newVal = value * other.value
        val newErr = when (model) {
            ErrorPropagation.WORST_CASE ->
                // worst-case linear addition: Δ(xy)=|y|Δx + |x|Δy
                other.value * error + value * other.error
            ErrorPropagation.QUADRATURE ->
                // quadrature: Δ(xy)=sqrt( (yΔx)^2 + (xΔy)^2 )
                sqrt((other.value * error).sq() + (value * other.error).sq())
        }
        return Quantity(newVal, newErr, units * other.units)
    }

    fun div(other: Quantity, model: ErrorPropagation): Quantity {
        val newVal = value / other.value

        // If the denominator's range crosses zero, do something "big"
        if (other.value - other.error <= 0.0 && other.value + other.error >= 0.0) {
            return Quantity(
                newVal,
                Double.POSITIVE_INFINITY,
                units / other.units
            )
        }

        val newErr = when (model) {
            ErrorPropagation.WORST_CASE ->
                // linear addition: Δ(x/y)=Δx/|y| + |x|Δy/|y|^2
                (error / other.value) + (value * other.error) / (other.value * other.value)
            ErrorPropagation.QUADRATURE -> {
                // quadrature version:
                // Δ(x/y)= sqrt( (Δx/|y|)^2 + (|x|Δy/|y|^2)^2 )
                val a = error / other.value
                val b = (value * other.error) / (other.value * other.value)
                sqrt(a * a + b * b)
            }
        }
        return Quantity(newVal, newErr, units / other.units)
    }

    fun pow(power: Rational): Quantity {
        // dimension: (units^power)
        // For error: f(x) = x^p => df/dx = p x^(p-1)
        // => Δf ~ |p x^(p-1)| Δx, in worst-case or quadrature with single variable.
        // We'll do the single-variable approach:
        val powerReal = power.toDouble()
        val newVal = value.pow(powerReal)
        val dfdx = abs(powerReal * value.pow(powerReal - 1))
        val newErr = dfdx * error

        // Raise all exponents in the units by 'power'
        val newUnits = units.pow(power)
        return Quantity(newVal, newErr, newUnits)
    }

    fun sqrt(): Quantity =
        pow(Rational.half)

    fun exp(): Quantity {
        // dimensionless check
        require(units.isDimensionless) { "exp(...) requires dimensionless quantity" }
        // f(x) = exp(x) => df/dx = exp(x). So Δf ~ exp(x)*Δx
        val newVal = exp(value)
        val dfdx = abs(newVal)  // = e^value
        val newErr = dfdx * error
        return Quantity(newVal, newErr, Units.None)
    }

    fun log(): Quantity {
        // dimensionless check
        require(units.isDimensionless) { "log(...) requires dimensionless quantity" }
        // f(x) = ln(x) => df/dx = 1/x. So Δf ~ |1/x| Δx
        require(value > 0) { "log(...) requires a positive value" }
        val dfdx = 1.0 / value
        val newVal = ln(value)
        val newErr = abs(dfdx) * error
        // log is dimensionless => no units
        return Quantity(newVal, newErr, Units.None)
    }

    fun sin(): Quantity {
        // dimensionless check (assuming argument is in radians if dimensionless)
        require(units.isDimensionless) { "sin(...) requires dimensionless quantity (angle in radians)" }
        // f(x) = sin(x) => df/dx = cos(x), Δf ~ |cos(x)| Δx
        val newVal = sin(value)
        val dfdx = abs(cos(value))
        val newErr = dfdx * error
        return Quantity(newVal, newErr, Units.None)
    }

    fun cos(): Quantity {
        // dimensionless check
        require(units.isDimensionless) { "cos(...) requires dimensionless quantity (angle in radians)" }
        // f(x) = cos(x) => df/dx = -sin(x), Δf ~ |sin(x)| Δx
        val newVal = cos(value)
        val dfdx = abs(sin(value))
        val newErr = dfdx * error
        return Quantity(newVal, newErr, Units.None)
    }

    fun formatWithSignificantError(sigDigits: Int, leadingOneException: Boolean): String =
        formatWithSignificantError(this, sigDigits, leadingOneException)

    override fun toString(): String {
        val unitStr = if (units.isDimensionless) "" else " $units"
        return "$value +/- $error$unitStr"
    }
}
