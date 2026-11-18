package ca.justinmo.countdown

import android.app.Activity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import ca.justinmo.countdown.calendar.CalendarEvent
import ca.justinmo.countdown.detail.CountdownDetailScreen
import ca.justinmo.countdown.editor.AddEditCountdownScreen
import ca.justinmo.countdown.info.InfoScreen
import ca.justinmo.countdown.list.CountdownListScreen
import ca.justinmo.countdown.loading.LoadingScreen
import ca.justinmo.countdown.onboarding.OnboardingScreen
import ca.justinmo.countdown.onboarding.OnboardingSuggestionsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun CountdownNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = CountdownDestinations.LOADING_ROUTE,
    navActions: CountdownNavigationActions =
        remember(navController) { CountdownNavigationActions(navController) },
    initialCalendarEvent: CalendarEvent? = null,
    incomingCalendarEvents: Flow<CalendarEvent> = emptyFlow(),
) {
  val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
  currentNavBackStackEntry?.destination?.route ?: startDestination

  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    // List of countdowns
    composable(
        CountdownDestinations.COUNTDOWNS_ROUTE,
        arguments =
            listOf(
                navArgument(USER_MESSAGE_ARG) {
                  type = NavType.IntType
                  defaultValue = 0
                }
            ),
    ) { entry ->
      CountdownListScreen(
          userMessage = entry.arguments?.getInt(USER_MESSAGE_ARG)!!,
          onUserMessageDisplayed = { entry.arguments?.putInt(USER_MESSAGE_ARG, 0) },
          onAddCountdown = { navActions.navigateToAddEditCountdown(id = null) },
          onCountdownClick = { countdown -> navActions.navigateToCountdownDetail(countdown.id) },
          onInfo = { navActions.navigateToInfo() },
      )
    }
    // Add or edit countdown
    verticalNavGraph(
        CountdownDestinations.ADD_EDIT_COUNTDOWN_ROUTE,
        arguments =
            listOf(
                navArgument(ID_ARG) {
                  type = NavType.StringType
                  nullable = true
                },
                navArgument(IS_FIRST_COUNTDOWN_ARG) {
                  type = NavType.BoolType
                  defaultValue = false
                },
                navArgument(PREFILL_TITLE_ARG) {
                  type = NavType.StringType
                  nullable = true
                },
                navArgument(PREFILL_YEAR_ARG) {
                  type = NavType.IntType
                  defaultValue = 0
                },
                navArgument(PREFILL_MONTH_ARG) {
                  type = NavType.IntType
                  defaultValue = 0
                },
                navArgument(PREFILL_DAY_ARG) {
                  type = NavType.IntType
                  defaultValue = 0
                },
                navArgument(PREFILL_HOUR_ARG) {
                  type = NavType.IntType
                  defaultValue = 9
                },
                navArgument(PREFILL_MINUTE_ARG) {
                  type = NavType.IntType
                  defaultValue = 0
                },
                navArgument(PREFILL_YEARLY_ARG) {
                  type = NavType.BoolType
                  defaultValue = false
                },
            ),
    ) { entry ->
      AddEditCountdownScreen(
          onSaved = { isEdit ->
            if (isEdit) {
              navActions.popBackStack()
            } else {
              navActions.navigateToCountdownListAndClearBackstack()
            }
          },
          onBack = { navController.popBackStack() },
          onInfo = { navActions.navigateToInfo() },
          isFirstCountdown = entry.arguments?.getBoolean(IS_FIRST_COUNTDOWN_ARG) ?: false,
      )
    }
    // Countdown details
    horizontalNavGraph(
        CountdownDestinations.COUNTDOWN_DETAIL_ROUTE,
        arguments = listOf(navArgument(ID_ARG) { type = NavType.StringType }),
    ) { entry ->
      CountdownDetailScreen(
          onEdit = { navActions.navigateToAddEditCountdown(entry.arguments?.getString(ID_ARG)!!) },
          onBack = { navController.popBackStack() },
          onDelete = { wasLastCountdown ->
            if (wasLastCountdown) {
              // Nothing left to show in the list, so start over with the first countdown setup
              navActions.navigateToAddEditCountdownAndClearBackstack()
            } else {
              navActions.popBackStack()
            }
          },
      )
    }
    // Initial screen that checks for existing countdowns
    composable(CountdownDestinations.LOADING_ROUTE) {
      LoadingScreen(
          callback = { hasCountdown ->
            if (hasCountdown) {
              navActions.navigateToCountdownListAndClearBackstack()
            } else {
              // Onboarding is shown on every launch that has no countdowns
              navActions.navigateToOnboardingAndClearBackstack()
            }
            // A calendar event that started the app opens on top of the list (or onboarding).
            if (initialCalendarEvent != null) {
              navActions.navigateToAddEditCountdown(prefill = initialCalendarEvent)
            }
          }
      )
    }
    // First-run onboarding
    composable(CountdownDestinations.ONBOARDING_ROUTE) {
      OnboardingScreen(onGetStarted = { navActions.navigateToOnboardingSuggestions() })
    }
    // Onboarding step where the user picks countdown suggestions
    horizontalNavGraph(CountdownDestinations.ONBOARDING_SUGGESTIONS_ROUTE) {
      OnboardingSuggestionsScreen(
          onFinished = { hasCountdown ->
            if (hasCountdown) {
              navActions.navigateToCountdownListAndClearBackstack()
            } else {
              navActions.navigateToAddEditCountdownAndClearBackstack()
            }
          }
      )
    }
    // About screen
    horizontalNavGraph(CountdownDestinations.INFO_ROUTE) {
      InfoScreen(onBack = { navController.popBackStack() })
    }
  }

  // Calendar events received while the app is already open open a pre-filled editor.
  LaunchedEffect(incomingCalendarEvents) {
    incomingCalendarEvents.collect { event ->
      navActions.navigateToAddEditCountdown(prefill = event)
    }
  }
}

fun NavGraphBuilder.horizontalNavGraph(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
  this@horizontalNavGraph.composable(
      route = route,
      arguments = arguments,
      deepLinks = deepLinks,
      enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(350),
        )
      },
      exitTransition = { fadeOut(tween(350)) },
      popEnterTransition = { fadeIn(tween(350)) },
      popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(350),
        )
      },
      content = content,
  )
}

fun NavGraphBuilder.verticalNavGraph(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
  this@verticalNavGraph.composable(
      route = route,
      arguments = arguments,
      deepLinks = deepLinks,
      enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = tween(350),
        )
      },
      exitTransition = { fadeOut(tween(350)) },
      popEnterTransition = { fadeIn(tween(350)) },
      popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = tween(350),
        )
      },
      content = content,
  )
}

// Keys for navigation
const val DELETE_RESULT_OK = Activity.RESULT_FIRST_USER + 1
