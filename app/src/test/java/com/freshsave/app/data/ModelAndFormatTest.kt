package com.freshsave.app.data

import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.util.formatTaka
import com.freshsave.app.util.formatTimeLeft
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelAndFormatTest {
    private val now = System.currentTimeMillis()

    @Test
    fun `discount percent is derived from the two prices`() {
        val bag = MockData.deals(now).first { it.id == "tasty-bag" }
        assertEquals(70, bag.discountPercent) // ৳75 instead of ৳250
    }

    @Test
    fun `an uncollected order becomes missed once the window closes`() {
        val order = MockData.orders(now).first { it.id == "o-4f29" }
        assertEquals(OrderStatus.ACTIVE, order.effectiveStatus(now))
        assertEquals(OrderStatus.MISSED, order.effectiveStatus(order.pickupEndMillis + 1))
    }

    @Test
    fun `customer names are shortened for the pickup queue`() {
        val order = MockData.orders(now).first { it.id == "o-2c77" }
        assertEquals("Sadia M.", order.customerShortName)
    }

    @Test
    fun `taka amounts use the symbol and thousands separators`() {
        assertEquals("৳75", formatTaka(75))
        assertEquals("৳4,280", formatTaka(4280))
    }

    @Test
    fun `countdown text switches format as time runs out`() {
        assertEquals("1h 20m", formatTimeLeft(80 * 60_000L))
        assertEquals("42 min", formatTimeLeft(42 * 60_000L))
        assertEquals("9:05", formatTimeLeft(9 * 60_000L + 5_000))
        assertEquals("0:00", formatTimeLeft(-1))
    }
}
