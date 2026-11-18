package ca.justinmo.countdown.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.data.CountdownRepository
import ca.justinmo.countdown.util.Async
import ca.justinmo.countdown.util.TimeCalculator
import ca.justinmo.countdown.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CountdownUiState(
    val items: List<CountDownEntity> = emptyList(),
    val userMessage: Int? = null,
    val time: Long = 0,
)

@HiltViewModel
class CountdownListViewModel
@Inject
constructor(
    @ApplicationContext private val application: Context,
    countdownRepository: CountdownRepository,
) : ViewModel() {

  private val _time: MutableStateFlow<Long> = MutableStateFlow(0)

  private val _userMessage: MutableStateFlow<Int?> = MutableStateFlow(null)

  private val _sortedCountdownAsync: Flow<Async<List<CountDownEntity>>> =
      combine(countdownRepository.getCountdownsStream()) { countdowns ->
            sortCountdowns(countdowns)
          }
          .map { Async.Success(it) }
          .catch<Async<List<CountDownEntity>>> { cause ->
            emit(Async.Error(R.string.loading_error))
          }

  private fun sortCountdowns(countdowns: Array<List<CountDownEntity>>): List<CountDownEntity> {
    val timeCalculator = TimeCalculator(application)
    return countdowns
        .flatMap { it }
        .map {
          timeCalculator.setTime(it, ticker = 0)
          Pair(it, timeCalculator.durationMillis())
        }
        .sortedWith(
            compareBy(
                { it.second < 0 },
                { it.second },
            )
        )
        .map { it.first }
  }

  fun snackbarMessageShown() {
    _userMessage.value = null
  }

  fun showSnackbarMessage(message: Int) {
    _userMessage.value = message
  }

  fun updateTime(time: Long) {
    _time.value = time
  }

  val uiState: StateFlow<CountdownUiState> =
      combine(_userMessage, _sortedCountdownAsync, _time) { userMessage, countdowns, time ->
            when (countdowns) {
              Async.Loading -> {
                CountdownUiState()
              }

              is Async.Error -> {
                CountdownUiState(userMessage = countdowns.errorMessage)
              }

              is Async.Success -> {
                CountdownUiState(items = countdowns.data, userMessage = userMessage, time = time)
              }
            }
          }
          .stateIn(
              scope = viewModelScope,
              started = WhileUiSubscribed,
              initialValue = CountdownUiState(),
          )
}
