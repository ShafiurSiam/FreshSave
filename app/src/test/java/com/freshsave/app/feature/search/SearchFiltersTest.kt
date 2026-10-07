package com.freshsave.app.feature.search

import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.FoodCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** SearchViewModel.applyFilters is a pure function, so we test it directly with mock deals. */
class SearchFiltersTest {
    // Fixed "now" at midday so "today" / "tomorrow" never straddle midnight.
    private val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val deals = MockData.deals(now)

    private fun filter(f: SearchFilters) = SearchViewModel.applyFilters(deals, f, now)

    @Test
    fun `no filters returns every available deal, soonest ending first`() {
        val result = filter(SearchFilters())

        assertTrue(result.all { it.isAvailable(now) })
        assertFalse(result.any { it.id == "tasty-croissant" }) // sold out
        assertEquals(result.sortedBy { it.pickupEndMillis }, result)
    }

    @Test
    fun `food type filter keeps only that category`() {
        val result = filter(SearchFilters(categories = setOf(FoodCategory.BAKERY)))

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.category == FoodCategory.BAKERY })
    }

    @Test
    fun `several food types are combined with OR`() {
        val result = filter(SearchFilters(categories = setOf(FoodCategory.SWEETS, FoodCategory.DAIRY)))

        assertEquals(setOf("mithai-box", "mithai-cake", "dairy-pack"), result.map { it.id }.toSet())
    }

    @Test
    fun `price filter respects each range`() {
        assertTrue(filter(SearchFilters(price = PriceRange.UNDER_100)).all { it.price < 100 })
        assertTrue(filter(SearchFilters(price = PriceRange.FROM_100_TO_300)).all { it.price in 100..300 })
        assertEquals(listOf("mithai-cake"), filter(SearchFilters(price = PriceRange.OVER_300)).map { it.id })
    }

    @Test
    fun `distance slider excludes shops that are too far`() {
        val result = filter(SearchFilters(maxDistanceKm = 1f))

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.distanceKm <= 1.0 })
    }

    @Test
    fun `search text matches title, shop or description, ignoring case`() {
        val result = filter(SearchFilters(query = "CROISSANT"))

        assertTrue(result.any { it.id == "north-choco" }) // in the title
        assertTrue(result.any { it.id == "tasty-bag" }) // only in the description
        assertTrue(result.none { it.id == "spice-biryani" })
    }

    @Test
    fun `tomorrow filter returns only tomorrow's pickups`() {
        val result = filter(SearchFilters(pickupTime = PickupTime.TOMORROW))

        assertEquals(setOf("star-paratha", "agora-veg"), result.map { it.id }.toSet())
    }

    @Test
    fun `filters combine - bakery under 100 within 2_5 km`() {
        val result = filter(
            SearchFilters(maxDistanceKm = 2.5f, price = PriceRange.UNDER_100, categories = setOf(FoodCategory.BAKERY)),
        )

        // Bread & Beyond (2.6 km) is too far and the Pastry Box (৳140) too pricey.
        assertEquals(setOf("tasty-bag", "north-choco"), result.map { it.id }.toSet())
    }
}
