package com.freshsave.app.data

import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.model.PaymentMethod
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.testutil.TestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reserving a deal: stock goes down and the order reaches shopper and business. */
class ReservationTest {
    private val env = TestEnv()

    @After fun tearDown() = env.close()

    private suspend fun quantityLeft(dealId: String) = env.deals.observeDeal(dealId).first()!!.quantityLeft

    @Test
    fun `reserving lowers X left`() = runTest {
        assertEquals(3, quantityLeft("tasty-bag"))

        assertTrue(env.deals.reserve("tasty-bag", 2))

        assertEquals(1, quantityLeft("tasty-bag"))
    }

    @Test
    fun `cannot reserve more than is left`() = runTest {
        assertFalse(env.deals.reserve("tasty-bag", 4))
        assertEquals(3, quantityLeft("tasty-bag"))
    }

    @Test
    fun `cannot reserve a sold out deal`() = runTest {
        assertFalse(env.deals.reserve("tasty-croissant", 1))
        assertEquals(0, quantityLeft("tasty-croissant"))
    }

    @Test
    fun `placing an order creates it in Orders and in the Pickup queue`() = runTest {
        val order = env.orders.placeOrder("tasty-bag", 1, PaymentMethod.NAGAD).getOrThrow()

        // Shopper side: "My orders".
        val mine = env.orders.observeMyOrders().first()
        assertTrue(mine.any { it.id == order.id })

        // Business side: Tasty Treat's pickup queue.
        val queue = env.orders.observeShopOrders(MockData.BUSINESS_SHOP_ID).first()
        val queued = queue.firstOrNull { it.id == order.id }
        assertNotNull(queued)
        assertEquals(OrderStatus.ACTIVE, queued!!.status)

        // And the shelf has one fewer bag.
        assertEquals(2, quantityLeft("tasty-bag"))
    }

    @Test
    fun `new order gets a unique 4 character pickup code and correct totals`() = runTest {
        val existingCodes = env.orders.observeOrders().first().map { it.pickupCode }.toSet()

        val order = env.orders.placeOrder("tasty-bag", 2, PaymentMethod.BKASH).getOrThrow()

        assertEquals(4, order.pickupCode.length)
        assertFalse(order.pickupCode in existingCodes)
        assertEquals(150, order.subtotal) // 2 × ৳75
        assertEquals(155, order.total) // + ৳5 service fee
        assertEquals(350, order.savings) // 2 × (৳250 − ৳75)
    }

    @Test
    fun `placing an order notifies both the shopper and the business`() = runTest {
        val order = env.orders.placeOrder("tasty-bag", 1, PaymentMethod.BKASH).getOrThrow()

        val shopper = env.notifications.observe(UserRole.SHOPPER).first()
        val business = env.notifications.observe(UserRole.BUSINESS).first()
        assertTrue(shopper.any { it.type == NotificationType.ORDER_UPDATE && it.targetId == order.id })
        assertTrue(business.any { it.type == NotificationType.NEW_ORDER && it.targetId == order.id })
    }

    @Test
    fun `ordering a sold out deal fails and creates no order`() = runTest {
        val before = env.orders.observeOrders().first().size

        val result = env.orders.placeOrder("tasty-croissant", 1, PaymentMethod.BKASH)

        assertTrue(result.isFailure)
        assertEquals(before, env.orders.observeOrders().first().size)
    }
}
