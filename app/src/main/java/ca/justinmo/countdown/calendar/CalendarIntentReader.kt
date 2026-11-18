package ca.justinmo.countdown.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/**
 * Reads the calendar event carried by an intent that was shared or opened into the app.
 *
 * Calendar apps share an .ics payload either as a stream (the canonical case) or as text, and file
 * managers open the .ics file as a URI.
 */
object CalendarIntentReader {

  /** Guards against huge or bogus payloads; calendar events are a few kilobytes at most. */
  private const val MaxPayloadBytes = 512 * 1024

  private val CalendarMimeTypes = setOf("text/calendar", "text/x-vcalendar")

  /** Returns the event carried by [intent], or null when it carries nothing usable. */
  fun read(context: Context, intent: Intent?): CalendarEvent? {
    val payload = payload(context, intent ?: return null) ?: return null
    return IcsEventParser.parse(payload)
  }

  /** True when [intent] carries calendar shaped data, even if it ends up unreadable. */
  fun carriesCalendarData(intent: Intent?): Boolean {
    if (intent == null) return false
    if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_VIEW) return false
    if (intent.type?.lowercase(Locale.US) in CalendarMimeTypes) return true
    return payloadUri(intent)?.path?.endsWith(".ics", ignoreCase = true) == true
  }

  private fun payload(context: Context, intent: Intent): String? {
    val uri = payloadUri(intent)
    if (uri != null) {
      readText(context, uri)?.let { return it.stripByteOrderMark() }
    }
    if (intent.action != Intent.ACTION_SEND) return null
    return intent.getStringExtra(Intent.EXTRA_TEXT)?.stripByteOrderMark()
  }

  private fun payloadUri(intent: Intent): Uri? =
      when (intent.action) {
        Intent.ACTION_SEND ->
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        Intent.ACTION_VIEW -> intent.data
        else -> null
      }

  private fun readText(context: Context, uri: Uri): String? =
      try {
        context.contentResolver.openInputStream(uri)?.use { input -> input.readText(MaxPayloadBytes) }
      } catch (e: IOException) {
        null
      } catch (e: SecurityException) {
        null
      } catch (e: IllegalArgumentException) {
        // Unsupported URI scheme.
        null
      }

  private fun InputStream.readText(maxBytes: Int): String {
    val buffer = ByteArray(8 * 1024)
    val output = ByteArrayOutputStream()
    var total = 0
    while (total <= maxBytes) {
      val read = read(buffer)
      if (read < 0) break
      output.write(buffer, 0, read)
      total += read
    }
    return output.toString("UTF-8")
  }

  /** Some exporters start the payload with a UTF-8 byte order mark. */
  private fun String.stripByteOrderMark(): String = if (startsWith('\uFEFF')) substring(1) else this
}
