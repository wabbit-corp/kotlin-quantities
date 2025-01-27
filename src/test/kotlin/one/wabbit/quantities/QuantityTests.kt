package one.wabbit.quantities

import kotlin.math.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantityTests {
    /**
     * A small helper data class for the test table below, with four elements:
     * (value, error, sigDigits, leadingOneException, expectedString)
     */
    private data class RoundingTestCase(
        val value: Double,
        val error: Double,
        val sigDigits: Int,
        val leadingOneException: Boolean,
        val expectedString: String
    )

    @Test
    fun testSignificantErrorFormattingExamples() {
        // We'll test multiple scenarios for the formatWithSignificantError() function.
        // Each tuple: (value, error, sigDigits, expectedString)
        val testCases = listOf(
            RoundingTestCase(1.321, 0.214, 1, false, "1.3 ± 0.2"),
            RoundingTestCase(1.321, 0.214, 2, false, "1.32 ± 0.21"),
            RoundingTestCase(1.321, 0.214, 3, false, "1.321 ± 0.214"),
            RoundingTestCase(1.321, 0.214, 1, true, "1.3 ± 0.2"),
            RoundingTestCase(1.321, 0.214, 2, true, "1.32 ± 0.21"),
            RoundingTestCase(1.321, 0.214, 3, true, "1.321 ± 0.214"),

            RoundingTestCase(12.345, 0.6789, 1, false, "12.3 ± 0.7"),
            RoundingTestCase(12.345, 0.6789, 2, false, "12.35 ± 0.68"),
            RoundingTestCase(12.345, 0.6789, 3, false, "12.345 ± 0.679"),
            RoundingTestCase(12.345, 0.6789, 1, true, "12.3 ± 0.7"),
            RoundingTestCase(12.345, 0.6789, 2, true, "12.35 ± 0.68"),
            RoundingTestCase(12.345, 0.6789, 3, true, "12.345 ± 0.679"),

            RoundingTestCase(0.0123, 0.0022, 1, false, "0.012 ± 0.002"),
            RoundingTestCase(0.0123, 0.0022, 2, false, "0.0123 ± 0.0022"),
            RoundingTestCase(0.0123, 0.0022, 1, true, "0.012 ± 0.002"),
            RoundingTestCase(0.0123, 0.0022, 2, true, "0.0123 ± 0.0022"),

            RoundingTestCase(12345.0, 120.0, 1, false, "12300 ± 100"),
            RoundingTestCase(12345.0, 120.0, 2, false, "12350 ± 120"),
            RoundingTestCase(12345.0, 123.0, 3, false, "12345 ± 123"),
            RoundingTestCase(12345.0, 123.0, 4, false, "12345.0 ± 123.0"),
            RoundingTestCase(12345.0, 120.0, 1, true, "12350 ± 120"),
            RoundingTestCase(12345.0, 120.0, 2, true, "12350 ± 120"),
            RoundingTestCase(12345.0, 123.0, 3, true, "12345 ± 123"),
            RoundingTestCase(12345.0, 123.0, 4, true, "12345.0 ± 123.0"),

            RoundingTestCase(12345.0, 1000.0, 1, false, "12000 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 2, false, "12300 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 3, false, "12350 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 4, false, "12345 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 5, false, "12345.0 ± 1000.0"),
            RoundingTestCase(12345.0, 1000.0, 1, true, "12300 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 2, true, "12300 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 3, true, "12350 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 4, true, "12345 ± 1000"),
            RoundingTestCase(12345.0, 1000.0, 5, true, "12345.0 ± 1000.0"),

            RoundingTestCase(12.345, 0.5000, 1, false, "12.3 ± 0.5"),
            RoundingTestCase(12.345, 0.5000, 2, false, "12.35 ± 0.50"),
            RoundingTestCase(12.345, 0.5000, 3, false, "12.345 ± 0.500"),
            RoundingTestCase(12.345, 0.5000, 1, true, "12.3 ± 0.5"),
            RoundingTestCase(12.345, 0.5000, 2, true, "12.35 ± 0.50"),
            RoundingTestCase(12.345, 0.5000, 3, true, "12.345 ± 0.500"),
        )

        val failed = mutableListOf<Pair<RoundingTestCase, String>>()
        for (testCase in testCases) {
            val (value, error, sig, leadingOneException, expected) = testCase
            val q = Quantity(value, error, Units(emptyMap()))
            val formatted = q.formatWithSignificantError(sig, leadingOneException)
            if (formatted != expected) {
                failed.add(Pair(testCase, formatted))
            }
        }

        assertTrue(failed.isEmpty(), "Failed cases: ${failed.joinToString("\n")}")
    }

    @Test
    fun testZeroErrorFormatting() {
        // If error=0.0, we might show "no error" or some special text
        // (depending on your actual function’s logic).
        val q = Quantity(1.234, 0.0, Units(emptyMap()))
        val formatted = q.formatWithSignificantError(2, false)
        // By the sample implementation, it might return "1.234 (no error) {}"
        // or something like that. Check for your actual result:
        assertTrue(formatted.contains("(no error)"), "Expected mention of no error")
    }

    @Test
    fun testArithmetic() {
        // Simple check of plus, minus, times, div for same units
        val unitless = Units(emptyMap())
        val q1 = Quantity(10.0, 1.0, unitless)
        val q2 = Quantity(5.0, 0.5, unitless)

        // plus => 15.0 ± 1.5 (worst-case by default)
        val sum = q1 + q2
        assertEquals(15.0, sum.value, 1e-12)
        assertEquals(1.5, sum.error, 1e-12)
        assertEquals(unitless, sum.units)

        // minus => 5.0 ± 1.5
        val diff = q1 - q2
        assertEquals(5.0, diff.value, 1e-12)
        assertEquals(1.5, diff.error, 1e-12)

        // times => 50.0 ± 15.0 => because worst-case => |5.0|*1.0 + |10.0|*0.5 = 5 + 5=10
        // Wait, let's check carefully:
        // q1.value=10, q2.value=5 => product=50
        // error => =5 * 1 + 10 * 0.5=5+5=10 (not 15!)
        // So total = 50 ± 10.
        val prod = q1 * q2
        assertEquals(50.0, prod.value, 1e-12)
        assertEquals(10.0, prod.error, 1e-12)

        // div => (10/5)=2.0
        // error => error= 1/5 + 10*(0.5)/(5^2)=0.2 + (10*0.5)/25=0.2 + 5/25=0.2+0.2=0.4
        val ratio = q1 / q2
        assertEquals(2.0, ratio.value, 1e-12)
        assertEquals(0.4, ratio.error, 1e-12)
    }

//    @Test
//    fun testConversionBasic() {
//        // Suppose we have a UnitRepository with m->cm and K->°C
//        val repo = buildDefaultUnitRepository()
//
//        // length in meters
//        val qMeters = Quantity(1.0, 0.01, Units(mapOf("m" to Rational.one)))
//        val qCm = qMeters.convertTo("cm", repo)
//        // factor=100, offset=0 => newValue=1.0*100=100, newError=0.01*100=1.0
//        assertEquals(100.0, qCm.value, 1e-12)
//        assertEquals(1.0, qCm.error, 1e-12)
//        // Check that units are the new "cm"
//        assertEquals(Units(mapOf("cm" to Rational.one)), qCm.units)
//
//        // temperature in Kelvin -> Celsius
//        val qKelvin = Quantity(300.0, 1.0, Units(mapOf("K" to Rational.one)))
//        val qCelsius = qKelvin.convertTo("°C", repo)
//        // factor=1.0, offset=-273.15 => newValue=300.0 + (-273.15)=26.85, newError=1.0
//        assertEquals(26.85, qCelsius.value, 1e-12)
//        assertEquals(1.0, qCelsius.error, 1e-12)
//        assertEquals(Units(mapOf("°C" to Rational.one)), qCelsius.units)
//    }

    @Test
    fun testAdvancedMath() {
        val unitless = Units(emptyMap())
        val q1 = Quantity(4.0, 0.4, unitless)

        // sqrt(4 ± 0.4) => sqrt(4)=2 => derivative=1/(2*sqrt(4))=1/(2*2)=1/4? Actually let's do it carefully:
        // f(x)=x^0.5 => df/dx=0.5 * x^(-0.5) => at x=4 => 0.5*(4^(-0.5))=0.5*(1/2)=0.25
        // worst-case => error=0.25*0.4=0.1 => value=2.0 ± 0.1
        val sqrtQ = q1.sqrt()
        assertEquals(2.0, sqrtQ.value, 1e-12)
        assertEquals(0.1, sqrtQ.error, 1e-12)

        // log(4 ± 0.4) => log(4)=1.386..., derivative=1/x=1/4=0.25 => error=0.25*0.4=0.1
        val logQ = q1.log()
        assertEquals(kotlin.math.ln(4.0), logQ.value, 1e-12)
        assertEquals(0.1, logQ.error, 1e-12)

        // exp(1 ± 0.1) => check dimensionless
        val q2 = Quantity(1.0, 0.1, unitless)
        val expQ = q2.exp()
        // exp(1)=2.71828..., derivative=exp(1)=2.71828..., error=2.71828*0.1 ~ 0.2718
        // We'll approximate
        val expectedVal = exp(1.0)
        val expectedErr = expectedVal*0.1
        assertEquals(expectedVal, expQ.value, 1e-12)
        // Tolerate a small rounding difference
        assertTrue(abs(expQ.error - expectedErr) < 1e-7, "exp error mismatch")

        // sin(1 ± 0.1) => derivative=cos(1)=0.5403..., error=0.5403*0.1=0.05403
        val sinQ = q2.sin()
        val expectedSinVal = sin(1.0)
        val expectedSinErr = abs(cos(1.0))*0.1
        assertEquals(expectedSinVal, sinQ.value, 1e-12)
        assertTrue(abs(sinQ.error - expectedSinErr) < 1e-7, "sin error mismatch")
    }
}