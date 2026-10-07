package com.freshsave.app.data.fake

import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.AppNotification
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.model.PaymentMethod
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.NotificationRepository
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.util.formatClockTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class FakeOrderRepository(
    private val clock: AppClock,
    private val dealRepository: DealRepository,
    private val notificationRepository: NotificationRepository,
) : OrderRepository {

    private val orders = MutableStateFlow(MockData.orders(clock.millis()))

    override fun observeOrders(): Flow<List<Order>> = orders

    override fun observeOrder(id: String): Flow<Order?> = orders.map { list -> list.firstOrNull { it.id == id } }

    override fun observeMyOrders(): Flow<List<Order>> =
        orders.map { list -> list.filter { it.customerId == MockData.ME_ID } }

    override fun observeShopOrders(shopId: String): Flow<List<Order>> =
        orders.map { list -> list.filter { it.shopId == shopId } }

    override suspend fun placeOrder(dealId: String, quantity: Int, paymentMethod: PaymentMethod): Result<Order> {
        delay(900) // Pretend we're talking to bKash / Nagad.
        val deal = dealRepository.observeDeal(dealId).first()
            ?: return Result.failure(IllegalArgumentException("This deal no longer exists."))
        if (!dealRepository.reserve(dealId, quantity)) {
            return Result.failure(IllegalStateException("Sorry, someone just grabbed the last one."))
        }
        val now = clock.millis()
        val order = Order(
            id = "o-" + UUID.randomUUID().toString().take(8),
            dealId = deal.id,
            shopId = deal.shopId,
            shopName = deal.shopName,
            area = deal.area,
            itemTitle = deal.title,
            quantity = quantity,
            unitPrice = deal.price,
            originalUnitPrice = deal.originalPrice,
            serviceFee = MockData.SERVICE_FEE,
            paymentMethod = paymentMethod,
            pickupCode = newPickupCode(),
            pickupStartMillis = deal.pickupStartMillis,
            pickupEndMillis = deal.pickupEndMillis,
            customerId = MockData.ME_ID,
            customerName = MockData.currentUser.name,
            createdAtMillis = now,
            status = OrderStatus.ACTIVE,
        )
        orders.update { listOf(order) + it }

        notificationRepository.add(
            AppNotification(
                id = "n-" + order.id, audience = UserRole.SHOPPER, type = NotificationType.ORDER_UPDATE,
                title = "Reservation confirmed",
                body = "Code ${order.pickupCode} · pick up by ${formatClockTime(order.pickupEndMillis)} at ${order.area}.",
                createdAtMillis = now, targetId = order.id,
            ),
        )
        notificationRepository.add(
            AppNotification(
                id = "b-" + order.id, audience = UserRole.BUSINESS, type = NotificationType.NEW_ORDER,
                title = "New order",
                body = "${order.customerShortName} reserved ${quantity}× ${order.itemTitle} · code ${order.pickupCode}.",
                createdAtMillis = now, targetId = order.id,
            ),
        )
        return Result.success(order)
    }

    override suspend fun markCollected(orderId: String) {
        orders.update { list ->
            list.map {
                if (it.id == orderId) it.copy(status = OrderStatus.COLLECTED, collectedAtMillis = clock.millis()) else it
            }
        }
    }

    override suspend fun markReviewed(orderId: String) {
        orders.update { list -> list.map { if (it.id == orderId) it.copy(reviewed = true) else it } }
    }

    /** Four characters, skipping look-alikes such as O/0 and I/1. */
    private fun newPickupCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val taken = orders.value.map { it.pickupCode }.toSet()
        while (true) {
            val code = (1..4).map { alphabet.random() }.joinToString("")
            if (code !in taken) return code
        }
    }
}
