package com.example.asm_final.ui

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.app.Fragment
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.asm_final.R
import com.example.asm_final.activities.HomeActivity
import com.example.asm_final.activities.fragments.*
import com.example.mentor.ui.design.*

/** Navigation Compose owns the only back stack; AndroidFragment safely hosts retained Java screens. */
class ShellUi(private val activity: HomeActivity, private val username: String, private val initial: Int) {
    private var navigation: NavHostController? = null
    private var pending: Pair<Int, Boolean>? = null

    fun navigate(id: Int, remember: Boolean) {
        val route = destinations.firstOrNull { it.id == id }?.route ?: return
        if (navigation == null) {
            pending = id to remember
            return
        }
        if (navigation?.currentDestination?.route == route) return
        // Settings and editing sit above Profile; return to that screen instead of restoring
        // a saved tab stack that may bring the subpage back to the top.
        if (route == "profile" && navigation?.currentDestination?.route in listOf("settings", "edit-profile")
            && navigation?.popBackStack("profile", false) == true) return
        navigation?.navigate(route) {
            launchSingleTop = true
            if (!remember) {
                popUpTo(navigation!!.graph.startDestinationId) { saveState = true }
                restoreState = true
            }
        }
    }

    fun view(): View = composeHost(activity) {
        val nav = rememberNavController()
        DisposableEffect(nav) { onDispose { navigation = null } }
        LaunchedEffect(nav) {
            navigation = nav
            val request = pending
            pending = null
            if (request != null) navigate(request.first, request.second)
        }
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val top = destinations.firstOrNull { it.route == route }
        val selectedRoute = if (route == "settings" || route == "edit-profile") "profile" else route
        val motion = motionEnabled()
        val duration = if (motion) MentorTokens.MotionMillis else 0
        val arguments = remember(username) { Bundle().apply { putString("USERNAME", username) } }
        val backgroundPath = remember { com.example.asm_final.utils.ThemeHelper.getCustomBackground(activity) }
        BackHandler(enabled = route in tabs.map { it.route } && route != destinations.first { it.id == initial }.route) {
            navigate(initial, false)
        }
        val keyboard = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        
        Box(Modifier.fillMaxSize()) {
            if (backgroundPath != null) {
                AsyncImage(
                    model = java.io.File(backgroundPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            
            Scaffold(
                containerColor = if (backgroundPath != null) Color.Transparent else MaterialTheme.colorScheme.background,
                bottomBar = {
                  if (!keyboard) {
                    NavigationBar(containerColor = if (backgroundPath != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            NavigationBarItem(selected = selectedRoute == tab.route,
                                onClick = { navigate(tab.id, false) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = if (backgroundPath != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer
                                ),
                                icon = { Icon(painterResource(tab.icon), null, Modifier.size(22.dp)) },
                                label = { Text(stringResource(tab.label), maxLines = 2, style = MaterialTheme.typography.labelSmall, overflow = TextOverflow.Ellipsis) })
                        }
                    }
                  }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                    if (top != null && top !in tabs) Row(Modifier.fillMaxWidth()
                        .background(if (backgroundPath != null) Color.Transparent else MaterialTheme.colorScheme.background)) {
                        TextButton({ nav.popBackStack() }) {
                            Text(stringResource(R.string.course_back))
                        }
                    }
                    NavHost(nav, startDestination = destinations.first { it.id == initial }.route,
                        modifier = Modifier.weight(1f),
                        enterTransition = { fadeIn(tween(duration)) + slideInHorizontally(tween(duration)) { it / 14 } },
                        exitTransition = { fadeOut(tween(duration)) },
                        popEnterTransition = { fadeIn(tween(duration)) },
                        popExitTransition = { fadeOut(tween(duration)) }) {
                        destinations.forEach { destination ->
                            composable(destination.route) {
                                AndroidFragment(clazz = destination.fragment, arguments = arguments, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }

    private data class Destination(val id: Int, val route: String, val label: Int, val icon: Int, val fragment: Class<out Fragment>)
    private companion object {
        val destinations = listOf(
            Destination(R.id.nav_home, "home", R.string.design_tab_home, R.drawable.ic_home, HomeFragment::class.java),
            Destination(R.id.nav_chat, "chat", R.string.design_tab_chat, R.drawable.ic_chat, GeneralChatFragment::class.java),
            Destination(R.id.nav_category, "lessons", R.string.design_tab_lessons, R.drawable.ic_category, LessonHubFragment::class.java),
            Destination(R.id.nav_competition, "competition", R.string.design_tab_competition, R.drawable.ic_competition, CompetitionFragment::class.java),
            Destination(R.id.nav_profile, "profile", R.string.nav_profile_label, R.drawable.ic_nav_profile, ProfileHubFragment::class.java),
            Destination(R.id.nav_settings, "settings", R.string.settings_title, R.drawable.ic_nav_profile, SettingsFragment::class.java),
            Destination(R.id.nav_edit_profile, "edit-profile", R.string.profile_edit, R.drawable.ic_nav_profile, ProfileFragment::class.java),
            Destination(0, "schedule", 0, 0, com.example.mentor.schedule.ui.ScheduleFragment::class.java)
        )
        val tabs = destinations.take(5)
    }
}
