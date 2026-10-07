package com.freshsave.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freshsave.app.FreshSaveApplication
import com.freshsave.app.di.AppContainer

/** Gets the app-wide [AppContainer] from any composable. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as FreshSaveApplication).container

/**
 * Creates (or reuses) a ViewModel and hands it the repositories it needs.
 * Usage: `val vm = appViewModel { HomeViewModel(it.dealRepository) }`.
 * The ViewModel survives screen rotation and lives as long as its navigation entry.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(key = key) { create(container) }
}
