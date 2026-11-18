package ca.justinmo.countdown

import android.net.Uri
import androidx.navigation.NavHostController
import ca.justinmo.countdown.CountdownDestinationsArgs.ID_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.IS_FIRST_COUNTDOWN_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_DAY_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_HOUR_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_MINUTE_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_MONTH_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_TITLE_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_YEARLY_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.PREFILL_YEAR_ARG
import ca.justinmo.countdown.CountdownDestinationsArgs.USER_MESSAGE_ARG
import ca.justinmo.countdown.CountdownScreens.ADD_EDIT_COUNTDOWN_SCREEN
import ca.justinmo.countdown.CountdownScreens.COUNTDOWNS_SCREEN
import ca.justinmo.countdown.CountdownScreens.COUNTDOWN_DETAIL_SCREEN
import ca.justinmo.countdown.CountdownScreens.INFO_SCREEN
import ca.justinmo.countdown.CountdownScreens.LOADING_SCREEN
import ca.justinmo.countdown.CountdownScreens.ONBOARDING_SCREEN
import ca.justinmo.countdown.CountdownScreens.ONBOARDING_SUGGESTIONS_SCREEN
import ca.justinmo.countdown.calendar.CalendarEvent

private object CountdownScreens {
  const val ADD_EDIT_COUNTDOWN_SCREEN = "addEditCountdown"
  const val COUNTDOWNS_SCREEN = "countdowns"
  const val LOADING_SCREEN = "loading"
  const val INFO_SCREEN = "info"
  const val COUNTDOWN_DETAIL_SCREEN = "countdownDetail"
  const val ONBOARDING_SCREEN = "onboarding"
  const val ONBOARDING_SUGGESTIONS_SCREEN = "onboardingSuggestions"
}

object CountdownDestinationsArgs {
  const val USER_MESSAGE_ARG = "userMessage"
  const val ID_ARG = "id"
  const val IS_FIRST_COUNTDOWN_ARG = "isFirstCountdown"

  /** Values used to pre-fill the editor with an event shared into the app. */
  const val PREFILL_TITLE_ARG = "prefillTitle"
  const val PREFILL_YEAR_ARG = "prefillYear"
  const val PREFILL_MONTH_ARG = "prefillMonth"
  const val PREFILL_DAY_ARG = "prefillDay"
  const val PREFILL_HOUR_ARG = "prefillHour"
  const val PREFILL_MINUTE_ARG = "prefillMinute"
  const val PREFILL_YEARLY_ARG = "prefillYearly"
}

object CountdownDestinations {
  const val ADD_EDIT_COUNTDOWN_ROUTE =
      "$ADD_EDIT_COUNTDOWN_SCREEN?$ID_ARG={$ID_ARG}&$IS_FIRST_COUNTDOWN_ARG={$IS_FIRST_COUNTDOWN_ARG}" +
          "&$PREFILL_TITLE_ARG={$PREFILL_TITLE_ARG}&$PREFILL_YEAR_ARG={$PREFILL_YEAR_ARG}" +
          "&$PREFILL_MONTH_ARG={$PREFILL_MONTH_ARG}&$PREFILL_DAY_ARG={$PREFILL_DAY_ARG}" +
          "&$PREFILL_HOUR_ARG={$PREFILL_HOUR_ARG}&$PREFILL_MINUTE_ARG={$PREFILL_MINUTE_ARG}" +
          "&$PREFILL_YEARLY_ARG={$PREFILL_YEARLY_ARG}"

  const val COUNTDOWNS_ROUTE = "$COUNTDOWNS_SCREEN?$USER_MESSAGE_ARG={$USER_MESSAGE_ARG}"
  const val LOADING_ROUTE = LOADING_SCREEN
  const val INFO_ROUTE = INFO_SCREEN
  const val COUNTDOWN_DETAIL_ROUTE = "$COUNTDOWN_DETAIL_SCREEN/{$ID_ARG}"
  const val ONBOARDING_ROUTE = ONBOARDING_SCREEN
  const val ONBOARDING_SUGGESTIONS_ROUTE = ONBOARDING_SUGGESTIONS_SCREEN
}

class CountdownNavigationActions(private val navController: NavHostController) {

  fun popBackStack() {
    navController.popBackStack()
  }

  fun navigateToCountdownDetail(countdownId: String) {
    navController.navigate("$COUNTDOWN_DETAIL_SCREEN/$countdownId")
  }

  /**
   * Opens the editor. When [prefill] is given the form starts with the event's title, date, time
   * and yearly flag, and the user only has to pick an icon before saving.
   */
  fun navigateToAddEditCountdown(
      id: String? = null,
      isFirstCountdown: Boolean = false,
      prefill: CalendarEvent? = null,
  ) {
    val arguments = mutableListOf<String>()
    if (id != null) {
      arguments += "$ID_ARG=${Uri.encode(id)}"
    }
    arguments += "$IS_FIRST_COUNTDOWN_ARG=$isFirstCountdown"
    if (prefill != null) {
      arguments += "$PREFILL_TITLE_ARG=${Uri.encode(prefill.title.orEmpty())}"
      arguments += "$PREFILL_YEAR_ARG=${prefill.year}"
      arguments += "$PREFILL_MONTH_ARG=${prefill.month}"
      arguments += "$PREFILL_DAY_ARG=${prefill.day}"
      arguments += "$PREFILL_HOUR_ARG=${prefill.hour}"
      arguments += "$PREFILL_MINUTE_ARG=${prefill.minute}"
      arguments += "$PREFILL_YEARLY_ARG=${prefill.yearly}"
    }
    navController.navigate("$ADD_EDIT_COUNTDOWN_SCREEN?" + arguments.joinToString("&"))
  }

  fun navigateToAddEditCountdownAndClearBackstack() {
    navigateAndClearBackstack("$ADD_EDIT_COUNTDOWN_SCREEN?$IS_FIRST_COUNTDOWN_ARG=true")
  }

  fun navigateToCountdownListAndClearBackstack() {
    navigateAndClearBackstack(COUNTDOWNS_SCREEN)
  }

  fun navigateToOnboardingAndClearBackstack() {
    navigateAndClearBackstack(CountdownDestinations.ONBOARDING_ROUTE)
  }

  fun navigateToOnboardingSuggestions() {
    navController.navigate(CountdownDestinations.ONBOARDING_SUGGESTIONS_ROUTE)
  }

  fun navigateAndClearBackstack(newDestinationRoute: String) {
    navController.navigate(newDestinationRoute) {
      popUpTo(navController.graph.id) {
        inclusive = true // Removes every screen from the backstack
      }
      launchSingleTop = true // Avoids multiple copies of the same destination
    }
  }

  fun navigateToInfo() {
    navController.navigate(CountdownDestinations.INFO_ROUTE)
  }

  fun navigateToCountdownList(deleteResultOk: Int) {
    navController.navigate(
        CountdownDestinations.COUNTDOWNS_ROUTE.let {
          if (deleteResultOk != 0) "$it?$USER_MESSAGE_ARG=$deleteResultOk" else it
        }
    )
  }
}
