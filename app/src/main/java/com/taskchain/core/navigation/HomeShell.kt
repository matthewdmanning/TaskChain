@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.taskchain.R

/** The persistent top-level destinations hosted by the application shell. */
internal enum class HomeTab(@param:StringRes val label: Int) {
    HOME(R.string.tab_home),
    ROUTINES(R.string.tab_routines),
    PROGRESS(R.string.tab_progress),
    SETTINGS(R.string.tab_settings),
}

/** Hosts shared tab chrome while executable-root wiring supplies each feature's content. */
@Composable
internal fun HomeShell(
    selectedTab: HomeTab,
    onSelectedTabChange: (HomeTab) -> Unit,
    todayContent: @Composable (PaddingValues) -> Unit,
    routinesContent: @Composable (PaddingValues) -> Unit,
    progressContent: @Composable (PaddingValues) -> Unit,
    settingsContent: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(selectedTab.label)) }) },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onSelectedTabChange(tab) },
                        icon = {},
                        label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall, softWrap = false) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            HomeTab.HOME -> todayContent(padding)
            HomeTab.ROUTINES -> routinesContent(padding)
            HomeTab.PROGRESS -> progressContent(padding)
            HomeTab.SETTINGS -> settingsContent(padding)
        }
    }
}
