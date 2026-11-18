package ca.justinmo.countdown.onboarding

import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownSuggestionTest {

  private val birthDate = LocalDate.of(1990, 5, 12)

  private val titles =
      mapOf(
          R.string.suggestion_birth to "Birth",
          R.string.suggestion_birthday to "Birthday",
          R.string.suggestion_christmas to "Christmas",
      )

  @Test
  fun `birthday creates the birth and then the yearly birthday`() {
    val countdowns = CountdownSuggestion.BIRTHDAY.toCountdowns({ titles.getValue(it) }, birthDate)

    assertEquals(2, countdowns.size)

    val birth = countdowns[0]
    assertEquals("👶", birth.emoji)
    assertEquals("Birth", birth.title)
    assertFalse(birth.yearly)
    assertEquals(birthDate, birth.date())
    assertEquals(CountdownSuggestion.NOON, birth.targetHour)

    val birthday = countdowns[1]
    assertEquals("🎂", birthday.emoji)
    assertEquals("Birthday", birthday.title)
    assertTrue(birthday.yearly)
    assertEquals(birthDate, birthday.date())
  }

  @Test
  fun `birthday is previewed by its yearly birthday`() {
    val preview = CountdownSuggestion.BIRTHDAY.previewCountdown({ titles.getValue(it) }, birthDate)

    assertEquals("🎂", preview.emoji)
    assertEquals("Birthday", preview.title)
    assertTrue(preview.yearly)
  }

  @Test
  fun `fixed date suggestions create a single countdown at midnight`() {
    val countdowns =
        CountdownSuggestion.CHRISTMAS.toCountdowns(
            { titles.getValue(it) },
            now = LocalDateTime.of(2026, 3, 1, 9, 0),
        )

    assertEquals(1, countdowns.size)
    val christmas = countdowns.single()
    assertEquals("🎄", christmas.emoji)
    assertEquals("Christmas", christmas.title)
    assertTrue(christmas.yearly)
    assertEquals(LocalDate.of(2026, 12, 25), christmas.date())
    assertEquals(CountdownSuggestion.MIDNIGHT, christmas.targetHour)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `suggestions that need a date cannot be created without one`() {
    CountdownSuggestion.WEDDING.toCountdowns({ "Wedding" })
  }
}

/** The date of this countdown, without its time of day. */
private fun CountDownEntity.date(): LocalDate =
    LocalDate.of(targetYear, targetMonth, targetDay)
