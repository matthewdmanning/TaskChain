@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.UserPreferences
import com.taskchain.ui.designsystem.TaskChainTheme
import com.taskchain.ui.designsystem.ProvideUiComponents
import com.taskchain.ui.designsystem.UiProvider
import com.taskchain.ui.designsystem.componentsFor
import kotlinx.coroutines.flow.first
import com.taskchain.ui.builder.RoutineBuilderRoute
import com.taskchain.ui.home.HomeShell
import com.taskchain.ui.home.HomeTab
import com.taskchain.ui.runner.RoutineRunnerRoute

/**
 * Hosts TaskChain navigation and installs the selected appearance and UI component provider.
 *
 * The provider follows persisted preference while CyberTheme remains available for unmigrated screens.
 *
 * @param container App dependency graph and persisted preference repositories.
 * @param openActiveRun Whether to navigate to a persisted active run on initial composition.
 * @param providedNavController Optional externally owned navigation controller for integration tests.
 */
@Composable
fun TaskChainApp(
    container: AppContainer,
    openActiveRun: Boolean = false,
    providedNavController: NavHostController? = null,
) {
    val preferences by container.preferences.observe().collectAsStateWithLifecycle(UserPreferences())
    val provider = UiProvider.fromId(preferences.selectedUiProvider)
    TaskChainTheme(preferences.selectedTheme, provider.id) {
        ProvideUiComponents(componentsFor(provider)) {
        CompositionLocalProvider(LocalVibrationIntensity provides preferences.vibrationIntensity) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = providedNavController ?: rememberNavController()
            var openActiveRunConsumed by rememberSaveable { mutableStateOf(false) }
            var selectedTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
            NavHost(navController = navController, startDestination = HOME_ROUTE) {
                composable(HOME_ROUTE) {
                    HomeShell(
                        container = container,
                        selectedTab = selectedTab,
                        onSelectedTabChange = { selectedTab = it },
                        onCreate = { navController.navigate(builderRoute(null)) },
                        onEdit = { navController.navigate(builderRoute(it.id)) },
                        onStart = { navController.navigate(runnerRoute(it.id)) },
                        onResume = { navController.navigate(runnerRoute(it)) },
                    )
                }
                composable(
                    route = BUILDER_ROUTE,
                    arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }),
                ) { entry ->
                    run {
                        RoutineBuilderRoute(
                            container = container,
                            animationsEnabled = preferences.screenTransitionsEnabled,
                            routineId = entry.arguments?.getString(ROUTINE_ID_ARGUMENT)?.let(::RoutineId),
                            onClose = navController::popBackStack,
                            onSaved = {
                                selectedTab = HomeTab.ROUTINES
                                navController.popBackStack()
                            },
                        )
                    }
                }
                composable(
                    route = RUNNER_ROUTE,
                    arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) { type = NavType.StringType }),
                ) { entry ->
                    run {
                        RoutineRunnerRoute(
                            container = container,
                            preferences = preferences,
                            routineId = RoutineId(requireNotNull(entry.arguments?.getString(ROUTINE_ID_ARGUMENT))),
                            onFinished = navController::popBackStack,
                        )
                    }
                }
            }
            LaunchedEffect(openActiveRun) {
                if (openActiveRun && !openActiveRunConsumed) {
                    container.activeRun.observeActive()
                        .first { it?.status == RunStatus.ACTIVE }
                        ?.let { activeRun ->
                            val restoredRunner = navController.currentBackStackEntry
                            val alreadyShowingActiveRun = restoredRunner?.destination?.route == RUNNER_ROUTE &&
                                restoredRunner.arguments?.getString(ROUTINE_ID_ARGUMENT) == activeRun.routineId.value
                            if (!alreadyShowingActiveRun) {
                                navController.navigate(runnerRoute(activeRun.routineId)) {
                                    launchSingleTop = true
                                }
                            }
                            openActiveRunConsumed = true
                        }
                }
            }
        }
    }
}
}

}

private const val HOME_ROUTE = "home"
private const val ROUTINE_ID_ARGUMENT = "routineId"
private const val BUILDER_ROUTE = "builder?routineId={$ROUTINE_ID_ARGUMENT}"
private const val RUNNER_ROUTE = "runner/{$ROUTINE_ID_ARGUMENT}"

/** Use this function when navigating to a new or existing builder route. */
private fun builderRoute(routineId: RoutineId?): String = routineId?.let { "builder?routineId=${it.value}" } ?: "builder"

/** Use this function when starting a routine by stable identity. */
private fun runnerRoute(routineId: RoutineId): String = "runner/${routineId.value}"
