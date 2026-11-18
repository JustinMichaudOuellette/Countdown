package ca.justinmo.countdown.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.editor.DatePickerModal
import ca.justinmo.countdown.list.CountdownItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSuggestionsScreen(
    onFinished: (hasCountdown: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Update the screen on when needed
  LaunchedEffect(Unit) {
    while (true) {
      withFrameNanos { time -> viewModel.updateTime(time) }
    }
  }

  OnboardingSuggestionsContent(
      uiState = uiState,
      onSuggestionClicked = viewModel::onSuggestionClicked,
      onSuggestionCheckedChange = viewModel::onSuggestionCheckedChange,
      onContinue = { viewModel.finish(onFinished) },
      snackbarHostState = snackbarHostState,
      modifier = modifier,
  )

  // Pick a year first, so that the date picker only has to deal with days
  val yearPickerSuggestionId = uiState.yearPickerSuggestionId
  val yearPickerInitialYear = uiState.yearPickerInitialYear
  if (yearPickerSuggestionId != null && yearPickerInitialYear != null) {
    YearPickerDialog(
        initialYear = yearPickerInitialYear,
        onYearSelected = { year -> viewModel.onYearSelected(yearPickerSuggestionId, year) },
        onDismiss = viewModel::onDateSelectionDismissed,
    )
  }

  // Then the month
  val monthPickerSuggestionId = uiState.monthPickerSuggestionId
  val monthPickerYear = uiState.monthPickerYear
  val monthPickerInitialMonth = uiState.monthPickerInitialMonth
  if (monthPickerSuggestionId != null &&
      monthPickerYear != null &&
      monthPickerInitialMonth != null) {
    MonthPickerDialog(
        year = monthPickerYear,
        initialMonth = monthPickerInitialMonth,
        onMonthSelected = { month -> viewModel.onMonthSelected(monthPickerSuggestionId, month) },
        onDismiss = viewModel::onDateSelectionDismissed,
    )
  }

  // Then the day within that month
  val datePickerSuggestionId = uiState.datePickerSuggestionId
  val datePickerYear = uiState.datePickerYear
  val datePickerMonth = uiState.datePickerMonth
  if (datePickerSuggestionId != null && datePickerYear != null && datePickerMonth != null) {
    val existingDate =
        uiState.suggestions
            .firstOrNull { it.id == datePickerSuggestionId }
            ?.countdown
            ?.targetDate()
            ?.toLocalDate()
    val selectedDate =
        existingDate
            ?.takeIf { it.year == datePickerYear && it.monthValue == datePickerMonth }
            ?.toUtcCalendar()
    val displayedMonthMillis =
        remember(datePickerYear, datePickerMonth) {
          LocalDate.of(datePickerYear, datePickerMonth, 15)
              .atTime(12, 0)
              .toInstant(ZoneOffset.UTC)
              .toEpochMilli()
        }
    val selectableDates =
        remember(datePickerYear, datePickerMonth) {
          object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
              val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
              return date.year == datePickerYear && date.monthValue == datePickerMonth
            }

            override fun isSelectableYear(year: Int): Boolean = year == datePickerYear
          }
        }
    DatePickerModal(
        selectedDate = selectedDate,
        displayedMonthMillis = displayedMonthMillis,
        yearRange = datePickerYear..datePickerYear,
        selectableDates = selectableDates,
        onDateSelected = { dateMillis ->
          if (dateMillis != null) {
            viewModel.onDateSelected(datePickerSuggestionId, dateMillis)
          }
        },
        onDismiss = viewModel::onDateSelectionDismissed,
    )
  }

  // Check for user messages to display on the screen
  uiState.userMessage?.let { message ->
    val snackbarText = stringResource(message)
    LaunchedEffect(snackbarHostState, message, snackbarText) {
      snackbarHostState.showSnackbar(snackbarText)
      viewModel.snackbarMessageShown()
    }
  }
}

@Composable
fun OnboardingSuggestionsContent(
    uiState: OnboardingUiState,
    onSuggestionClicked: (String) -> Unit,
    onSuggestionCheckedChange: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp),
    ) {
      Text(
          text = stringResource(R.string.onboarding_suggestions_title),
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(top = 32.dp),
      )
      Text(
          text = stringResource(R.string.onboarding_suggestions_message),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(top = 12.dp),
      )
      LazyColumn(modifier = Modifier.weight(1f).padding(top = 16.dp)) {
        items(uiState.suggestions, key = { it.id }) { suggestion ->
          val leadingContent: @Composable () -> Unit = {
            Checkbox(
                checked = suggestion.isSelected,
                onCheckedChange = { checked ->
                  onSuggestionCheckedChange(suggestion.id, checked)
                },
            )
          }
          val countdown = suggestion.countdown
          if (countdown != null) {
            CountdownItem(
                countdown = countdown,
                time = uiState.time,
                onClick = { onSuggestionClicked(suggestion.id) },
                leadingContent = leadingContent,
            )
          } else {
            CountdownItem(
                emoji = suggestion.emoji,
                title = suggestion.title,
                timeText = stringResource(R.string.onboarding_suggestion_pick_date),
                onClick = { onSuggestionClicked(suggestion.id) },
                leadingContent = leadingContent,
            )
          }
        }
      }
      Button(
          onClick = onContinue,
          enabled = !uiState.isFinishing,
          modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
      ) {
        Text(text = stringResource(R.string.onboarding_suggestions_continue))
      }
    }
  }
}

// Date helpers

/** The local calendar date of this calendar. */
private fun Calendar.toLocalDate(): LocalDate =
    LocalDate.of(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))

/**
 * A calendar for this date at midnight UTC, the representation expected by the Material3 date
 * picker so that the highlighted day does not shift with the device time zone.
 */
private fun LocalDate.toUtcCalendar(): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(year, monthValue - 1, dayOfMonth)
    }

// Preview

private fun previewSuggestion(
    suggestion: CountdownSuggestion,
    title: String,
    isSelected: Boolean = false,
    // Previews use the same title for every countdown, so the resource id can be ignored.
    countdown: CountDownEntity? =
        if (suggestion.requiresDate) null
        else suggestion.previewCountdown(titleOf = { title }),
): OnboardingSuggestionUi =
    OnboardingSuggestionUi(
        id = suggestion.id,
        emoji = suggestion.emoji,
        title = title,
        countdown = countdown,
        isSelected = isSelected,
    )

@Preview
@Composable
private fun OnboardingSuggestionsContentPreview() {
  MaterialTheme {
    OnboardingSuggestionsContent(
        uiState =
            OnboardingUiState(
                suggestions =
                    listOf(
                        previewSuggestion(CountdownSuggestion.HALLOWEEN, "Halloween"),
                        previewSuggestion(CountdownSuggestion.CHRISTMAS, "Christmas"),
                        previewSuggestion(CountdownSuggestion.VALENTINES_DAY, "Valentine's Day"),
                        previewSuggestion(CountdownSuggestion.WEDDING, "Wedding"),
                        previewSuggestion(CountdownSuggestion.BIRTHDAY, "Birthday"),
                    ),
            ),
        onSuggestionClicked = {},
        onSuggestionCheckedChange = { _, _ -> },
        onContinue = {},
    )
  }
}
