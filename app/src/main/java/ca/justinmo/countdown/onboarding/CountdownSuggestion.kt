package ca.justinmo.countdown.onboarding

import androidx.annotation.StringRes
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Events suggested to the user during onboarding.
 *
 * Suggestions with a fixed [month] and [day] repeat every year. The other suggestions require the
 * user to pick a date before they can be added.
 *
 * Picking a date can also add more than one countdown to the list: a birthday for example is
 * celebrated every year, but the birth it celebrates only happened once.
 */
enum class CountdownSuggestion(
    private val month: Int?,
    private val day: Int?,
    /** The countdowns created from the picked date, in the order they are created. */
    private val countdowns: List<SuggestionCountdown>,
    /** The countdown of [countdowns] this suggestion is named after and previews in the list. */
    private val previewedIndex: Int = 0,
) {
  HALLOWEEN(10, 31, listOf(yearlyCountdown("🎃", R.string.suggestion_halloween))),
  CHRISTMAS(12, 25, listOf(yearlyCountdown("🎄", R.string.suggestion_christmas))),
  NEW_YEARS_DAY(1, 1, listOf(yearlyCountdown("🎆", R.string.suggestion_new_years_day))),
  VALENTINES_DAY(2, 14, listOf(yearlyCountdown("💘", R.string.suggestion_valentines_day))),
  WEDDING(
      null,
      null,
      listOf(SuggestionCountdown("💍", R.string.suggestion_wedding, yearly = false)),
  ),
  BIRTHDAY(
      null,
      null,
      listOf(
          // The birth only happens once, but its date comes back every year as a birthday.
          SuggestionCountdown("👶", R.string.suggestion_birth, yearly = false),
          SuggestionCountdown("🎂", R.string.suggestion_birthday, yearly = true),
      ),
      previewedIndex = 1,
  );

  /** Emoji of the countdown previewed in the list. */
  val emoji: String
    get() = countdowns[previewedIndex].emoji

  /** Title of the countdown previewed in the list. */
  @get:StringRes
  val titleRes: Int
    get() = countdowns[previewedIndex].titleRes

  /** Whether the user has to pick a date before this suggestion can be added. */
  val requiresDate: Boolean
    get() = month == null || day == null

  /** Stable identifier used while the countdown has not been saved yet. */
  val id: String
    get() = name.lowercase()

  /** Time of day at which the countdowns of this suggestion start. */
  private val hour: Int
    get() = if (requiresDate) NOON else MIDNIGHT

  /** The next date on which this recurring event starts. */
  fun nextOccurrence(now: LocalDateTime = LocalDateTime.now()): LocalDate {
    check(!requiresDate) { "$name does not have a fixed date" }
    val date = LocalDate.of(now.year, month!!, day!!)
    return if (date.isBefore(now.toLocalDate())) date.plusYears(1) else date
  }

  /**
   * The countdowns this suggestion adds, in the order they are created.
   *
   * [pickedDate] is required for suggestions that [requiresDate]; the others are created on their
   * [nextOccurrence]. [titleOf] resolves the title of each of them.
   */
  fun toCountdowns(
      titleOf: (Int) -> String,
      pickedDate: LocalDate? = null,
      now: LocalDateTime = LocalDateTime.now(),
  ): List<CountDownEntity> {
    val date =
        if (requiresDate) {
          requireNotNull(pickedDate) { "$name needs a date before it can be added" }
        } else {
          nextOccurrence(now)
        }
    return countdowns.mapIndexed { index, countdown ->
      // The id is replaced when the countdown is saved, so it only needs to be unique until then.
      val countdownId = if (index == 0) id else "${id}_$index"
      countdown.toEntity(countdownId, titleOf(countdown.titleRes), date)
    }
  }

  /**
   * The countdown this suggestion is named after, which previews it in the list of suggestions.
   *
   * Takes the same arguments as [toCountdowns].
   */
  fun previewCountdown(
      titleOf: (Int) -> String,
      pickedDate: LocalDate? = null,
      now: LocalDateTime = LocalDateTime.now(),
  ): CountDownEntity = toCountdowns(titleOf, pickedDate, now)[previewedIndex]

  private fun SuggestionCountdown.toEntity(
      id: String,
      title: String,
      date: LocalDate,
  ): CountDownEntity =
      CountDownEntity(
          id = id,
          emoji = emoji,
          title = title,
          targetYear = date.year,
          targetMonth = date.monthValue,
          targetDay = date.dayOfMonth,
          targetHour = hour,
          targetMinute = 0,
          yearly = yearly,
      )

  companion object {
    const val MIDNIGHT = 0
    const val NOON = 12
  }
}

/**
 * Description of one of the countdowns a [CountdownSuggestion] creates.
 *
 * Every countdown of a suggestion is placed on the same date, so only the parts that can differ
 * between them are described here.
 */
data class SuggestionCountdown(
    val emoji: String,
    @param:StringRes val titleRes: Int,
    val yearly: Boolean,
)

/** A countdown for an event that happens on the same date every year. */
private fun yearlyCountdown(emoji: String, @StringRes titleRes: Int): SuggestionCountdown =
    SuggestionCountdown(emoji, titleRes, yearly = true)
