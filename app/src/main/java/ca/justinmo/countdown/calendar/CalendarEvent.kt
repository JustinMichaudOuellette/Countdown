package ca.justinmo.countdown.calendar

/**
 * An event read from calendar data that the app can turn into a countdown.
 *
 * [hour] and [minute] are always set: all-day events get a default time so the editor always has a
 * valid time to show.
 */
data class CalendarEvent(
    val title: String?,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val yearly: Boolean,
)
