package ca.justinmo.countdown.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoadingScreen(
    viewModel: LoadingViewModel = hiltViewModel(),
    callback: (hasCountdown: Boolean) -> Unit,
) {
  LaunchedEffect(Unit) { viewModel.resolveStartDestination(callback) }
}
