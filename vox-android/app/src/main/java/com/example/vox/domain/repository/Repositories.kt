package com.example.vox.domain.repository

import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.GroupExpense
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Person
import com.example.vox.domain.model.Receipt
import com.example.vox.domain.model.RecurringPayment
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.UserProfile
import com.example.vox.domain.model.VoiceResult
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: String): Flow<Transaction?>
    suspend fun upsert(transaction: Transaction)
    suspend fun delete(id: String)
    suspend fun categorize(id: String, category: Category)
}

interface GroupRepository {
    fun observeGroups(): Flow<List<Group>>
    fun observeGroup(id: String): Flow<Group?>
    fun observeContacts(): Flow<List<Person>>
    suspend fun syncContacts()
    /** Returns the new group's id. */
    suspend fun createGroup(name: String, type: GroupType, members: List<Person>): String
    suspend fun addExpense(groupId: String, expense: GroupExpense)
    suspend fun settleUp(groupId: String)
    suspend fun deleteGroup(groupId: String)
}

interface RecurringPaymentRepository {
    fun observePayments(): Flow<List<RecurringPayment>>
    suspend fun upsert(payment: RecurringPayment)
}

interface ReceiptRepository {
    /** Scans the current camera frame or a picked image; returns the stored receipt. */
    suspend fun scan(imageUri: String?): Receipt
    fun observeReceipt(id: String): Flow<Receipt?>
}

interface UserRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun signOut()
    suspend fun login(identifier: String, password: String)
    suspend fun startSignup(identifier: String)
    suspend fun verifySignup(identifier: String, code: String)
    suspend fun finishSignup(identifier: String, code: String, password: String, confirmPassword: String)
}

interface BudgetRepository {
    fun observeBudgets(): Flow<Map<Pair<String, String>, Long>>
    suspend fun setBudget(month: String, currencyCode: String, amountMinor: Long)
}

interface VoiceAssistant {
    /** Listens for one utterance and interprets it. */
    suspend fun listen(): VoiceResult
}
