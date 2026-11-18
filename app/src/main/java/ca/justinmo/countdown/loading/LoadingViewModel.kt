package ca.justinmo.countdown.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.justinmo.countdown.data.CountdownRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel
@Inject
constructor(
    private val countdownRepository: CountdownRepository,
) : ViewModel() {

  /**
   * Reports whether any countdown exists, which decides where the app starts: the onboarding
   * screen when there is nothing to count down to, otherwise the list of countdowns.
   */
  fun resolveStartDestination(callback: (hasCountdown: Boolean) -> Unit) {
    viewModelScope.launch { callback(countdownRepository.getCountdownCount() > 0) }
  }
}
