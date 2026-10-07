package com.freshsave.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.freshsave.app.ui.appContainer
import com.freshsave.app.ui.components.BusinessBottomBar
import com.freshsave.app.ui.components.BusinessTab
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.ShopperBottomBar
import com.freshsave.app.ui.components.ShopperTab

/** Lets any screen show a snackbar ("Saved to favorites") without passing state around. */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/** Root of the UI: provides the ticking clock, the bottom bars and every destination. */
@Composable
fun FreshSaveApp() {
    val container = appContainer()
    val now by container.clock.now.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    CompositionLocalProvider(LocalNow provides now, LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            // Screens handle the status bar themselves (some draw green behind it),
            // so the Scaffold only reserves space at the bottom.
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                destination.shopperTab()?.let { tab ->
                    ShopperBottomBar(selected = tab, onSelect = { navController.navigateToShopperTab(it) })
                }
                destination.businessTab()?.let { tab ->
                    BusinessBottomBar(selected = tab, onSelect = { navController.navigateToBusinessTab(it) })
                }
            },
        ) { padding ->
            val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
            FreshSaveNavHost(
                navController = navController,
                modifier = Modifier
                    .padding(bottom)
                    .consumeWindowInsets(bottom),
            )
        }
    }
}

@Composable
private fun FreshSaveNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(navController = navController, startDestination = SplashRoute, modifier = modifier) {
        composable<SplashRoute> {
            PlaceholderScreen("Splash", "Continue" to { navController.navigate(OnboardingRoute) })
        }
        composable<OnboardingRoute> {
            PlaceholderScreen("Onboarding", "Skip" to { navController.navigate(LoginRoute) })
        }
        composable<LoginRoute> {
            PlaceholderScreen("Log in", "Log in" to { navController.navigate(RoleSelectionRoute) })
        }
        composable<RoleSelectionRoute> {
            PlaceholderScreen(
                "Role selection",
                "Continue as shopper" to { navController.navigateToRoot(ShopperGraph) },
                "I'm a business" to { navController.navigateToRoot(BusinessGraph) },
            )
        }

        navigation<ShopperGraph>(startDestination = HomeRoute) {
            composable<HomeRoute> {
                PlaceholderScreen(
                    "Home feed",
                    "Deal detail" to { navController.navigate(DealDetailRoute("tasty-bag")) },
                    "Notifications" to { navController.navigate(NotificationsRoute) },
                    "Full map" to { navController.navigate(FullMapRoute) },
                )
            }
            composable<SearchRoute> { PlaceholderScreen("Search & filters") }
            composable<OrdersRoute> { PlaceholderScreen("My orders") }
            composable<ImpactRoute> { PlaceholderScreen("Impact") }
            composable<ProfileRoute> {
                PlaceholderScreen(
                    "Profile",
                    "Favorites" to { navController.navigate(FavoritesRoute) },
                    "Help & support" to { navController.navigate(HelpRoute) },
                    "Switch to business" to { navController.navigateToRoot(BusinessGraph) },
                    "Log out" to { navController.navigateToRoot(LoginRoute) },
                )
            }
            composable<DealDetailRoute> { entry ->
                val route = entry.toRoute<DealDetailRoute>()
                PlaceholderScreen(
                    "Deal ${route.dealId}",
                    "Reserve now" to { navController.navigate(CheckoutRoute(route.dealId, 1)) },
                )
            }
            composable<CheckoutRoute> {
                PlaceholderScreen("Checkout", "Pay" to { navController.navigate(OrderConfirmationRoute("o-4f29")) })
            }
            composable<OrderConfirmationRoute> {
                PlaceholderScreen("Order confirmation", "View order" to { navController.navigateToShopperTab(ShopperTab.Orders) })
            }
            composable<FavoritesRoute> { PlaceholderScreen("Favorites") }
            composable<RateReviewRoute> { PlaceholderScreen("Rate & review") }
            composable<FullMapRoute> { PlaceholderScreen("Full map") }
        }

        navigation<BusinessGraph>(startDestination = DashboardRoute) {
            composable<DashboardRoute> {
                PlaceholderScreen("Business dashboard", "Post surplus listing" to { navController.navigate(CreateListingRoute()) })
            }
            composable<ListingsRoute> { PlaceholderScreen("Active listings") }
            composable<PickupQueueRoute> { PlaceholderScreen("Pickup queue") }
            composable<AnalyticsRoute> { PlaceholderScreen("Analytics") }
            composable<BusinessProfileRoute> {
                PlaceholderScreen("Business profile", "Ratings" to { navController.navigate(RatingsRoute) })
            }
            composable<CreateListingRoute> { PlaceholderScreen("New listing") }
            composable<RatingsRoute> { PlaceholderScreen("Your ratings") }
        }

        composable<NotificationsRoute> { PlaceholderScreen("Notifications") }
        composable<HelpRoute> { PlaceholderScreen("Help & support") }
    }
}

// --- Which tab (if any) is showing? Used to decide which bottom bar to draw. ---

private fun NavDestination?.shopperTab(): ShopperTab? = when {
    this == null -> null
    hasRoute(HomeRoute::class) -> ShopperTab.Home
    hasRoute(SearchRoute::class) -> ShopperTab.Search
    hasRoute(OrdersRoute::class) -> ShopperTab.Orders
    hasRoute(ImpactRoute::class) -> ShopperTab.Impact
    hasRoute(ProfileRoute::class) -> ShopperTab.Profile
    else -> null
}

private fun NavDestination?.businessTab(): BusinessTab? = when {
    this == null -> null
    hasRoute(DashboardRoute::class) -> BusinessTab.Dashboard
    hasRoute(ListingsRoute::class) -> BusinessTab.Listings
    hasRoute(PickupQueueRoute::class) -> BusinessTab.Orders
    hasRoute(AnalyticsRoute::class) -> BusinessTab.Analytics
    hasRoute(BusinessProfileRoute::class) -> BusinessTab.Profile
    else -> null
}

// --- Navigation helpers shared by screens ---

/**
 * Switches bottom-bar tab the standard way: pop back to the first tab (saving the state
 * of the tab we leave), avoid duplicate copies, and restore the target tab's old state.
 */
fun NavController.navigateToShopperTab(tab: ShopperTab) {
    val route: Any = when (tab) {
        ShopperTab.Home -> HomeRoute
        ShopperTab.Search -> SearchRoute
        ShopperTab.Orders -> OrdersRoute
        ShopperTab.Impact -> ImpactRoute
        ShopperTab.Profile -> ProfileRoute
    }
    navigate(route) {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToBusinessTab(tab: BusinessTab) {
    val route: Any = when (tab) {
        BusinessTab.Dashboard -> DashboardRoute
        BusinessTab.Listings -> ListingsRoute
        BusinessTab.Orders -> PickupQueueRoute
        BusinessTab.Analytics -> AnalyticsRoute
        BusinessTab.Profile -> BusinessProfileRoute
    }
    navigate(route) {
        popUpTo(DashboardRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Replaces the whole back stack (after login, logout or a role switch). */
fun NavController.navigateToRoot(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
