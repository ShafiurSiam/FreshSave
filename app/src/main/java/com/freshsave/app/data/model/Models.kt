package com.freshsave.app.data.model

import kotlin.math.roundToInt

// Plain data classes shared by every layer. They know nothing about Compose or storage,
// so a Firebase or REST backend can map its JSON into these later.

enum class UserRole { SHOPPER, BUSINESS }

enum class FoodCategory(val label: String) {
    BAKERY("Bakery"),
    MEALS("Meals"),
    GROCERY("Grocery"),
    SWEETS("Sweets"),
    DAIRY("Dairy"),
}

data class Shop(
    val id: String,
    val name: String,
    val area: String,
    val kind: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    /** Position on the placeholder mini-map, 0..1 in each direction. */
    val mapX: Float,
    val mapY: Float,
)

enum class DealStatus { LIVE, SOLD_OUT, EXPIRED }

data class Deal(
    val id: String,
    val shopId: String,
    val shopName: String,
    val area: String,
    val title: String,
    val description: String,
    val category: FoodCategory,
    val originalPrice: Int,
    val price: Int,
    val quantityTotal: Int,
    val quantityLeft: Int,
    val pickupStartMillis: Long,
    val pickupEndMillis: Long,
    val distanceKm: Double,
    val rating: Double,
    val reviewCount: Int,
    /** Caption shown on the striped placeholder until real photos exist. */
    val imageLabel: String,
    /** Remote photo URL or local content:// URI. Null means "show the placeholder". */
    val imageUrl: String? = null,
    val expiredManually: Boolean = false,
) {
    val discountPercent: Int
        get() = if (originalPrice == 0) 0 else ((1 - price.toDouble() / originalPrice) * 100).roundToInt()

    val quantitySold: Int get() = quantityTotal - quantityLeft

    fun status(now: Long): DealStatus = when {
        expiredManually || now >= pickupEndMillis -> DealStatus.EXPIRED
        quantityLeft <= 0 -> DealStatus.SOLD_OUT
        else -> DealStatus.LIVE
    }

    fun isAvailable(now: Long) = status(now) == DealStatus.LIVE
}

enum class PaymentMethod(val label: String) {
    BKASH("bKash"),
    NAGAD("Nagad"),
    CARD("Card"),
}

enum class OrderStatus { ACTIVE, COLLECTED, MISSED }

data class Order(
    val id: String,
    val dealId: String,
    val shopId: String,
    val shopName: String,
    val area: String,
    val itemTitle: String,
    val quantity: Int,
    val unitPrice: Int,
    val originalUnitPrice: Int,
    val serviceFee: Int,
    val paymentMethod: PaymentMethod,
    val pickupCode: String,
    val pickupStartMillis: Long,
    val pickupEndMillis: Long,
    val customerId: String,
    val customerName: String,
    val createdAtMillis: Long,
    val status: OrderStatus,
    val collectedAtMillis: Long? = null,
    val reviewed: Boolean = false,
) {
    val subtotal: Int get() = unitPrice * quantity
    val total: Int get() = subtotal + serviceFee
    val savings: Int get() = (originalUnitPrice - unitPrice) * quantity

    /** "Ayesha Rahman" -> "Ayesha R." as shown in the business pickup queue. */
    val customerShortName: String
        get() {
            val parts = customerName.trim().split(" ")
            return if (parts.size < 2) customerName else "${parts.first()} ${parts.last().first()}."
        }

    /** An order still waiting for pickup whose window has closed counts as missed. */
    fun effectiveStatus(now: Long): OrderStatus =
        if (status == OrderStatus.ACTIVE && now >= pickupEndMillis) OrderStatus.MISSED else status

    fun isReadyToCollect(now: Long) = effectiveStatus(now) == OrderStatus.ACTIVE && now >= pickupStartMillis
}

data class Review(
    val id: String,
    val shopId: String,
    val authorName: String,
    val rating: Int,
    val text: String,
    val tags: List<String>,
    val createdAtMillis: Long,
    val reply: String? = null,
)

data class RatingSummary(
    val average: Double,
    val total: Int,
    /** Count per star, index 0 = 1 star ... index 4 = 5 stars. */
    val distribution: List<Int>,
)

enum class NotificationType {
    DEAL_ENDING,
    ORDER_UPDATE,
    NEW_FROM_FAVORITE,
    REVIEW_PROMPT,
    WEEKLY_RECAP,
    NEW_ORDER,
    NEW_REVIEW,
}

data class AppNotification(
    val id: String,
    val audience: UserRole,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val read: Boolean = false,
    /** Deal id, order id, etc. depending on [type]. */
    val targetId: String? = null,
)

data class AlertSettings(
    val newDealsFromFavorites: Boolean = true,
    val dealsEndingSoon: Boolean = true,
    val weeklyImpactRecap: Boolean = false,
)

data class MonthSaving(val label: String, val amount: Int)

data class ImpactStats(
    val moneySaved: Int,
    val rescues: Int,
    val mealsRescued: Int,
    val foodKg: Int,
    val co2Kg: Int,
    val monthly: List<MonthSaving>,
    val level: Int,
    val levelName: String,
    val rescuesToNextLevel: Int,
)

data class UserProfile(
    val id: String,
    val name: String,
    val phone: String,
) {
    val initial: String get() = name.take(1).uppercase()
    val firstName: String get() = name.substringBefore(" ")
}

data class Branch(
    val id: String,
    val name: String,
    val liveListings: Int,
    val acceptingOrders: Boolean,
)

data class BusinessProfile(
    val shop: Shop,
    val branches: List<Branch>,
    val selectedBranchId: String,
    val verified: Boolean,
    val isPro: Boolean,
) {
    val selectedBranch: Branch? get() = branches.firstOrNull { it.id == selectedBranchId }
}

data class TopSeller(val name: String, val sold: Int)

data class AnalyticsSummary(
    val revenue30d: Int,
    val revenueChangePct: Int,
    val wasteCutKg: Int,
    val wasteChangePct: Int,
    /** Revenue recovered per week, oldest first. */
    val weeklyRevenue: List<Int>,
    val topSellers: List<TopSeller>,
)

/** Input from the Create listing form. */
data class ListingDraft(
    val title: String,
    val category: FoodCategory,
    val originalPrice: Int,
    val quantity: Int,
    val discountPercent: Int,
    val pickupStartMillis: Long,
    val pickupEndMillis: Long,
    val description: String,
    val imageUrl: String?,
) {
    val salePrice: Int get() = (originalPrice * (100 - discountPercent) / 100.0).roundToInt()
}
