package com.freshsave.app.data.repository

import com.freshsave.app.data.model.AlertSettings
import com.freshsave.app.data.model.AnalyticsSummary
import com.freshsave.app.data.model.AppNotification
import com.freshsave.app.data.model.BusinessProfile
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.ImpactStats
import com.freshsave.app.data.model.ListingDraft
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.model.PaymentMethod
import com.freshsave.app.data.model.RatingSummary
import com.freshsave.app.data.model.Review
import com.freshsave.app.data.model.Shop
import com.freshsave.app.data.model.UserProfile
import com.freshsave.app.data.model.UserRole
import kotlinx.coroutines.flow.Flow

// Each interface below is what the UI depends on. Today they are backed by fakes in
// data/fake; a Firebase or REST version only has to implement the same functions and be
// swapped in inside AppContainer. Flows let screens update automatically when data changes.

interface DealRepository {
    /** Every deal, including sold-out and expired ones. Screens filter by status. */
    fun observeDeals(): Flow<List<Deal>>
    fun observeDeal(id: String): Flow<Deal?>
    fun observeShopDeals(shopId: String): Flow<List<Deal>>

    suspend fun createDeal(shopId: String, draft: ListingDraft): Deal
    suspend fun updateDeal(id: String, draft: ListingDraft)
    suspend fun expireNow(id: String)
    suspend fun extendWindow(id: String, minutes: Int)
    suspend fun relist(id: String)

    /** Takes [quantity] items off the shelf. Returns false if not enough are left. */
    suspend fun reserve(id: String, quantity: Int): Boolean
}

interface ShopRepository {
    fun observeShops(): Flow<List<Shop>>
    fun observeShop(id: String): Flow<Shop?>
    /** Saved shops mapped to "send me alerts when this shop posts". */
    fun observeFavorites(): Flow<Map<String, Boolean>>
    suspend fun setFavorite(shopId: String, favorite: Boolean)
    suspend fun setFollowing(shopId: String, following: Boolean)
    fun observeAlertSettings(): Flow<AlertSettings>
    suspend fun updateAlertSettings(settings: AlertSettings)
}

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
    fun observeOrder(id: String): Flow<Order?>
    /** Orders placed by the signed-in shopper. */
    fun observeMyOrders(): Flow<List<Order>>
    /** Orders a business has to hand over. */
    fun observeShopOrders(shopId: String): Flow<List<Order>>

    suspend fun placeOrder(dealId: String, quantity: Int, paymentMethod: PaymentMethod): Result<Order>
    suspend fun markCollected(orderId: String)
    suspend fun markReviewed(orderId: String)
}

interface ReviewRepository {
    fun observeShopReviews(shopId: String): Flow<List<Review>>
    fun observeRatingSummary(shopId: String): Flow<RatingSummary>
    suspend fun submitReview(shopId: String, rating: Int, tags: List<String>, text: String)
    suspend fun reply(reviewId: String, text: String)
}

interface NotificationRepository {
    fun observe(audience: UserRole): Flow<List<AppNotification>>
    suspend fun add(notification: AppNotification)
    suspend fun markRead(id: String)
    suspend fun markAllRead(audience: UserRole)
}

interface ImpactRepository {
    fun observeImpact(): Flow<ImpactStats>
}

interface BusinessRepository {
    /** The shop the signed-in business user manages. */
    val currentShopId: String
    fun observeProfile(): Flow<BusinessProfile>
    fun observeAnalytics(): Flow<AnalyticsSummary>
    suspend fun addBranch(name: String)
    suspend fun selectBranch(id: String)
    suspend fun setBranchAcceptingOrders(id: String, accepting: Boolean)
}

interface UserRepository {
    fun observeCurrentUser(): Flow<UserProfile>
}

/** What the app remembers between launches. */
data class Session(
    val onboardingSeen: Boolean = false,
    val loggedIn: Boolean = false,
    val role: UserRole? = null,
)

interface SessionRepository {
    val session: Flow<Session>
    suspend fun setOnboardingSeen()
    suspend fun logIn()
    suspend fun setRole(role: UserRole)
    suspend fun logOut()
}
