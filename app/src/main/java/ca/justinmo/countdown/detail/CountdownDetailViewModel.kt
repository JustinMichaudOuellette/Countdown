package ca.justinmo.countdown.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.justinmo.countdown.CountdownDestinationsArgs
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.data.CountdownRepository
import ca.justinmo.countdown.util.Async
import ca.justinmo.countdown.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CountdownDetailUiState(
    val countdown: CountDownEntity? = null,
    val userMessage: Int? = null,
    val time: Long = 0,
)

@HiltViewModel
class CountdownDetailViewModel
@Inject
constructor(
    private val countdownRepository: CountdownRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val countdownId: String = savedStateHandle[CountdownDestinationsArgs.ID_ARG]!!

  private val _userMessage: MutableStateFlow<Int?> = MutableStateFlow(null)

  private val _timeSecond: MutableStateFlow<Long> = MutableStateFlow(0)

  private val _countdownAsync =
      countdownRepository
          .getCountdownStream(countdownId)
          .map { handleCountdown(it) }
          .catch { emit(Async.Error(R.string.loading_error)) }

  val uiState: StateFlow<CountdownDetailUiState> =
      combine(_userMessage, _countdownAsync, _timeSecond) { userMessage, countdownAsync, time ->
            when (countdownAsync) {
              Async.Loading -> {
                CountdownDetailUiState()
              }
              is Async.Error -> {
                CountdownDetailUiState(
                    userMessage = countdownAsync.errorMessage,
                )
              }
              is Async.Success -> {
                CountdownDetailUiState(
                    countdown = countdownAsync.data,
                    userMessage = userMessage,
                    time = time,
                )
              }
            }
          }
          .stateIn(
              scope = viewModelScope,
              started = WhileUiSubscribed,
              initialValue = CountdownDetailUiState(),
          )

  fun deleteCountdown(onDeleted: (wasLastCountdown: Boolean) -> Unit) =
      viewModelScope.launch {
        countdownRepository.deleteCountdown(countdownId)
        onDeleted(countdownRepository.getCountdownCount() == 0)
      }

  fun snackbarMessageShown() {
    _userMessage.value = null
  }

  private fun showSnackbarMessage(message: Int) {
    _userMessage.value = message
  }

  fun updateTime(timeMs: Long) {
    _timeSecond.value = timeMs / 1000L
  }

  private fun handleCountdown(countdown: CountDownEntity?): Async<CountDownEntity?> {
    if (countdown == null) {
      return Async.Error(R.string.countdown_not_found)
    }
    return Async.Success(countdown)
  }
}
