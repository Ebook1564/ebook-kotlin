package com.example.ebookapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ebookapp.models.Book

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Library : Screen("library", "Library", Icons.Default.LibraryBooks)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object BookDetail : Screen("book_detail/{bookId}", "Book Detail") {
        fun createRoute(bookId: String) = "book_detail/$bookId"
    }
    object Category : Screen("category/{categoryTitle}", "Category") {
        fun createRoute(categoryTitle: String) = "category/$categoryTitle"
    }
    object Subscription : Screen("subscription", "Subscription")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Library,
    Screen.Profile
)

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onTimeout = { navController.navigate(Screen.Home.route) })
        }
        composable(Screen.Home.route) {
            HomeScreen(onBookClick = { book ->
                navController.navigate(Screen.BookDetail.createRoute(book.id))
            })
        }
        composable(Screen.Search.route) {
            SearchScreen(onBookClick = { book ->
                navController.navigate(Screen.BookDetail.createRoute(book.id))
            })
        }
        composable(Screen.Library.route) {
            LibraryScreen(onBookClick = { book ->
                navController.navigate(Screen.BookDetail.createRoute(book.id))
            })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.BookDetail.route) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            // You'll need a way to get the book from the ID, e.g., from a ViewModel
            // For now, we'll just show a placeholder
            if (bookId != null) {
                // BookDetailScreen(book = book, onReadClick = { /*...*/ }, onBackClick = { navController.popBackStack() })
            }
        }
        composable(Screen.Category.route) { backStackEntry ->
            val categoryTitle = backStackEntry.arguments?.getString("categoryTitle")
            if (categoryTitle != null) {
                // CategoryScreen(categoryTitle = categoryTitle, books = emptyList(), onBookClick = { book ->
                //     navController.navigate(Screen.BookDetail.createRoute(book.id))
                // }, onBackClick = { navController.popBackStack() })
            }
        }
        composable(Screen.Subscription.route) {
            SubscriptionScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
