package ca.justinmo.countdown.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.justinmo.countdown.CountdownDestinationsArgs
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_DAY_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_HOUR_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_MINUTE_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_MONTH_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_TITLE_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_YEARLY_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_YEAR_ARG
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.data.CountdownRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Time of day used when calendar data does not provide one. */
private const val DefaultHour = 9

data class AddEditCountdownUiState(
    val countdown: CountDownEntity = CountDownEntity.createEmpty(),
    val userMessage: Int? = null,
    val focusedField: CountdownField? = null,
    val isSaved: Boolean = false,
)

enum class CountdownField {
  EMOJI,
  DATE,
  TIME,
}

@HiltViewModel
class AddEditCountdownViewModel
@Inject
constructor(
    private val countdownRepository: CountdownRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val countdownId: String? = savedStateHandle[CountdownDestinationsArgs.ID_ARG]
  private val _uiState = MutableStateFlow(AddEditCountdownUiState())

  val uiState: StateFlow<AddEditCountdownUiState> = _uiState.asStateFlow()

  init {
    if (countdownId != null) {
      loadCountdown(countdownId)
    } else {
      applyCalendarPrefill(savedStateHandle)
    }
  }

  /**
   * Pre-fills a new countdown with an event that was shared into the app. The emoji is left empty on
   * purpose so the user picks one before saving.
   */
  private fun applyCalendarPrefill(savedStateHandle: SavedStateHandle) {
    val year = savedStateHandle.get<Int>(PREFILL_YEAR_ARG) ?: return
    if (year <= 0) return

    val prefilled =
        runCatching {
              CountDownEntity(
                  id = "",
                  emoji = "",
                  title =
                      savedStateHandle
                          .get<String>(PREFILL_TITLE_ARG)
                          ?.trim()
                          ?.takeIf { it.isNotEmpty() },
                  targetYear = year,
                  targetMonth = savedStateHandle.get<Int>(PREFILL_MONTH_ARG) ?: 1,
                  targetDay = savedStateHandle.get<Int>(PREFILL_DAY_ARG) ?: 1,
                  targetHour = savedStateHandle.get<Int>(PREFILL_HOUR_ARG) ?: DefaultHour,
                  targetMinute = savedStateHandle.get<Int>(PREFILL_MINUTE_ARG) ?: 0,
                  yearly = savedStateHandle.get<Boolean>(PREFILL_YEARLY_ARG) ?: false,
              )
            }
            .getOrNull() ?: return

    _uiState.update { it.copy(countdown = prefilled) }
  }

  // Called when clicking on fab.
  fun saveCountdown() {
    val countdown = uiState.value.countdown
    if (countdown.emoji.isBlank()) {
      _uiState.update { it.copy(focusedField = CountdownField.EMOJI) }
      return
    } else if (countdown.targetDate() == null) {
      _uiState.update { it.copy(focusedField = CountdownField.DATE) }
      return
    } else if (countdown.targetTime() == null) {
      _uiState.update { it.copy(focusedField = CountdownField.TIME) }
      return
    }

    maybeTrimTitle()

    if (countdownId == null) {
      createCountdown()
    } else {
      updateCountdown()
    }
  }

  fun focusOnField(field: CountdownField?) {
    _uiState.update { it.copy(focusedField = field) }
  }

  fun snackbarMessageShown() {
    _uiState.update { it.copy(userMessage = null) }
  }

  fun updateEmoji(emoji: String) {
    _uiState.update { it.copy(countdown = it.countdown.copy(emoji = emoji)) }
  }

  fun updateTitle(title: String) {
    _uiState.update { it.copy(countdown = it.countdown.copy(title = title)) }
  }

  fun updateTargetData(year: Int, month: Int, day: Int) {
    _uiState.update {
      it.copy(
          countdown =
              it.countdown.copy(
                  targetYear = year,
                  targetMonth = month,
                  targetDay = day,
                  targetHour = if (it.countdown.targetHour < 0) 9 else it.countdown.targetHour,
                  targetMinute =
                      if (it.countdown.targetMinute < 0) 0 else it.countdown.targetMinute,
              )
      )
    }
  }

  fun updateTargetTime(hour: Int, minute: Int) {
    _uiState.update {
      it.copy(countdown = it.countdown.copy(targetHour = hour, targetMinute = minute))
    }
  }

  private fun createCountdown() =
      viewModelScope.launch {
        countdownRepository.createCountdown(uiState.value.countdown)
        _uiState.update { it.copy(isSaved = true) }
      }

  fun updateCountdown() {
    if (countdownId == null) {
      throw RuntimeException("updateCountdown() was called but countdown is new.")
    }
    viewModelScope.launch {
      countdownRepository.updateCountdown(uiState.value.countdown)
      _uiState.update { it.copy(isSaved = true) }
    }
  }

  private fun loadCountdown(id: String) {
    viewModelScope.launch {
      countdownRepository.getCountdown(id).let { countdown ->
        if (countdown != null) {
          _uiState.update { it.copy(countdown = countdown) }
        }
      }
    }
  }

  fun updateYearly(yearly: Boolean) {
    _uiState.update { it.copy(countdown = it.countdown.copy(yearly = yearly)) }
  }

  fun maybeTrimTitle() {
    val title = _uiState.value.countdown.title ?: return
    val trimmedTitle = title.trim()
    if (title != trimmedTitle) {
      updateTitle(trimmedTitle)
    }
  }
}
