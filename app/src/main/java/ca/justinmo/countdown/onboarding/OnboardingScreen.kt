@file:OptIn(ExperimentalMaterial3Api::class)

package ca.justinmo.countdown.onboarding

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.justinmo.countdown.R

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val activity = LocalActivity.current
  OnboardingContent(
      onGetStarted = onGetStarted,
      onClose = { activity?.finish() },
      modifier = modifier,
  )
}

@Composable
fun OnboardingContent(
    onGetStarted: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
            title = { Text(text = stringResource(R.string.main_list_title)) },
            navigationIcon = {
              IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, stringResource(R.string.onboarding_skip))
              }
            },
        )
      },
  ) { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp),
    ) {
      Column(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
      ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Text(
            text = stringResource(R.string.onboarding_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Column(
            modifier = Modifier.padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          stringArrayResource(R.array.onboarding_examples).forEach { example ->
            Text(
                text = "• $example",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
          }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OnboardingBullet(
              icon = Icons.Filled.Add,
              text = stringResource(R.string.onboarding_point_create),
          )
          OnboardingBullet(
              icon = Icons.AutoMirrored.Filled.List,
              text = stringResource(R.string.onboarding_point_track),
          )
          OnboardingBullet(
              icon = Icons.Filled.Refresh,
              text = stringResource(R.string.onboarding_point_yearly),
          )
          OnboardingBullet(
              icon = Icons.Filled.Lock,
              text = stringResource(R.string.onboarding_privacy),
          )
        }
      }
      Button(
          onClick = onGetStarted,
          modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
      ) {
        Text(text = stringResource(R.string.onboarding_get_started))
      }
    }
  }
}

@Composable
private fun OnboardingBullet(icon: ImageVector, text: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
    )
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
  }
}

@Preview
@Composable
private fun OnboardingContentPreview() {
  MaterialTheme { OnboardingContent(onGetStarted = {}, onClose = {}) }
}
