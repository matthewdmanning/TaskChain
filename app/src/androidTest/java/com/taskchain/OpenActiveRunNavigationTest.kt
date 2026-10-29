package com.taskchain

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskchain.TaskChainApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that an open-active-run request does not duplicate a restored runner entry.
 * Constructor inputs: none.
 * Dependencies: AppContainer, TaskChainApp, MainActivity, NavHostController, and StateRestorationTester.
 */
@RunWith(AndroidJUnit4::class)
class OpenActiveRunNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Use this function before the open-active-run test to remove prior isolated test routines and runs. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @Before
    fun clearIsolatedTestState() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this function after the open-active-run test to remove only state created in the isolated test package. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @After
    fun clearIsolatedTestStateAfterTest() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /**
     * Use this function to verify recreation consumes the open-active-run request after restoration.
     * Inputs: none.
     * Dependencies: composeRule, AppContainer, MainActivity.EXTRA_OPEN_ACTIVE_RUN, TaskChainApp, and NavHostController back-stack state.
     */
    @Test
    fun openActiveRunRequestAfterRecreation_doesNotLeaveDuplicateRunnerEntry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = testAppContainer(context)
        val openActiveRun = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ACTIVE_RUN, true)
            .getBooleanExtra(MainActivity.EXTRA_OPEN_ACTIVE_RUN, false)
        runBlocking {
            val routine = container.builtInLibrary.load(System.currentTimeMillis()).first()
            val activeRun = container.runEngine.start(routine, container.newRunId(), System.currentTimeMillis())
            container.activeRun.saveActive(activeRun)
        }
        var navController: NavHostController? = null
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            val controller = rememberNavController()
            navController = controller
            TaskChainApp(
                container = container,
                openActiveRun = openActiveRun,
                providedNavController = controller,
            )
        }
        composeRule.waitUntil(10_000) {
            navController?.currentBackStackEntry?.destination?.route == "runner/{routineId}"
        }
        composeRule.waitUntil(10_000) {
            navController?.currentBackStackEntry?.lifecycle?.currentState
                ?.isAtLeast(Lifecycle.State.CREATED) == true
        }
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitUntil(10_000) {
            navController?.currentBackStackEntry?.destination?.route == "runner/{routineId}" &&
                navController?.currentBackStackEntry?.lifecycle?.currentState
                    ?.isAtLeast(Lifecycle.State.CREATED) == true
        }
        check(navController?.previousBackStackEntry?.destination?.route == "home") {
            "Restoring a runner opened by the open-active-run request must keep Home as the previous destination"
        }
        composeRule.runOnIdle { navController?.popBackStack() }
        composeRule.waitUntil(10_000) {
            navController?.currentBackStackEntry?.destination?.route == "home" &&
                navController?.currentBackStackEntry?.lifecycle?.currentState
                    ?.isAtLeast(Lifecycle.State.CREATED) == true
        }
    }
}
