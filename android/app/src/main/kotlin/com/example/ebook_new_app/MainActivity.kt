package com.example.ebook_new_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.ebook_new_app.ui.AnimatedModernNavBar
import com.example.ebook_new_app.ui.HomeScreen
import com.example.ebook_new_app.ui.LibraryScreen
import com.example.ebook_new_app.ui.NavTab
import com.example.ebook_new_app.ui.ProfileScreen
import com.example.ebook_new_app.ui.SearchScreen
import com.example.ebook_new_app.ui.ProvideBookTheme
import com.example.ebook_new_app.ui.ThemeType
import com.example.ebook_new_app.ui.LocalBookTheme
import com.google.firebase.FirebaseApp

@OptIn(ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookVerseApp(onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { NavTab.entries.size })
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    val theme = LocalBookTheme.current

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = NavTab.entries[pagerState.currentPage]
    }

    Scaffold(
        containerColor = theme.background,
        bottomBar = {
            AnimatedModernNavBar(
                selectedTab = selectedTab,
                pagerState = pagerState,
                onTabSelected = { tab ->
                    selectedTab = tab
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            when (NavTab.entries[page]) {
                NavTab.HOME -> HomeScreen(onThemeToggle = onThemeToggle)
                NavTab.SEARCH -> SearchScreen()
                NavTab.LIBRARY -> LibraryScreen(onProfileClick = { navController.navigate("profile") })
            }
        }
    }
}
