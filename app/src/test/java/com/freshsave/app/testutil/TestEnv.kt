package com.freshsave.app.testutil

import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.FakeBusinessRepository
import com.freshsave.app.data.fake.FakeDealRepository
import com.freshsave.app.data.fake.FakeNotificationRepository
import com.freshsave.app.data.fake.FakeOrderRepository
import com.freshsave.app.data.fake.FakeReviewRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * The same fake repositories the app uses, wired together like AppContainer does
 * (minus DataStore, which needs an Android Context).
 *
 * The clock ticks on a real background thread so its endless loop never blocks
 * the test's virtual time. Call [close] when done.
 */
class TestEnv {
    private val clockScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock = AppClock(clockScope)
    val deals = FakeDealRepository(clock)
    val notifications = FakeNotificationRepository(clock)
    val orders = FakeOrderRepository(clock, deals, notifications)
    val reviews = FakeReviewRepository(clock, notifications)
    val business = FakeBusinessRepository()

    fun close() = clockScope.cancel()
}

/**
 * ViewModels launch work on Dispatchers.Main, which doesn't exist in plain JVM tests.
 * This rule swaps in a test dispatcher for each test and restores it afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
