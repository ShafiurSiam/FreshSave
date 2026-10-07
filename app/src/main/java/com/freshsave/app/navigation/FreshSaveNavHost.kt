package com.freshsave.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.feature.auth.LoginRoute as LoginScreen
import com.freshsave.app.feature.checkout.CheckoutRoute as CheckoutScreen
import com.freshsave.app.feature.confirmation.OrderConfirmationRoute as OrderConfirmationScreen
import com.freshsave.app.feature.deal.DealDetailRoute as DealDetailScreen
import com.freshsave.app.feature.home.FullMapRoute as FullMapScreen
import com.freshsave.app.feature.home.HomeRoute as HomeScreen
import com.freshsave.app.feature.orders.OrdersRoute as OrdersScreen
import com.freshsave.app.feature.search.SearchRoute as SearchScreen
import com.freshsave.app.feature.onboarding.OnboardingRoute as OnboardingScreen
import com.freshsave.app.feature.role.RoleSelectionRoute as RoleSelectionScreen
import com.freshsave.app.feature.splash.SplashDestination
import com.freshsave.app.feature.splash.SplashRoute as SplashScreen
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
            // Screens handle system bars themselves (some draw green behind them), so the
            // Scaffold only reserves room for the bottom bar when one is showing.
            contentWindowInsets = WindowInsets(0),
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
            SplashScreen(onFinished = { destination ->
                navController.navigateToRoot(
                    when (destination) {
                        SplashDestination.Onboarding -> OnboardingRoute
                        SplashDestination.Login -> LoginRoute
                        SplashDestination.RoleSelection -> RoleSelectionRoute
                        SplashDestination.ShopperHome -> ShopperGraph
                        SplashDestination.BusinessDashboard -> BusinessGraph
                    },
                )
            })
        }
        composable<OnboardingRoute> {
            OnboardingScreen(onDone = { navController.navigateToRoot(LoginRoute) })
        }
        composable<LoginRoute> {
            LoginScreen(onAuthenticated = { navController.navigateToRoot(RoleSelectionRoute) })
        }
        composable<RoleSelectionRoute> {
            RoleSelectionScreen(onRoleChosen = { role ->
                navController.navigateToRoot(if (role == UserRole.SHOPPER) ShopperGraph else BusinessGraph)
            })
        }

        navigation<ShopperGraph>(startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onDealClick = { navController.navigate(DealDetailRoute(it)) },
                    onNotificationsClick = { navController.navigate(NotificationsRoute) },
                    onOpenMap = { navController.navigate(FullMapRoute) },
                    onSeeAll = { navController.navigateToShopperTab(ShopperTab.Search) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    onCancel = { navController.navigateToShopperTab(ShopperTab.Home) },
                    onDealClick = { navController.navigate(DealDetailRoute(it)) },
                )
            }
            composable<OrdersRoute> {
                OrdersScreen(
                    onOrderClick = { navController.navigate(OrderConfirmationRoute(it)) },
                    onRate = { navController.navigate(RateReviewRoute(it)) },
                    onBrowse = { navController.navigateToShopperTab(ShopperTab.Home) },
                )
            }
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
                // toRoute() rebuilds the DealDetailRoute object (and its dealId) from the back stack entry.
                val route = entry.toRoute<DealDetailRoute>()
                DealDetailScreen(
                    dealId = route.dealId,
                    onBack = { navController.navigateUp() },
                    onReserve = { dealId, qty -> navController.navigate(CheckoutRoute(dealId, qty)) },
                    onOpenFavorites = { navController.navigate(FavoritesRoute) },
                )
            }
            composable<CheckoutRoute> { entry ->
                val route = entry.toRoute<CheckoutRoute>()
                CheckoutScreen(
                    dealId = route.dealId,
                    quantity = route.quantity,
                    onBack = { navController.navigateUp() },
                    onOrderPlaced = { orderId ->
                        // Drop Deal detail + Checkout so Back from the confirmation returns to the feed.
                        navController.navigate(OrderConfirmationRoute(orderId)) {
                            popUpTo<DealDetailRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<OrderConfirmationRoute> { entry ->
                OrderConfirmationScreen(
                    orderId = entry.toRoute<OrderConfirmationRoute>().orderId,
                    onClose = { navController.navigateUp() },
                    onViewOrder = { navController.navigateToShopperTab(ShopperTab.Orders) },
                )
            }
            composable<FavoritesRoute> { PlaceholderScreen("Favorites") }
            composable<RateReviewRoute> { PlaceholderScreen("Rate & review") }
            composable<FullMapRoute> {
                FullMapScreen(
                    onBack = { navController.navigateUp() },
                    onDealClick = { navController.navigate(DealDetailRoute(it)) },
                )
            }
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
