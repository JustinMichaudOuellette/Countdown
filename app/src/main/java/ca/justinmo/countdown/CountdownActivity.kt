package ca.justinmo.countdown

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import ca.justinmo.countdown.calendar.CalendarEvent
import ca.justinmo.countdown.calendar.CalendarIntentReader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow

/** Main activity. */
@AndroidEntryPoint
class CountdownActivity : ComponentActivity() {

  /** Calendar events shared or opened while the activity is already running. */
  private val incomingCalendarEvents = MutableSharedFlow<CalendarEvent>(extraBufferCapacity = 1)

  private var initialCalendarEvent: CalendarEvent? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // Only a fresh launch consumes the intent: after a configuration change the editor that was
    // opened for the event is restored from the navigation state instead.
    if (savedInstanceState == null) {
      initialCalendarEvent = readCalendarEvent(intent)
    }
    setContent {
      CountdownTheme { colorScheme ->
        window.decorView.setBackgroundColor(colorScheme.background.toArgb())
        CountdownNavGraph(
            initialCalendarEvent = initialCalendarEvent,
            incomingCalendarEvents = incomingCalendarEvents,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val event = readCalendarEvent(intent)
    if (event != null) {
      incomingCalendarEvents.tryEmit(event)
    }
  }

  private fun readCalendarEvent(intent: Intent?): CalendarEvent? {
    val event = CalendarIntentReader.read(this, intent)
    if (event == null && CalendarIntentReader.carriesCalendarData(intent)) {
      Toast.makeText(this, R.string.calendar_event_unreadable, Toast.LENGTH_LONG).show()
    }
    return event
  }
}
