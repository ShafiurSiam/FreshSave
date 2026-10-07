package com.freshsave.app.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation: every screen is a @Serializable object or data class.
// Navigation Compose turns these into URL-like routes behind the scenes, and
// screens read their arguments back with `backStackEntry.toRoute<DealDetailRoute>()`.

// Onboarding & auth
@Serializable object SplashRoute
@Serializable object OnboardingRoute
@Serializable object LoginRoute
@Serializable object RoleSelectionRoute

// Shopper — tabs live inside ShopperGraph so the bottom bar can save/restore each tab.
@Serializable object ShopperGraph
@Serializable object HomeRoute
@Serializable object SearchRoute
@Serializable object OrdersRoute
@Serializable object ImpactRoute
@Serializable object ProfileRoute
@Serializable data class DealDetailRoute(val dealId: String)
@Serializable data class CheckoutRoute(val dealId: String, val quantity: Int)
@Serializable data class OrderConfirmationRoute(val orderId: String)
@Serializable object FavoritesRoute
@Serializable data class RateReviewRoute(val orderId: String)
@Serializable object FullMapRoute

// Business
@Serializable object BusinessGraph
@Serializable object DashboardRoute
@Serializable object ListingsRoute
@Serializable object PickupQueueRoute
@Serializable object AnalyticsRoute
@Serializable object BusinessProfileRoute
@Serializable data class CreateListingRoute(val editDealId: String? = null)
@Serializable object RatingsRoute

// Shared
@Serializable object NotificationsRoute
@Serializable object HelpRoute
