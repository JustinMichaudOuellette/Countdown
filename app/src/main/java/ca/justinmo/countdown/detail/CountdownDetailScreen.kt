package ca.justinmo.countdown.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.editor.dpToSp
import ca.justinmo.countdown.list.createCountdown
import ca.justinmo.countdown.util.CountdownTopAppBar
import ca.justinmo.countdown.util.TimeCalculator

@Composable
fun CountdownDetailScreen(
    onEdit: () -> Unit,
    onDelete: (wasLastCountdown: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: CountdownDetailViewModel = hiltViewModel(),
) {
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        CountdownTopAppBar(
            titleRes = R.string.countdown,
            onBack = onBack,
            onEdit = onEdit,
            onDelete = { viewModel.deleteCountdown(onDeleted = onDelete) },
        )
      },
  ) { paddingValues ->
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Update the screen on every frame
    LaunchedEffect(Unit) {
      while (true) {
        withFrameNanos { time -> viewModel.updateTime(System.nanoTime()) }
      }
    }

    uiState.countdown?.let { countdown ->
      CountdownContent(
          countdown = countdown,
          uiState.time,
          modifier = Modifier.padding(paddingValues),
      )
    }
  }
}

@Composable
fun CountdownContent(
    countdown: CountDownEntity,
    time: Long,
    modifier: Modifier,
) {
  Column(
      modifier
          .fillMaxWidth()
          .padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin))
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val context = LocalContext.current
    val timeCalculator = remember { TimeCalculator(context) }
    timeCalculator.setTime(countdown, time)
    Emoji(countdown)
    Title(countdown)
    TimeSummary(timeCalculator.formatShort())
    TargetDateTime(timeCalculator.formatTargetDateTime())
    BreakDown(timeCalculator.formatDetailedBreakdown())
  }
}

@Composable
fun Emoji(countdown: CountDownEntity) {
  Text(
      text = countdown.emoji,
      style = MaterialTheme.typography.displayLarge.copy(fontSize = dpToSp(192.dp)),
  )
}

@Composable
fun Title(countdown: CountDownEntity) {
  countdown.title?.let { title ->
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = title,
        style = MaterialTheme.typography.displayMedium,
    )
  }
}

@Composable
fun TimeSummary(summary: String) {
  Text(
      text = summary,
      style = MaterialTheme.typography.headlineSmall,
  )
}

@Composable
fun TargetDateTime(dateTime: String) {
  Text(
      text = dateTime,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
  )
}

@Composable
fun BreakDown(breakdown: List<String>) {
  Column(modifier = Modifier.padding(top = 32.dp).width(IntrinsicSize.Min)) {
    Text(
        text = stringResource(R.string.breakdown),
        style = MaterialTheme.typography.titleMedium,
    )
    breakdown.forEachIndexed { index, item ->
      Divider()
      Text(
          text = item,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.width(intrinsicSize = IntrinsicSize.Max),
      )
    }
  }
}

@Composable
fun Divider() {
  val height = (1f / LocalDensity.current.density).dp
  val color = MaterialTheme.colorScheme.outlineVariant
  Canvas(Modifier.height(height).fillMaxWidth()) {
    drawLine(
        color = color,
        strokeWidth = 1f,
        start = Offset(0f, 1f),
        end = Offset(size.width, 2f),
    )
  }
}

@Preview
@Composable
fun PreviewContent() {
  MaterialTheme {
    Surface {
      CountdownContent(
          countdown =
              createCountdown(
                  emoji = "🦋",
                  title = "Retirement",
                  deltaDay = 1,
                  deltaYear = 2,
                  deltaMonth = 3,
                  deltaHour = 4,
                  deltaMinute = 5,
                  deltaSeconds = 6,
                  yearly = false,
              ),
          time = 0L,
          modifier = Modifier,
      )
    }
  }
}
