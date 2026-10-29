@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskchain.core.browsing.RoutinesRoute
import com.taskchain.core.browsing.RoutinesViewModel
import com.taskchain.core.designsystem.LocalVibrationIntensity
import com.taskchain.core.designsystem.ProvideUiComponents
import com.taskchain.core.designsystem.TaskChainTheme
import com.taskchain.core.designsystem.UiProvider
import com.taskchain.core.designsystem.componentsFor
import com.taskchain.core.navigation.HomeShell
import com.taskchain.core.navigation.HomeTab
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.UserPreferences
import com.taskchain.features.authoring.ui.RoutineBuilderRoute
import com.taskchain.features.authoring.ui.RoutineBuilderViewModel
import com.taskchain.features.execution.ui.ExecutionEntryDialog
import com.taskchain.features.execution.ui.ExecutionEntryViewModel
import com.taskchain.features.execution.ui.RoutineRunnerRoute
import com.taskchain.features.progress.ui.ProgressRoute
import com.taskchain.features.progress.ui.ProgressViewModel
import com.taskchain.features.scheduling.ui.TodayRoute
import com.taskchain.features.scheduling.ui.TodayViewModel
import com.taskchain.features.settings.ui.SettingsRoute
import com.taskchain.features.settings.ui.SettingsViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Hosts TaskChain navigation and installs persisted appearance and component preferences. */
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
                            val routinesViewModel: RoutinesViewModel = viewModel(key = "routines") {
                                RoutinesViewModel(container.routines, container.builtInLibrary::load, container::now)
                            }
                            val todayViewModel: TodayViewModel = viewModel(key = "today") {
                                TodayViewModel(
                                    container.routines,
                                    container.activeRun,
                                    container.completions,
                                    container.builtInLibrary,
                                    container.reminders,
                                    container::now,
                                    container.homeDayStartHour,
                                    container::newRunId,
                                )
                            }
                            val progressViewModel: ProgressViewModel = viewModel(key = "progress") {
                                ProgressViewModel(container.completions, container::now)
                            }
                            val settingsViewModel: SettingsViewModel = viewModel(key = "settings") {
                                SettingsViewModel(container.preferences)
                            }
                            val entryViewModel: ExecutionEntryViewModel = viewModel(key = "execution-entry") {
                                ExecutionEntryViewModel(container.activeRun)
                            }
                            val todayListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                            val routinesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                            HomeShell(
                                selectedTab = selectedTab,
                                onSelectedTabChange = { selectedTab = it },
                                todayContent = { padding ->
                                    TodayRoute(todayViewModel, padding, todayListState, { navController.navigate(builderRoute(null)) }, entryViewModel::requestStart) { run ->
                                        navController.navigate(runnerRoute(run.routineId))
                                    }
                                },
                                routinesContent = { padding ->
                                    RoutinesRoute(routinesViewModel, padding, routinesListState, { navController.navigate(builderRoute(null)) }, { navController.navigate(builderRoute(it.id)) }, entryViewModel::requestStart)
                                },
                                progressContent = { padding -> ProgressRoute(progressViewModel, padding) },
                                settingsContent = { padding -> SettingsRoute(settingsViewModel, padding) },
                            )
                            val entryState by entryViewModel.state.collectAsStateWithLifecycle()
                            val pendingStart = entryState.pendingStart
                            LaunchedEffect(pendingStart) {
                                pendingStart?.let {
                                    navController.navigate(runnerRoute(it.id))
                                    entryViewModel.startHandled()
                                }
                            }
                            ExecutionEntryDialog(entryViewModel) { navController.navigate(runnerRoute(it)) }
                        }
                        composable(
                            route = BUILDER_ROUTE,
                            arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }),
                        ) { entry ->
                            val routineId = entry.arguments?.getString(ROUTINE_ID_ARGUMENT)?.let(::RoutineId)
                            RoutineBuilderRoute(
                                createViewModel = {
                                    RoutineBuilderViewModel(
                                        container.routines,
                                        container.builtInLibrary::load,
                                        container.reminders,
                                        container::now,
                                        container::newRoutineId,
                                        container::newTaskId,
                                        routineId,
                                    )
                                },
                                routineId = routineId,
                                onClose = navController::popBackStack,
                                onSaved = {
                                    selectedTab = HomeTab.ROUTINES
                                    navController.popBackStack()
                                },
                                animationsEnabled = preferences.screenTransitionsEnabled,
                            )
                        }
                        composable(
                            route = RUNNER_ROUTE,
                            arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) { type = NavType.StringType }),
                        ) { entry ->
                            RoutineRunnerRoute(
                                dependencies = container.execution,
                                preferences = preferences,
                                routineId = RoutineId(requireNotNull(entry.arguments?.getString(ROUTINE_ID_ARGUMENT))),
                                onFinished = navController::popBackStack,
                            )
                        }
                    }
                    LaunchedEffect(navController) {
                        combine(
                            container.activeRun.observeActive().map { run ->
                                run?.takeIf(container.runEngine::requiresRunnerScreen)?.routineId
                            }.distinctUntilChanged(),
                            navController.currentBackStackEntryFlow,
                        ) { runningRoutineId, entry -> runningRoutineId to entry }
                            .collect { (runningRoutineId, entry) ->
                                if (runningRoutineId == null) return@collect
                                val onRunningRunner = entry.destination.route == RUNNER_ROUTE &&
                                    entry.arguments?.getString(ROUTINE_ID_ARGUMENT) == runningRoutineId.value
                                if (!onRunningRunner) navController.navigate(runnerRoute(runningRoutineId)) { launchSingleTop = true }
                            }
                    }
                    LaunchedEffect(openActiveRun) {
                        if (openActiveRun && !openActiveRunConsumed) {
                            container.activeRun.observeActive().first { it?.status == RunStatus.ACTIVE }?.let { activeRun ->
                                val current = navController.currentBackStackEntry
                                val alreadyShowing = current?.destination?.route == RUNNER_ROUTE &&
                                    current.arguments?.getString(ROUTINE_ID_ARGUMENT) == activeRun.routineId.value
                                if (!alreadyShowing) navController.navigate(runnerRoute(activeRun.routineId)) { launchSingleTop = true }
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
