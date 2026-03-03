package com.example.ebook_new_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ebook_new_app.ui.AnimatedModernNavBar
import com.example.ebook_new_app.ui.HomeScreen
import com.example.ebook_new_app.ui.LibraryScreen
import com.example.ebook_new_app.ui.NavTab
import com.example.ebook_new_app.ui.ProfileScreen
import com.example.ebook_new_app.ui.Screen
import com.example.ebook_new_app.ui.SearchScreen
import com.example.ebook_new_app.ui.ProvideBookTheme
import com.example.ebook_new_app.ui.ThemeType
import com.example.ebook_new_app.ui.LocalBookTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
            var themeType by rememberSaveable { mutableStateOf(ThemeType.VINTAGE) }
            ProvideBookTheme(themeType = themeType) {
                BookVerseApp(
                    onThemeToggle = {
                        themeType = if (themeType == ThemeType.VINTAGE) ThemeType.PITCH_BLACK else ThemeType.VINTAGE
                    }
                )
            }
        }
    }
}

@Composable
fun BookVerseApp(onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    val theme = LocalBookTheme.current

    Scaffold(
        containerColor = theme.background,
        bottomBar = {
            AnimatedModernNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab != selectedTab) {
                        selectedTab = tab
                        val route = when (tab) {
                            NavTab.HOME -> Screen.Home.route
                            NavTab.SEARCH -> Screen.Search.route
                            NavTab.LIBRARY -> Screen.Library.route
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(onThemeToggle = onThemeToggle) 
            }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
