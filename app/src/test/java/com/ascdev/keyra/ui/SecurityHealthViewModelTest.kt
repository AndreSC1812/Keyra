package com.ascdev.keyra.ui

import app.cash.turbine.test
import com.ascdev.keyra.domain.audit.PasswordAuditor
import com.ascdev.keyra.domain.entry
import com.ascdev.keyra.domain.strength.PasswordStrengthCalculator
import com.ascdev.keyra.ui.health.SecurityHealthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityHealthViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `report updates when entries change`() = runTest {
        val repository = FakeEntryRepository(listOf(entry(1), entry(2, password = "123456")))
        val viewModel = SecurityHealthViewModel(repository, PasswordAuditor(PasswordStrengthCalculator()))

        viewModel.state.test {
            val first = awaitItem().takeIf { !it.isLoading } ?: awaitItem()
            assertEquals(2, first.total)
            assertEquals(1, first.report.weak.size)
            assertEquals(1, first.healthy)

            repository.delete(2)
            val second = awaitItem()
            assertEquals(1, second.total)
            assertEquals(0, second.report.issueCount)
        }
    }
}
