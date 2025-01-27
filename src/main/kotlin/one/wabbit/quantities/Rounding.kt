package one.wabbit.quantities

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Formats a Quantity (value ± error) so that the error has exactly 'sigDigits'
 * significant digits (with a "leading-one" exception if desired).
 *
 * Algorithm:
 *   1. Identify exponent of the error (i.e. floor(log10(abs(error)))) => call it E.
 *   2. If leadingOneException == true AND the first digit == 1 AND sigDigits==1, use sigDigits=2.
 *   3. The "rounding shift" = (sigDigits - 1) - E.
 *   4. Round error and value by factor = 10^shift (HALF_UP).
 *   5. Convert the error to a string with exactly 'sigDigits' significant digits (including trailing zeros).
 *   6. Count the decimals in that error string => use the same # of decimals for the value string.
 */
fun formatWithSignificantError(
    q: Quantity,
    sigDigits: Int,
    leadingOneException: Boolean
): String {
    // If no error, just return the value + unit
    if (q.error == 0.0) {
        val unitStr = if (q.units.units.isNotEmpty()) " ${q.units}" else ""
        return "${q.value} (no error)$unitStr"
    }

    val absErr = q.error.absoluteValue
    // Step 1: Exponent of the error
    val exponent = if (absErr == 0.0) 0 else floor(log10(absErr)).toInt()

    // Find the (first) leading digit
    val leadingDigit = if (absErr == 0.0) 0 else floor(absErr / 10.0.pow(exponent.toDouble())).toInt()

    // Step 2: Possibly adjust sigDigits if leading-one exception
    var finalSig = sigDigits
    if (leadingOneException && leadingDigit == 1 && sigDigits == 1) {
        finalSig = 2
    }

    // Step 3: rounding shift
    val shift = finalSig - 1 - exponent
    val factor = 10.0.pow(shift)

    // Step 4a: Round the error numerically
    val roundedErrorNum = BigDecimal.valueOf(q.error * factor)
        .setScale(0, RoundingMode.HALF_UP)
        .toDouble() / factor

    // Step 5: Convert the error to exactly 'finalSig' significant digits
    val errorStr = formatNumberToSigDigits(roundedErrorNum, finalSig)

    // Step 4b: Round the value with the same shift/factor
    val roundedValueNum = BigDecimal.valueOf(q.value * factor)
        .setScale(0, RoundingMode.HALF_UP)
        .toDouble() / factor

    // Step 6: match decimals to the error
    val decimalsForValue = countDecimals(errorStr)
    val valueStr = formatNumberToMinDecimals(roundedValueNum, decimalsForValue)

    // Append the unit if present
    val unitStr = if (q.units.units.isNotEmpty()) " ${q.units}" else ""

    return "$valueStr ± $errorStr$unitStr"
}

/**
 * Convert 'num' (already correctly rounded) to a string with exactly 'sigDigits'
 * significant digits. This includes adding trailing zeros if necessary.
 *
 * Examples:
 *   formatNumberToSigDigits(0.5, 2)   -> "0.50"
 *   formatNumberToSigDigits(0.5, 3)   -> "0.500"
 *   formatNumberToSigDigits(12.3, 3)  -> "12.3" => Actually "12.3" has 3 sig digits? Let's see:
 *       leading digits: '1', '2', '3' => that is 3. If needed, it might add trailing zeros, but not always.
 *   formatNumberToSigDigits(123, 3)   -> "123"
 *   formatNumberToSigDigits(123, 4)   -> "123.0"
 */
private fun formatNumberToSigDigits(num: Double, sigDigits: Int): String {
    // Special case zero
    if (num == 0.0) {
        return if (sigDigits == 1) {
            "0"
        } else {
            // e.g. sigDigits=3 => "0.00"
            "0." + "0".repeat(sigDigits - 1)
        }
    }

    // 1) Convert to plain string with minimal trailing zeroes
    //    (stripTrailingZeros might remove decimals if not needed).
    val bdStripped = BigDecimal.valueOf(num).stripTrailingZeros().toPlainString()

    // 2) Count how many SIGNIFICANT digits are present (ignoring leading zeros and decimal point).
    val sigCountNow = countSignificantDigits(bdStripped)

    // If we already have at least sigDigits, return it as-is
    if (sigCountNow >= sigDigits) {
        return bdStripped
    }

    // Otherwise, we need to pad zeros to reach 'sigDigits'
    val needed = sigDigits - sigCountNow

    // We'll do it by adding trailing zeros after a decimal point.
    // If there's no decimal, we add one. Then append the necessary zeros.
    val dotIndex = bdStripped.indexOf('.')
    return if (dotIndex < 0) {
        // No decimal => add decimal then zeros
        bdStripped + "." + "0".repeat(needed)
    } else {
        // Already has decimal => just append zeros
        bdStripped + "0".repeat(needed)
    }
}

/**
 * Count how many significant digits are in 'str'.
 *
 *  - We ignore leading zeros and decimal point.
 *  - We do count zeros that occur after the first nonzero digit.
 *
 *  For example:
 *    "0.50" -> the digits are '5' and '0' => 2 significant digits
 *    "0.500" -> '5','0','0' => 3
 *    "12.345" -> '1','2','3','4','5' => 5
 *    "100" -> '1','0','0' => 3
 */
private fun countSignificantDigits(str: String): Int {
    val noDecimal = str.replace(".", "")
    // Strip leading sign if any
    val noSign = if (noDecimal.startsWith('-')) noDecimal.substring(1) else noDecimal

    // Now strip leading zeros
    val firstNonZero = noSign.indexOfFirst { it != '0' }
    if (firstNonZero < 0) {
        // It's all zeros, e.g. "0000", significance is 0
        return 0
    }
    // Count from that first nonzero char to the end
    return noSign.length - firstNonZero
}

/**
 * Counts how many decimal digits a string has.
 * E.g.:
 *   "0.50"   -> 2
 *   "12.345" -> 3
 *   "100"    -> 0
 */
private fun countDecimals(str: String): Int {
    val idx = str.indexOf('.')
    return if (idx >= 0) str.length - idx - 1 else 0
}

/**
 * Rounds 'valIn' to 'decimals' decimal places, half-up, and ensures
 * we *keep* trailing zeros if decimals>0.
 *
 * Examples:
 *   formatNumberToMinDecimals(12.3, 2)   -> "12.30"
 *   formatNumberToMinDecimals(12300.0,0) -> "12300"
 */
private fun formatNumberToMinDecimals(valIn: Double, decimals: Int): String {
    val bd = BigDecimal.valueOf(valIn).setScale(decimals, RoundingMode.HALF_UP)
    // If decimals=0, we strip trailing zeros beyond the decimal
    return if (decimals == 0) {
        bd.stripTrailingZeros().toPlainString()
    } else {
        // Keep trailing zeroes if decimals > 0
        bd.toPlainString()
    }
}
