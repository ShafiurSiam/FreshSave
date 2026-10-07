package com.freshsave.app.data.fake

import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.AlertSettings
import com.freshsave.app.data.model.AnalyticsSummary
import com.freshsave.app.data.model.AppNotification
import com.freshsave.app.data.model.Branch
import com.freshsave.app.data.model.BusinessProfile
import com.freshsave.app.data.model.ImpactStats
import com.freshsave.app.data.model.MonthSaving
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.model.RatingSummary
import com.freshsave.app.data.model.Review
import com.freshsave.app.data.model.Shop
import com.freshsave.app.data.model.UserProfile
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.ImpactRepository
import com.freshsave.app.data.repository.NotificationRepository
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.data.repository.ReviewRepository
import com.freshsave.app.data.repository.ShopRepository
import com.freshsave.app.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt

class FakeShopRepository : ShopRepository {
    private val shops = MutableStateFlow(MockData.shops)
    private val favorites = MutableStateFlow(
        linkedMapOf(
            "tasty-treat" to true,
            "star-kabab" to true,
            "agora-fresh" to false,
            "north-end" to true,
            "mithai-ghor" to false,
        ) as Map<String, Boolean>,
    )
    private val alerts = MutableStateFlow(AlertSettings())

    override fun observeShops(): Flow<List<Shop>> = shops
    override fun observeShop(id: String): Flow<Shop?> = shops.map { list -> list.firstOrNull { it.id == id } }
    override fun observeFavorites(): Flow<Map<String, Boolean>> = favorites

    override suspend fun setFavorite(shopId: String, favorite: Boolean) {
        favorites.update { if (favorite) it + (shopId to true) else it - shopId }
    }

    override suspend fun setFollowing(shopId: String, following: Boolean) {
        favorites.update { if (shopId in it) it + (shopId to following) else it }
    }

    override fun observeAlertSettings(): Flow<AlertSettings> = alerts
    override suspend fun updateAlertSettings(settings: AlertSettings) {
        alerts.value = settings
    }
}

class FakeNotificationRepository(clock: AppClock) : NotificationRepository {
    private val items = MutableStateFlow(MockData.notifications(clock.millis()))

    override fun observe(audience: UserRole): Flow<List<AppNotification>> =
        items.map { list -> list.filter { it.audience == audience }.sortedByDescending { it.createdAtMillis } }

    override suspend fun add(notification: AppNotification) {
        items.update { listOf(notification) + it }
    }

    override suspend fun markRead(id: String) {
        items.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
    }

    override suspend fun markAllRead(audience: UserRole) {
        items.update { list -> list.map { if (it.audience == audience) it.copy(read = true) else it } }
    }
}

class FakeReviewRepository(
    private val clock: AppClock,
    private val notificationRepository: NotificationRepository,
) : ReviewRepository {
    private val reviews = MutableStateFlow(MockData.reviews(clock.millis()))

    /** Reviews that exist "on the server" but aren't loaded as individual cards. */
    private val baseDistribution = mapOf(MockData.BUSINESS_SHOP_ID to MockData.tastyTreatDistribution)

    override fun observeShopReviews(shopId: String): Flow<List<Review>> =
        reviews.map { list -> list.filter { it.shopId == shopId }.sortedByDescending { it.createdAtMillis } }

    override fun observeRatingSummary(shopId: String): Flow<RatingSummary> = reviews.map { list ->
        val counts = (baseDistribution[shopId] ?: List(5) { 0 }).toMutableList()
        // Mock reviews r1..r4 are already part of the base counts; only add new ones.
        list.filter { it.shopId == shopId && it.id.startsWith("rv-") }.forEach { counts[it.rating - 1]++ }
        val total = counts.sum()
        val average = if (total == 0) 0.0 else counts.mapIndexed { i, c -> (i + 1) * c }.sum().toDouble() / total
        RatingSummary(average = (average * 10).roundToInt() / 10.0, total = total, distribution = counts)
    }

    override suspend fun submitReview(shopId: String, rating: Int, tags: List<String>, text: String) {
        val user = MockData.currentUser
        val author = "${user.firstName} ${user.name.substringAfterLast(" ").first()}."
        val review = Review(
            id = "rv-" + UUID.randomUUID().toString().take(8),
            shopId = shopId, authorName = author, rating = rating, text = text.trim(), tags = tags,
            createdAtMillis = clock.millis(),
        )
        reviews.update { listOf(review) + it }
        if (shopId == MockData.BUSINESS_SHOP_ID) {
            notificationRepository.add(
                AppNotification(
                    id = "b-" + review.id, audience = UserRole.BUSINESS, type = NotificationType.NEW_REVIEW,
                    title = "New $rating★ review",
                    body = if (review.text.isBlank()) "$author rated their pickup." else "$author: \"${review.text}\"",
                    createdAtMillis = review.createdAtMillis, targetId = review.id,
                ),
            )
        }
    }

    override suspend fun reply(reviewId: String, text: String) {
        reviews.update { list -> list.map { if (it.id == reviewId) it.copy(reply = text.trim()) else it } }
    }
}

/** Lifetime impact = the mock baseline plus every rescue made in this session. */
class FakeImpactRepository(
    private val clock: AppClock,
    orderRepository: OrderRepository,
) : ImpactRepository {
    private val sessionStart = clock.millis()
    private val myOrders = orderRepository.observeMyOrders()

    override fun observeImpact(): Flow<ImpactStats> = myOrders.map { orders ->
        val fresh = orders.filter { it.createdAtMillis >= sessionStart && it.status != OrderStatus.MISSED }
        val newSavings = fresh.sumOf { it.savings }
        val newItems = fresh.sumOf { it.quantity }
        val rescues = MockData.BASE_RESCUES + fresh.size
        // Rough conversion used by food-rescue apps: ~0.8 kg per bag, ~2.5 kg CO2e per kg of food.
        val foodKg = MockData.BASE_FOOD_KG + (newItems * 0.8).roundToInt()
        val co2Kg = MockData.BASE_CO2_KG + (newItems * 0.8 * 2.5).roundToInt()
        val (level, name, toNext) = levelFor(rescues)
        ImpactStats(
            moneySaved = MockData.BASE_MONEY_SAVED + newSavings,
            rescues = rescues,
            mealsRescued = MockData.BASE_RESCUES + newItems,
            foodKg = foodKg,
            co2Kg = co2Kg,
            monthly = lastFiveMonths(newSavings),
            level = level,
            levelName = name,
            rescuesToNextLevel = toNext,
        )
    }

    private fun lastFiveMonths(newSavings: Int): List<MonthSaving> {
        val names = DateFormatSymbols.getInstance().shortMonths
        val cal = Calendar.getInstance().apply { timeInMillis = clock.millis() }
        val current = cal.get(Calendar.MONTH)
        return MockData.baseMonthlySavings.mapIndexed { index, amount ->
            val monthIndex = (current - (4 - index) + 12) % 12
            MonthSaving(names[monthIndex], if (index == 4) amount + newSavings else amount)
        }
    }

    private fun levelFor(rescues: Int): Triple<Int, String, Int> {
        val thresholds = listOf(0, 10, 20, 25, 40, 60)
        val names = listOf("Rescue Rookie", "Food Saver", "Waste Warrior", "Planet Protector", "Zero-Waste Hero", "Legend")
        val index = thresholds.indexOfLast { rescues >= it }
        val next = thresholds.getOrNull(index + 1)
        return Triple(index + 1, names[index], if (next == null) 0 else next - rescues)
    }
}

class FakeBusinessRepository : BusinessRepository {
    override val currentShopId: String = MockData.BUSINESS_SHOP_ID

    private val profile = MutableStateFlow(
        BusinessProfile(
            shop = MockData.shops.first { it.id == currentShopId },
            branches = MockData.branches,
            selectedBranchId = "banani",
            verified = true,
            isPro = true,
        ),
    )

    override fun observeProfile(): Flow<BusinessProfile> = profile
    override fun observeAnalytics(): Flow<AnalyticsSummary> = flowOf(MockData.analytics)

    override suspend fun addBranch(name: String) {
        val branch = Branch(
            id = "branch-" + UUID.randomUUID().toString().take(6),
            name = name.trim(),
            liveListings = 0,
            acceptingOrders = false,
        )
        profile.update { it.copy(branches = it.branches + branch) }
    }

    override suspend fun selectBranch(id: String) {
        profile.update { it.copy(selectedBranchId = id) }
    }

    override suspend fun setBranchAcceptingOrders(id: String, accepting: Boolean) {
        profile.update { p ->
            p.copy(branches = p.branches.map { if (it.id == id) it.copy(acceptingOrders = accepting) else it })
        }
    }
}

class FakeUserRepository : UserRepository {
    override fun observeCurrentUser(): Flow<UserProfile> = flowOf(MockData.currentUser)
}
