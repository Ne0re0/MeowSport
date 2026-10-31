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
import fr.meowsport.app
import fr.meowsport.ui.exercises.ExercisePickerScreen
import fr.meowsport.ui.history.SessionDetailScreen
import fr.meowsport.ui.profile.ProfileScreen
import fr.meowsport.ui.session.LiveSessionScreen
import fr.meowsport.ui.settings.SettingsScreen
import fr.meowsport.ui.templates.ExamplesScreen
import fr.meowsport.ui.templates.TemplateEditScreen
import fr.meowsport.ui.templates.TemplatesScreen

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PICKER = "picker"
    const val EXAMPLES = "examples"
    const val SETTINGS = "settings"
    fun template(id: Long?) = "template/${id ?: -1}"
    fun session(id: Long) = "session/$id"
    fun history(id: Long) = "history/$id"
    fun replace(muscle: fr.meowsport.data.Muscle) = "picker?single=true&muscle=${muscle.name}"
}

/** Key used to hand the picked exercise ids back to the previous screen. */
const val PICKED_KEY = "picked_exercises"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeowSportNav(openSessionRequest: Int = 0) {
    val nav = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    // Tapping the session notification opens the running session.
    androidx.compose.runtime.LaunchedEffect(openSessionRequest) {
        if (openSessionRequest > 0) {
            val active = context.app.repo.activeSession()
            if (active != null && nav.currentBackStackEntry?.destination?.route != "session/{id}") {
                nav.navigate(Routes.session(active.id)) { launchSingleTop = true }
            }
        }
    }
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
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
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
                    onPickReplacement = { nav.navigate(Routes.replace(it)) },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                "picker?single={single}&muscle={muscle}",
                arguments = listOf(
                    navArgument("single") { type = NavType.BoolType; defaultValue = false },
                    navArgument("muscle") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { e ->
                val muscleName = e.arguments?.getString("muscle")
                ExercisePickerScreen(
                    single = e.arguments?.getBoolean("single") ?: false,
                    initialMuscle = fr.meowsport.data.Muscle.entries.firstOrNull { it.name == muscleName },
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
                    onPickReplacement = { nav.navigate(Routes.replace(it)) },
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

