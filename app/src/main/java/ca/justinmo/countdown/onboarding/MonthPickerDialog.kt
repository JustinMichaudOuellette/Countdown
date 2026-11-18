package ca.justinmo.countdown.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.justinmo.countdown.R
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * A dialog listing the months of the given [year], shown after the year picker so that the date
 * picker only has to deal with days.
 *
 * [initialMonth] is highlighted but does not select it; the user still has to tap a month.
 */
@Composable
fun MonthPickerDialog(
    year: Int,
    initialMonth: Int,
    onMonthSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Column {
          Text(text = stringResource(R.string.onboarding_pick_month))
          Text(
              text = year.toString(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      text = {
        val locale = Locale.getDefault()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              for (column in 0 until 3) {
                val month = row * 3 + column + 1
                MonthCell(
                    label = Month.of(month).getDisplayName(TextStyle.SHORT, locale),
                    isSelected = month == initialMonth,
                    onClick = { onMonthSelected(month) },
                    modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
      },
  )
}

@Composable
private fun MonthCell(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          modifier
              .clip(CircleShape)
              .background(
                  if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
              )
              .clickable(onClick = onClick)
              .padding(vertical = 12.dp),
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color =
            if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
  }
}
