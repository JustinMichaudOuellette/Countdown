package ca.justinmo.countdown.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.justinmo.countdown.R
import java.time.LocalDate

/** The current year and the 99 years before it, so that no future year can be picked. */
internal fun lastHundredYears(today: LocalDate = LocalDate.now()): IntRange =
    (today.year - 99)..today.year

/**
 * A dialog listing the years of [lastHundredYears] in a grid, shown before the month and date
 * pickers so that a year is picked explicitly instead of through the date picker header.
 */
@Composable
fun YearPickerDialog(
    initialYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
  val years = remember { lastHundredYears().toList() }
  val selectedYear = initialYear.coerceIn(years.first(), years.last())
  val gridState =
      rememberLazyGridState(initialFirstVisibleItemIndex = years.indexOf(selectedYear))
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = stringResource(R.string.onboarding_pick_year)) },
      text = {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxWidth().heightIn(max = 336.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(years, key = { it }) { year ->
            YearCell(
                year = year,
                isSelected = year == selectedYear,
                onClick = { onYearSelected(year) },
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
      },
  )
}

@Composable
private fun YearCell(year: Int, isSelected: Boolean, onClick: () -> Unit) {
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          Modifier.fillMaxWidth()
              .clip(CircleShape)
              .background(
                  if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
              )
              .clickable(onClick = onClick)
              .padding(vertical = 12.dp),
  ) {
    Text(
        text = year.toString(),
        style = MaterialTheme.typography.bodyLarge,
        color =
            if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
  }
}
