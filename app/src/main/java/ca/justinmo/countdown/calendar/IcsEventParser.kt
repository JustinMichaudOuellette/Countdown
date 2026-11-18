package ca.justinmo.countdown.calendar

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Reads the first VEVENT of an iCalendar (RFC 5545) payload.
 *
 * Only what a countdown needs is extracted: the summary, the start date and time, and whether the
 * event repeats yearly. All-day events get [DefaultAllDayHour] so they still have a time, and UTC or
 * TZID based start times are converted to the device time zone.
 */
object IcsEventParser {

  /** Time of day used for all-day events, matching the editor's default. */
  const val DefaultAllDayHour = 9

  private val StartDateTimeRegex = Regex("""^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})?(Z)?$""")
  private val StartDateRegex = Regex("""^(\d{4})(\d{2})(\d{2})$""")
  private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

  /** Returns the first event of [content], or null when there is nothing a countdown can use. */
  fun parse(content: String): CalendarEvent? {
    val eventLines = firstEventLines(content) ?: return null
    val properties = readProperties(eventLines)

    val start = properties["DTSTART"]?.let(::readStart) ?: return null
    val title = properties["SUMMARY"]?.value?.let(::unescapeText)?.trim()?.takeIf { it.isNotEmpty() }
    val yearly = properties["RRULE"]?.value?.let(::isYearlyRule) ?: false

    if (start.year !in 1900..2200) return null
    if (start.month !in 1..12 || start.day !in 1..31) return null
    if (start.hour !in 0..23 || start.minute !in 0..59) return null

    return CalendarEvent(
        title = title,
        year = start.year,
        month = start.month,
        day = start.day,
        hour = start.hour,
        minute = start.minute,
        yearly = yearly,
    )
  }

  private data class Start(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)

  private data class Property(val value: String, val parameters: Map<String, String>)

  private fun readStart(property: Property): Start? {
    val rawValue = property.value.trim()

    // "VALUE=DATE" and date-only values such as 20240715 describe an all-day event.
    if (!rawValue.contains('T')) {
      val match = StartDateRegex.find(rawValue) ?: return null
      val year = match.groupValues[1].toInt()
      val month = match.groupValues[2].toInt()
      val day = match.groupValues[3].toInt()
      if (!isRealDate(year, month, day)) return null
      return Start(year = year, month = month, day = day, hour = DefaultAllDayHour, minute = 0)
    }

    val match = StartDateTimeRegex.find(rawValue) ?: return null
    val year = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val day = match.groupValues[3].toInt()
    val hour = match.groupValues[4].toInt()
    val minute = match.groupValues[5].toInt()

    val sourceTimeZone =
        if (match.groupValues[7] == "Z") {
          Utc
        } else {
          property.parameters["TZID"]?.let { tzid -> TimeZone.getTimeZone(tzid) }
              ?: TimeZone.getDefault()
        }
    val source =
        Calendar.getInstance(sourceTimeZone).apply {
          clear()
          set(year, month - 1, day, hour, minute)
        }
    // A lenient calendar rolls impossible values over (February 30th becomes March 1st); reject
    // those instead of silently moving the countdown.
    if (!source.holds(year, month, day, hour, minute)) return null

    val local = Calendar.getInstance().apply { timeInMillis = source.timeInMillis }

    return Start(
        year = local.get(Calendar.YEAR),
        month = local.get(Calendar.MONTH) + 1,
        day = local.get(Calendar.DAY_OF_MONTH),
        hour = local.get(Calendar.HOUR_OF_DAY),
        minute = local.get(Calendar.MINUTE),
    )
  }

  private fun Calendar.holds(year: Int, month: Int, day: Int, hour: Int, minute: Int): Boolean =
      get(Calendar.YEAR) == year &&
          get(Calendar.MONTH) == month - 1 &&
          get(Calendar.DAY_OF_MONTH) == day &&
          get(Calendar.HOUR_OF_DAY) == hour &&
          get(Calendar.MINUTE) == minute

  private fun isRealDate(year: Int, month: Int, day: Int): Boolean {
    val calendar =
        Calendar.getInstance(Utc).apply {
          clear()
          set(year, month - 1, day)
        }
    return calendar.get(Calendar.YEAR) == year &&
        calendar.get(Calendar.MONTH) == month - 1 &&
        calendar.get(Calendar.DAY_OF_MONTH) == day
  }

  /** Unfolds the content and returns the lines of the first VEVENT, or null when there is none. */
  private fun firstEventLines(content: String): List<String>? {
    val unfolded = StringBuilder(content.length)
    for (line in content.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
      // A leading space or tab continues the previous line (RFC 5545 folding).
      if (line.startsWith(" ") || line.startsWith("\t")) {
        unfolded.append(line.substring(1))
      } else {
        unfolded.append('\n').append(line)
      }
    }

    var insideEvent = false
    var event: MutableList<String>? = null
    for (line in unfolded.split('\n')) {
      when {
        line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
          insideEvent = true
          event = mutableListOf()
        }
        line.equals("END:VEVENT", ignoreCase = true) -> if (insideEvent) return event
        insideEvent -> event?.add(line)
      }
    }
    return null
  }

  private fun readProperties(lines: List<String>): Map<String, Property> {
    val properties = LinkedHashMap<String, Property>()
    for (line in lines) {
      val separator = valueSeparatorIndex(line)
      if (separator <= 0) continue
      val header = line.substring(0, separator)
      val value = line.substring(separator + 1)
      val headerParts = header.split(';')
      val name = headerParts.first().trim().uppercase(Locale.US)
      if (name.isEmpty()) continue
      val parameters =
          headerParts.drop(1).mapNotNull { part ->
            val equals = part.indexOf('=')
            if (equals <= 0) {
              null
            } else {
              part.substring(0, equals).trim().uppercase(Locale.US) to
                  part.substring(equals + 1).trim().removeSurrounding("\"")
            }
          }
      properties.putIfAbsent(name, Property(value, parameters.toMap()))
    }
    return properties
  }

  /** Index of the colon that separates the property name and parameters from its value. */
  private fun valueSeparatorIndex(line: String): Int {
    var quoted = false
    for (index in line.indices) {
      when (line[index]) {
        '"' -> quoted = !quoted
        ':' -> if (!quoted) return index
      }
    }
    return -1
  }

  /** Resolves the TEXT escapes defined by RFC 5545 (\\\\n, \\\\, \\\\; and \\\\\\\\). */
  private fun unescapeText(value: String): String {
    val result = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
      val character = value[index]
      if (character == '\\' && index + 1 < value.length) {
        when (val escaped = value[index + 1]) {
          'n', 'N' -> result.append('\n')
          ',', ';', '\\' -> result.append(escaped)
          else -> result.append(character).append(escaped)
        }
        index += 2
      } else {
        result.append(character)
        index++
      }
    }
    return result.toString()
  }

  private fun isYearlyRule(rule: String): Boolean =
      rule.split(';').any { part -> part.trim().uppercase(Locale.US).startsWith("FREQ=YEARLY") }
}
