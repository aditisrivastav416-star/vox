package com.example.vox.widget

import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.model.VoiceInteraction
import com.example.vox.domain.model.VoiceResult
import com.example.vox.domain.repository.BudgetRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.VoiceAssistant
import com.example.vox.feature.vox.VoxAction
import com.example.vox.feature.vox.VoxUiState
import com.example.vox.feature.vox.VoxViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceCaptureTest {
    private val result = VoiceResult("Spent 400 rupees on groceries", TransactionType.EXPENSE,
        Money(40_000, "INR"), "Groceries", Category.GROCERIES)
    private var interaction: VoiceInteraction = VoiceInteraction.TransactionCommand(result)
    private var keepListening = false
    private var microphoneReleased = false
    private val transactions = FakeTransactions()
    private val voice = object : VoiceAssistant {
        override suspend fun listen(languageCode: String): VoiceInteraction {
            try {
                if (keepListening) awaitCancellation()
                return interaction
            } finally {
                microphoneReleased = true
            }
        }
        override suspend fun speak(text: String, languageCode: String) = error("Widget must not speak answers")
    }
    private val budgets = object : BudgetRepository {
        override fun observeBudgets() = flowOf(emptyMap<Pair<String, String>, Long>())
        override suspend fun setBudget(month: String, currencyCode: String, amountMinor: Long) = error("Widget must not change budgets")
    }
    private val model = VoxViewModel(voice, transactions, budgets, transactionsOnly = true)
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @After fun cleanUp() = onMain { model.viewModelScope.cancel() }

    @Test fun savesOnlyAfterConfirmationAndIgnoresRepeatedTaps() = onMain {
        model.onAction(VoxAction.StartListening)
        assertEquals(VoxUiState.Recognized(result), model.state.value)
        assertTrue(transactions.saved.isEmpty())
        transactions.gate = CompletableDeferred()
        model.onAction(VoxAction.Confirm)
        model.onAction(VoxAction.Confirm)
        model.onAction(VoxAction.StartListening)
        assertEquals(1, transactions.attempts.size)
        assertTrue((model.state.value as VoxUiState.Recognized).isSaving)
        transactions.gate!!.complete(Unit)
        assertEquals(1, transactions.saved.size)
        assertEquals(40_000L, transactions.saved.single().amount.amountMinor)
        assertEquals(TransactionSource.VOICE, transactions.saved.single().source)
        model.onAction(VoxAction.Confirm)
        assertEquals(1, transactions.saved.size)
    }

    @Test fun failedSaveKeepsTheDraftAndRetriesTheSameTransaction() = onMain {
        model.onAction(VoxAction.StartListening)
        transactions.fail = true
        model.onAction(VoxAction.Confirm)
        val failed = model.state.value as VoxUiState.Recognized
        assertEquals(result, failed.result)
        assertFalse(failed.isSaving)
        assertEquals("Storage unavailable", failed.error)
        transactions.fail = false
        model.onAction(VoxAction.Confirm)
        assertEquals(2, transactions.attempts.size)
        assertEquals(transactions.attempts[0].id, transactions.attempts[1].id)
        assertEquals(1, transactions.saved.size)
    }

    @Test fun cancelReleasesTheMicrophoneWithoutSaving() = onMain {
        keepListening = true
        model.onAction(VoxAction.StartListening)
        assertEquals(VoxUiState.Listening, model.state.value)
        model.onAction(VoxAction.CancelListening)
        assertEquals(VoxUiState.Idle, model.state.value)
        assertTrue(microphoneReleased)
        assertTrue(transactions.saved.isEmpty())
    }

    @Test fun widgetRejectsBudgetAndSpendingCommandsWithoutSideEffects() = onMain {
        listOf(VoiceInteraction.SetBudget("Set budget to 4000", 400_000),
            VoiceInteraction.SpendingQuestion("How much did I spend?")).forEach {
            interaction = it
            model.onAction(VoxAction.StartListening)
            assertTrue((model.state.value as VoxUiState.Error).message.startsWith("Say a transaction"))
            model.onAction(VoxAction.Confirm)
            assertTrue(transactions.saved.isEmpty())
        }
    }

    private class FakeTransactions : TransactionRepository {
        val attempts = mutableListOf<Transaction>()
        val saved = mutableListOf<Transaction>()
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        override fun observeTransactions() = flowOf(saved.toList())
        override fun observeTransaction(id: String) = flowOf(saved.find { it.id == id })
        override suspend fun upsert(transaction: Transaction) {
            attempts += transaction
            gate?.await()
            check(!fail) { "Storage unavailable" }
            saved += transaction
        }
        override suspend fun delete(id: String) = error("Unexpected delete")
        override suspend fun categorize(id: String, category: Category) = error("Unexpected categorization")
    }
}
