@file:OptIn(ExperimentalMaterial3Api::class)

package ca.justinmo.countdown.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ca.justinmo.countdown.CountdownTheme
import ca.justinmo.countdown.R

@Composable
fun CountdownsTopAppBar(onInfo: () -> Unit) {
  TopAppBar(
      title = { Text(text = stringResource(id = R.string.main_list_title)) },
      modifier = Modifier.fillMaxWidth(),
      actions = {
        IconButton(onClick = onInfo) {
          Icon(Icons.Filled.Info, stringResource(id = R.string.about))
        }
      },
  )
}

@Composable
fun HorizontalNavigationTopAppBar(titleRes: Int, onBack: () -> Unit) {
  TopAppBar(
      title = { Text(text = stringResource(titleRes)) },
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
        }
      },
      modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
fun CountdownTopAppBar(
    titleRes: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
  TopAppBar(
      title = { Text(text = stringResource(titleRes)) },
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
        }
      },
      actions = {
        IconButton(onClick = onEdit) {
          Icon(Icons.Filled.Edit, stringResource(id = R.string.menu_edit))
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete))
        }
      },
      modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
fun AddEditCountdownTopAppBar(
    isEdit: Boolean,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    isFirstCountdown: Boolean,
) {
  val title = stringResource(if (isEdit) R.string.edit_countdown else R.string.add_countdown)
  TopAppBar(
      title = { Text(text = title) },
      navigationIcon = {
        if (!isFirstCountdown) {
          IconButton(onClick = onBack) {
            Icon(Icons.Filled.Close, stringResource(id = R.string.menu_back))
          }
        }
      },
      actions = {
        if (isFirstCountdown) {
          IconButton(onClick = onInfo) {
            Icon(Icons.Filled.Info, stringResource(id = R.string.about))
          }
        }
      },
      modifier = Modifier.fillMaxWidth(),
  )
}

@Preview
@Composable
private fun CountdownsTopAppBarPreview() {
  CountdownTheme { Surface { CountdownsTopAppBar(onInfo = {}) } }
}

@Preview
@Composable
private fun HorizontalNavigationTopAppBarPreview() {
  CountdownTheme { Surface { HorizontalNavigationTopAppBar(R.string.countdown, onBack = {}) } }
}

@Preview
@Composable
private fun AddEditCountdownTopAppBarPreviewFirstCountdown() {
  CountdownTheme {
    Surface { AddEditCountdownTopAppBar(isEdit = true, {}, {}, isFirstCountdown = true) }
  }
}

@Preview
@Composable
private fun AddEditCountdownTopAppBarPreview() {
  CountdownTheme {
    Surface { AddEditCountdownTopAppBar(isEdit = true, {}, {}, isFirstCountdown = false) }
  }
}
