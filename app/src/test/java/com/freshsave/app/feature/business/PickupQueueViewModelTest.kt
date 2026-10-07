package com.freshsave.app.feature.business

import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.feature.business.queue.PickupQueueViewModel
import com.freshsave.app.testutil.MainDispatcherRule
import com.freshsave.app.testutil.TestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** "Mark collected" only closes an order when staff type the customer's code. */
class PickupQueueViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val env = TestEnv()
    // lazy: created inside the test, after MainDispatcherRule has installed the test dispatcher.
    private val viewModel by lazy { PickupQueueViewModel(env.clock, env.business, env.orders) }

    @After fun tearDown() = env.close()

    private suspend fun order(id: String) = env.orders.observeOrder(id).first()!!

    @Test
    fun `wrong pickup code is rejected and the order stays to fulfil`() = runTest {
        val karim = order("o-9b14") // code 9B14

        val accepted = viewModel.verifyAndCollect(karim, "4F29")

        assertFalse(accepted)
        assertEquals(OrderStatus.ACTIVE, order("o-9b14").status)
    }

    @Test
    fun `correct pickup code moves the order to Collected`() = runTest {
        // Keep the ViewModel's state flow active, the way the screen would.
        backgroundScope.launch { viewModel.state.collect {} }
        val karim = order("o-9b14")

        val accepted = viewModel.verifyAndCollect(karim, "9b14") // lower case is fine

        assertTrue(accepted)
        advanceUntilIdle() // let the state flow pick up the change
        val updated = order("o-9b14")
        assertEquals(OrderStatus.COLLECTED, updated.status)
        assertTrue(updated.collectedAtMillis != null)
        val state = viewModel.state.value
        assertTrue(state.collected.any { it.id == "o-9b14" })
        assertFalse(state.toFulfil.any { it.id == "o-9b14" })
    }

    @Test
    fun `collected order shows in the shopper's past orders`() = runTest {
        val ayesha = order("o-4f29") // Ayesha's own reservation

        viewModel.verifyAndCollect(ayesha, "4F29")

        val mine = env.orders.observeMyOrders().first().first { it.id == "o-4f29" }
        assertEquals(OrderStatus.COLLECTED, mine.status)
    }
}
