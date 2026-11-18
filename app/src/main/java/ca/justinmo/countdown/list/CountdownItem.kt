package ca.justinmo.countdown.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.util.TimeCalculator

/**
 * A single countdown row showing the emoji, title, and time remaining until [countdown].
 *
 * [leadingContent] can be used to display an action or selection control (such as a checkbox) in
 * front of the row.
 */
@Composable
fun CountdownItem(
    countdown: CountDownEntity,
    time: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
  val context = LocalContext.current
  val timeCalculator = remember { TimeCalculator(context) }
  timeCalculator.setTime(countdown, time)
  CountdownItem(
      emoji = countdown.emoji,
      title = countdown.title,
      timeText = timeCalculator.formatShort(),
      onClick = onClick,
      modifier = modifier,
      leadingContent = leadingContent,
  )
}

/**
 * A single countdown row showing the emoji, title, and a caller-provided [timeText].
 *
 * Use this overload when there is no countdown to compute a time from yet, for example a
 * countdown suggestion that still needs a date.
 */
@Composable
fun CountdownItem(
    emoji: String,
    title: String?,
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          modifier
              .fillMaxWidth()
              .padding(vertical = dimensionResource(id = R.dimen.list_item_padding))
              .defaultMinSize(minHeight = 48.dp)
              .clickable(onClick = onClick),
  ) {
    leadingContent?.invoke()
    Text(
        text = emoji,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
      if (title?.isNotBlank() == true) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.horizontal_margin)),
        )
      }
      Text(
          text = timeText,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(end = dimensionResource(id = R.dimen.horizontal_margin)),
      )
    }
  }
}

// Preview

@Preview
@Composable
private fun FarCountdownItemPreview() {
  MaterialTheme {
    Surface {
      CountdownItem(
          createCountdown(
              emoji = "🦋",
              title = "Retirement",
              deltaYear = 1,
              deltaMonth = 2,
              deltaDay = 3,
              deltaHour = 4,
              deltaMinute = 5,
          ),
          onClick = {},
          time = 0L,
      )
    }
  }
}

@Preview
@Composable
private fun CloseCountdownItemPreview() {
  MaterialTheme {
    Surface {
      CountdownItem(
          createCountdown(
              emoji = "✈️",
              title = "Trip to Paris",
              deltaDay = 3,
              deltaHour = 4,
              deltaMinute = 5,
          ),
          onClick = {},
          time = 0L,
      )
    }
  }
}

@Preview
@Composable
private fun VeryCloseCountdownItemPreview() {
  MaterialTheme {
    Surface {
      CountdownItem(
          createCountdown(
              emoji = "🎺",
              title = "NOFX Show",
              deltaSeconds = 42,
          ),
          onClick = {},
          time = 0L,
      )
    }
  }
}

@Preview
@Composable
private fun PastFarCountdownItemPreview() {
  MaterialTheme {
    Surface {
      CountdownItem(
          createCountdown(
              emoji = "📖",
              title = "Book Release",
              deltaYear = -1,
              deltaMonth = -2,
              deltaDay = -3,
              deltaHour = -4,
              deltaMinute = -5,
          ),
          onClick = {},
          time = 0L,
      )
    }
  }
}

@Preview
@Composable
private fun PastCloseNoTitleCountdownItemPreview() {
  MaterialTheme {
    Surface {
      CountdownItem(
          createCountdown(
              emoji = "😎",
              deltaYear = -1,
              deltaMonth = -2,
              deltaDay = -3,
              deltaHour = -4,
              deltaMinute = -5,
          ),
          onClick = {},
          time = 0L,
      )
    }
  }
}

@Preview
@Composable
private fun NoDateCountdownItemPreview() {
  MaterialTheme {
    Surface {
      CountdownItem(
          emoji = "💍",
          title = "Wedding",
          timeText = stringResource(R.string.onboarding_suggestion_pick_date),
          onClick = {},
      )
    }
  }
}
