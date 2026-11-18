package ca.justinmo.countdown

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CountdownTheme(content: @Composable (ColorScheme) -> Unit) {
  val colorScheme =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isSystemInDarkTheme()) {
          dynamicDarkColorScheme(LocalContext.current)
        } else {
          dynamicLightColorScheme(LocalContext.current)
        }
      } else if (isSystemInDarkTheme()) {
        darkColorScheme()
      } else {
        lightColorScheme()
      }
  MaterialTheme(colorScheme = colorScheme) { content(colorScheme) }
}
