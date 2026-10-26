package com.example.vox.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.vox.domain.model.*
import com.example.vox.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.stringOrNull(key: String) = if (isNull(key)) null else optString(key).ifBlank { null }

class DeviceStore(context: Context) {
    private val prefs = context.getSharedPreferences("vox_device", Context.MODE_PRIVATE)
    fun list(key: String): List<JSONObject> = JSONArray(prefs.getString(key, "[]")).objects()
    fun save(key: String, values: List<JSONObject>) {
        prefs.edit().putString(key, JSONArray(values).toString()).apply()
    }
    fun get(key: String) = prefs.getString(key, null)
    fun put(key: String, value: String?) { prefs.edit().putString(key, value).apply() }
}

class LocalTransactionRepository(private val store: DeviceStore) : TransactionRepository {
    private val rows = MutableStateFlow(store.list("transactions").map { it.toTransaction() })
    override fun observeTransactions(): Flow<List<Transaction>> = rows
    override fun observeTransaction(id: String): Flow<Transaction?> = rows.map { items -> items.find { it.id == id } }
    override suspend fun upsert(transaction: Transaction) { update(rows.value.filterNot { it.id == transaction.id } + transaction) }
    override suspend fun delete(id: String) { update(rows.value.filterNot { it.id == id }) }
    override suspend fun categorize(id: String, category: Category) {
        rows.value.find { it.id == id }?.let { upsert(it.copy(category = category)) }
    }
    private fun update(values: List<Transaction>) {
        rows.value = values.sortedByDescending { it.timestamp }
        store.save("transactions", rows.value.map { it.toJson() })
    }
}

private fun Transaction.toJson() = JSONObject().apply {
    put("id", id); put("type", type.name); put("amountMinor", amount.amountMinor.toString())
    put("currencyCode", amount.currencyCode); put("title", title); put("category", category?.name)
    put("paymentMethod", paymentMethod.name); put("timestamp", timestamp.toString())
    put("source", source.name); put("groupId", groupId); put("notes", notes)
}
private fun JSONObject.toTransaction() = Transaction(
    getString("id"), TransactionType.valueOf(getString("type")),
    Money(getString("amountMinor").toLong(), getString("currencyCode")), getString("title"),
    stringOrNull("category")?.let(Category::valueOf), PaymentMethod.valueOf(getString("paymentMethod")),
    Instant.parse(getString("timestamp")), TransactionSource.valueOf(getString("source")),
    stringOrNull("groupId"), stringOrNull("notes"),
)

class LocalUserRepository(private val store: DeviceStore) : UserRepository {
    private val profile = MutableStateFlow(store.get("profile")?.let { JSONObject(it).let { p ->
        UserProfile(p.getString("name"), p.getString("currencyCode"), p.stringOrNull("phone"))
    } })
    override fun observeProfile(): Flow<UserProfile?> = profile
    override suspend fun saveProfile(profile: UserProfile) {
        this.profile.value = profile
        store.put("profile", JSONObject().put("name", profile.name).put("currencyCode", profile.currencyCode).put("phone", profile.phone).toString())
    }
    override suspend fun signOut() { profile.value = null; store.put("profile", null) }
    override suspend fun login(identifier: String, password: String) { error("Use device profile setup for this demo") }
    override suspend fun startSignup(identifier: String) { error("Use device profile setup for this demo") }
    override suspend fun verifySignup(identifier: String, code: String) { error("Use device profile setup for this demo") }
    override suspend fun finishSignup(identifier: String, code: String, password: String, confirmPassword: String) { error("Use device profile setup for this demo") }
    fun hasProfile() = profile.value != null
}

class LocalBudgetRepository(private val store: DeviceStore) : BudgetRepository {
    private val values = MutableStateFlow(store.list("budgets").associate {
        (it.getString("month") to it.getString("currencyCode")) to it.getString("amountMinor").toLong()
    })
    override fun observeBudgets(): Flow<Map<Pair<String, String>, Long>> = values
    override suspend fun setBudget(month: String, currencyCode: String, amountMinor: Long) {
        values.value = values.value + ((month to currencyCode) to amountMinor)
        store.save("budgets", values.value.map { (key, amount) ->
            JSONObject().put("month", key.first).put("currencyCode", key.second).put("amountMinor", amount.toString())
        })
    }
}

class LocalRecurringRepository(private val store: DeviceStore) : RecurringPaymentRepository {
    private val rows = MutableStateFlow(store.list("recurring").map { r ->
        RecurringPayment(r.getString("id"), r.getString("name"), Category.valueOf(r.getString("category")),
            r.stringOrNull("amountMinor")?.let { Money(it.toLong(), r.getString("currencyCode")) },
            Frequency.valueOf(r.getString("frequency")), if (r.isNull("customIntervalDays")) null else r.getInt("customIntervalDays"),
            LocalDate.parse(r.getString("nextDueDate")), r.getBoolean("remindDaysBefore"), r.getBoolean("remindOnDueDate"))
    })
    override fun observePayments(): Flow<List<RecurringPayment>> = rows
    override suspend fun upsert(payment: RecurringPayment) {
        rows.value = rows.value.filterNot { it.id == payment.id } + payment
        store.save("recurring", rows.value.map {
            JSONObject().put("id", it.id).put("name", it.name).put("category", it.category.name)
                .put("amountMinor", it.amount?.amountMinor?.toString()).put("currencyCode", it.amount?.currencyCode ?: "INR")
                .put("frequency", it.frequency.name).put("customIntervalDays", it.customIntervalDays)
                .put("nextDueDate", it.nextDueDate.toString()).put("remindDaysBefore", it.remindDaysBefore)
                .put("remindOnDueDate", it.remindOnDueDate)
        })
    }
}

class LocalGroupRepository(private val context: Context, private val store: DeviceStore) : GroupRepository {
    private val groups = MutableStateFlow(store.list("groups").map { it.toGroup() })
    private val contacts = MutableStateFlow<List<Person>>(emptyList())
    override fun observeGroups(): Flow<List<Group>> = groups
    override fun observeGroup(id: String): Flow<Group?> = groups.map { it.find { group -> group.id == id } }
    override fun observeContacts(): Flow<List<Person>> = contacts
    override suspend fun syncContacts() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return
        val people = mutableListOf<Person>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty()
                val phone = cursor.getString(1).orEmpty()
                if (name.isNotBlank()) people += Person("guest:$phone", name, phone, false)
            }
        }
        contacts.value = people.distinctBy { it.id }
    }
    override suspend fun createGroup(name: String, type: GroupType, members: List<Person>): String {
        val id = UUID.randomUUID().toString()
        save(groups.value + Group(id, name, type, "INR", listOf(Person(CURRENT_USER_ID, "Me")) + members))
        return id
    }
    override suspend fun addExpense(groupId: String, expense: GroupExpense) {
        save(groups.value.map { if (it.id == groupId) it.copy(expenses = it.expenses + expense) else it })
    }
    override suspend fun settleUp(groupId: String) {
        save(groups.value.map { group ->
            if (group.id != groupId) group else group.copy(settlements = group.settlements + group.otherMembers.mapNotNull { member ->
                val balance = group.balanceWith(member.id).amountMinor
                if (balance == 0L) null else Settlement(
                    if (balance > 0) member.id else CURRENT_USER_ID,
                    if (balance > 0) CURRENT_USER_ID else member.id, kotlin.math.abs(balance))
            })
        })
    }
    override suspend fun deleteGroup(groupId: String) { save(groups.value.filterNot { it.id == groupId }) }
    private fun save(values: List<Group>) { groups.value = values; store.save("groups", values.map { it.toJson() }) }
}

private fun Group.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name); put("currencyCode", currencyCode)
    put("members", JSONArray(members.map { JSONObject().put("id", it.id).put("name", it.name).put("contact", it.contact).put("isOnVox", it.isOnVox) }))
    put("expenses", JSONArray(expenses.map { e -> JSONObject().put("id", e.id).put("description", e.description)
        .put("amountMinor", e.total.amountMinor.toString()).put("paidById", e.paidById)
        .put("shares", JSONObject(e.shares.mapValues { it.value.toString() })).put("timestamp", e.timestamp.toString()) }))
    put("settlements", JSONArray(settlements.map { JSONObject().put("fromId", it.fromId).put("toId", it.toId).put("amountMinor", it.amountMinor.toString()) }))
}
private fun JSONObject.toGroup(): Group {
    val currency = getString("currencyCode")
    return Group(getString("id"), getString("name"), GroupType.valueOf(getString("type")), currency,
        getJSONArray("members").objects().map { Person(it.getString("id"), it.getString("name"), it.stringOrNull("contact"), it.getBoolean("isOnVox")) },
        getJSONArray("expenses").objects().map { e ->
            val shares = e.getJSONObject("shares")
            GroupExpense(e.getString("id"), e.getString("description"), Money(e.getString("amountMinor").toLong(), currency),
                e.getString("paidById"), shares.keys().asSequence().associateWith { shares.getString(it).toLong() },
                Instant.parse(e.getString("timestamp")))
        },
        getJSONArray("settlements").objects().map { Settlement(it.getString("fromId"), it.getString("toId"), it.getString("amountMinor").toLong()) })
}
