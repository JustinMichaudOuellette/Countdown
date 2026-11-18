@file:OptIn(ExperimentalMaterial3Api::class)

package ca.justinmo.countdown.editor

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.justinmo.countdown.R
import ca.justinmo.countdown.data.CountDownEntity
import ca.justinmo.countdown.emojis.EmojiBottomSheet
import ca.justinmo.countdown.util.AddEditCountdownTopAppBar
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

@Composable
fun AddEditCountdownScreen(
    onSaved: (isEdit: Boolean) -> Unit,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditCountdownViewModel = hiltViewModel(),
    isFirstCountdown: Boolean,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        AddEditCountdownTopAppBar(
            isEdit = uiState.countdown.id.isNotEmpty(),
            onBack,
            onInfo,
            isFirstCountdown,
        )
      },
      floatingActionButton = {
        SmallFloatingActionButton(onClick = viewModel::saveCountdown) {
          Icon(Icons.Filled.Done, stringResource(id = R.string.save_countdown))
        }
      },
  ) { paddingValues ->
    AddEditCountDownContent(
        isFirstCountdown,
        viewModel,
        uiState,
        modifier = Modifier.padding(paddingValues),
    )

    LaunchedEffect(uiState.isSaved) {
      if (uiState.isSaved) {
        onSaved(uiState.countdown.id.isNotEmpty())
      }
    }
  }
}

@Composable
private fun AddEditCountDownContent(
    isFirstCountdown: Boolean,
    viewModel: AddEditCountdownViewModel,
    uiState: AddEditCountdownUiState,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier
          .fillMaxWidth()
          .padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin))
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    InstructionLabel(isFirstCountdown)
    EmojiField(viewModel, uiState)
    TitleField(viewModel, uiState)
    DatePickerFieldToModal(viewModel, uiState)
    TimePickerFieldToModal(viewModel, uiState)
    YearlyField(viewModel, uiState)
  }
}

@Composable
fun InstructionLabel(isFirstCountdown: Boolean) {
  if (isFirstCountdown) {
    Text(
        text = stringResource(R.string.instruction_label),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
    )
  }
}

@Composable
fun EmojiField(viewModel: AddEditCountdownViewModel, uiState: AddEditCountdownUiState) {
  val sheetState = rememberModalBottomSheetState()
  val coroutineScope = rememberCoroutineScope()

  OutlinedTextField(
      value = uiState.countdown.emoji,
      onValueChange = {},
      label = { Text(stringResource(R.string.icon)) },
      readOnly = true,
      textStyle =
          LocalTextStyle.current.copy(fontSize = dpToSp(112.dp), textAlign = TextAlign.Center),
      modifier =
          Modifier.width(192.dp).height(192.dp).wrapContentHeight().pointerInput(
              uiState.countdown.emoji
          ) {
            awaitEachGesture {
              // Modifier.clickable doesn't work for text fields, so we use
              // Modifier.pointerInput
              // in the Initial pass to observe events before the text field consumes them
              // in the Main pass.
              awaitFirstDown(pass = PointerEventPass.Initial)
              val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
              if (upEvent != null) {
                viewModel.focusOnField(CountdownField.EMOJI)
              }
            }
          },
  )

  if (uiState.focusedField == CountdownField.EMOJI) {
    EmojiBottomSheet(
        onDismissRequest = { viewModel.focusOnField(null) },
        sheetState = sheetState,
        onEmojiSelected = { emoji ->
          viewModel.updateEmoji(emoji)
          coroutineScope.launch {
            sheetState.hide()
            viewModel.focusOnField(null)
          }
        },
    )
  }
}

@Composable fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }

@Composable
fun TitleField(viewModel: AddEditCountdownViewModel, uiState: AddEditCountdownUiState) {
  OutlinedTextField(
      value = uiState.countdown.title.orEmpty(),
      onValueChange = { viewModel.updateTitle(it) },
      label = { Text(stringResource(R.string.title_optional)) },
      placeholder = { Text(stringResource(R.string.title)) },
      modifier =
          Modifier.fillMaxWidth()
              .padding(top = 8.dp)
              .onFocusChanged({ focusState ->
                if (!focusState.isFocused) {
                  viewModel.maybeTrimTitle()
                }
              }),
      singleLine = true,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
  )
}

@Composable
fun DatePickerFieldToModal(viewModel: AddEditCountdownViewModel, uiState: AddEditCountdownUiState) {
  val selectedDate = uiState.countdown.targetDate()

  val dateString =
      selectedDate
          ?.let { selectedDate ->
            if (uiState.countdown.yearly) {
              val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR
              DateUtils.formatDateTime(LocalContext.current, selectedDate.timeInMillis, flags)
            } else {
              DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
                  .format(selectedDate.time)
            }
          }
          .orEmpty()

  OutlinedTextField(
      value = dateString,
      onValueChange = {},
      label = { Text(stringResource(R.string.date)) },
      trailingIcon = {
        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.date))
      },
      readOnly = true,
      modifier =
          Modifier.fillMaxWidth().padding(top = 8.dp).pointerInput(selectedDate) {
            awaitEachGesture {
              // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
              // in the Initial pass to observe events before the text field consumes them
              // in the Main pass.
              awaitFirstDown(pass = PointerEventPass.Initial)
              val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
              if (upEvent != null) {
                viewModel.focusOnField(CountdownField.DATE)
              }
            }
          },
  )

  if (uiState.focusedField == CountdownField.DATE) {
    DatePickerModal(
        selectedDate = selectedDate,
        onDateSelected = { date ->
          if (date != null) {
            val localDateTime =
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(date),
                    ZoneId.of("Z"),
                )
            viewModel.updateTargetData(
                localDateTime.year,
                localDateTime.monthValue,
                localDateTime.dayOfMonth,
            )
          }
        },
        onDismiss = { viewModel.focusOnField(null) },
    )
  }
}

@Composable
fun DatePickerModal(
    selectedDate: Calendar?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    displayedMonthMillis: Long? = null,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
) {
  val datePickerState =
      rememberDatePickerState(
          initialSelectedDateMillis = selectedDate?.timeInMillis,
          initialDisplayedMonthMillis = displayedMonthMillis ?: selectedDate?.timeInMillis,
          yearRange = yearRange,
          selectableDates = selectableDates,
      )
  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
            onClick = {
              onDateSelected(datePickerState.selectedDateMillis)
              onDismiss()
            }
        ) {
          Text(stringResource(R.string.ok))
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
      colors = DatePickerDefaults.colors(),
  ) {
    DatePicker(state = datePickerState)
  }
}

@Composable
fun TimePickerFieldToModal(viewModel: AddEditCountdownViewModel, uiState: AddEditCountdownUiState) {
  val selectedTime = uiState.countdown.targetTime()
  val timeString =
      selectedTime?.let { selectedDate ->
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(selectedDate.time)
      } ?: ""

  OutlinedTextField(
      value = timeString,
      onValueChange = {},
      label = { Text(stringResource(R.string.time)) },
      trailingIcon = {
        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.time))
      },
      readOnly = true,
      modifier =
          Modifier.fillMaxWidth().padding(top = 8.dp).pointerInput(selectedTime) {
            awaitEachGesture {
              // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
              // in the Initial pass to observe events before the text field consumes them
              // in the Main pass.
              awaitFirstDown(pass = PointerEventPass.Initial)
              val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
              if (upEvent != null) {
                viewModel.focusOnField(CountdownField.TIME)
              }
            }
          },
  )

  if (uiState.focusedField == CountdownField.TIME) {
    TimePickerModal(
        countdown = uiState.countdown,
        onTimeSelected = { hour, minute -> viewModel.updateTargetTime(hour, minute) },
        onDismiss = { viewModel.focusOnField(null) },
    )
  }
}

@Composable
fun TimePickerModal(
    countdown: CountDownEntity,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
  val timePickerState = rememberTimePickerState()
  if (countdown.targetHour >= 0) {
    timePickerState.hour = countdown.targetHour
    timePickerState.minute = countdown.targetMinute
  }
  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
            onClick = {
              onTimeSelected(timePickerState.hour, timePickerState.minute)
              onDismiss()
            }
        ) {
          Text(stringResource(R.string.ok))
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
      colors = DatePickerDefaults.colors(),
  ) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      Column(modifier = Modifier.wrapContentWidth()) {
        Text(
            text = stringResource(R.string.select_time),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        TimePicker(state = timePickerState, modifier = Modifier.wrapContentWidth())
      }
    }
  }
}

@Composable
fun YearlyField(viewModel: AddEditCountdownViewModel, uiState: AddEditCountdownUiState) {
  Box(
      modifier =
          Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
            viewModel.updateYearly(!uiState.countdown.yearly)
          },
  ) {
    Text(
        text = stringResource(R.string.yearly),
        modifier = Modifier.align(Alignment.CenterStart),
    )
    Switch(
        checked = uiState.countdown.yearly,
        onCheckedChange = { viewModel.updateYearly(it) },
        modifier = Modifier.align(Alignment.CenterEnd),
    )
  }
}
