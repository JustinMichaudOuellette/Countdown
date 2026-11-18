package ca.justinmo.countdown.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "countdown")
data class CountDownEntity(
    @PrimaryKey val id: String,
    val emoji: String,
    val title: String?,
    val targetYear: Int,
    val targetMonth: Int,
    val targetDay: Int,
    val targetHour: Int,
    val targetMinute: Int,
    val yearly: Boolean,
) {

  init {
    if (targetYear != Int.MIN_VALUE || targetMonth != Int.MIN_VALUE || targetDay != Int.MIN_VALUE) {
      check(targetYear != Int.MIN_VALUE)
      check(targetMonth in 1..12)
      check(targetDay in 1..31)
    }
    if (targetHour != Int.MIN_VALUE || targetMinute != Int.MIN_VALUE) {
      check(targetHour in 0..23)
      check(targetMinute in 0..59)
    }
  }

  fun targetDate(): Calendar? =
      if (targetYear != Int.MIN_VALUE) {
        Calendar.getInstance().also {
          it.set(targetYear, targetMonth - 1, targetDay, targetHour, targetMinute)
        }
      } else {
        null
      }

  fun targetTime(): Calendar? =
      if (targetHour != Int.MIN_VALUE) {
        Calendar.getInstance().also {
          it.set(Calendar.HOUR_OF_DAY, targetHour)
          it.set(Calendar.MINUTE, targetMinute)
        }
      } else {
        null
      }

  companion object {
    fun createEmpty(): CountDownEntity =
        CountDownEntity(
            id = "",
            emoji = "",
            title = null,
            targetYear = Int.MIN_VALUE,
            targetMonth = Int.MIN_VALUE,
            targetDay = Int.MIN_VALUE,
            targetHour = Int.MIN_VALUE,
            targetMinute = Int.MIN_VALUE,
            yearly = false,
        )
  }
}
