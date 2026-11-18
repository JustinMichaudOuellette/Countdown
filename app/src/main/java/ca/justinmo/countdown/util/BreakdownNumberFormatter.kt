package ca.justinmo.countdown.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Formats the numbers of the countdown breakdown.
 *
 * Digits are grouped in threes with a space ("1 000 000") and past countdowns get a space after the
 * minus sign ("- 5 days"). The decimal separator and plural forms still follow the device locale.
 */
internal class BreakdownNumberFormatter(locale: Locale = Locale.getDefault()) {

  private val formatter: NumberFormat =
      NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
        isGroupingUsed = true
        if (this is DecimalFormat) {
          val symbols = decimalFormatSymbols
          symbols.groupingSeparator = GroupSeparator
          decimalFormatSymbols = symbols
          negativePrefix = NegativePrefix
        }
      }

  fun format(value: Long): String = formatter.format(value)

  fun format(value: Float): String = formatter.format(value)

  private companion object {
    /** A non-breaking space, so grouped digits never wrap onto two lines. */
    const val GroupSeparator = '\u00A0'

    const val NegativePrefix = "- "
  }
}
