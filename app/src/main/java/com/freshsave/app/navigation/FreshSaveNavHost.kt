package com.freshsave.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.freshsave.app.data.model.AppNotification
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.feature.business.analytics.AnalyticsRoute as AnalyticsScreen
import com.freshsave.app.feature.business.dashboard.DashboardRoute as DashboardScreen
import com.freshsave.app.feature.business.listing.CreateListingRoute as CreateListingScreen
import com.freshsave.app.feature.business.listings.ManageListingsRoute as ManageListingsScreen
import com.freshsave.app.feature.business.profile.BusinessProfileRoute as BusinessProfileScreen
import com.freshsave.app.feature.business.queue.PickupQueueRoute as PickupQueueScreen
import com.freshsave.app.feature.business.ratings.RatingsRoute as RatingsScreen
import com.freshsave.app.feature.favorites.FavoritesRoute as FavoritesScreen
import com.freshsave.app.feature.help.HelpRoute as HelpScreen
import com.freshsave.app.feature.impact.ImpactRoute as ImpactScreen
import com.freshsave.app.feature.notifications.NotificationsRoute as NotificationsScreen
import com.freshsave.app.feature.profile.ProfileRoute as ProfileScreen
import com.freshsave.app.feature.review.RateReviewRoute as RateReviewScreen
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
    // Screens slide in slightly from the right and fade; going back reverses it.
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
        enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 12 } },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it / 12 } },
    ) {
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
            composable<ImpactRoute> { ImpactScreen() }
            composable<ProfileRoute> {
                ProfileScreen(
                    onFavorites = { navController.navigate(FavoritesRoute) },
                    onNotifications = { navController.navigate(NotificationsRoute) },
                    onHelp = { navController.navigate(HelpRoute) },
                    onSwitchedToBusiness = { navController.navigateToRoot(BusinessGraph) },
                    onLoggedOut = { navController.navigateToRoot(LoginRoute) },
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
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onBack = { navController.navigateUp() },
                    onDealClick = { navController.navigate(DealDetailRoute(it)) },
                )
            }
            composable<RateReviewRoute> { entry ->
                RateReviewScreen(
                    orderId = entry.toRoute<RateReviewRoute>().orderId,
                    onClose = { navController.navigateUp() },
                )
            }
            composable<FullMapRoute> {
                FullMapScreen(
                    onBack = { navController.navigateUp() },
                    onDealClick = { navController.navigate(DealDetailRoute(it)) },
                )
            }
        }

        navigation<BusinessGraph>(startDestination = DashboardRoute) {
            composable<DashboardRoute> {
                DashboardScreen(
                    onNotifications = { navController.navigate(NotificationsRoute) },
                    onPostListing = { navController.navigate(CreateListingRoute()) },
                    onOpenQueue = { navController.navigateToBusinessTab(BusinessTab.Orders) },
                    onOpenRatings = { navController.navigate(RatingsRoute) },
                )
            }
            composable<ListingsRoute> {
                ManageListingsScreen(
                    onEdit = { navController.navigate(CreateListingRoute(editDealId = it)) },
                    onCreate = { navController.navigate(CreateListingRoute()) },
                )
            }
            composable<PickupQueueRoute> { PickupQueueScreen() }
            composable<AnalyticsRoute> { AnalyticsScreen() }
            composable<BusinessProfileRoute> {
                BusinessProfileScreen(
                    onRatings = { navController.navigate(RatingsRoute) },
                    onNotifications = { navController.navigate(NotificationsRoute) },
                    onHelp = { navController.navigate(HelpRoute) },
                    onSwitchedToShopper = { navController.navigateToRoot(ShopperGraph) },
                    onLoggedOut = { navController.navigateToRoot(LoginRoute) },
                )
            }
            composable<CreateListingRoute> { entry ->
                val editId = entry.toRoute<CreateListingRoute>().editDealId
                CreateListingScreen(
                    editDealId = editId,
                    onClose = { navController.navigateUp() },
                    onPublished = {
                        // New listing -> show it in Active listings; an edit just goes back.
                        if (editId == null) {
                            navController.navigate(ListingsRoute) {
                                popUpTo(DashboardRoute)
                                launchSingleTop = true
                            }
                        } else navController.navigateUp()
                    },
                )
            }
            composable<RatingsRoute> { RatingsScreen(onBack = { navController.navigateUp() }) }
        }

        composable<NotificationsRoute> {
            NotificationsScreen(
                onBack = { navController.navigateUp() },
                onOpen = { navController.openNotification(it) },
            )
        }
        composable<HelpRoute> { HelpScreen(onBack = { navController.navigateUp() }) }
    }
}

/** Follows the arrows from the Notifications inbox in App.pdf. */
private fun NavController.openNotification(n: AppNotification) {
    val target = n.targetId
    when (n.type) {
        NotificationType.DEAL_ENDING ->
            if (n.audience == UserRole.BUSINESS) navigateToBusinessTab(BusinessTab.Listings)
            else if (target != null) navigate(DealDetailRoute(target))
        NotificationType.NEW_FROM_FAVORITE -> if (target != null) navigate(DealDetailRoute(target))
        NotificationType.ORDER_UPDATE -> navigateToShopperTab(ShopperTab.Orders)
        NotificationType.REVIEW_PROMPT -> if (target != null) navigate(RateReviewRoute(target))
        NotificationType.WEEKLY_RECAP -> navigateToShopperTab(ShopperTab.Impact)
        NotificationType.NEW_ORDER -> navigateToBusinessTab(BusinessTab.Orders)
        NotificationType.NEW_REVIEW -> navigate(RatingsRoute)
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
