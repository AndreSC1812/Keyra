package com.ascdev.keyra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ascdev.keyra.ui.entry.EntryDetailScreen
import com.ascdev.keyra.ui.entry.EntryEditScreen
import com.ascdev.keyra.ui.generator.GeneratorScreen
import com.ascdev.keyra.ui.health.SecurityHealthScreen
import com.ascdev.keyra.ui.settings.SettingsScreen
import com.ascdev.keyra.ui.vault.VaultListScreen
import kotlinx.serialization.Serializable

@Serializable data object VaultListRoute

@Serializable data class EntryDetailRoute(val id: Long)

@Serializable data class EntryEditRoute(val id: Long = NEW_ENTRY_ID)

@Serializable data object GeneratorRoute

@Serializable data object SecurityHealthRoute

@Serializable data object SettingsRoute

const val NEW_ENTRY_ID = -1L

@Composable
fun KeyraNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = VaultListRoute) {
        composable<VaultListRoute> {
            VaultListScreen(
                onEntryClick = { navController.navigate(EntryDetailRoute(it)) },
                onAddEntry = { navController.navigate(EntryEditRoute()) },
                onOpenGenerator = { navController.navigate(GeneratorRoute) },
                onOpenHealth = { navController.navigate(SecurityHealthRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<EntryDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EntryDetailRoute>()
            EntryDetailScreen(
                onBack = navController::navigateUp,
                onEdit = { navController.navigate(EntryEditRoute(route.id)) },
            )
        }
        composable<EntryEditRoute> {
            EntryEditScreen(onDone = navController::navigateUp)
        }
        composable<GeneratorRoute> {
            GeneratorScreen(onBack = navController::navigateUp)
        }
        composable<SecurityHealthRoute> {
            SecurityHealthScreen(
                onBack = navController::navigateUp,
                onEntryClick = { navController.navigate(EntryDetailRoute(it)) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = navController::navigateUp)
        }
    }
}
