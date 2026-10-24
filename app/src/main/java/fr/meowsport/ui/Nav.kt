package fr.meowsport.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.meowsport.ui.exercises.ExercisePickerScreen
import fr.meowsport.ui.history.SessionDetailScreen
import fr.meowsport.ui.profile.ProfileScreen
import fr.meowsport.ui.session.LiveSessionScreen
import fr.meowsport.ui.templates.ExamplesScreen
import fr.meowsport.ui.templates.TemplateEditScreen
import fr.meowsport.ui.templates.TemplatesScreen

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PICKER = "picker"
    const val EXAMPLES = "examples"
    fun template(id: Long?) = "template/${id ?: -1}"
    fun session(id: Long) = "session/$id"
    fun history(id: Long) = "history/$id"
}

/** Key used to hand the picked exercise ids back to the previous screen. */
const val PICKED_KEY = "picked_exercises"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeowSportNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route == Routes.HOME || route == Routes.PROFILE

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.HOME,
                        onClick = { nav.switchTab(Routes.HOME) },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                        label = { Text("Séances") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.PROFILE,
                        onClick = { nav.switchTab(Routes.PROFILE) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profil") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .padding(bottom = if (showBar) padding.calculateBottomPadding() else 0.dp)
                .consumeWindowInsets(PaddingValues(bottom = if (showBar) padding.calculateBottomPadding() else 0.dp)),
        ) {
            composable(Routes.HOME) {
                TemplatesScreen(
                    onEdit = { nav.navigate(Routes.template(it)) },
                    onOpenSession = { nav.navigate(Routes.session(it)) },
                    onExamples = { nav.navigate(Routes.EXAMPLES) },
                )
            }
            composable(Routes.EXAMPLES) {
                ExamplesScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenSession = { nav.navigate(Routes.history(it)) },
                    onResumeSession = { nav.navigate(Routes.session(it)) },
                )
            }
            composable(
                "template/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                val id = e.arguments?.getLong("id")?.takeIf { it >= 0 }
                val picked by e.savedStateHandle.getStateFlow<LongArray?>(PICKED_KEY, null).collectAsStateWithLifecycle()
                TemplateEditScreen(
                    templateId = id,
                    picked = picked,
                    onPickedConsumed = { e.savedStateHandle[PICKED_KEY] = null },
                    onPickExercises = { nav.navigate(Routes.PICKER) },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Routes.PICKER) {
                ExercisePickerScreen(
                    onBack = { nav.popBackStack() },
                    onPicked = { ids ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(PICKED_KEY, ids.toLongArray())
                        nav.popBackStack()
                    },
                )
            }
            composable(
                "session/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                val id = e.arguments!!.getLong("id")
                val picked by e.savedStateHandle.getStateFlow<LongArray?>(PICKED_KEY, null).collectAsStateWithLifecycle()
                LiveSessionScreen(
                    sessionId = id,
                    picked = picked,
                    onPickedConsumed = { e.savedStateHandle[PICKED_KEY] = null },
                    onPickExercises = { nav.navigate(Routes.PICKER) },
                    onBack = { nav.popBackStack() },
                    onFinished = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(
                "history/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                SessionDetailScreen(
                    sessionId = e.arguments!!.getLong("id"),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

