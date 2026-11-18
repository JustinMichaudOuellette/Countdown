package ca.justinmo.countdown.util

import android.content.Context
import android.text.format.DateUtils
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import java.text.DateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

class TimeCalculator(private val context: Context) {

  private val breakdownNumbers = BreakdownNumberFormatter()

  private var years: Int = 0
  private var months: Int = 0
  private var days: Int = 0
  private var hours: Int = 0
  private var minutes: Int = 0
  private var seconds: Int = 0
  private var isPast: Boolean = false

  private val stringBuilder = StringBuilder()

  private var duration: Duration = Duration.ZERO

  private var targetTime: LocalDateTime = LocalDateTime.now()

  private var isYearly: Boolean = false

  fun setTime(countdown: CountDownEntity, ticker: Long) {
    isYearly = countdown.yearly

    val now = LocalDateTime.now()
    val nowLocalDate = now.toLocalDate()

    targetTime =
        LocalDateTime.of(
            if (isYearly) nowLocalDate.year else countdown.targetYear,
            countdown.targetMonth,
            countdown.targetDay,
            countdown.targetHour,
            countdown.targetMinute,
        )

    // 1. Years & months & days
    var period = Period.between(nowLocalDate, targetTime.toLocalDate())

    if (countdown.yearly) {
      targetTime =
          if (targetTime.isBefore(now)) {
            // Already happened this year
            targetTime.withYear(nowLocalDate.year + 1)
          } else if (period.years > 1) {
            // Ensure it is this year
            targetTime.withYear(nowLocalDate.year)
          } else {
            targetTime
          }
      period = Period.between(nowLocalDate, targetTime.toLocalDate())
    }

    // 2. Adjust start date for time difference calculation
    val adjustedStart =
        now.plusYears(period.years.toLong())
            .plusMonths(period.months.toLong())
            .plusDays(period.days.toLong())

    // 3. Hours, minutes, seconds
    val adjustedDuration = Duration.between(adjustedStart, targetTime)

    isPast = targetTime.isBefore(now)

    years = abs(period.years)
    months = abs(period.months)
    days = abs(period.days)
    hours = abs(adjustedDuration.toHours().toInt())
    minutes = abs(adjustedDuration.toMinutes().toInt() % 60)
    seconds = abs(adjustedDuration.seconds.toInt() % 60)

    duration = Duration.between(now, targetTime)
  }

  fun formatShort(): String {
    stringBuilder.clear()

    val resources = context.resources

    if (years != 0) {
      stringBuilder.append(resources.getQuantityString(R.plurals.year, years, years))
    }
    if (months != 0) {
      maybeAppendSpace()
      stringBuilder.append(resources.getQuantityString(R.plurals.month, months, months))
    }
    if (days != 0) {
      maybeAppendSpace()
      stringBuilder.append(resources.getQuantityString(R.plurals.day, days, days))
    }
    if (years == 0 && months == 0 && days == 0) {
      if (hours > 0) {
        maybeAppendSpace()
        stringBuilder.append(resources.getQuantityString(R.plurals.hour, hours, hours))
      }
      if (minutes > 0) {
        maybeAppendSpace()
        stringBuilder.append(resources.getQuantityString(R.plurals.minute, minutes, minutes))
      }
      if (hours == 0 && minutes == 0) {
        if (seconds > 0) {
          maybeAppendSpace()
          stringBuilder.append(resources.getQuantityString(R.plurals.second, seconds, seconds))
        }
      }
    }
    return if (isPast) {
      resources.getString(R.string.past_time, stringBuilder.toString())
    } else {
      stringBuilder.toString()
    }
  }

  private fun maybeAppendSpace() {
    if (stringBuilder.isNotEmpty()) {
      stringBuilder.append(" ")
    }
  }

  fun formatDetailedBreakdown(): List<String> {
    val list = mutableListOf<String>()

    floatingTime.forEach { (seconds, plural) ->
      val floatValue = duration.seconds / seconds
      val integerValue = floor(floatValue).toInt()
      list.add(
          context.resources.getQuantityString(
              plural,
              integerValue,
              breakdownNumbers.format(floatValue),
          )
      )
    }

    val durationMs = duration.toMillis()
    integerTime.forEach { (ms, plural) ->
      val integerValue = durationMs / ms
      list.add(
          context.resources.getQuantityString(
              plural,
              if (abs(integerValue) > 2) 2 else 0,
              breakdownNumbers.format(integerValue),
          )
      )
    }

    return list
  }

  fun formatTargetDateTime(): String {
    val instant = targetTime.atZone(ZoneId.systemDefault()).toInstant()
    val date = Date.from(instant)
    val dateString =
        if (isYearly) {
          val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR
          DateUtils.formatDateTime(context, date.time, flags)
        } else {
          DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(date)
        }
    val timeString = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    return "$dateString\n$timeString"
  }

  fun durationMillis(): Long {
    return duration.toMillis()
  }

  companion object {
    const val SECONDS_PER_YEAR = 60f * 60f * 24f * 365f
    const val SECONDS_PER_MONTH = SECONDS_PER_YEAR / 12f
    const val SECONDS_PER_WEEK = SECONDS_PER_YEAR / 52f

    val floatingTime =
        mapOf(
            SECONDS_PER_YEAR to R.plurals.year,
            SECONDS_PER_MONTH to R.plurals.month,
            SECONDS_PER_WEEK to R.plurals.week,
        )

    const val MS_PER_DAY = 60L * 60L * 24L * 1000L
    const val MS_PER_HOUR = 60L * 60L * 1000L
    const val MS_PER_MINUTE = 60L * 1000L
    const val MS = 1L

    val integerTime =
        mapOf(
            MS_PER_DAY to R.plurals.day,
            MS_PER_HOUR to R.plurals.hour,
            MS_PER_MINUTE to R.plurals.minute,
            MS to R.plurals.millisecond,
        )
  }
}
