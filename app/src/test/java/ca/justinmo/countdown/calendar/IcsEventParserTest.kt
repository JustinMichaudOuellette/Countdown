package ca.justinmo.countdown.calendar

import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IcsEventParserTest {

  private lateinit var originalTimeZone: TimeZone

  @Before
  fun setUp() {
    originalTimeZone = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"))
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(originalTimeZone)
  }

  @Test
  fun `reads a UTC start time in the device time zone`() {
    val event =
        IcsEventParser.parse(
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:42@example.com
            DTSTART:20240715T120000Z
            SUMMARY:Trip to Paris
            END:VEVENT
            END:VCALENDAR
            """
                .trimIndent()
        )

    assertEquals("Trip to Paris", event?.title)
    assertEquals(2024, event?.year)
    assertEquals(7, event?.month)
    assertEquals(15, event?.day)
    // Paris is UTC+2 in July.
    assertEquals(14, event?.hour)
    assertEquals(0, event?.minute)
    assertFalse(event?.yearly ?: true)
  }

  @Test
  fun `keeps floating start times as a local wall clock`() {
    val event = parseSingleEvent("DTSTART:20240715T093000", "Dentist")

    assertEquals(2024, event?.year)
    assertEquals(7, event?.month)
    assertEquals(15, event?.day)
    assertEquals(9, event?.hour)
    assertEquals(30, event?.minute)
  }

  @Test
  fun `converts TZID start times to the device time zone`() {
    val event = parseSingleEvent("DTSTART;TZID=America/New_York:20240715T120000", "Standup")

    // Noon in New York (UTC-4 in July) is 18:00 in Paris (UTC+2).
    assertEquals(18, event?.hour)
    assertEquals(15, event?.day)
  }

  @Test
  fun `uses the fallback hour for all day events`() {
    val event = parseSingleEvent("DTSTART;VALUE=DATE:20240715", "Birthday")

    assertEquals(2024, event?.year)
    assertEquals(7, event?.month)
    assertEquals(15, event?.day)
    assertEquals(IcsEventParser.DefaultAllDayHour, event?.hour)
    assertEquals(0, event?.minute)
  }

  @Test
  fun `marks yearly recurring events`() {
    val event =
        parseSingleEvent(
            "DTSTART:20241224T180000\nRRULE:FREQ=YEARLY;BYMONTH=12;BYMONTHDAY=24",
            "Christmas Eve",
        )

    assertTrue(event?.yearly ?: false)
  }

  @Test
  fun `unfolds long summaries and unescapes text`() {
    val event =
        parseSingleEvent(
            "DTSTART:20240715T120000\nSUMMARY:Paris\\, France\\nwith friends\n , and family",
            null,
        )

    assertEquals("Paris, France\nwith friends, and family", event?.title)
  }

  @Test
  fun `reads only the first event`() {
    val event =
        IcsEventParser.parse(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            DTSTART:20240101T100000
            SUMMARY:First
            END:VEVENT
            BEGIN:VEVENT
            DTSTART:20240202T100000
            SUMMARY:Second
            END:VEVENT
            END:VCALENDAR
            """
                .trimIndent()
        )

    assertEquals("First", event?.title)
    assertEquals(1, event?.month)
  }

  @Test
  fun `event without a summary has no title`() {
    val event = parseSingleEvent("DTSTART:20240715T120000", null)

    assertNull(event?.title)
  }

  @Test
  fun `tolerates crlf line endings`() {
    val event =
        IcsEventParser.parse(
            "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nDTSTART:20240715T120000\r\nSUMMARY:CRLF\r\n"
                .plus("END:VEVENT\r\nEND:VCALENDAR\r\n")
        )

    assertEquals("CRLF", event?.title)
    assertEquals(12, event?.hour)
  }

  @Test
  fun `returns null without a start time`() {
    val event = parseSingleEvent("SUMMARY:No start", null)

    assertNull(event)
  }

  @Test
  fun `returns null for impossible dates`() {
    assertNull(parseSingleEvent("DTSTART:20240230T120000", "February 30th"))
    assertNull(parseSingleEvent("DTSTART:20241315T120000", "Month 13"))
    assertNull(parseSingleEvent("DTSTART:20240715T256100", "Hour 25"))
  }

  @Test
  fun `returns null for content that is not a calendar event`() {
    assertNull(IcsEventParser.parse("hello world"))
    assertNull(IcsEventParser.parse(""))
  }

  private fun parseSingleEvent(details: String, summary: String?): CalendarEvent? {
    val text =
        buildString {
          appendLine("BEGIN:VCALENDAR")
          appendLine("BEGIN:VEVENT")
          appendLine(details)
          if (summary != null) appendLine("SUMMARY:$summary")
          appendLine("END:VEVENT")
          appendLine("END:VCALENDAR")
        }
    return IcsEventParser.parse(text)
  }
}
