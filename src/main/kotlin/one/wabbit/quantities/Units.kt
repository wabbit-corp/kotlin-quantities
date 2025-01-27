package one.wabbit.quantities

import one.wabbit.math.Rational

class Units(units: Map<String, Rational>) {
    // Remove any units with a zero coefficient
    val units: Map<String, Rational> = units.filterValues { it != Rational.zero }

    val isDimensionless: Boolean
        get() = units.isEmpty()

    override fun toString(): String {
        return units.entries.sortedBy { it.key }.joinToString(" ") { (u, c) ->
            if (c != Rational.one) "$u^$c" else u
        }
    }

    operator fun times(other: Units): Units {
        val newUnits = mutableMapOf<String, Rational>()
        newUnits.putAll(units)
        for ((u, c) in other.units) {
            newUnits[u] = newUnits.getOrDefault(u, Rational.zero) + c
        }
        return Units(newUnits)
    }

    operator fun div(other: Units): Units {
        val newUnits = mutableMapOf<String, Rational>()
        newUnits.putAll(units)
        for ((u, c) in other.units) {
            newUnits[u] = newUnits.getOrDefault(u, Rational.zero) - c
        }
        return Units(newUnits)
    }

    fun invert(): Units {
        val newUnits = mutableMapOf<String, Rational>()
        for ((u, c) in units) {
            newUnits[u] = -c
        }
        return Units(newUnits)
    }

    fun pow(factor: Rational): Units {
        val newUnits = mutableMapOf<String, Rational>()
        for ((u, c) in units) {
            newUnits[u] = c * factor
        }
        return Units(newUnits)
    }

    override fun hashCode(): Int {
        return units.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Units) {
            return units == other.units
        }
        return false
    }

    companion object {
        val None = Units(emptyMap())
    }
}