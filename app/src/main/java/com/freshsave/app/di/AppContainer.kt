package com.freshsave.app.di

import android.content.Context
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.FakeBusinessRepository
import com.freshsave.app.data.fake.FakeDealRepository
import com.freshsave.app.data.fake.FakeImpactRepository
import com.freshsave.app.data.fake.FakeNotificationRepository
import com.freshsave.app.data.fake.FakeOrderRepository
import com.freshsave.app.data.fake.FakeReviewRepository
import com.freshsave.app.data.fake.FakeShopRepository
import com.freshsave.app.data.fake.FakeUserRepository
import com.freshsave.app.data.local.DataStoreSessionRepository
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.ImpactRepository
import com.freshsave.app.data.repository.NotificationRepository
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.data.repository.ReviewRepository
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.data.repository.ShopRepository
import com.freshsave.app.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection: this class builds every repository exactly once and hands
 * them to ViewModels. To move to a real backend, change the right-hand side of these lines
 * (e.g. `FirebaseDealRepository(...)`) and nothing else in the app has to know.
 */
class AppContainer(context: Context) {
    /** Lives as long as the app process; used for background work like the clock tick. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock = AppClock(applicationScope)

    val sessionRepository: SessionRepository = DataStoreSessionRepository(context)
    val userRepository: UserRepository = FakeUserRepository()
    val shopRepository: ShopRepository = FakeShopRepository()
    val dealRepository: DealRepository = FakeDealRepository(clock)
    val notificationRepository: NotificationRepository = FakeNotificationRepository(clock)
    val orderRepository: OrderRepository = FakeOrderRepository(clock, dealRepository, notificationRepository)
    val reviewRepository: ReviewRepository = FakeReviewRepository(clock, notificationRepository)
    val impactRepository: ImpactRepository = FakeImpactRepository(clock, orderRepository)
    val businessRepository: BusinessRepository = FakeBusinessRepository()
}
