package com.freshsave.app.data.fake

import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.ListingDraft
import com.freshsave.app.data.repository.DealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/** Keeps deals in memory. Everything resets when the app process is killed. */
class FakeDealRepository(private val clock: AppClock) : DealRepository {

    private val deals = MutableStateFlow(MockData.deals(clock.millis()))

    override fun observeDeals(): Flow<List<Deal>> = deals

    override fun observeDeal(id: String): Flow<Deal?> = deals.map { list -> list.firstOrNull { it.id == id } }

    override fun observeShopDeals(shopId: String): Flow<List<Deal>> =
        deals.map { list -> list.filter { it.shopId == shopId } }

    override suspend fun createDeal(shopId: String, draft: ListingDraft): Deal {
        val shop = MockData.shops.first { it.id == shopId }
        val deal = Deal(
            id = "deal-" + UUID.randomUUID().toString().take(8),
            shopId = shop.id,
            shopName = shop.name,
            area = shop.area,
            title = draft.title,
            description = draft.description,
            category = draft.category,
            originalPrice = draft.originalPrice,
            price = draft.salePrice,
            quantityTotal = draft.quantity,
            quantityLeft = draft.quantity,
            pickupStartMillis = draft.pickupStartMillis,
            pickupEndMillis = draft.pickupEndMillis,
            distanceKm = shop.distanceKm,
            rating = shop.rating,
            reviewCount = shop.reviewCount,
            imageLabel = "${draft.category.label.lowercase()} photo",
            imageUrl = draft.imageUrl,
        )
        // New listings go first so they show up at the top of Manage listings.
        deals.update { listOf(deal) + it }
        return deal
    }

    override suspend fun updateDeal(id: String, draft: ListingDraft) = edit(id) { deal ->
        val sold = deal.quantitySold
        deal.copy(
            title = draft.title,
            description = draft.description,
            category = draft.category,
            originalPrice = draft.originalPrice,
            price = draft.salePrice,
            quantityTotal = maxOf(draft.quantity, sold),
            quantityLeft = maxOf(draft.quantity - sold, 0),
            pickupStartMillis = draft.pickupStartMillis,
            pickupEndMillis = draft.pickupEndMillis,
            imageUrl = draft.imageUrl ?: deal.imageUrl,
            expiredManually = false,
        )
    }

    override suspend fun expireNow(id: String) = edit(id) { it.copy(expiredManually = true) }

    override suspend fun extendWindow(id: String, minutes: Int) = edit(id) {
        // Extending from "now" if the window already closed, otherwise from the current end.
        val base = maxOf(it.pickupEndMillis, clock.millis())
        it.copy(pickupEndMillis = base + minutes * 60_000L, expiredManually = false)
    }

    override suspend fun relist(id: String) = edit(id) {
        val now = clock.millis()
        it.copy(
            quantityLeft = it.quantityTotal,
            pickupStartMillis = now,
            pickupEndMillis = now + 2 * 60 * 60_000L,
            expiredManually = false,
        )
    }

    override suspend fun reserve(id: String, quantity: Int): Boolean {
        var ok = false
        deals.update { list ->
            list.map { deal ->
                if (deal.id == id && deal.isAvailable(clock.millis()) && deal.quantityLeft >= quantity) {
                    ok = true
                    deal.copy(quantityLeft = deal.quantityLeft - quantity)
                } else deal
            }
        }
        return ok
    }

    private fun edit(id: String, change: (Deal) -> Deal) {
        deals.update { list -> list.map { if (it.id == id) change(it) else it } }
    }
}
