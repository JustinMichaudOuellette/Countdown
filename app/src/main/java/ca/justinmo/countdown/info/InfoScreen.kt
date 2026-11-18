package ca.justinmo.countdown.info

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.justinmo.countdown.BuildConfig
import ca.justinmo.countdown.R
import ca.justinmo.countdown.util.HorizontalNavigationTopAppBar

private const val PROJECT_URL = "https://github.com/JustinMichaudOuellette/Countdown"
private const val AUTHOR_URL = "https://www.justinmo.ca"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Scaffold(topBar = { HorizontalNavigationTopAppBar(titleRes = R.string.about, onBack) }) {
      innerPadding ->
    val context = LocalContext.current
    val author = stringResource(R.string.copyright_author)
    val copyright = buildAnnotatedString {
      append(stringResource(R.string.copyright, BuildConfig.COPYRIGHT_YEAR))
      append(' ')
      withLink(
          LinkAnnotation.Url(
              url = AUTHOR_URL,
              styles =
                  TextLinkStyles(
                      style =
                          SpanStyle(
                              color = MaterialTheme.colorScheme.primary,
                              textDecoration = TextDecoration.Underline,
                          )
                  ),
          )
      ) {
        append(author)
      }
    }
    Column(
        modifier = modifier.padding(innerPadding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
          modifier = Modifier.padding(horizontal = 16.dp),
          textAlign = TextAlign.Center,
      )
      Text(
          text = "Version ${BuildConfig.VERSION_NAME}",
          style = MaterialTheme.typography.bodySmall,
      )
      Text(
          text = copyright,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
          textAlign = TextAlign.Center,
      )
      Button(
          onClick = {
            val sendIntent: Intent =
                Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, PROJECT_URL)
                  type = "text/plain"
                }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
          },
          modifier =
              Modifier.padding(start = 32.dp, top = 24.dp, end = 32.dp)
                  .fillMaxWidth()
                  .height(56.dp),
      ) {
        Icon(Filled.Share, contentDescription = stringResource(R.string.share))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.share),
            style = MaterialTheme.typography.titleMedium,
        )
      }
      Text(
          text = stringResource(R.string.app_description),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
          textAlign = TextAlign.Center,
      )
    }
  }
}

// Preview

@Preview
@Composable
fun InfoScreenPreview() {
  InfoScreen(onBack = {})
}
