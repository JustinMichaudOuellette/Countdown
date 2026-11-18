package ca.justinmo.countdown.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.data.CountdownRepository
import ca.justinmo.countdown.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class OnboardingSuggestionUi(
    val id: String,
    val emoji: String,
    val title: String,
    /** `null` while the suggestion still needs a date. */
    val countdown: CountDownEntity?,
    val isSelected: Boolean,
)

data class OnboardingUiState(
    val suggestions: List<OnboardingSuggestionUi> = emptyList(),
    val time: Long = 0L,
    val isFinishing: Boolean = false,
    val userMessage: Int? = null,
    /** Suggestion for which the year picker is currently shown. */
    val yearPickerSuggestionId: String? = null,
    /** Year the year picker starts on. */
    val yearPickerInitialYear: Int? = null,
    /** Suggestion for which the month picker is currently shown. */
    val monthPickerSuggestionId: String? = null,
    /** Year whose months are shown by the month picker. */
    val monthPickerYear: Int? = null,
    /** Month highlighted when the month picker opens. */
    val monthPickerInitialMonth: Int? = null,
    /** Suggestion for which the date picker is currently shown. */
    val datePickerSuggestionId: String? = null,
    /** Year chosen for [datePickerSuggestionId]. */
    val datePickerYear: Int? = null,
    /** Month chosen for [datePickerSuggestionId]. */
    val datePickerMonth: Int? = null,
)

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    @ApplicationContext application: Context,
    private val onboardingRepository: OnboardingRepository,
    private val countdownRepository: CountdownRepository,
) : ViewModel() {

  /** Resolves the title of every countdown a suggestion creates. */
  private val titleOf: (Int) -> String = application::getString

  /** Countdowns of the suggestions that don't need a date, which never change once built. */
  private val fixedDateCountdowns: Map<CountdownSuggestion, CountDownEntity> =
      CountdownSuggestion.entries
          .filterNot { it.requiresDate }
          .associateWith { it.previewCountdown(titleOf) }

  private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
  private val pickedDates = MutableStateFlow<Map<String, LocalDate>>(emptyMap())
  private val pickedYears = MutableStateFlow<Map<String, Int>>(emptyMap())
  private val dateSelection = MutableStateFlow<DateSelection?>(null)
  private val time = MutableStateFlow(0L)
  private val isFinishing = MutableStateFlow(false)
  private val userMessage = MutableStateFlow<Int?>(null)

  private val selectionState =
      combine(selectedIds, pickedDates, pickedYears, dateSelection) {
              selected,
              dates,
              years,
              selection ->
            SelectionState(selected, dates, years, selection)
          }

  val uiState: StateFlow<OnboardingUiState> =
      combine(selectionState, time, isFinishing, userMessage) { state, tick, finishing, message ->
            val suggestions = buildSuggestions(state.selectedIds, state.pickedDates)
            val yearStep = state.dateSelection as? DateSelection.Year
            val monthStep = state.dateSelection as? DateSelection.Month
            val dayStep = state.dateSelection as? DateSelection.Day
            val yearPickerSuggestion = suggestions.firstOrNull { it.id == yearStep?.suggestionId }
            val monthPickerSuggestion = suggestions.firstOrNull { it.id == monthStep?.suggestionId }
            OnboardingUiState(
                suggestions = suggestions,
                time = tick,
                isFinishing = finishing,
                userMessage = message,
                yearPickerSuggestionId = yearPickerSuggestion?.id,
                yearPickerInitialYear =
                    yearPickerSuggestion?.let { suggestion ->
                      suggestion.countdown?.targetYear
                          ?: state.pickedYears[suggestion.id]
                          ?: LocalDate.now().year
                    },
                monthPickerSuggestionId = monthPickerSuggestion?.id,
                monthPickerYear = monthStep?.year,
                monthPickerInitialMonth =
                    monthPickerSuggestion?.let { suggestion ->
                      suggestion.countdown?.targetMonth ?: LocalDate.now().monthValue
                    },
                datePickerSuggestionId = dayStep?.suggestionId,
                datePickerYear = dayStep?.year,
                datePickerMonth = dayStep?.month,
            )
          }
          .stateIn(
              scope = viewModelScope,
              started = WhileUiSubscribed,
              initialValue =
                  OnboardingUiState(suggestions = buildSuggestions(emptySet(), emptyMap())),
          )

  private fun buildSuggestions(
      selected: Set<String>,
      dates: Map<String, LocalDate>,
  ): List<OnboardingSuggestionUi> =
      CountdownSuggestion.entries.map { suggestion ->
        val date = dates[suggestion.id]
        val countdown =
            when {
              !suggestion.requiresDate -> fixedDateCountdowns.getValue(suggestion)
              // The suggestion cannot be added until its date is known.
              date == null -> null
              else -> suggestion.previewCountdown(titleOf, date)
            }
        OnboardingSuggestionUi(
            id = suggestion.id,
            emoji = suggestion.emoji,
            title = titleOf(suggestion.titleRes),
            countdown = countdown,
            isSelected = countdown != null && suggestion.id in selected,
        )
      }

  /**
   * Every countdown a suggestion adds, empty while a suggestion that needs a date is still missing
   * one.
   */
  private fun countdownsFor(
      suggestion: CountdownSuggestion,
      dates: Map<String, LocalDate>,
  ): List<CountDownEntity> =
      if (!suggestion.requiresDate) {
        listOf(fixedDateCountdowns.getValue(suggestion))
      } else {
        dates[suggestion.id]?.let { date -> suggestion.toCountdowns(titleOf, date) }.orEmpty()
      }

  fun updateTime(time: Long) {
    this.time.value = time
  }

  /** Row click: fixed-date suggestions are toggled, the others ask for their year first. */
  fun onSuggestionClicked(id: String) {
    val suggestion = suggestionFor(id) ?: return
    if (suggestion.requiresDate) {
      dateSelection.value = DateSelection.Year(id)
    } else {
      toggleSelection(id)
    }
  }

  fun onSuggestionCheckedChange(id: String, isChecked: Boolean) {
    val suggestion = suggestionFor(id) ?: return
    when {
      !isChecked -> selectedIds.update { it - id }
      suggestion.requiresDate && id !in pickedDates.value ->
          dateSelection.value = DateSelection.Year(id)
      else -> selectedIds.update { it + id }
    }
  }

  /** Called once a year has been picked; moves on to the month picker for that year. */
  fun onYearSelected(id: String, year: Int) {
    pickedYears.update { it + (id to year) }
    dateSelection.value = DateSelection.Month(id, year)
  }

  /** Called once a month has been picked; moves on to the date picker for that month. */
  fun onMonthSelected(id: String, month: Int) {
    val step = dateSelection.value
    if (step !is DateSelection.Month || step.suggestionId != id) {
      return
    }
    dateSelection.value = DateSelection.Day(id, step.year, month)
  }

  fun onDateSelected(id: String, dateMillis: Long) {
    // DatePicker reports the selected day as midnight UTC.
    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    pickedDates.update { it + (id to date) }
    pickedYears.update { it + (id to date.year) }
    selectedIds.update { it + id }
    dateSelection.value = null
  }

  fun onDateSelectionDismissed() {
    dateSelection.value = null
  }

  fun snackbarMessageShown() {
    userMessage.value = null
  }

  /**
   * Creates the selected countdowns, marks onboarding as completed, and reports the result.
   *
   * A suggestion can create more than one countdown, so all of them are added.
   */
  fun finish(onFinished: (hasCountdown: Boolean) -> Unit) {
    if (isFinishing.value) {
      return
    }
    isFinishing.value = true

    viewModelScope.launch {
      try {
        CountdownSuggestion.entries
            .filter { it.id in selectedIds.value }
            .flatMap { countdownsFor(it, pickedDates.value) }
            .forEach { countdown -> countdownRepository.createCountdown(countdown) }
        onboardingRepository.setCompleted()
        onFinished(countdownRepository.getCountdownCount() > 0)
      } catch (e: Exception) {
        isFinishing.value = false
        userMessage.value = R.string.onboarding_suggestions_error
      }
    }
  }

  private fun toggleSelection(id: String) {
    selectedIds.update { if (id in it) it - id else it + id }
  }

  private fun suggestionFor(id: String): CountdownSuggestion? =
      CountdownSuggestion.entries.firstOrNull { it.id == id }
}

/** The step of the year, then month, then day selection flow that is currently shown. */
private sealed interface DateSelection {
  val suggestionId: String

  data class Year(override val suggestionId: String) : DateSelection

  data class Month(override val suggestionId: String, val year: Int) : DateSelection

  data class Day(override val suggestionId: String, val year: Int, val month: Int) : DateSelection
}

private data class SelectionState(
    val selectedIds: Set<String>,
    val pickedDates: Map<String, LocalDate>,
    val pickedYears: Map<String, Int>,
    val dateSelection: DateSelection?,
)
