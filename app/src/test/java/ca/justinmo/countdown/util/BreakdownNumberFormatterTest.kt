package ca.justinmo.countdown.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class BreakdownNumberFormatterTest {

  private val space = '\u00A0'

  @Test
  fun `groups digits in threes with a space`() {
    val formatter = BreakdownNumberFormatter(Locale.US)

    assertEquals("0", formatter.format(0L))
    assertEquals("999", formatter.format(999L))
    assertEquals("1${space}000", formatter.format(1_000L))
    assertEquals("12${space}345", formatter.format(12_345L))
    assertEquals("1${space}000${space}000", formatter.format(1_000_000L))
  }

  @Test
  fun `puts a space after the minus sign of past values`() {
    val formatter = BreakdownNumberFormatter(Locale.US)

    assertEquals("- 5", formatter.format(-5L))
    assertEquals("- 1${space}000", formatter.format(-1_000L))
    assertEquals("- 2${space}500${space}000", formatter.format(-2_500_000L))
  }

  @Test
  fun `keeps at most one decimal digit`() {
    val formatter = BreakdownNumberFormatter(Locale.US)

    assertEquals("27", formatter.format(27.0f))
    assertEquals("27.5", formatter.format(27.5f))
    assertEquals("- 27.5", formatter.format(-27.5f))
    assertEquals("1${space}234.5", formatter.format(1234.5f))
  }

  @Test
  fun `keeps the locale decimal separator`() {
    val formatter = BreakdownNumberFormatter(Locale.CANADA_FRENCH)

    assertEquals("1${space}234,5", formatter.format(1234.5f))
  }
}
