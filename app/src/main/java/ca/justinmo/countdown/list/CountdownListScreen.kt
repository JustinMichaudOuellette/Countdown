package ca.justinmo.countdown.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.util.CountdownsTopAppBar
import java.time.LocalDateTime

@Composable
fun CountdownListScreen(
    @StringRes userMessage: Int,
    onAddCountdown: () -> Unit,
    onCountdownClick: (CountDownEntity) -> Unit,
    onUserMessageDisplayed: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CountdownListViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = { CountdownsTopAppBar(onInfo = onInfo) },
      floatingActionButton = {
        SmallFloatingActionButton(onClick = onAddCountdown) {
          Icon(Icons.Filled.Add, stringResource(id = R.string.add_countdown))
        }
      },
  ) { paddingValues ->
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Update the screen on when needed
    LaunchedEffect(Unit) {
      while (true) {
        withFrameNanos { time -> viewModel.updateTime(time) }
      }
    }

    CountdownsContent(
        uiState = uiState,
        onCountdownClicked = onCountdownClick,
        modifier = Modifier.padding(paddingValues),
    )

    // Check for user messages to display on the screen
    uiState.userMessage?.let { message ->
      val snackbarText = stringResource(message)
      LaunchedEffect(snackbarHostState, viewModel, message, snackbarText) {
        snackbarHostState.showSnackbar(snackbarText)
        viewModel.snackbarMessageShown()
      }
    }

    // Check if there's a userMessage to show to the user
    val currentOnUserMessageDisplayed by rememberUpdatedState(onUserMessageDisplayed)
    LaunchedEffect(userMessage) {
      if (userMessage != 0) {
        viewModel.showSnackbarMessage(userMessage)
        currentOnUserMessageDisplayed()
      }
    }
  }
}

@Composable
private fun CountdownsContent(
    uiState: CountdownUiState,
    onCountdownClicked: (CountDownEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    LazyColumn {
      items(uiState.items) { countdown ->
        CountdownItem(
            countdown = countdown,
            time = uiState.time,
            onClick = { onCountdownClicked(countdown) },
        )
      }
    }
  }
}

// Preview

fun createCountdown(
    emoji: String,
    title: String = "",
    deltaYear: Long = 0L,
    deltaMonth: Long = 0L,
    deltaDay: Long = 0L,
    deltaHour: Long = 0L,
    deltaMinute: Long = 0L,
    deltaSeconds: Long = 0L,
    yearly: Boolean = false,
): CountDownEntity {
  val time =
      LocalDateTime.now()
          .plusYears(deltaYear)
          .plusMonths(deltaMonth)
          .plusDays(deltaDay)
          .plusHours(deltaHour)
          .plusMinutes(deltaMinute)
          .plusSeconds(deltaSeconds)
  return CountDownEntity(
      id = (emoji.hashCode() + title.hashCode() + time.hashCode()).toString(),
      emoji = emoji,
      title = title,
      targetYear = time.year,
      targetMonth = time.monthValue,
      targetDay = time.dayOfMonth,
      targetHour = time.hour,
      targetMinute = time.minute,
      yearly = yearly,
  )
}

@Preview
@Composable
private fun CountdownsContentPreview() {
  MaterialTheme {
    Surface {
      CountdownsContent(
          uiState =
              CountdownUiState(
                  items =
                      listOf(
                          createCountdown(
                              emoji = "✈️",
                              title = "Trip to Paris",
                              deltaDay = 1,
                              deltaMonth = 2,
                              deltaYear = 3,
                          ),
                          createCountdown(
                              emoji = "🦋",
                              title = "Retirement",
                              deltaDay = 0,
                              deltaMonth = 1,
                              deltaYear = 1,
                          ),
                      )
              ),
          onCountdownClicked = { countdown -> },
      )
    }
  }
}
