package com.freshsave.app.data.fake

import com.freshsave.app.data.model.AnalyticsSummary
import com.freshsave.app.data.model.AppNotification
import com.freshsave.app.data.model.Branch
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.FoodCategory
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.model.PaymentMethod
import com.freshsave.app.data.model.Review
import com.freshsave.app.data.model.Shop
import com.freshsave.app.data.model.TopSeller
import com.freshsave.app.data.model.UserProfile
import com.freshsave.app.data.model.UserRole
import java.util.Calendar

/**
 * Sample data from App.pdf. All times are relative to "now" when the app starts,
 * so countdowns like "42 min left" look right whenever you open it.
 */
object MockData {
    const val ME_ID = "me"
    const val BUSINESS_SHOP_ID = "tasty-treat"
    const val SERVICE_FEE = 5

    val currentUser = UserProfile(id = ME_ID, name = "Ayesha Rahman", phone = "+880 1712 345678")

    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val DAY = 24 * HOUR

    val shops = listOf(
        Shop("tasty-treat", "Tasty Treat", "Banani", "Bakery & café", 4.8, 312, 0.6, 0.30f, 0.42f),
        Shop("agora-fresh", "Agora Fresh", "Gulshan", "Supermarket", 4.5, 208, 1.1, 0.62f, 0.62f),
        Shop("star-kabab", "Star Kabab", "Dhanmondi", "Kabab & grill", 4.6, 540, 1.4, 0.86f, 0.30f),
        Shop("north-end", "North End Coffee", "Gulshan", "Café", 4.7, 187, 0.9, 0.48f, 0.22f),
        Shop("mithai-ghor", "Mithai Ghor", "Gulshan", "Sweet shop", 4.7, 266, 1.8, 0.15f, 0.75f),
        Shop("bread-beyond", "Bread & Beyond", "Banani", "Bakery", 4.4, 98, 2.6, 0.45f, 0.80f),
        Shop("spice-route", "Spice Route Kitchen", "Banani", "Restaurant", 4.5, 143, 1.9, 0.10f, 0.20f),
        Shop("dairy-corner", "Gulshan Dairy Corner", "Gulshan", "Dairy shop", 4.3, 64, 2.9, 0.90f, 0.55f),
    )

    private fun shop(id: String) = shops.first { it.id == id }

    /** 8:00 am tomorrow plus [offsetMinutes]. */
    private fun tomorrowAt(now: Long, hour: Int, offsetMinutes: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }.timeInMillis

    private fun deal(
        id: String, shopId: String, title: String, description: String, category: FoodCategory,
        original: Int, price: Int, total: Int, left: Int, start: Long, end: Long, imageLabel: String,
    ): Deal {
        val s = shop(shopId)
        // Round to 5 minutes so windows read "6:30–8:00 pm" rather than "6:32–8:14 pm".
        val step = 5 * MIN
        return Deal(
            id = id, shopId = s.id, shopName = s.name, area = s.area, title = title,
            description = description, category = category, originalPrice = original, price = price,
            quantityTotal = total, quantityLeft = left,
            pickupStartMillis = start / step * step, pickupEndMillis = (end + step - 1) / step * step,
            distanceKm = s.distanceKm, rating = s.rating, reviewCount = s.reviewCount, imageLabel = imageLabel,
        )
    }

    fun deals(now: Long): List<Deal> = listOf(
        deal(
            "tasty-bag", "tasty-treat", "Surprise Bakery Bag",
            "A generous mix of the day's unsold croissants, pastries & breads. Contents vary — always fresh, " +
                "always a surprise. Bring your own bag to save extra waste.",
            FoodCategory.BAKERY, 250, 75, 8, 3, now - 30 * MIN, now + 42 * MIN, "bakery bag photo",
        ),
        deal(
            "agora-box", "agora-fresh", "Produce Rescue Box",
            "Slightly imperfect fruit and vegetables that are still crisp and tasty — about 3 kg per box.",
            FoodCategory.GROCERY, 300, 150, 10, 5, now - 20 * MIN, now + 80 * MIN, "grocery box photo",
        ),
        deal(
            "tasty-sandwich", "tasty-treat", "Sandwich Combo",
            "Two of today's sandwiches with a bottle of juice. Fillings change daily.",
            FoodCategory.MEALS, 300, 120, 6, 1, now - 45 * MIN, now + 15 * MIN, "sandwich photo",
        ),
        deal(
            "tasty-croissant", "tasty-treat", "Croissant 6-pack",
            "Six butter croissants baked this morning.",
            FoodCategory.BAKERY, 300, 90, 5, 0, now - 60 * MIN, now + 60 * MIN, "croissant photo",
        ),
        deal(
            "north-pastry", "north-end", "Pastry & Croissant Box",
            "Four pastries and two almond croissants from today's counter.",
            FoodCategory.BAKERY, 400, 140, 8, 6, now - 10 * MIN, now + 130 * MIN, "pastry box photo",
        ),
        deal(
            "north-choco", "north-end", "Chocolate Croissant Duo",
            "Two chocolate croissants, still flaky.",
            FoodCategory.BAKERY, 220, 70, 6, 4, now - 5 * MIN, now + 55 * MIN, "croissant photo",
        ),
        deal(
            "star-platter", "star-kabab", "Kabab Platter",
            "Mixed seekh and chicken tikka kababs with naan, packed for two.",
            FoodCategory.MEALS, 450, 180, 6, 4, now - 15 * MIN, now + 95 * MIN, "kabab platter photo",
        ),
        deal(
            "spice-biryani", "spice-route", "Chicken Biryani Box",
            "A full portion of kacchi-style chicken biryani with egg and salad.",
            FoodCategory.MEALS, 380, 160, 10, 7, now + 20 * MIN, now + 150 * MIN, "biryani photo",
        ),
        deal(
            "mithai-box", "mithai-ghor", "Assorted Mishti Box",
            "Half a kilo of rasgulla, chomchom and sandesh made today.",
            FoodCategory.SWEETS, 550, 220, 6, 3, now - 25 * MIN, now + 170 * MIN, "sweets box photo",
        ),
        deal(
            "mithai-cake", "mithai-ghor", "Celebration Cake (1 kg)",
            "A whole vanilla or chocolate cake left from today's orders.",
            FoodCategory.SWEETS, 900, 350, 2, 2, now - 25 * MIN, now + 170 * MIN, "cake photo",
        ),
        deal(
            "bread-loaves", "bread-beyond", "Day-old Loaves",
            "Three loaves of sourdough, brown or milk bread.",
            FoodCategory.BAKERY, 180, 60, 9, 6, now - 40 * MIN, now + 110 * MIN, "bread photo",
        ),
        deal(
            "dairy-pack", "dairy-corner", "Yogurt & Cheese Pack",
            "Mishti doi, plain yogurt and a block of cottage cheese near their best-before date.",
            FoodCategory.DAIRY, 240, 95, 5, 5, now, now + 200 * MIN, "dairy pack photo",
        ),
        deal(
            "star-paratha", "star-kabab", "Breakfast Paratha Set",
            "Paratha, dal and egg for two — prepared fresh for tomorrow morning's leftovers run.",
            FoodCategory.MEALS, 200, 80, 8, 8, tomorrowAt(now, 8), tomorrowAt(now, 10), "paratha photo",
        ),
        deal(
            "agora-veg", "agora-fresh", "Veggie Bundle",
            "Seasonal greens and vegetables pulled from shelves overnight.",
            FoodCategory.GROCERY, 260, 110, 6, 6, tomorrowAt(now, 9), tomorrowAt(now, 12), "vegetables photo",
        ),
    )

    fun orders(now: Long): List<Order> = listOf(
        order("o-4f29", "tasty-bag", "tasty-treat", "Surprise Bakery Bag", 1, 75, 250, "4F29",
            now - 30 * MIN, now + 38 * MIN, ME_ID, currentUser.name, now - 20 * MIN, OrderStatus.ACTIVE),
        order("o-agora", "agora-box", "agora-fresh", "Produce Rescue Box", 1, 150, 300, "7KQ3",
            now + 150 * MIN, now + 210 * MIN, ME_ID, currentUser.name, now - 50 * MIN, OrderStatus.ACTIVE),
        order("o-star-past", "star-platter", "star-kabab", "Kabab Platter", 1, 180, 450, "H2MX",
            now - DAY - 3 * HOUR, now - DAY - HOUR, ME_ID, currentUser.name, now - DAY - 4 * HOUR,
            OrderStatus.COLLECTED, collectedAt = now - DAY - 2 * HOUR),
        order("o-tasty-past", "tasty-croissant", "tasty-treat", "Croissant 6-pack", 1, 90, 300, "P8RT",
            now - 3 * DAY - 2 * HOUR, now - 3 * DAY, ME_ID, currentUser.name, now - 3 * DAY - 3 * HOUR,
            OrderStatus.COLLECTED, collectedAt = now - 3 * DAY - HOUR, reviewed = true),
        // Other customers' orders, visible in the Tasty Treat pickup queue.
        order("o-9b14", "tasty-croissant", "tasty-treat", "Croissant 6-pack", 1, 90, 300, "9B14",
            now - 60 * MIN, now + 60 * MIN, "karim", "Karim Hossain", now - 35 * MIN, OrderStatus.ACTIVE),
        order("o-2c77", "tasty-sandwich", "tasty-treat", "Sandwich Combo", 2, 120, 300, "2C77",
            now - 45 * MIN, now + 15 * MIN, "sadia", "Sadia Mahmud", now - 25 * MIN, OrderStatus.ACTIVE),
        order("o-rafi", "tasty-bag", "tasty-treat", "Surprise Bakery Bag", 1, 75, 250, "M5ZD",
            now - 30 * MIN, now + 42 * MIN, "rafi", "Rafi Karim", now - 90 * MIN, OrderStatus.COLLECTED,
            collectedAt = now - 10 * MIN),
    )

    private fun order(
        id: String, dealId: String, shopId: String, title: String, qty: Int, unit: Int, original: Int,
        code: String, start: Long, end: Long, customerId: String, customerName: String, created: Long,
        status: OrderStatus, collectedAt: Long? = null, reviewed: Boolean = false,
    ): Order {
        val s = shop(shopId)
        val step = 5 * MIN
        return Order(
            id = id, dealId = dealId, shopId = shopId, shopName = s.name, area = s.area, itemTitle = title,
            quantity = qty, unitPrice = unit, originalUnitPrice = original, serviceFee = SERVICE_FEE,
            paymentMethod = PaymentMethod.BKASH, pickupCode = code, pickupStartMillis = start / step * step,
            pickupEndMillis = (end + step - 1) / step * step, customerId = customerId, customerName = customerName,
            createdAtMillis = created, status = status, collectedAtMillis = collectedAt, reviewed = reviewed,
        )
    }

    fun reviews(now: Long): List<Review> = listOf(
        Review("r1", "tasty-treat", "Ayesha R.", 5,
            "Amazing value — bag was full of fresh croissants. Pickup took 30 seconds!",
            listOf("Fresh", "Easy pickup"), now - 3 * DAY),
        Review("r2", "tasty-treat", "Karim H.", 4,
            "Good deal, saved a lot. Wish there were more veg options in the box.",
            listOf("Generous portion"), now - 4 * DAY),
        Review("r3", "tasty-treat", "Nusrat J.", 5,
            "Staff were lovely and the bread was still warm.",
            listOf("Friendly staff", "Fresh"), now - 6 * DAY, reply = "Thank you Nusrat, see you soon!"),
        Review("r4", "tasty-treat", "Tanvir A.", 4,
            "Great pastries, though the queue was a bit long at 7pm.",
            listOf("Generous portion"), now - 9 * DAY),
    )

    /** Star counts behind Tasty Treat's 4.8 from 312 reviews (1★..5★). */
    val tastyTreatDistribution = listOf(2, 4, 8, 36, 262)

    fun notifications(now: Long): List<AppNotification> = listOf(
        AppNotification("n1", UserRole.SHOPPER, NotificationType.DEAL_ENDING, "Deal ending soon!",
            "Tasty Treat's Bakery Bag expires in 40 min — 3 left.", now - 2 * MIN, targetId = "tasty-bag"),
        AppNotification("n2", UserRole.SHOPPER, NotificationType.ORDER_UPDATE, "Reservation confirmed",
            "Code 4F29 · pick up by 8:00 pm at Banani.", now - HOUR, targetId = "o-4f29"),
        AppNotification("n3", UserRole.SHOPPER, NotificationType.NEW_FROM_FAVORITE, "New from a favorite",
            "Agora Fresh posted a Produce Rescue Box.", now - 3 * HOUR, targetId = "agora-box"),
        AppNotification("n4", UserRole.SHOPPER, NotificationType.REVIEW_PROMPT, "Rate your last pickup",
            "How was the Kabab Platter from Star Kabab?", now - DAY, read = true, targetId = "o-star-past"),
        AppNotification("n5", UserRole.SHOPPER, NotificationType.WEEKLY_RECAP, "Weekly impact recap",
            "You saved ৳620 and 3 meals this week 🌱", now - 2 * DAY, read = true),
        AppNotification("b1", UserRole.BUSINESS, NotificationType.NEW_ORDER, "New order",
            "Sadia M. reserved 2× Sandwich Combo · code 2C77.", now - 25 * MIN, targetId = "o-2c77"),
        AppNotification("b2", UserRole.BUSINESS, NotificationType.DEAL_ENDING, "Listing ending soon",
            "Sandwich Combo closes in 15 min with 1 left.", now - 5 * MIN, targetId = "tasty-sandwich"),
        AppNotification("b3", UserRole.BUSINESS, NotificationType.NEW_REVIEW, "New 5★ review",
            "Ayesha R.: \"Amazing value — bag was full of fresh croissants.\"", now - 3 * DAY, read = true,
            targetId = "r1"),
    )

    val branches = listOf(
        Branch("banani", "Banani", liveListings = 4, acceptingOrders = true),
        Branch("gulshan-1", "Gulshan 1", liveListings = 2, acceptingOrders = true),
        Branch("dhanmondi-27", "Dhanmondi 27", liveListings = 0, acceptingOrders = false),
    )

    val analytics = AnalyticsSummary(
        revenue30d = 68_200,
        revenueChangePct = 18,
        wasteCutKg = 312,
        wasteChangePct = 9,
        weeklyRevenue = listOf(9_800, 12_400, 11_600, 15_200, 14_300, 18_900, 17_500, 23_800),
        topSellers = listOf(TopSeller("Bakery Bag", 142), TopSeller("Croissant 6pk", 98), TopSeller("Sandwich", 61)),
    )

    /** Lifetime impact before this session, so the dashboard isn't empty on first launch. */
    const val BASE_MONEY_SAVED = 4_280
    const val BASE_RESCUES = 23
    const val BASE_FOOD_KG = 18
    const val BASE_CO2_KG = 46
    /** Savings for the last five months, oldest first; the newest month grows as you rescue. */
    val baseMonthlySavings = listOf(420, 760, 640, 1_050, 1_410)

    /** Shown on the business dashboard before any orders arrive this session. */
    const val BASE_REVENUE_TODAY = 3_450
}
