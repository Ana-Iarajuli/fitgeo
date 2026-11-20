package com.personal.calorietracker.ui

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personal.calorietracker.ui.products.ProductEditScreen
import com.personal.calorietracker.ui.products.ProductsScreen
import com.personal.calorietracker.ui.recipes.RecipeDetailScreen
import com.personal.calorietracker.ui.recipes.RecipeEditScreen
import com.personal.calorietracker.ui.recipes.RecipesScreen
import com.personal.calorietracker.ui.scanner.ScannerScreen
import com.personal.calorietracker.ui.today.AddFoodScreen
import com.personal.calorietracker.ui.today.TodayScreen
import com.personal.calorietracker.ui.wishlist.WishlistEditScreen
import com.personal.calorietracker.ui.wishlist.WishlistScreen

/** Key under which the scanner hands its result back to the screen that opened it. */
private const val KEY_BARCODE = "scanned_barcode"

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination("today", "Today", Icons.Filled.Today),
    TopDestination("products", "Products", Icons.Filled.Inventory2),
    TopDestination("recipes", "Recipes", Icons.Filled.RestaurantMenu),
    TopDestination("wishlist", "Wishlist", Icons.Filled.Favorite),
)

private fun productRoute(id: Long, barcode: String = "", name: String = "") =
    "product/$id?barcode=${Uri.encode(barcode)}&name=${Uri.encode(name)}"

@Composable
private fun scannedBarcode(entry: NavBackStackEntry): String? {
    val value by entry.savedStateHandle
        .getStateFlow<String?>(KEY_BARCODE, null)
        .collectAsStateWithLifecycle()
    return value
}

private fun NavHostController.openScanner() = navigate("scanner")

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val showBottomBar = topDestinations.any { it.route == currentDestination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { d ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == d.route } == true,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icon, contentDescription = null) },
                            label = { Text(d.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable("today") {
                TodayScreen(onAddFood = { day -> nav.navigate("addFood/$day") })
            }

            composable(
                "addFood/{day}",
                arguments = listOf(navArgument("day") { type = NavType.LongType }),
            ) { entry ->
                AddFoodScreen(
                    scannedBarcode = scannedBarcode(entry),
                    onScannedHandled = { entry.savedStateHandle[KEY_BARCODE] = null },
                    onScan = { nav.openScanner() },
                    onBack = { nav.popBackStack() },
                    onCreateProduct = { barcode, name -> nav.navigate(productRoute(0, barcode, name)) },
                )
            }

            composable("products") { entry ->
                ProductsScreen(
                    scannedBarcode = scannedBarcode(entry),
                    onScannedHandled = { entry.savedStateHandle[KEY_BARCODE] = null },
                    onScan = { nav.openScanner() },
                    onOpenProduct = { id -> nav.navigate(productRoute(id)) },
                    onNewProduct = { barcode, name -> nav.navigate(productRoute(0, barcode, name)) },
                )
            }

            composable(
                "product/{id}?barcode={barcode}&name={name}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                ProductEditScreen(
                    scannedBarcode = scannedBarcode(entry),
                    onScannedHandled = { entry.savedStateHandle[KEY_BARCODE] = null },
                    onScan = { nav.openScanner() },
                    onBack = { nav.popBackStack() },
                )
            }

            composable("scanner") {
                ScannerScreen(
                    onResult = { code ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(KEY_BARCODE, code)
                        nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() },
                )
            }

            composable("recipes") {
                RecipesScreen(
                    onOpen = { id -> nav.navigate("recipe/$id") },
                    onCreate = { nav.navigate("recipeEdit/0") },
                )
            }

            composable(
                "recipe/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                RecipeDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate("recipeEdit/$id") },
                )
            }

            composable(
                "recipeEdit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                RecipeEditScreen(onBack = { nav.popBackStack() })
            }

            composable("wishlist") {
                WishlistScreen(
                    onOpen = { id -> nav.navigate("wishlistEdit/$id") },
                    onCreate = { nav.navigate("wishlistEdit/0") },
                )
            }

            composable(
                "wishlistEdit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                WishlistEditScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
