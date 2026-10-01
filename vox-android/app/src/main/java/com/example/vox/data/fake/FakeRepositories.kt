package com.example.vox.data.fake

import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.GroupExpense
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Receipt
import com.example.vox.domain.model.RecurringPayment
import com.example.vox.domain.model.Settlement
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.model.UserProfile
import com.example.vox.domain.model.VoiceResult
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.ReceiptRepository
import com.example.vox.domain.repository.RecurringPaymentRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.UserRepository
import com.example.vox.domain.repository.VoiceAssistant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class FakeTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow(FakeSeedData.transactions())

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactions.map { list -> list.sortedByDescending { it.timestamp } }

    override fun observeTransaction(id: String): Flow<Transaction?> =
        transactions.map { list -> list.find { it.id == id } }

    override suspend fun upsert(transaction: Transaction) =
        transactions.update { list -> list.filterNot { it.id == transaction.id } + transaction }

    override suspend fun delete(id: String) = transactions.update { list -> list.filterNot { it.id == id } }

    override suspend fun categorize(id: String, category: Category) =
        transactions.update { list -> list.map { if (it.id == id) it.copy(category = category) else it } }
}

class FakeGroupRepository : GroupRepository {
    private val groups = MutableStateFlow(FakeSeedData.groups())

    override fun observeGroups(): Flow<List<Group>> = groups

    override fun observeGroup(id: String): Flow<Group?> = groups.map { list -> list.find { it.id == id } }

    override fun observeContacts() = MutableStateFlow(FakeSeedData.contacts)

    override suspend fun createGroup(name: String, type: GroupType, memberIds: List<String>): String {
        val id = UUID.randomUUID().toString()
        val members = listOf(FakeSeedData.me) + FakeSeedData.contacts.filter { it.id in memberIds }
        groups.update { it + Group(id, name, type, "INR", members) }
        return id
    }

    override suspend fun addExpense(groupId: String, expense: GroupExpense) = updateGroup(groupId) {
        it.copy(expenses = it.expenses + expense)
    }

    override suspend fun settleUp(groupId: String) = updateGroup(groupId) { group ->
        val settlements = group.otherMembers.mapNotNull { member ->
            val owed = group.balanceWith(member.id).amountMinor
            when {
                owed > 0 -> Settlement(member.id, CURRENT_USER_ID, owed)
                owed < 0 -> Settlement(CURRENT_USER_ID, member.id, -owed)
                else -> null
            }
        }
        group.copy(settlements = group.settlements + settlements)
    }

    override suspend fun deleteGroup(groupId: String) = groups.update { list -> list.filterNot { it.id == groupId } }

    private fun updateGroup(id: String, transform: (Group) -> Group) =
        groups.update { list -> list.map { if (it.id == id) transform(it) else it } }
}

class FakeRecurringPaymentRepository : RecurringPaymentRepository {
    private val payments = MutableStateFlow(FakeSeedData.recurringPayments())

    override fun observePayments(): Flow<List<RecurringPayment>> =
        payments.map { list -> list.sortedBy { it.nextDueDate } }

    override suspend fun upsert(payment: RecurringPayment) =
        payments.update { list -> list.filterNot { it.id == payment.id } + payment }
}

/** Simulates OCR: every scan returns the same sample receipt. */
class FakeReceiptRepository : ReceiptRepository {
    private val receipts = MutableStateFlow<Map<String, Receipt>>(emptyMap())

    override suspend fun scan(imageUri: String?): Receipt {
        delay(800)
        val receipt = FakeSeedData.receipt(UUID.randomUUID().toString())
        receipts.update { it + (receipt.id to receipt) }
        return receipt
    }

    override fun observeReceipt(id: String): Flow<Receipt?> = receipts.map { it[id] }
}

class FakeUserRepository : UserRepository {
    private val profile = MutableStateFlow<UserProfile?>(null)

    override fun observeProfile(): Flow<UserProfile?> = profile

    override suspend fun saveProfile(profile: UserProfile) {
        this.profile.value = profile
    }

    override suspend fun signOut() {
        profile.value = null
    }
}

/** Simulates speech recognition: always "hears" the same sentence after a short pause. */
class FakeVoiceAssistant : VoiceAssistant {
    override suspend fun listen(): VoiceResult {
        delay(2_500)
        return VoiceResult(
            transcript = "Spent ₹400 on groceries",
            type = TransactionType.EXPENSE,
            amount = FakeSeedData.inr(400),
            title = "Groceries",
            category = Category.GROCERIES,
        )
    }
}
