package com.freshsave.app.feature.business

import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.DealStatus
import com.freshsave.app.data.model.FoodCategory
import com.freshsave.app.feature.business.listing.CreateListingViewModel
import com.freshsave.app.feature.business.listing.ListingForm
import com.freshsave.app.testutil.MainDispatcherRule
import com.freshsave.app.testutil.TestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateListingViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val env = TestEnv()

    @After fun tearDown() = env.close()

    private fun newListingViewModel() = CreateListingViewModel(null, env.clock, env.deals, env.business)

    @Test
    fun `discount slider recalculates the sale price`() {
        assertEquals(75, ListingForm(originalPrice = "250", discountPercent = 70).salePrice)
        assertEquals(150, ListingForm(originalPrice = "250", discountPercent = 40).salePrice)
        assertEquals(0, ListingForm(originalPrice = "", discountPercent = 50).salePrice)
    }

    @Test
    fun `publishing a new listing adds it to the deal list and the shop's listings`() = runTest {
        val viewModel = newListingViewModel()
        viewModel.update {
            it.copy(title = "Evening Pastry Box", category = FoodCategory.BAKERY, originalPrice = "400", quantity = "5", discountPercent = 60)
        }

        viewModel.publish()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.saved)
        val created = env.deals.observeDeals().first().firstOrNull { it.title == "Evening Pastry Box" }
        assertNotNull(created)
        assertEquals(MockData.BUSINESS_SHOP_ID, created!!.shopId)
        assertEquals(160, created.price) // ৳400 − 60%
        assertEquals(5, created.quantityLeft)
        assertEquals(DealStatus.LIVE, created.status(env.clock.millis()))
        assertTrue(env.deals.observeShopDeals(MockData.BUSINESS_SHOP_ID).first().any { it.id == created.id })
    }

    @Test
    fun `invalid form shows errors and publishes nothing`() = runTest {
        val before = env.deals.observeDeals().first().size
        val viewModel = newListingViewModel()
        viewModel.update { it.copy(title = "  ", originalPrice = "", quantity = "0") }

        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.saved)
        assertEquals(setOf("title", "price", "quantity"), state.errors.keys)
        assertEquals(before, env.deals.observeDeals().first().size)
    }

    @Test
    fun `editing a listing updates it instead of creating a new one`() = runTest {
        val before = env.deals.observeDeals().first().size
        val viewModel = CreateListingViewModel("tasty-bag", env.clock, env.deals, env.business)
        advanceUntilIdle() // loads the existing deal into the form
        assertEquals("Surprise Bakery Bag", viewModel.state.value.form.title)

        viewModel.update { it.copy(discountPercent = 50) }
        viewModel.publish()
        advanceUntilIdle()

        val deals = env.deals.observeDeals().first()
        assertEquals(before, deals.size)
        assertEquals(125, deals.first { it.id == "tasty-bag" }.price) // ৳250 − 50%
    }
}
