package com.freshsave.app.data

import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.DealStatus
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.testutil.TestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Manage-listings actions and reviews, straight against the fake repositories. */
class ListingAndReviewTest {
    private val env = TestEnv()

    @After fun tearDown() = env.close()

    private suspend fun deal(id: String) = env.deals.observeDeal(id).first()!!

    @Test
    fun `expire now takes a listing off sale`() = runTest {
        env.deals.expireNow("tasty-bag")

        assertEquals(DealStatus.EXPIRED, deal("tasty-bag").status(env.clock.millis()))
    }

    @Test
    fun `extend window adds 30 minutes`() = runTest {
        val before = deal("tasty-sandwich").pickupEndMillis

        env.deals.extendWindow("tasty-sandwich", 30)

        assertEquals(before + 30 * 60_000L, deal("tasty-sandwich").pickupEndMillis)
    }

    @Test
    fun `relist restocks a sold out listing`() = runTest {
        env.deals.relist("tasty-croissant")

        val relisted = deal("tasty-croissant")
        assertEquals(relisted.quantityTotal, relisted.quantityLeft)
        assertEquals(DealStatus.LIVE, relisted.status(env.clock.millis()))
    }

    @Test
    fun `submitting a review updates the shop's rating count and notifies the business`() = runTest {
        val shop = MockData.BUSINESS_SHOP_ID
        assertEquals(312, env.reviews.observeRatingSummary(shop).first().total)

        env.reviews.submitReview(shop, 5, listOf("Fresh"), "Lovely bread")

        assertEquals(313, env.reviews.observeRatingSummary(shop).first().total)
        assertTrue(env.reviews.observeShopReviews(shop).first().any { it.text == "Lovely bread" })
        assertTrue(env.notifications.observe(UserRole.BUSINESS).first().any { it.type == NotificationType.NEW_REVIEW })
    }

    @Test
    fun `replying to a review saves the reply`() = runTest {
        env.reviews.reply("r2", "Thanks Karim, more veg soon!")

        assertEquals("Thanks Karim, more veg soon!", env.reviews.observeShopReviews(MockData.BUSINESS_SHOP_ID).first().first { it.id == "r2" }.reply)
    }
}
